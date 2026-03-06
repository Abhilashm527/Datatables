# AI-Powered DataTable API

## Overview
This system allows you to manage data tables using natural language prompts powered by Google's Gemini AI. You can create tables, insert records, search data, and manage indexes just by describing what you want in plain English.

## Setup

### 1. Set your Gemini API Key
```bash
export GEMINI_API_KEY=your_actual_api_key_here
```

### 2. Start the application
```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8888`

## Using the AI Assistant

### Endpoint
```
POST /ai/ask
Content-Type: application/json

{
  "prompt": "your natural language request"
}
```

## Example Prompts

### 1. Create a Table

**Prompt:**
```json
{
  "prompt": "Create a table called 'users' with columns: name (string, required), email (string, required, unique), age (number), and isActive (boolean with default true)"
}
```

**Response:**
```json
{
  "success": true,
  "action": "CREATE_TABLE",
  "aiResponse": "...",
  "result": {
    "success": true,
    "tableId": "679c6d48a3915174f836968d",
    "tableName": "users",
    "message": "Table created successfully"
  }
}
```

### 2. Create a Complex Table

**Prompt:**
```json
{
  "prompt": "Create a products table with name (string, required, indexed), description (string), price (number, required), category (string, indexed), inStock (boolean), tags (array), and metadata (object)"
}
```

### 3. Insert a Record

**Prompt:**
```json
{
  "prompt": "Insert a record into table 679c6d48a3915174f836968d with name 'John Doe', email 'john@example.com', age 30, and isActive true"
}
```

**Response:**
```json
{
  "success": true,
  "action": "INSERT_RECORD",
  "result": {
    "success": true,
    "recordId": "679c6d4ba3915174f836968e",
    "message": "Record inserted successfully"
  }
}
```

### 4. Search Records

**Prompt:**
```json
{
  "prompt": "Search for records in table 679c6d48a3915174f836968d where email is 'john@example.com'"
}
```

### 5. Create an Index

**Prompt:**
```json
{
  "prompt": "Create a unique index on the email column in table 679c6d48a3915174f836968d"
}
```

### 6. Delete Multiple Records

**Prompt:**
```json
{
  "prompt": "Delete records with IDs 679c6d48a3915174f836968d, 679c6d4ba3915174f836968e, 679c6d4ca3915174f836968f"
}
```

## More Example Prompts

### Creating Different Types of Tables

**E-commerce Product Table:**
```
Create a table for products with: sku (string, required, unique, indexed), name (string, required), description (string), price (number, required), discount (number), category (string, indexed), brand (string), images (array), specifications (object), inStock (boolean with default true), createdDate (date)
```

**Blog Posts Table:**
```
Create a blog_posts table with: title (string, required, indexed), slug (string, required, unique), content (string, required), author (string, required, indexed), tags (array), publishedAt (date), status (string with default 'draft'), viewCount (number with default 0)
```

**Customer Orders Table:**
```
Create an orders table with: orderNumber (string, required, unique, indexed), customerId (string, required, indexed), items (array, required), totalAmount (number, required), status (string with default 'pending'), shippingAddress (object), paymentMethod (string), orderDate (date)
```

## Direct API Endpoints (Without AI)

If you prefer to use the REST API directly without AI:

### Tables
- `POST /api/datatables/tables` - Create table
- `GET /api/datatables/tables` - List all tables
- `GET /api/datatables/tables/{id}` - Get table by ID
- `PUT /api/datatables/tables/{id}` - Update table
- `DELETE /api/datatables/tables/{id}` - Delete table

### Records
- `POST /api/datatables/tables/{tableId}/records` - Insert record
- `GET /api/datatables/tables/{tableId}/records` - Get records (paginated)
- `GET /api/datatables/records/{recordId}` - Get single record
- `PUT /api/datatables/records/{recordId}` - Update record
- `DELETE /api/datatables/records/{recordId}` - Delete single record
- `DELETE /api/datatables/records/batch` - Delete multiple records

### Search
- `GET /api/datatables/tables/{tableId}/search?field=name&value=John` - Search by field
- `GET /api/datatables/tables/{tableId}/search/text?query=searchterm` - Full-text search

### Indexes
- `POST /api/datatables/tables/{tableId}/indexes` - Create index
- `DELETE /api/datatables/tables/{tableId}/indexes/{columnName}` - Drop index

## Supported Data Types

- `string` - Text data
- `number` - Floating point numbers
- `integer` - Whole numbers
- `boolean` - true/false values
- `date` - Date/time values
- `array` - Lists of values
- `object` - Nested JSON objects

## Column Options

- `required` - Field must have a value
- `unique` - Value must be unique across all records
- `indexed` - Create database index for faster queries
- `defaultValue` - Default value if not provided

## Tips for Better AI Prompts

1. **Be specific about data types**: "age (number)" instead of just "age"
2. **Mention constraints**: "email (string, required, unique)"
3. **Specify defaults**: "isActive (boolean with default true)"
4. **Use clear table names**: Prefer "user_profiles" over "up"
5. **Include indexes for searchable fields**: "category (string, indexed)"

## Troubleshooting

### API Quota Exceeded
If you see `429 TOO_MANY_REQUESTS`, you've hit your Gemini API quota. Wait a few minutes or upgrade your plan.

### Invalid JSON Response
The AI might occasionally return invalid JSON. The system will try to clean it up, but if it fails, try rephrasing your prompt to be more specific.

### Table Not Found
Make sure to use the correct table ID (returned when creating a table) in subsequent operations.

## Example Workflow

```bash
# 1. Create a table
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a customers table with name (string, required), email (string, required, unique, indexed), phone (string), and registeredAt (date)"
  }'

# Response will include tableId: "abc123..."

# 2. Insert records
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Insert into table abc123 a customer with name Alice Smith, email alice@example.com, phone 555-1234"
  }'

# 3. Search for records
curl -X POST http://localhost:8888/ai/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Search table abc123 for email alice@example.com"
  }'
```

## Advanced Features

### Nested Objects
```
Create a users table with profile (object) containing: bio (string), avatar (string), socialLinks (object)
```

### Arrays
```
Create a products table with tags (array) and images (array)
```

### Complex Queries
The AI can understand complex requests like:
- "Create an index on the category field"
- "Delete records with these IDs: id1, id2, id3"
- "Search for all products where price is 99.99"

## API Documentation

Full Swagger/OpenAPI documentation is available at:
```
http://localhost:8888/swagger-ui.html
```

## License
This project is for demonstration purposes.
