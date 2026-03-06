# AI-Powered DataTable System - Complete Guide

## 🎯 Overview
This AI assistant can understand natural language and automatically execute DataTable operations. Just describe what you want in plain English!

## ✨ Key Features
- **Smart Table Resolution**: Use table names instead of IDs - the AI figures it out!
- **Full CRUD Operations**: Create, Read, Update, Delete for both tables and records
- **Intelligent Search**: Full-text search and field-specific queries
- **Index Management**: Create and drop indexes with simple commands
- **Batch Operations**: Delete multiple records at once

## 🚀 Quick Start

### Example Prompts You Can Use:

#### 📊 Table Management

**Create a table:**
```
Create a users table with name (string, required), email (string, required, unique, indexed), age (number), and isActive (boolean with default true)
```

**List all tables:**
```
Show me all tables
```
or
```
List all my tables
```

**Get table details:**
```
Show me the schema for the users table
```

**Delete a table:**
```
Delete the users table
```

#### 📝 Record Operations

**Insert a record:**
```
Add a user to the users table with name "John Doe", email "john@example.com", age 30
```

**Get a specific record:**
```
Get record with ID 679c6d48a3915174f836968d
```

**Update a record:**
```
Update record 679c6d48a3915174f836968d and set age to 31
```

**Delete a record:**
```
Delete record 679c6d48a3915174f836968d
```

**Delete multiple records:**
```
Delete records with IDs 679c6d48a3915174f836968d, 679c6d4ba3915174f836968e
```

#### 🔍 Search Operations

**Full-text search:**
```
Search the users table for "john"
```

**Search by specific field:**
```
Find all users where email is "john@example.com"
```
or
```
Search users table where age is 30
```

**Get record count:**
```
How many records are in the users table?
```

#### 🔧 Index Management

**Create an index:**
```
Create an index on the email column in the users table
```

**Create a unique index:**
```
Create a unique index on the username field in users
```

**Drop an index:**
```
Remove the index from the email column in users
```

## 📚 Real-World Examples

### Example 1: E-commerce Product Catalog

```bash
# 1. Create products table
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a products table with sku (string, required, unique, indexed), name (string, required, indexed), description (string), price (number, required), category (string, indexed), inStock (boolean with default true), tags (array), and images (array)"
  }'

# 2. Add a product
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Add a product to products table: sku ABC123, name Wireless Mouse, description Ergonomic wireless mouse, price 29.99, category Electronics, tags [wireless, mouse, computer]"
  }'

# 3. Search for products
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find all products in the Electronics category"
  }'

# 4. Get product count
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "How many products do I have?"
  }'
```

### Example 2: Customer Management

```bash
# Create customers table
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a customers table with firstName (string, required), lastName (string, required), email (string, required, unique, indexed), phone (string), address (object), registeredAt (date), loyaltyPoints (number with default 0)"
  }'

# Add a customer
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Add a customer: firstName Alice, lastName Smith, email alice@example.com, phone 555-1234"
  }'

# Search by email
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find customer with email alice@example.com"
  }'
```

### Example 3: Blog System

```bash
# Create posts table
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a blog_posts table with title (string, required, indexed), slug (string, required, unique), content (string, required), author (string, required, indexed), tags (array), publishedAt (date), status (string with default draft), viewCount (number with default 0)"
  }'

# Add a post
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a blog post: title Getting Started with AI, slug getting-started-ai, content This is an introduction to AI, author John Doe, tags [ai, tutorial, beginner], status published"
  }'

# Search posts
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Search blog posts for AI"
  }'
```

## 🎨 Advanced Features

### Nested Objects
```
Create a users table with profile (object) containing bio, avatar, and socialLinks
```

### Complex Queries
```
Find all products where price is greater than 50
```
(Note: The AI will interpret this and use the appropriate search method)

### Batch Operations
```
Delete all draft posts
```

### Table Management
```
Show me the structure of the products table
```

## 🔑 Available Actions

The AI can perform these operations:

1. **CREATE_TABLE** - Create new tables with custom schemas
2. **LIST_TABLES** - List all tables
3. **GET_TABLE** - Get table schema and details
4. **DELETE_TABLE** - Delete a table and all its records
5. **INSERT_RECORD** - Add new records
6. **GET_RECORD** - Retrieve a specific record
7. **UPDATE_RECORD** - Modify existing records
8. **DELETE_RECORD** - Delete a single record
9. **DELETE_RECORDS** - Batch delete multiple records
10. **SEARCH_RECORDS** - Full-text search
11. **SEARCH_BY_FIELD** - Search by specific field value
12. **CREATE_INDEX** - Create database indexes
13. **DROP_INDEX** - Remove indexes
14. **GET_RECORD_COUNT** - Count records in a table

## 💡 Tips for Better Prompts

1. **Be specific about data types**: "age (number)" instead of just "age"
2. **Mention constraints clearly**: "email (string, required, unique, indexed)"
3. **Use table names**: "users table" instead of table IDs
4. **Be natural**: The AI understands conversational language
5. **Specify defaults**: "isActive (boolean with default true)"

## 🛠️ API Endpoint

```
POST /ai/ask
Content-Type: application/json

{
  "prompt": "your natural language request"
}
```

## 📊 Response Format

```json
{
  "success": true,
  "action": "CREATE_TABLE",
  "aiResponse": "...",
  "result": {
    "success": true,
    "tableId": "abc123",
    "tableName": "users",
    "message": "Table created successfully"
  }
}
```

## 🚨 Error Handling

If something goes wrong, you'll get a detailed error response:

```json
{
  "success": false,
  "error": "Table not found: products",
  "errorType": "RuntimeException",
  "rawAiResponse": "..."
}
```

## 🔐 Setup

1. Set your Gemini API key:
```bash
export GEMINI_API_KEY=your_api_key_here
```

2. Start the application:
```bash
mvn spring-boot:run
```

3. The server runs on `http://localhost:8888`

## 📖 More Examples

### Inventory Management
```
Create an inventory table with productId (string, required, indexed), warehouseLocation (string, indexed), quantity (number, required), lastRestocked (date), minimumStock (number with default 10)
```

### Order Processing
```
Create an orders table with orderNumber (string, required, unique, indexed), customerId (string, required, indexed), items (array, required), totalAmount (number, required), status (string with default pending), shippingAddress (object), paymentMethod (string), orderDate (date)
```

### Analytics
```
How many orders have status completed?
```

```
Find all orders from customer ID cust_12345
```

## 🎯 Best Practices

1. **Always create indexes** on fields you'll search frequently
2. **Use unique constraints** for fields like email, username, SKU
3. **Set appropriate defaults** to simplify data entry
4. **Use descriptive table names** for better AI understanding
5. **Test with small datasets** before scaling up

## 📝 Notes

- The AI automatically resolves table names to IDs
- You can use either table names or IDs in your prompts
- Full-text search works across all fields in a record
- Field-specific search is more precise and faster

## 🆘 Troubleshooting

**API Quota Exceeded?**
- Wait a few minutes for quota reset
- Check usage at https://ai.dev/rate-limit
- Consider upgrading your Gemini API plan

**Table Not Found?**
- Use `List all tables` to see available tables
- Check table name spelling
- Ensure the table was created successfully

**Invalid JSON Response?**
- Check the `rawAiResponse` in error messages
- Try rephrasing your prompt more clearly
- Be more specific about what you want

## 🌟 Coming Soon

- Bulk import from CSV/JSON
- Advanced query filters (greater than, less than, etc.)
- Table relationships and joins
- Scheduled operations
- Data export functionality

---

**Happy DataTable Management! 🚀**
