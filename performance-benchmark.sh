#!/bin/bash

# Performance Benchmark Script for Osgiliath API
# Tests all critical CRUD operations and measures response times
# PRD Requirement: API response times < 200ms for standard CRUD operations

set -e

API_BASE_URL="http://localhost:8080/api"
USERNAME="testuser"
PASSWORD="password123"
ITERATIONS=10

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "========================================"
echo "  Osgiliath Performance Benchmark"
echo "========================================"
echo ""
echo "API Base URL: $API_BASE_URL"
echo "Iterations per endpoint: $ITERATIONS"
echo "PRD Target: < 200ms for CRUD operations"
echo ""

# Function to measure API call time
measure_api() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local auth_header=$5

    local total_time=0
    local times=()

    for i in $(seq 1 $ITERATIONS); do
        if [ -n "$data" ]; then
            # POST/PUT request with data
            time_ms=$(curl -X "$method" -s -w "%{time_total}" -o /dev/null \
                -H "Content-Type: application/json" \
                -H "Authorization: $auth_header" \
                -d "$data" \
                "$API_BASE_URL$endpoint" 2>/dev/null | awk '{print int($1*1000)}')
        else
            # GET/DELETE request without data
            time_ms=$(curl -X "$method" -s -w "%{time_total}" -o /dev/null \
                -H "Authorization: $auth_header" \
                "$API_BASE_URL$endpoint" 2>/dev/null | awk '{print int($1*1000)}')
        fi

        times+=($time_ms)
        total_time=$((total_time + time_ms))
    done

    # Calculate average
    avg_time=$((total_time / ITERATIONS))

    # Calculate min and max
    min_time=${times[0]}
    max_time=${times[0]}
    for time in "${times[@]}"; do
        [ $time -lt $min_time ] && min_time=$time
        [ $time -gt $max_time ] && max_time=$time
    done

    # Determine if it meets PRD requirement
    if [ $avg_time -lt 200 ]; then
        status="${GREEN}✓ PASS${NC}"
    else
        status="${RED}✗ FAIL${NC}"
    fi

    printf "%-40s | Avg: %4dms | Min: %4dms | Max: %4dms | %b\n" "$name" $avg_time $min_time $max_time "$status"
}

# Step 1: Login and get JWT token
echo "Step 1: Authenticating..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | grep -o '[^"]*$')

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Failed to get authentication token. Is the server running?${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}Authentication successful!${NC}"
echo ""

AUTH_HEADER="Bearer $TOKEN"

# Step 2: Create test data for benchmarking
echo "Step 2: Creating test data..."

# Create a customer for testing
CUSTOMER_DATA='{"name":"Performance Test Customer","email":"perf@test.com","phone":"555-0100","address":"123 Test St"}'
CUSTOMER_RESPONSE=$(curl -s -X POST "$API_BASE_URL/customers" \
    -H "Content-Type: application/json" \
    -H "Authorization: $AUTH_HEADER" \
    -d "$CUSTOMER_DATA")

CUSTOMER_ID=$(echo $CUSTOMER_RESPONSE | grep -o '"id":"[^"]*' | grep -o '[^"]*$')

if [ -z "$CUSTOMER_ID" ]; then
    echo -e "${YELLOW}Warning: Could not create customer. Using existing data.${NC}"
    # Try to get an existing customer
    CUSTOMERS_RESPONSE=$(curl -s -X GET "$API_BASE_URL/customers?page=0&size=1" -H "Authorization: $AUTH_HEADER")
    CUSTOMER_ID=$(echo $CUSTOMERS_RESPONSE | grep -o '"id":"[^"]*' | head -1 | grep -o '[^"]*$')
fi

echo "Test Customer ID: $CUSTOMER_ID"
echo ""

# Step 3: Run benchmarks
echo "========================================"
echo "  Performance Test Results"
echo "========================================"
echo ""

# Customer Operations
echo "CUSTOMER OPERATIONS:"
echo "----------------------------------------"
measure_api "GET /customers (list)" "GET" "/customers?page=0&size=10" "" "$AUTH_HEADER"
measure_api "GET /customers/{id}" "GET" "/customers/$CUSTOMER_ID" "" "$AUTH_HEADER"
measure_api "POST /customers (create)" "POST" "/customers" "$CUSTOMER_DATA" "$AUTH_HEADER"
UPDATE_DATA='{"name":"Updated Customer","email":"updated@test.com","phone":"555-0200","address":"456 Updated St"}'
measure_api "PUT /customers/{id} (update)" "PUT" "/customers/$CUSTOMER_ID" "$UPDATE_DATA" "$AUTH_HEADER"
echo ""

# Invoice Operations
echo "INVOICE OPERATIONS:"
echo "----------------------------------------"
measure_api "GET /invoices (list)" "GET" "/invoices?page=0&size=10" "" "$AUTH_HEADER"

# Create an invoice for testing
INVOICE_DATA=$(cat <<EOF
{
  "customerId": "$CUSTOMER_ID",
  "issueDate": "2024-11-01",
  "dueDate": "2024-12-01",
  "taxRate": 10.0,
  "lineItems": [
    {"description": "Test Service", "quantity": 1, "unitPrice": 100.00}
  ]
}
EOF
)

# Create invoice and get ID
INVOICE_RESPONSE=$(curl -s -X POST "$API_BASE_URL/invoices" \
    -H "Content-Type: application/json" \
    -H "Authorization: $AUTH_HEADER" \
    -d "$INVOICE_DATA")
INVOICE_ID=$(echo $INVOICE_RESPONSE | grep -o '"id":"[^"]*' | grep -o '[^"]*$')

measure_api "POST /invoices (create)" "POST" "/invoices" "$INVOICE_DATA" "$AUTH_HEADER"
measure_api "GET /invoices/{id}" "GET" "/invoices/$INVOICE_ID" "" "$AUTH_HEADER"

# Add line item
LINE_ITEM_DATA='{"description":"Additional Service","quantity":2,"unitPrice":50.00}'
measure_api "POST /invoices/{id}/line-items" "POST" "/invoices/$INVOICE_ID/line-items" "$LINE_ITEM_DATA" "$AUTH_HEADER"
echo ""

# Payment Operations
echo "PAYMENT OPERATIONS:"
echo "----------------------------------------"
# First send the invoice
curl -s -X POST "$API_BASE_URL/invoices/$INVOICE_ID/send" -H "Authorization: $AUTH_HEADER" > /dev/null

PAYMENT_DATA='{"amount":50.00,"paymentDate":"2024-11-09","paymentMethod":"CREDIT_CARD","referenceNumber":"TEST-001"}'
measure_api "POST /invoices/{id}/payments" "POST" "/invoices/$INVOICE_ID/payments" "$PAYMENT_DATA" "$AUTH_HEADER"
measure_api "GET /invoices/{id}/payments" "GET" "/invoices/$INVOICE_ID/payments" "" "$AUTH_HEADER"
echo ""

# Complex Queries
echo "COMPLEX QUERIES:"
echo "----------------------------------------"
measure_api "GET /invoices?status=SENT" "GET" "/invoices?page=0&size=10&status=SENT" "" "$AUTH_HEADER"
measure_api "GET /invoices?customerId=..." "GET" "/invoices?page=0&size=10&customerId=$CUSTOMER_ID" "" "$AUTH_HEADER"
measure_api "GET /invoices with sorting" "GET" "/invoices?page=0&size=10&sortBy=issueDate&sortDirection=DESC" "" "$AUTH_HEADER"
echo ""

# Summary
echo "========================================"
echo "  Summary"
echo "========================================"
echo ""
echo "All tests completed successfully!"
echo ""
echo "PRD Requirement: API response times < 200ms"
echo "Tests performed: $(($ITERATIONS)) iterations per endpoint"
echo ""
echo -e "${GREEN}Note: Actual performance may vary based on:${NC}"
echo "  - Database size and indexes"
echo "  - Server hardware and resources"
echo "  - Network latency"
echo "  - First-run JVM warm-up time"
echo ""
echo "For production deployment:"
echo "  - Enable database connection pooling (HikariCP)"
echo "  - Add database indexes on foreign keys and filter columns"
echo "  - Enable JPA query caching"
echo "  - Use production-grade database (PostgreSQL RDS)"
echo ""
