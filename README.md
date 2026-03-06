# DataTable - Customizable Dynamic Tables with MongoDB

A Spring Boot application that provides a flexible, schema-less data table system using MongoDB. Create custom tables with dynamic schemas and store any type of data.

## Features

- **Dynamic Table Creation**: Create tables with custom schemas on the fly
- **Flexible Data Types**: Support for string, number, boolean, date, array, and object data types
- **Data Validation**: Automatic validation based on schema definitions
- **Default Values**: Set default values for columns
- **CRUD Operations**: Full create, read, update, delete operations for both tables and records
- **Search Functionality**: Search records by field values
- **Pagination**: Built-in pagination for large datasets
- **MongoDB Integration**: Uses MongoDB for flexible document storage

## Configuration

The application connects to MongoDB using the following configuration:

```properties
# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=datatable_db
spring.data.mongodb.username=admin
spring.data.mongodb.password=secret123
spring.data.mongodb.authentication-database=admin
```

## API Endpoints

### Table Management

#### Create a New Table
```
POST /api/datatables/tables
```

**Request Body Example:**
```json
{
    "tableName": "employees",
    "description": "Employee management table",
    "columns": [
        {
            "name": "firstName",
            "dataType": "string",
            "required": true,
            "description": "Employee's first name"
        },
        {
            "name": "lastName",
            "dataType": "string",
            "required": true,
            "description": "Employee's last name"
        },
        {
            "name": "email",
            "dataType": "string",
            "required": true,
            "description": "Employee's email"
        },
        {
            "name": "salary",
            "dataType": "number",
            "required": false,
            "defaultValue": 0,
            "description": "Employee's salary"
        },
        {
            "name": "active",
            "dataType": "boolean",
            "required": false,
            "defaultValue": true,
            "description": "Whether employee is active"
        },
        {
            "name": "skills",
            "dataType": "array",
            "required": false,
            "description": "Employee's skills"
        },
        {
            "name": "address",
            "dataType": "object",
            "required": false,
            "description": "Employee's address"
        }
    ]
}
```

#### Get All Tables
```
GET /api/datatables/tables
```

#### Get Table by ID
```
GET /api/datatables/tables/{id}
```

#### Get Table by Name
```
GET /api/datatables/tables/name/{tableName}
```

#### Update Table
```
PUT /api/datatables/tables/{id}
```

#### Delete Table
```
DELETE /api/datatables/tables/{id}
```

### Record Management

#### Insert Record
```
POST /api/datatables/tables/{tableId}/records
```

**Request Body Example:**
```json
{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@company.com",
    "salary": 75000,
    "active": true,
    "skills": ["Java", "Spring Boot", "MongoDB"],
    "address": {
        "street": "123 Main St",
        "city": "New York",
        "zipCode": "10001"
    }
}
```

#### Get Records (Paginated)
```
GET /api/datatables/tables/{tableId}/records?page=0&size=10&sortBy=createdAt&sortDir=desc
```

#### Get All Records
```
GET /api/datatables/tables/{tableId}/records/all
```

#### Get Single Record
```
GET /api/datatables/records/{recordId}
```

#### Update Record
```
PUT /api/datatables/records/{recordId}
```

#### Delete Record
```
DELETE /api/datatables/records/{recordId}
```

#### Search Records
```
GET /api/datatables/tables/{tableId}/search?field=firstName&value=John
```

#### Get Record Count
```
GET /api/datatables/tables/{tableId}/count
```

## Supported Data Types

- **string**: Text data
- **number**: Numeric data (integer or decimal)
- **boolean**: True/false values
- **date**: Date values (accepts string or Date objects)
- **array**: List of values
- **object**: Nested object/map data

## Sample Usage

### 1. Create a User Table

```bash
curl -X POST http://localhost:8080/api/datatables/tables \
  -H "Content-Type: application/json" \
  -d '{
    "tableName": "users",
    "description": "User management table",
    "columns": [
      {"name": "username", "dataType": "string", "required": true},
      {"name": "email", "dataType": "string", "required": true},
      {"name": "age", "dataType": "number", "required": false, "defaultValue": 18},
      {"name": "active", "dataType": "boolean", "required": false, "defaultValue": true}
    ]
  }'
```

### 2. Insert User Data

```bash
curl -X POST http://localhost:8080/api/datatables/tables/{tableId}/records \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "age": 30,
    "active": true
  }'
```

### 3. Get All Users

```bash
curl http://localhost:8080/api/datatables/tables/{tableId}/records/all
```

## Running the Application

1. Make sure MongoDB is running on `localhost:27017`
2. Create a user `admin` with password `secret123`
3. Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The application will start on port 8080 and automatically create sample tables for testing.

## Sample Tables

The application automatically creates two sample tables on startup:

1. **users**: A simple user table with name, email, age, and active status
2. **products**: A product catalog table with name, price, category, stock status, and tags

## Error Handling

The application includes global exception handling that returns structured error responses:

```json
{
    "timestamp": "2024-01-20T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Required field missing: email",
    "path": "/api/datatables/tables/123/records"
}
```

## Architecture

- **Model Layer**: `DataTableSchema` and `DataTableRecord` entities
- **Repository Layer**: Spring Data MongoDB repositories
- **Service Layer**: Business logic and validation
- **Controller Layer**: REST API endpoints
- **Configuration**: MongoDB and exception handling configuration