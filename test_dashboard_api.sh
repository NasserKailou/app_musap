#!/bin/bash

echo "Testing Dashboard API Endpoint"
echo "==============================="
echo ""

# Test the API endpoint
echo "Making request to: http://localhost:9000/api/stats/reglements-structure"
echo ""

curl -v -X GET "http://localhost:9000/api/stats/reglements-structure" \
  -H "Accept: application/json" \
  2>&1

echo ""
echo ""
echo "==============================="
echo "Test completed"
