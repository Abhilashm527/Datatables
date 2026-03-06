# Sample Data for Testing Search API

Use these JSON payloads to test the API.

## 1. Create a Table Schema
**Endpoint:** `POST /api/datatables/tables`

```json
{
  "tableName": "SearchTestTable",
  "description": "A table specifically designed to test text search across various data types",
  "columns": [
    {
      "name": "name",
      "dataType": "string",
      "required": true,
      "description": "Name of the entity"
    },
    {
      "name": "age",
      "dataType": "number",
      "required": false
    },
    {
      "name": "isVerified",
      "dataType": "boolean",
      "required": false
    },
    {
      "name": "tags",
      "dataType": "array",
      "required": false,
      "description": "Tags associated with the entity"
    },
    {
      "name": "metadata",
      "dataType": "object",
      "required": false,
      "description": "Complex nested metadata"
    }
  ]
}
```

## 2. Insert Data Record
**Endpoint:** `POST /api/datatables/tables/{tableId}/records`
*(Replace `{tableId}` with the ID returned from the table creation step)*

```json
{
  "name": "Project Alpha",
  "age": 25,
  "isVerified": true,
  "tags": [
    "development",
    "java",
    "spring-boot",
    "search-api"
  ],
  "metadata": {
    "author": "John Doe",
    "specs": {
      "version": "1.0",
      "priority": "high"
    },
    "contributors": [
      "Alice",
      "Bob"
    ]
  },
  "notes": "This is an extra field not in the schema. The search should still index this text: 'HiddenTreasure'."
}
```

## 3. Test Search
**Endpoint:** `GET /api/datatables/tables/{tableId}/search/text?query=<your_search_term>`

**Examples:**
- `?query=Alpha` (Matches string field)
- `?query=25` (Matches number)
- `?query=spring` (Matches item in array)
- `?query=Alice` (Matches nested list in object)
- `?query=priority` (Matches key in nested object?) *Note: current implementation collects VALUES, not keys. So search for 'high' to match value.*
- `?query=high` (Matches value in nested object)
- `?query=HiddenTreasure` (Matches extra field value)

## 4. Search by Specific Field
**Endpoint:** `GET /api/datatables/tables/{tableId}/search`

**Parameters:**
- `field`: The specific field key to search (e.g., `name`, `age`, `metadata.author`).
- `value`: The exact value to match.
- `page`: (Optional) Page number (0-indexed).
- `size`: (Optional) Page size.

**Examples:**
```bash
# Basic exact match
curl "http://localhost:8080/api/datatables/tables/{tableId}/search?field=name&value=Project%20Alpha"

# Boolean match
curl "http://localhost:8080/api/datatables/tables/{tableId}/search?field=isVerified&value=true"

# Nested field match
curl "http://localhost:8080/api/datatables/tables/{tableId}/search?field=metadata.author&value=John%20Doe"
```
