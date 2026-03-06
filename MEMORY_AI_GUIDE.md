# 🧠 Memory-Aware AI with Database Training

## Overview

The Memory-Aware AI system provides:
- **Session-based conversations** - Remember context across multiple requests
- **Database training** - AI knows your entire database schema automatically
- **Conversation history** - Stored in MongoDB for persistence
- **Context awareness** - AI understands what tables and data you have

## Key Features

### 1. **Automatic Database Training**
The AI automatically knows:
- All table names and their schemas
- Column types, constraints, and indexes
- Record counts for each table
- Sample data from tables
- Common operation examples

### 2. **Persistent Memory**
- Conversations stored in MongoDB
- Session-based context
- Multi-user support
- Conversation history retrieval

### 3. **Context-Aware Responses**
- AI references your actual database structure
- Suggests operations based on existing data
- Remembers previous questions in the session

## API Endpoints

### 1. Start/Continue Conversation
```
POST /ai/conversation
Content-Type: application/json

{
  "prompt": "your question",
  "sessionId": "optional-session-id",
  "userId": "optional-user-id"
}
```

**Response:**
```json
{
  "success": true,
  "sessionId": "abc-123-def",
  "action": "LIST_TABLES",
  "result": { ... },
  "explanation": "I'm showing you all available tables",
  "conversationLength": 2
}
```

### 2. Get Conversation History
```
GET /ai/conversation/{sessionId}
```

**Response:**
```json
{
  "success": true,
  "sessionId": "abc-123-def",
  "startedAt": "2026-01-31T18:00:00",
  "messageCount": 5,
  "messages": [
    {
      "sequence": 1,
      "role": "user",
      "content": "What tables do I have?",
      "timestamp": "2026-01-31T18:00:01"
    },
    {
      "sequence": 2,
      "role": "assistant",
      "content": "You have 3 tables: users, products, orders",
      "action": "LIST_TABLES",
      "result": { ... },
      "timestamp": "2026-01-31T18:00:02"
    }
  ],
  "context": {
    "lastAction": "LIST_TABLES",
    "lastResult": { ... }
  }
}
```

### 3. End Conversation
```
DELETE /ai/conversation/{sessionId}
```

## Usage Examples

### Example 1: First Conversation

**Request 1:**
```bash
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What tables do I have?",
    "userId": "user123"
  }'
```

**Response:**
```json
{
  "success": true,
  "sessionId": "session-abc-123",
  "action": "LIST_TABLES",
  "result": {
    "tables": [
      {"tableName": "users", "id": "..."},
      {"tableName": "products", "id": "..."}
    ]
  },
  "explanation": "You have 2 tables: users and products",
  "conversationLength": 2
}
```

**Request 2 (Same Session):**
```bash
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Show me all users",
    "sessionId": "session-abc-123",
    "userId": "user123"
  }'
```

The AI remembers it just showed you the tables and knows "users" exists!

### Example 2: Context-Aware Search

**Request:**
```bash
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find users named John",
    "sessionId": "my-session"
  }'
```

The AI will:
1. Know the "users" table exists (from database training)
2. Know the schema of the users table
3. Execute the appropriate search
4. Remember this search in the session

### Example 3: Multi-Turn Conversation

```bash
# Turn 1
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a customers table with name, email, and phone",
    "sessionId": "session-xyz"
  }'

# Turn 2 (AI remembers the table was just created)
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Add a customer named Alice with email alice@example.com",
    "sessionId": "session-xyz"
  }'

# Turn 3 (AI knows about Alice)
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Show me all customers",
    "sessionId": "session-xyz"
  }'
```

## How It Works

### 1. Database Training

When you make a request, the AI automatically receives:

```
=== CURRENT DATABASE STATE ===

Total Tables: 2

TABLE: users
  ID: 679c6d48a3915174f836968d
  Description: User accounts
  Record Count: 150
  Columns:
    - name (string) [required]
    - email (string) [required, unique, indexed]
    - age (number)
    - isActive (boolean) [default: true]

TABLE: products
  ID: 679c6d4ba3915174f836968e
  Description: Product catalog
  Record Count: 450
  Columns:
    - sku (string) [required, unique, indexed]
    - name (string) [required, indexed]
    - price (number) [required]
    - category (string) [indexed]
```

### 2. Conversation Memory

```
=== CONVERSATION HISTORY ===

USER: What tables do I have?
  Action: LIST_TABLES

USER: Show me all users
  Action: SEARCH_RECORDS

USER: How many products are there?
  Action: GET_RECORD_COUNT
```

### 3. Context Accumulation

The AI builds context as the conversation progresses:

```json
{
  "context": {
    "lastAction": "SEARCH_RECORDS",
    "lastResult": {
      "count": 5,
      "records": [...]
    },
    "knownTables": ["users", "products"],
    "currentFocus": "users"
  }
}
```

## Advanced Features

### Session Management

**Create a new session:**
```bash
# Don't provide sessionId - one will be generated
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Hello, what can you help me with?",
    "userId": "user123"
  }'
```

**Continue existing session:**
```bash
# Use the sessionId from previous response
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a new table",
    "sessionId": "session-abc-123",
    "userId": "user123"
  }'
```

**View conversation history:**
```bash
curl http://localhost:8888/ai/conversation/session-abc-123
```

**End session:**
```bash
curl -X DELETE http://localhost:8888/ai/conversation/session-abc-123
```

### Multi-User Support

Each user can have multiple sessions:

```bash
# User 1, Session 1
curl -X POST http://localhost:8888/ai/conversation \
  -d '{"prompt": "...", "userId": "alice", "sessionId": "alice-session-1"}'

# User 1, Session 2
curl -X POST http://localhost:8888/ai/conversation \
  -d '{"prompt": "...", "userId": "alice", "sessionId": "alice-session-2"}'

# User 2, Session 1
curl -X POST http://localhost:8888/ai/conversation \
  -d '{"prompt": "...", "userId": "bob", "sessionId": "bob-session-1"}'
```

## Benefits

### 1. **No Need to Specify Table Names**
```
❌ Old: "Search table 679c6d48a3915174f836968d for users"
✅ New: "Find all users"
```

The AI knows the "users" table exists!

### 2. **Context-Aware Suggestions**
```
User: "Show me my data"
AI: "You have 3 tables: users (150 records), products (450 records), and orders (89 records). Which would you like to see?"
```

### 3. **Natural Conversations**
```
User: "Create a products table"
AI: "Table created successfully"

User: "Add a product"
AI: "I'll add it to the products table you just created"
```

### 4. **Learning from History**
```
User: "Delete all inactive users"
AI: "I found 5 inactive users from our previous search. Shall I delete them?"
```

## Comparison of AI Endpoints

| Feature | `/ai/ask` | `/ai/chat` | `/ai/conversation` |
|---------|-----------|------------|-------------------|
| **Memory** | None | Temporary | Persistent |
| **Database Context** | No | No | Yes |
| **Multi-turn** | No | Yes | Yes |
| **Session Storage** | No | No | Yes (MongoDB) |
| **Best For** | Simple commands | Complex tasks | Ongoing conversations |

## Data Storage

Conversations are stored in MongoDB collection `ai_conversations`:

```json
{
  "_id": "...",
  "sessionId": "session-abc-123",
  "userId": "user123",
  "startedAt": "2026-01-31T18:00:00",
  "lastUpdatedAt": "2026-01-31T18:05:00",
  "active": true,
  "messages": [
    {
      "sequence": 1,
      "role": "user",
      "content": "What tables do I have?",
      "timestamp": "2026-01-31T18:00:01"
    },
    {
      "sequence": 2,
      "role": "assistant",
      "content": "You have 2 tables",
      "action": "LIST_TABLES",
      "result": {...},
      "timestamp": "2026-01-31T18:00:02"
    }
  ],
  "context": {
    "lastAction": "LIST_TABLES",
    "lastResult": {...}
  }
}
```

## Best Practices

1. **Use consistent sessionIds** for related conversations
2. **Provide userId** for multi-user applications
3. **End sessions** when done to free up resources
4. **Review history** to understand AI's context
5. **Start new sessions** for unrelated topics

## Example Workflow

```bash
# 1. Start a new conversation
RESPONSE=$(curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What data do I have?", "userId": "alice"}')

# Extract sessionId
SESSION_ID=$(echo $RESPONSE | jq -r '.sessionId')

# 2. Continue the conversation
curl -X POST http://localhost:8888/ai/conversation \
  -H "Content-Type: application/json" \
  -d "{\"prompt\": \"Show me all users\", \"sessionId\": \"$SESSION_ID\", \"userId\": \"alice\"}"

# 3. View history
curl http://localhost:8888/ai/conversation/$SESSION_ID

# 4. End when done
curl -X DELETE http://localhost:8888/ai/conversation/$SESSION_ID
```

## Tips

- The AI automatically knows your database structure
- No need to provide table IDs - use table names
- Sessions persist across server restarts (stored in MongoDB)
- Conversation history helps AI give better answers
- Use descriptive table and column names for better AI understanding

---

**The AI now has memory and knows your database! 🧠🗄️**
