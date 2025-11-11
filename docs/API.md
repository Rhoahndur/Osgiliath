# Osgiliath API Documentation

Complete REST API reference for the Osgiliath application.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Common Patterns](#common-patterns)
- [Error Handling](#error-handling)
- [Authentication Endpoints](#authentication-endpoints)
- [Customer Endpoints](#customer-endpoints)
- [Invoice Endpoints](#invoice-endpoints)
- [Payment Endpoints](#payment-endpoints)
- [Data Models](#data-models)

## Overview

### Base URL

```
http://localhost:8080/api
```

### API Specification

The API follows REST principles and provides:
- **OpenAPI 3.0 Specification**: `http://localhost:8080/api/v3/api-docs`
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`

### Content Type

All endpoints accept and return JSON:
```
Content-Type: application/json
```

### HTTP Methods

- **GET**: Retrieve resources (queries)
- **POST**: Create resources (commands)
- **PUT**: Update resources (commands)
- **DELETE**: Delete resources (commands)

## Authentication

### JWT Token-Based Authentication

All endpoints except `/auth/login` and `/auth/register` require authentication.

#### Obtaining a Token

1. Register a new user via `/auth/register`
2. Login via `/auth/login` to receive a JWT token
3. Include token in all subsequent requests

#### Using the Token

Add the JWT token to the `Authorization` header:

```http
Authorization: Bearer <your-jwt-token>
```

Example:
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5..." \
  http://localhost:8080/api/customers
```

#### Token Expiration

- **Validity Period**: 24 hours
- **Renewal**: Login again to obtain a new token
- **Status Code**: 401 Unauthorized if token is invalid or expired

## Common Patterns

### Pagination

List endpoints support pagination:

```http
GET /api/customers?page=0&size=20&sortBy=createdAt&sortDirection=DESC
```

Parameters:
- `page`: Page number (0-based), default: 0
- `size`: Page size, default: 20
- `sortBy`: Field to sort by, default: createdAt
- `sortDirection`: ASC or DESC, default: DESC

Response:
```json
{
  "content": [...],
  "pageable": {...},
  "totalPages": 5,
  "totalElements": 100,
  "last": false,
  "size": 20,
  "number": 0
}
```

### Filtering

Invoice list endpoint supports filtering:

```http
GET /api/invoices?status=SENT&customerId=123&fromDate=2024-01-01&toDate=2024-12-31
```

Parameters:
- `status`: Filter by invoice status (DRAFT, SENT, PAID)
- `customerId`: Filter by customer UUID
- `fromDate`: Filter by issue date from (ISO date format)
- `toDate`: Filter by issue date to (ISO date format)

### Sorting

Specify sort field and direction:

```http
GET /api/customers?sortBy=name&sortDirection=ASC
```

Common sort fields:
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp
- `name`: Entity name
- `email`: Email address

## Error Handling

### Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2024-11-07T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/customers",
  "errors": [
    {
      "field": "email",
      "message": "Email is required"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request succeeded |
| 201 | Created | Resource created successfully |
| 204 | No Content | Resource deleted successfully |
| 400 | Bad Request | Invalid request data or business rule violation |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource (e.g., email already exists) |
| 500 | Internal Server Error | Unexpected server error |

### Common Error Scenarios

#### 1. Validation Errors (400)
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {"field": "name", "message": "Name cannot be empty"},
    {"field": "email", "message": "Invalid email format"}
  ]
}
```

#### 2. Not Found (404)
```json
{
  "status": 404,
  "message": "Customer not found with id: 123e4567-e89b-12d3-a456-426614174000"
}
```

#### 3. Business Rule Violation (400)
```json
{
  "status": 400,
  "message": "Cannot send an invoice without line items"
}
```

#### 4. Duplicate Resource (409)
```json
{
  "status": 409,
  "message": "Customer with email already exists: john@example.com"
}
```

## Authentication Endpoints

### Register New User

Create a new user account.

```http
POST /auth/register
```

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "username": "johndoe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Validation Rules**:
- Username: 3-50 characters, alphanumeric
- Email: Valid email format, must be unique
- Password: Minimum 8 characters

**Example**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

### Login

Authenticate and receive a JWT token.

```http
POST /auth/login
```

**Request Body**:
```json
{
  "username": "johndoe",
  "password": "SecurePassword123!"
}
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "username": "johndoe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePassword123!"
  }'
```

## Customer Endpoints

### Create Customer

Create a new customer.

```http
POST /customers
```

**Request Body**:
```json
{
  "name": "Acme Corporation",
  "email": "contact@acme.com",
  "phone": "+1-555-0123",
  "address": "123 Main St, City, State 12345"
}
```

**Response** (201 Created):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Acme Corporation",
  "email": "contact@acme.com",
  "phone": "+1-555-0123",
  "address": "123 Main St, City, State 12345",
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T10:30:00"
}
```

**Validation Rules**:
- `name`: Required, max 200 characters
- `email`: Required, valid format, must be unique
- `phone`: Optional, max 50 characters
- `address`: Optional, max 500 characters

**Example**:
```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation",
    "email": "contact@acme.com",
    "phone": "+1-555-0123",
    "address": "123 Main St, City, State 12345"
  }'
```

### Get Customer by ID

Retrieve a customer by their unique identifier.

```http
GET /customers/{id}
```

**Path Parameters**:
- `id`: Customer UUID

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Acme Corporation",
  "email": "contact@acme.com",
  "phone": "+1-555-0123",
  "address": "123 Main St, City, State 12345",
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T10:30:00"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/customers/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### List Customers

Retrieve a paginated list of customers.

```http
GET /customers?page=0&size=20&sortBy=createdAt&sortDirection=DESC
```

**Query Parameters**:
- `page`: Page number (0-based), default: 0
- `size`: Page size, default: 20
- `sortBy`: Sort field (name, email, createdAt), default: createdAt
- `sortDirection`: ASC or DESC, default: DESC

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Acme Corporation",
      "email": "contact@acme.com",
      "phone": "+1-555-0123",
      "address": "123 Main St, City, State 12345",
      "createdAt": "2024-11-07T10:30:00",
      "updatedAt": "2024-11-07T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 5,
  "totalElements": 100,
  "last": false
}
```

**Example**:
```bash
curl -X GET "http://localhost:8080/api/customers?page=0&size=10&sortBy=name&sortDirection=ASC" \
  -H "Authorization: Bearer <token>"
```

### Update Customer

Update an existing customer.

```http
PUT /customers/{id}
```

**Path Parameters**:
- `id`: Customer UUID

**Request Body**:
```json
{
  "name": "Acme Corporation Inc.",
  "email": "info@acme.com",
  "phone": "+1-555-0124",
  "address": "456 New St, City, State 12345"
}
```

**Response** (200 OK):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Acme Corporation Inc.",
  "email": "info@acme.com",
  "phone": "+1-555-0124",
  "address": "456 New St, City, State 12345",
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T11:45:00"
}
```

**Example**:
```bash
curl -X PUT http://localhost:8080/api/customers/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation Inc.",
    "email": "info@acme.com",
    "phone": "+1-555-0124",
    "address": "456 New St, City, State 12345"
  }'
```

### Delete Customer

Delete an existing customer.

```http
DELETE /customers/{id}
```

**Path Parameters**:
- `id`: Customer UUID

**Response** (204 No Content)

**Example**:
```bash
curl -X DELETE http://localhost:8080/api/customers/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

## Invoice Endpoints

### Create Invoice

Create a new invoice in DRAFT status.

```http
POST /invoices
```

**Request Body**:
```json
{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "invoiceNumber": "INV-2024-001",
  "issueDate": "2024-11-07",
  "dueDate": "2024-12-07",
  "lineItems": [
    {
      "description": "Consulting Services",
      "quantity": 10,
      "unitPrice": 150.00
    },
    {
      "description": "Development Work",
      "quantity": 20,
      "unitPrice": 200.00
    }
  ]
}
```

**Response** (201 Created):
```json
{
  "id": "789e4567-e89b-12d3-a456-426614174000",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "invoiceNumber": "INV-2024-001",
  "issueDate": "2024-11-07",
  "dueDate": "2024-12-07",
  "status": "DRAFT",
  "lineItems": [
    {
      "id": "111e4567-e89b-12d3-a456-426614174000",
      "description": "Consulting Services",
      "quantity": 10,
      "unitPrice": 150.00,
      "lineTotal": 1500.00
    },
    {
      "id": "222e4567-e89b-12d3-a456-426614174000",
      "description": "Development Work",
      "quantity": 20,
      "unitPrice": 200.00,
      "lineTotal": 4000.00
    }
  ],
  "subtotal": 5500.00,
  "taxAmount": 550.00,
  "totalAmount": 6050.00,
  "balanceDue": 0.00,
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T10:30:00"
}
```

**Validation Rules**:
- `customerId`: Required, must exist
- `invoiceNumber`: Required, must be unique
- `issueDate`: Required, valid date
- `dueDate`: Required, must be >= issueDate
- `lineItems`: Optional, but invoice cannot be sent without line items

**Calculations**:
- `lineTotal` = quantity × unitPrice
- `subtotal` = sum of all lineTotals
- `taxAmount` = subtotal × 10%
- `totalAmount` = subtotal + taxAmount
- `balanceDue` = 0 (until invoice is sent)

**Example**:
```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "invoiceNumber": "INV-2024-001",
    "issueDate": "2024-11-07",
    "dueDate": "2024-12-07",
    "lineItems": [
      {
        "description": "Consulting Services",
        "quantity": 10,
        "unitPrice": 150.00
      }
    ]
  }'
```

### Get Invoice by ID

Retrieve an invoice with all line items.

```http
GET /invoices/{id}
```

**Path Parameters**:
- `id`: Invoice UUID

**Response** (200 OK):
```json
{
  "id": "789e4567-e89b-12d3-a456-426614174000",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "invoiceNumber": "INV-2024-001",
  "issueDate": "2024-11-07",
  "dueDate": "2024-12-07",
  "status": "SENT",
  "lineItems": [...],
  "subtotal": 5500.00,
  "taxAmount": 550.00,
  "totalAmount": 6050.00,
  "balanceDue": 6050.00,
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T11:00:00"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### List Invoices

Retrieve a list of invoices with optional filters.

```http
GET /invoices?status=SENT&customerId={uuid}&fromDate=2024-01-01&toDate=2024-12-31&page=0&size=20
```

**Query Parameters**:
- `status`: Filter by status (DRAFT, SENT, PAID)
- `customerId`: Filter by customer UUID
- `fromDate`: Filter by issue date from (ISO date)
- `toDate`: Filter by issue date to (ISO date)
- `page`: Page number, default: 0
- `size`: Page size, default: 20

**Response** (200 OK):
```json
[
  {
    "id": "789e4567-e89b-12d3-a456-426614174000",
    "customerId": "123e4567-e89b-12d3-a456-426614174000",
    "invoiceNumber": "INV-2024-001",
    "issueDate": "2024-11-07",
    "dueDate": "2024-12-07",
    "status": "SENT",
    "subtotal": 5500.00,
    "taxAmount": 550.00,
    "totalAmount": 6050.00,
    "balanceDue": 6050.00,
    "createdAt": "2024-11-07T10:30:00",
    "updatedAt": "2024-11-07T11:00:00"
  }
]
```

**Example**:
```bash
curl -X GET "http://localhost:8080/api/invoices?status=SENT&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

### Update Invoice

Update invoice details (DRAFT status only).

```http
PUT /invoices/{id}
```

**Path Parameters**:
- `id`: Invoice UUID

**Request Body**:
```json
{
  "issueDate": "2024-11-08",
  "dueDate": "2024-12-08"
}
```

**Response** (200 OK):
```json
{
  "id": "789e4567-e89b-12d3-a456-426614174000",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "invoiceNumber": "INV-2024-001",
  "issueDate": "2024-11-08",
  "dueDate": "2024-12-08",
  "status": "DRAFT",
  "lineItems": [...],
  "subtotal": 5500.00,
  "taxAmount": 550.00,
  "totalAmount": 6050.00,
  "balanceDue": 0.00,
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T12:00:00"
}
```

**Business Rules**:
- Only DRAFT invoices can be updated
- Cannot change customerId or invoiceNumber
- dueDate must be >= issueDate

**Example**:
```bash
curl -X PUT http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "issueDate": "2024-11-08",
    "dueDate": "2024-12-08"
  }'
```

### Add Line Item

Add a line item to an invoice (DRAFT status only).

```http
POST /invoices/{id}/line-items
```

**Path Parameters**:
- `id`: Invoice UUID

**Request Body**:
```json
{
  "description": "Additional Services",
  "quantity": 5,
  "unitPrice": 100.00
}
```

**Response** (201 Created):
```json
{
  "id": "333e4567-e89b-12d3-a456-426614174000",
  "description": "Additional Services",
  "quantity": 5,
  "unitPrice": 100.00,
  "lineTotal": 500.00
}
```

**Business Rules**:
- Only DRAFT invoices can have line items added
- quantity must be > 0
- unitPrice must be > 0
- Invoice totals are automatically recalculated

**Example**:
```bash
curl -X POST http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/line-items \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Additional Services",
    "quantity": 5,
    "unitPrice": 100.00
  }'
```

### Remove Line Item

Remove a line item from an invoice (DRAFT status only).

```http
DELETE /invoices/{id}/line-items/{lineItemId}
```

**Path Parameters**:
- `id`: Invoice UUID
- `lineItemId`: Line item UUID

**Response** (204 No Content)

**Business Rules**:
- Only DRAFT invoices can have line items removed
- Invoice totals are automatically recalculated

**Example**:
```bash
curl -X DELETE http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/line-items/333e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### Send Invoice

Transition invoice from DRAFT to SENT status.

```http
POST /invoices/{id}/send
```

**Path Parameters**:
- `id`: Invoice UUID

**Response** (200 OK):
```json
{
  "id": "789e4567-e89b-12d3-a456-426614174000",
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "invoiceNumber": "INV-2024-001",
  "issueDate": "2024-11-07",
  "dueDate": "2024-12-07",
  "status": "SENT",
  "lineItems": [...],
  "subtotal": 5500.00,
  "taxAmount": 550.00,
  "totalAmount": 6050.00,
  "balanceDue": 6050.00,
  "createdAt": "2024-11-07T10:30:00",
  "updatedAt": "2024-11-07T13:00:00"
}
```

**Business Rules**:
- Only DRAFT invoices can be sent
- Invoice must have at least one line item
- Status changes to SENT
- balanceDue is set to totalAmount
- Invoice can no longer be edited

**Example**:
```bash
curl -X POST http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/send \
  -H "Authorization: Bearer <token>"
```

### Get Invoice Balance

Retrieve balance information for an invoice.

```http
GET /invoices/{id}/balance
```

**Path Parameters**:
- `id`: Invoice UUID

**Response** (200 OK):
```json
{
  "invoiceId": "789e4567-e89b-12d3-a456-426614174000",
  "totalAmount": 6050.00,
  "paidAmount": 3000.00,
  "balanceDue": 3050.00,
  "status": "SENT"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/balance \
  -H "Authorization: Bearer <token>"
```

## Payment Endpoints

### Record Payment

Record a payment against an invoice.

```http
POST /invoices/{invoiceId}/payments
```

**Path Parameters**:
- `invoiceId`: Invoice UUID

**Request Body**:
```json
{
  "paymentDate": "2024-11-07",
  "amount": 3000.00,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN-12345"
}
```

**Response** (201 Created):
```json
{
  "id": "444e4567-e89b-12d3-a456-426614174000",
  "invoiceId": "789e4567-e89b-12d3-a456-426614174000",
  "paymentDate": "2024-11-07",
  "amount": 3000.00,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN-12345",
  "createdAt": "2024-11-07T14:00:00"
}
```

**Payment Methods**:
- `BANK_TRANSFER`
- `CREDIT_CARD`
- `CASH`
- `CHECK`

**Validation Rules**:
- `amount`: Required, must be > 0
- `amount`: Cannot exceed invoice balance due
- `paymentDate`: Required, valid date
- `paymentMethod`: Required, one of the enum values
- `referenceNumber`: Optional, max 100 characters

**Business Rules**:
- Only SENT invoices can receive payments
- Payment amount cannot exceed balance due
- Invoice balance is automatically updated
- Invoice status changes to PAID when balance reaches 0

**Example**:
```bash
curl -X POST http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentDate": "2024-11-07",
    "amount": 3000.00,
    "paymentMethod": "BANK_TRANSFER",
    "referenceNumber": "TXN-12345"
  }'
```

### Get Payment by ID

Retrieve a payment by its unique identifier.

```http
GET /payments/{id}
```

**Path Parameters**:
- `id`: Payment UUID

**Response** (200 OK):
```json
{
  "id": "444e4567-e89b-12d3-a456-426614174000",
  "invoiceId": "789e4567-e89b-12d3-a456-426614174000",
  "paymentDate": "2024-11-07",
  "amount": 3000.00,
  "paymentMethod": "BANK_TRANSFER",
  "referenceNumber": "TXN-12345",
  "createdAt": "2024-11-07T14:00:00"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/payments/444e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### List Payments for Invoice

Retrieve all payments applied to an invoice.

```http
GET /invoices/{invoiceId}/payments
```

**Path Parameters**:
- `invoiceId`: Invoice UUID

**Response** (200 OK):
```json
[
  {
    "id": "444e4567-e89b-12d3-a456-426614174000",
    "invoiceId": "789e4567-e89b-12d3-a456-426614174000",
    "paymentDate": "2024-11-07",
    "amount": 3000.00,
    "paymentMethod": "BANK_TRANSFER",
    "referenceNumber": "TXN-12345",
    "createdAt": "2024-11-07T14:00:00"
  },
  {
    "id": "555e4567-e89b-12d3-a456-426614174000",
    "invoiceId": "789e4567-e89b-12d3-a456-426614174000",
    "paymentDate": "2024-11-15",
    "amount": 3050.00,
    "paymentMethod": "CREDIT_CARD",
    "referenceNumber": "TXN-12346",
    "createdAt": "2024-11-15T10:00:00"
  }
]
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/invoices/789e4567-e89b-12d3-a456-426614174000/payments \
  -H "Authorization: Bearer <token>"
```

## Data Models

### Customer

```typescript
{
  id: string;              // UUID
  name: string;            // Max 200 chars
  email: string;           // Valid email, unique
  phone?: string;          // Optional, max 50 chars
  address?: string;        // Optional, max 500 chars
  createdAt: string;       // ISO 8601 datetime
  updatedAt: string;       // ISO 8601 datetime
}
```

### Invoice

```typescript
{
  id: string;              // UUID
  customerId: string;      // UUID, foreign key
  invoiceNumber: string;   // Unique, max 50 chars
  issueDate: string;       // ISO 8601 date
  dueDate: string;         // ISO 8601 date
  status: "DRAFT" | "SENT" | "PAID";
  lineItems: LineItem[];   // Array of line items
  subtotal: number;        // Decimal(19,2)
  taxAmount: number;       // Decimal(19,2), 10% of subtotal
  totalAmount: number;     // Decimal(19,2)
  balanceDue: number;      // Decimal(19,2)
  createdAt: string;       // ISO 8601 datetime
  updatedAt: string;       // ISO 8601 datetime
}
```

### Line Item

```typescript
{
  id: string;              // UUID
  invoiceId: string;       // UUID, foreign key
  description: string;     // Max 500 chars
  quantity: number;        // Decimal(10,2), > 0
  unitPrice: number;       // Decimal(19,2), > 0
  lineTotal: number;       // Decimal(19,2), calculated
}
```

### Payment

```typescript
{
  id: string;              // UUID
  invoiceId: string;       // UUID, foreign key
  paymentDate: string;     // ISO 8601 date
  amount: number;          // Decimal(19,2), > 0
  paymentMethod: "BANK_TRANSFER" | "CREDIT_CARD" | "CASH" | "CHECK";
  referenceNumber?: string; // Optional, max 100 chars
  createdAt: string;       // ISO 8601 datetime
}
```

### User

```typescript
{
  id: string;              // UUID
  username: string;        // 3-50 chars, unique
  email: string;           // Valid email, unique
  token?: string;          // JWT token (only in auth responses)
}
```

## Complete Example Flow

### Step-by-Step: Create Customer, Invoice, and Payment

#### 1. Register and Login

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"Pass123!"}'

# Login and save token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Pass123!"}' | jq -r '.token')
```

#### 2. Create Customer

```bash
CUSTOMER_ID=$(curl -X POST http://localhost:8080/api/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Acme Corp",
    "email":"contact@acme.com",
    "phone":"+1-555-0123"
  }' | jq -r '.id')
```

#### 3. Create Invoice

```bash
INVOICE_ID=$(curl -X POST http://localhost:8080/api/invoices \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\":\"$CUSTOMER_ID\",
    \"invoiceNumber\":\"INV-001\",
    \"issueDate\":\"2024-11-07\",
    \"dueDate\":\"2024-12-07\",
    \"lineItems\":[
      {\"description\":\"Service\",\"quantity\":10,\"unitPrice\":100}
    ]
  }" | jq -r '.id')
```

#### 4. Send Invoice

```bash
curl -X POST http://localhost:8080/api/invoices/$INVOICE_ID/send \
  -H "Authorization: Bearer $TOKEN"
```

#### 5. Record Payment

```bash
curl -X POST http://localhost:8080/api/invoices/$INVOICE_ID/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentDate":"2024-11-07",
    "amount":1100.00,
    "paymentMethod":"BANK_TRANSFER",
    "referenceNumber":"TXN-123"
  }'
```

#### 6. Verify Invoice Paid

```bash
curl -X GET http://localhost:8080/api/invoices/$INVOICE_ID/balance \
  -H "Authorization: Bearer $TOKEN"
```

---

For interactive API exploration, use Swagger UI at:
```
http://localhost:8080/api/swagger-ui.html
```
