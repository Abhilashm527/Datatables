package com.dataflow.DataTable.service;

import com.dataflow.DataTable.config.GeminiClient;
import com.dataflow.DataTable.model.AiConversation;
import com.dataflow.DataTable.model.AiConversation.ConversationMessage;
import com.dataflow.DataTable.model.DataTableSchema;
import com.dataflow.DataTable.repository.AiConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MemoryAwareAiService {

  private final GeminiClient geminiClient;
  private final DataTableService dataTableService;
  private final AiTrainingDataService trainingDataService;
  private final AiConversationRepository conversationRepository;
  private final ObjectMapper objectMapper;

  public MemoryAwareAiService(
      GeminiClient geminiClient,
      DataTableService dataTableService,
      AiTrainingDataService trainingDataService,
      AiConversationRepository conversationRepository,
      ObjectMapper objectMapper) {
    this.geminiClient = geminiClient;
    this.dataTableService = dataTableService;
    this.trainingDataService = trainingDataService;
    this.conversationRepository = conversationRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Process a prompt with full context awareness and memory
   */
  public Map<String, Object> processWithMemory(String userPrompt, String sessionId, String userId) {
    try {
      // Get or create conversation (database record for persistence)
      AiConversation conversation = getOrCreateConversation(sessionId, userId);

      // Add user message to oudr persistent DB
      ConversationMessage userMessage = new ConversationMessage();
      userMessage.setSequence(conversation.getMessages().size() + 1);
      userMessage.setRole("user");
      userMessage.setContent(userPrompt);
      conversation.getMessages().add(userMessage);

      // Build context (Database schema, etc.) - We still need this as system
      // instruction
      String databaseContext = trainingDataService.buildDatabaseContext();
      String systemPrompt = buildMemoryAwareSystemPrompt(databaseContext, ""); // Static prompt

      // Get response using the Client's internal multi-turn memory
      Map<String, Object> geminiResponse = geminiClient.chat(sessionId, systemPrompt, userPrompt);
      String aiResponse = (String) geminiResponse.get("text");
      String responseId = (String) geminiResponse.get("responseId");

      // Parse the JSON action from AI
      Map<String, Object> parsedResponse = parseAiResponse(aiResponse);
      String action = (String) parsedResponse.get("action");
      @SuppressWarnings("unchecked")
      Map<String, Object> parameters = (Map<String, Object>) parsedResponse.get("parameters");

      // Execute action
      Object result = executeAction(action, parameters);

      // SECOND PASS: Generate a user-friendly formatted response
      String finalResponse = generateFinalResponse(userPrompt, (String) parsedResponse.get("explanation"), result);

      // Add assistant message to persistent DB
      ConversationMessage assistantMessage = new ConversationMessage();
      assistantMessage.setSequence(conversation.getMessages().size() + 1);
      assistantMessage.setRole("assistant");
      assistantMessage.setContent(finalResponse);
      assistantMessage.setAction(action);
      assistantMessage.setResult(result instanceof Map ? (Map<String, Object>) result : Map.of("data", result));

      // Store Gemini's responseId in metadata for reference
      if (responseId != null) {
        if (conversation.getMetadata() == null)
          conversation.setMetadata(new HashMap<>());
        conversation.getMetadata().put("lastResponseId", responseId);
      }

      conversation.getMessages().add(assistantMessage);

      // Update and save DB conversation
      conversation.setLastUpdatedAt(LocalDateTime.now());
      if (conversation.getContext() == null) {
        conversation.setContext(new HashMap<>());
      }
      conversation.getContext().put("lastAction", action);
      conversation.getContext().put("lastResult", result);

      conversationRepository.save(conversation);

      return Map.of(
          "success", true,
          "sessionId", sessionId,
          "responseId", responseId != null ? responseId : "N/A",
          "action", action,
          "result", result,
          "explanation", finalResponse,
          "conversationLength", conversation.getMessages().size());

    } catch (Exception e) {
      e.printStackTrace();
      return Map.of("success", false, "error", e.getMessage(), "sessionId", sessionId);
    }
  }

  private String generateFinalResponse(String userPrompt, String aiExplanation, Object result) {
    try {
      String resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
      String formattingPrompt = String.format(
          """
              You are a helpful data assistant. Your job is to format the final response for the user based on the internal data retrieved.

              USER ORIGINAL REQUEST: %s
              WHAT I DID: %s
              TECHNICAL RESULT DATA (JSON):
              %s

              INSTRUCTIONS:
              1. If the result contains a list of records (e.g. from SEARCH or LIST), present them in a clean Markdown Table.
              2. If the result is a confirmation of success (like "Table created"), provide a friendly confirmation message.
              3. If there's an error in the data, explain it simply.
              4. Match the tone and language of the user's original request.
              5. Do NOT include internal MongoDB IDs ($oid) or internal session IDs unless they are specifically part of what the user asked for.
              6. Focus on being concise and helpful.

              FORMATTED RESPONSE:
              """,
          userPrompt, aiExplanation, resultJson);

      return geminiClient.generateText(formattingPrompt);
    } catch (Exception e) {
      // Fallback to original explanation if formatting fails
      return aiExplanation + " (Direct Data: " + result.toString() + ")";
    }
  }

  /**
   * Get conversation history for a session
   */
  public Map<String, Object> getConversationHistory(String sessionId) {
    Optional<AiConversation> conversationOpt = conversationRepository.findBySessionIdAndActiveTrue(sessionId);

    if (conversationOpt.isEmpty()) {
      return Map.of(
          "success", false,
          "error", "No active conversation found for session: " + sessionId);
    }

    AiConversation conversation = conversationOpt.get();

    // Generate a technical log/transcript of what happened
    List<String> transcript = new ArrayList<>();
    for (ConversationMessage msg : conversation.getMessages()) {
      String entry = String.format("[%s]: %s", msg.getRole(), msg.getContent());
      if (msg.getAction() != null) {
        entry += String.format(" | ACTION: %s", msg.getAction());
        if (msg.getResult() != null) {
          entry += " | DATA RETRIEVED: YES";
        }
      }
      transcript.add(entry);
    }

    return Map.of(
        "success", true,
        "sessionId", sessionId,
        "startedAt", conversation.getStartedAt(),
        "messageCount", conversation.getMessages().size(),
        "technicalTranscript", transcript,
        "messages", conversation.getMessages(),
        "context", conversation.getContext() != null ? conversation.getContext() : Map.of());
  }

  /**
   * Clear/end a conversation session
   */
  public Map<String, Object> endConversation(String sessionId) {
    Optional<AiConversation> conversationOpt = conversationRepository.findBySessionIdAndActiveTrue(sessionId);

    if (conversationOpt.isEmpty()) {
      return Map.of("success", false, "error", "No active conversation found");
    }

    AiConversation conversation = conversationOpt.get();
    conversation.setActive(false);
    conversationRepository.save(conversation);

    return Map.of(
        "success", true,
        "message", "Conversation ended",
        "totalMessages", conversation.getMessages().size());
  }

  /**
   * Get all conversations for a user
   */
  public List<AiConversation> getUserConversations(String userId) {
    return conversationRepository.findByUserIdOrderByLastUpdatedAtDesc(userId);
  }

  private AiConversation getOrCreateConversation(String sessionId, String userId) {
    Optional<AiConversation> existing = conversationRepository.findBySessionIdAndActiveTrue(sessionId);

    if (existing.isPresent()) {
      return existing.get();
    }

    // Create new conversation
    AiConversation conversation = new AiConversation();
    conversation.setSessionId(sessionId);
    conversation.setUserId(userId);
    conversation.setContext(new HashMap<>());
    conversation.setMetadata(new HashMap<>());

    return conversationRepository.save(conversation);
  }

  private String buildMemoryAwareSystemPrompt(String databaseContext, String conversationHistory) {
    return """
        You are an intelligent AI assistant for a DataTable management system with full memory and context awareness.

        You have access to:
        1. Complete database schema and statistics
        2. Previous conversation history
        3. All available operations

        """
        + databaseContext + """

            """ + conversationHistory + """

            Available Actions:
            - CREATE_TABLE, LIST_TABLES, GET_TABLE, DELETE_TABLE
            - INSERT_RECORD, INSERT_MULTI_RECORDS, GET_RECORD, UPDATE_RECORD, DELETE_RECORD, DELETE_RECORDS
            - SEARCH_RECORDS, SEARCH_BY_FIELD
            - CREATE_INDEX, DROP_INDEX
            - GET_RECORD_COUNT

            IMPORTANT INSTRUCTIONS:
            1. Use the database context above to understand what tables and data exist
            2. Reference previous conversation messages when relevant
            3. If a table name is mentioned, use the exact name from the database context
            4. Provide helpful, context-aware responses
            5. Respond ONLY with valid JSON in this format:

            {
              "action": "ACTION_NAME",
              "parameters": {
                // action-specific parameters
              },
              "explanation": "What you're doing and why"
            }

            For table operations, use "tableName" parameter with the exact table name from the database.
            For record operations, use the appropriate IDs and data.

            NO markdown, NO code blocks, ONLY valid JSON.
            """;
  }

  private Map<String, Object> parseAiResponse(String aiResponse) throws JsonProcessingException {
    String cleanedResponse = aiResponse.trim();
    if (cleanedResponse.startsWith("```json")) {
      cleanedResponse = cleanedResponse.substring(7);
    }
    if (cleanedResponse.startsWith("```")) {
      cleanedResponse = cleanedResponse.substring(3);
    }
    if (cleanedResponse.endsWith("```")) {
      cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
    }
    cleanedResponse = cleanedResponse.trim();

    try {
      JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

      if (!jsonNode.has("action") || jsonNode.get("action") == null) {
        throw new RuntimeException("AI response missing 'action' field");
      }

      if (!jsonNode.has("parameters") || jsonNode.get("parameters") == null) {
        throw new RuntimeException("AI response missing 'parameters' field");
      }

      Map<String, Object> result = new HashMap<>();
      result.put("action", jsonNode.get("action").asText());
      result.put("parameters", objectMapper.convertValue(jsonNode.get("parameters"), Map.class));
      result.put("explanation", jsonNode.has("explanation") && jsonNode.get("explanation") != null
          ? jsonNode.get("explanation").asText()
          : "");

      return result;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse AI response: " + cleanedResponse, e);
    }
  }

  private Object executeAction(String action, Map<String, Object> parameters) {
    try {
      return switch (action) {
        case "LIST_TABLES" -> listTables();
        case "GET_TABLE" -> getTable(parameters);
        case "CREATE_TABLE" -> createTable(parameters);
        case "DELETE_TABLE" -> deleteTable(parameters);
        case "INSERT_RECORD" -> insertRecord(parameters);
        case "INSERT_MULTI_RECORDS" -> insertmultiRecords(parameters);
        case "GET_RECORD" -> getRecord(parameters);
        case "UPDATE_RECORD" -> updateRecord(parameters);
        case "DELETE_RECORD" -> deleteRecord(parameters);
        case "DELETE_RECORDS" -> deleteRecords(parameters);
        case "SEARCH_RECORDS" -> searchRecords(parameters);
        case "SEARCH_BY_FIELD" -> searchByField(parameters);
        case "CREATE_INDEX" -> createIndex(parameters);
        case "DROP_INDEX" -> dropIndex(parameters);
        case "GET_RECORD_COUNT" -> getRecordCount(parameters);
        default -> Map.of("error", "Unknown action: " + action);
      };
    } catch (Exception e) {
      return Map.of("success", false, "error", e.getMessage());
    }
  }

  private String resolveTableId(String tableNameOrId) {
    var tableById = dataTableService.getTableById(tableNameOrId);
    if (tableById.isPresent()) {
      return tableById.get().getId();
    }

    var tableByName = dataTableService.getTableByName(tableNameOrId);
    if (tableByName.isPresent()) {
      return tableByName.get().getId();
    }

    throw new RuntimeException("Table not found: " + tableNameOrId);
  }

  // Action methods (simplified versions - reuse from AiAssistantService)
  private Object listTables() {
    var tables = dataTableService.getAllTables();
    return Map.of("success", true, "count", tables.size(), "tables", tables);
  }

  private Object getTable(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    var table = dataTableService.getTableById(tableId);
    return table.map(t -> Map.of("success", true, "table", t))
        .orElse(Map.of("success", false, "error", "Table not found"));
  }

  @SuppressWarnings("unchecked")
  private Object createTable(Map<String, Object> parameters) {
    DataTableSchema schema = new DataTableSchema();
    schema.setTableName((String) parameters.get("tableName"));
    schema.setDescription((String) parameters.get("description"));

    List<Map<String, Object>> columnsData = (List<Map<String, Object>>) parameters.get("columns");
    List<DataTableSchema.ColumnDefinition> columns = new ArrayList<>();

    for (Map<String, Object> colData : columnsData) {
      DataTableSchema.ColumnDefinition column = new DataTableSchema.ColumnDefinition();
      column.setName((String) colData.get("name"));
      column.setDataType((String) colData.get("dataType"));
      column.setRequired(colData.containsKey("required") ? (Boolean) colData.get("required") : false);
      column.setUnique(colData.containsKey("unique") ? (Boolean) colData.get("unique") : false);
      column.setIndexed(colData.containsKey("indexed") ? (Boolean) colData.get("indexed") : false);
      if (colData.containsKey("defaultValue")) {
        column.setDefaultValue(colData.get("defaultValue"));
      }
      columns.add(column);
    }

    schema.setColumns(columns);
    DataTableSchema createdSchema = dataTableService.createTable(schema);

    return Map.of("success", true, "tableId", createdSchema.getId(),
        "tableName", createdSchema.getTableName(), "message", "Table created successfully");
  }

  private Object deleteTable(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    dataTableService.deleteTable(tableId);
    return Map.of("success", true, "message", "Table deleted successfully");
  }

  @SuppressWarnings("unchecked")
  private Object insertRecord(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    Map<String, Object> data = (Map<String, Object>) parameters.get("records");
    var record = dataTableService.insertRecord(tableId, data);
    return Map.of("success", true, "recordId", record.getId(), "message", "Record inserted successfully");
  }

  @SuppressWarnings("unchecked")
  private Object insertmultiRecords(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    List<Map<String, Object>> records = (List<Map<String, Object>>) parameters.get("records");
    var createdRecords = dataTableService.insertRecords(tableId, records);
    return Map.of("success", true, "count", createdRecords.size(), "message", "Records inserted successfully");
  }

  private Object getRecord(Map<String, Object> parameters) {
    String recordId = (String) parameters.get("recordId");
    var record = dataTableService.getRecord(recordId);
    return record.map(r -> Map.of("success", true, "record", r))
        .orElse(Map.of("success", false, "error", "Record not found"));
  }

  @SuppressWarnings("unchecked")
  private Object updateRecord(Map<String, Object> parameters) {
    String recordId = (String) parameters.get("recordId");
    Map<String, Object> data = (Map<String, Object>) parameters.get("data");
    var updatedRecord = dataTableService.updateRecord(recordId, data);
    return Map.of("success", true, "recordId", updatedRecord.getId(), "message", "Record updated successfully");
  }

  private Object deleteRecord(Map<String, Object> parameters) {
    String recordId = (String) parameters.get("recordId");
    dataTableService.deleteRecord(recordId);
    return Map.of("success", true, "message", "Record deleted successfully");
  }

  private Object searchRecords(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    String query = (String) parameters.get("query");
    var records = dataTableService.searchRecords(tableId, query);
    return Map.of("success", true, "count", records.size(), "records", records);
  }

  private Object searchByField(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    String field = (String) parameters.get("field");
    Object value = parameters.get("value");
    var records = dataTableService.searchRecords(tableId, field, value);
    return Map.of("success", true, "count", records.size(), "records", records);
  }

  private Object createIndex(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    String columnName = (String) parameters.get("columnName");
    boolean unique = parameters.containsKey("unique") ? (Boolean) parameters.get("unique") : false;
    String indexName = dataTableService.createIndex(tableId, columnName, unique);
    return Map.of("success", true, "indexName", indexName, "message", "Index created successfully");
  }

  private Object dropIndex(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    String columnName = (String) parameters.get("columnName");
    String indexName = dataTableService.dropIndex(tableId, columnName);
    return Map.of("success", true, "indexName", indexName, "message", "Index dropped successfully");
  }

  @SuppressWarnings("unchecked")
  private Object deleteRecords(Map<String, Object> parameters) {
    List<String> recordIds = (List<String>) parameters.get("recordIds");
    dataTableService.deleteRecords(recordIds);
    return Map.of("success", true, "deletedCount", recordIds.size(), "message", "Records deleted successfully");
  }

  private Object getRecordCount(Map<String, Object> parameters) {
    String tableName = (String) parameters.get("tableName");
    String tableId = resolveTableId(tableName);
    long count = dataTableService.getRecordCount(tableId);
    return Map.of("success", true, "count", count);
  }
}
