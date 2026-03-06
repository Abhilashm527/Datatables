#!/bin/bash

# Conversational AI Test Examples

BASE_URL="http://localhost:8888"

echo "=== Conversational AI Examples ==="
echo ""

echo "1. Exploratory Query - What tables exist and their record counts?"
echo "---"
curl -X POST "$BASE_URL/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What tables do I have and how many records are in each?"
  }' | jq '.'

echo ""
echo ""

echo "2. Complex Search - Find specific data"
echo "---"
curl -X POST "$BASE_URL/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Find all users named John"
  }' | jq '.'

echo ""
echo ""

echo "3. Multi-Step Operation - Create and populate"
echo "---"
curl -X POST "$BASE_URL/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a test_users table with name and email fields, then add 2 sample users"
  }' | jq '.'

echo ""
echo ""

echo "4. Conditional Logic - Check and act"
echo "---"
curl -X POST "$BASE_URL/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "If a products table exists, show me the first 5 products, otherwise tell me it doesn not exist"
  }' | jq '.'

echo ""
echo ""

echo "5. Data Analysis"
echo "---"
curl -X POST "$BASE_URL/ai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Which table has the most records?"
  }' | jq '.'

echo ""
echo "=== Tests Complete ==="
