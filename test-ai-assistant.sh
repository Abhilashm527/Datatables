#!/bin/bash

# Test the AI Assistant API

echo "=== Testing AI Assistant for DataTable Creation ==="
echo ""

# Set your API key if not already set
# export GEMINI_API_KEY=your_key_here

BASE_URL="http://localhost:8888"

echo "1. Creating a users table..."
curl -X POST "$BASE_URL/ai/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a table called users with name (string, required), email (string, required, unique, indexed), age (number), and isActive (boolean with default true)"
  }' | jq '.'

echo ""
echo ""
echo "2. Creating a products table..."
curl -X POST "$BASE_URL/ai/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a products table with sku (string, required, unique), name (string, required, indexed), price (number, required), category (string, indexed), inStock (boolean with default true), and tags (array)"
  }' | jq '.'

echo ""
echo ""
echo "Note: Save the tableId from the response to use in subsequent operations"
echo ""
echo "3. Example: Insert a record (replace TABLE_ID with actual ID)"
echo 'curl -X POST "$BASE_URL/ai/ask" \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{'
echo '    "prompt": "Insert into table TABLE_ID a record with name John Doe, email john@example.com, age 30, isActive true"'
echo '  }'"'"' | jq '"'"'.'"'"
