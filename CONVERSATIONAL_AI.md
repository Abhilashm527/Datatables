# 🤖 Conversational AI - Recursive Multi-Step Operations

## Overview

The Conversational AI endpoint (`/ai/chat`) enables **intelligent, multi-step conversations** where the AI can:
- Make multiple sequential calls to gather information
- Recursively call itself to complete complex tasks
- Build context from previous operations
- Provide comprehensive answers based on accumulated data

## Key Differences

| Feature | `/ai/ask` (Simple) | `/ai/chat` (Conversational) |
|---------|-------------------|----------------------------|
| **Calls** | Single action | Multiple recursive calls |
| **Context** | None | Maintains context across calls |
| **Complexity** | Simple tasks | Complex multi-step tasks |
| **History** | No history | Full conversation history |
| **Use Case** | Direct commands | Exploratory queries |

## How It Works

### Example Flow:

**User**: "Show me all users named John"

**AI Thinking Process**:
1. **Step 1**: "I need to find the users table first" → `LIST_TABLES`
2. **Step 2**: "Found 'users' table, now search for John" → `SEARCH_BY_FIELD`
3. **Step 3**: "Return results to user" → `FINAL_ANSWER`

## Usage

### Endpoint
```
POST /ai/chat
Content-Type: application/json

{
  "prompt": "your complex request"
}
```

### Response Format
```json
{
  "success": true,
  "result": {
    "answer": "Found 3 users named John",
    "data": { ... }
  },
  "conversationHistory": [
    {
      "iteration": 1,
      "prompt": "Show me all users named John",
      "action": "LIST_TABLES",
      "result": { ... }
    },
    {
      "iteration": 2,
      "prompt": "Search users table for John",
      "action": "SEARCH_BY_FIELD",
      "result": { ... }
    },
    {
      "iteration": 3,
      "action": "FINAL_ANSWER",
      "result": { ... },
      "isFinal": true
    }
  ],
  "totalSteps": 3
}
```

## Example Prompts

### 1. Exploratory Queries

**"What tables do I have and how many records in each?"**

The AI will:
1. List all tables
2. For each table, get record count
3. Summarize the results

```bash
curl -X POST http://localhost:8888/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What tables do I have and how many records in each?"
  }'
```

### 2. Complex Search

**"Find all products in the Electronics category that cost more than $50"**

The AI will:
1. Find the products table
2. Search for Electronics category
3. Filter by price (if needed)
4. Return results

```bash
curl -X POST http://localhost:8888/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find all products in the Electronics category that cost more than $50"
  }'
```

### 3. Data Analysis

**"Show me the user with the most orders"**

The AI will:
1. Find users and orders tables
2. Count orders per user
3. Identify the top user
4. Return the result

```bash
curl -X POST http://localhost:8888/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Show me the user with the most orders"
  }'
```

### 4. Multi-Table Operations

**"Create a customers table and add 3 sample customers"**

The AI will:
1. Create the customers table
2. Insert first customer
3. Insert second customer
4. Insert third customer
5. Confirm completion

```bash
curl -X POST http://localhost:8888/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a customers table with name, email, and phone, then add 3 sample customers"
  }'
```

### 5. Conditional Logic

**"If the users table exists, show me all users, otherwise create it first"**

The AI will:
1. Check if users table exists
2. If yes: fetch all users
3. If no: create the table first, then add sample data

```bash
curl -X POST http://localhost:8888/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "If the users table exists, show me all users, otherwise create it first with basic fields"
  }'
```

## Advanced Features

### Context Awareness

The AI maintains context across iterations:

```json
{
  "conversationHistory": [
    {
      "iteration": 1,
      "action": "LIST_TABLES",
      "result": {
        "tables": [
          {"tableName": "users", "id": "abc123"},
          {"tableName": "products", "id": "def456"}
        ]
      }
    },
    {
      "iteration": 2,
      "action": "SEARCH_BY_FIELD",
      "result": {
        "records": [...]
      }
    }
  ]
}
```

### Safety Limits

- **Maximum Iterations**: 5 (prevents infinite loops)
- **Timeout**: Each AI call has standard timeout
- **Error Handling**: Graceful failure with partial results

## Comparison Examples

### Simple Task (Use `/ai/ask`)
```
"Create a users table with name and email"
```
→ Single action, direct execution

### Complex Task (Use `/ai/chat`)
```
"Find all inactive users and delete them"
```
→ Multiple steps:
1. List tables to find "users"
2. Search for inactive users
3. Extract their IDs
4. Batch delete

## Response Analysis

### Successful Multi-Step Operation
```json
{
  "success": true,
  "result": {
    "answer": "Found and deleted 5 inactive users",
    "deletedCount": 5,
    "deletedIds": ["id1", "id2", "id3", "id4", "id5"]
  },
  "conversationHistory": [...],
  "totalSteps": 3
}
```

### Partial Success
```json
{
  "success": true,
  "result": {
    "answer": "Found users table but no inactive users",
    "count": 0
  },
  "conversationHistory": [...],
  "totalSteps": 2
}
```

### Error with Context
```json
{
  "success": false,
  "error": "Table 'users' not found",
  "conversationHistory": [
    {
      "iteration": 1,
      "action": "LIST_TABLES",
      "result": {
        "tables": []
      }
    }
  ]
}
```

## Best Practices

### ✅ Good Use Cases for `/ai/chat`

1. **Exploratory queries**: "What data do I have?"
2. **Multi-step operations**: "Create X, then add Y, then search for Z"
3. **Conditional logic**: "If X exists, do Y, otherwise do Z"
4. **Data analysis**: "Find the top 10 products by sales"
5. **Complex searches**: "Find all users who ordered in the last month"

### ❌ Use `/ai/ask` Instead

1. **Simple CRUD**: "Create a users table"
2. **Direct operations**: "Insert a record with name John"
3. **Single queries**: "List all tables"
4. **Known table IDs**: "Search table abc123 for email"

## Debugging

### View Conversation History

The response includes full history:

```javascript
{
  "conversationHistory": [
    {
      "iteration": 1,
      "prompt": "original user prompt",
      "aiResponse": { ... },
      "action": "LIST_TABLES",
      "result": { ... }
    },
    // ... more steps
  ]
}
```

### Console Logs

The server logs each iteration:

```
=== Iteration 1 ===
Prompt: Show me all users named John
AI Response: {"action": "LIST_TABLES", ...}

=== Iteration 2 ===
Prompt: Search users table for John
AI Response: {"action": "SEARCH_BY_FIELD", ...}
```

## Limitations

1. **Max 5 iterations**: Prevents infinite loops
2. **No session memory**: Each request is independent
3. **Sequential only**: Cannot parallelize operations
4. **API quota**: Each iteration uses Gemini API quota

## Tips for Better Results

1. **Be specific**: "Find users named John in the users table"
2. **Provide context**: "I have a users table, show me all admins"
3. **Use natural language**: "What's the most popular product?"
4. **Ask for summaries**: "Summarize my database structure"

## Future Enhancements

- [ ] Session-based conversations (remember across requests)
- [ ] Parallel operation execution
- [ ] Streaming responses
- [ ] Conversation branching
- [ ] User confirmation for destructive operations

---

**Use `/ai/chat` when you want the AI to think and plan, `/ai/ask` when you know exactly what you want!** 🚀
