# Osgiliath Backend

Spring Boot backend application implementing Domain-Driven Design (DDD), Command Query Responsibility Segregation (CQRS), and Vertical Slice Architecture (VSA) for the Osgiliath invoice management system.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Domain Model](#domain-model)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Code Examples](#code-examples)

## Overview

The Osgiliath backend is a RESTful API service built with Spring Boot 3.2, following clean architecture principles and modern design patterns. It provides a robust foundation for invoice management with strong business rule enforcement and data integrity.

### Key Characteristics

- **Clean Architecture**: Separation of domain, application, infrastructure, and API layers
- **Rich Domain Models**: Entities contain business logic and enforce invariants
- **CQRS Pattern**: Explicit separation of read and write operations
- **Vertical Slices**: Features organized by business capability
- **Type Safety**: Comprehensive use of Java's type system
- **Security**: JWT-based authentication with Spring Security
- **API Documentation**: Auto-generated OpenAPI 3.0 (Swagger) documentation

## Architecture

### Architectural Layers

```
┌─────────────────────────────────────────────────────┐
│                   API Layer                         │
│  (REST Controllers, DTOs, Exception Handlers)       │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│              Application Layer                      │
│  (Commands, Queries, Handlers, Mappers)            │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│               Domain Layer                          │
│  (Aggregates, Entities, Value Objects, Rules)      │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│           Infrastructure Layer                      │
│  (JPA Repositories, Database Configuration)         │
└─────────────────────────────────────────────────────┘
```

### Domain-Driven Design Implementation

#### Bounded Contexts

The system is divided into three bounded contexts:

1. **Customer Context**: Customer management and validation
2. **Invoice Context**: Invoice lifecycle and line item management
3. **Payment Context**: Payment processing and reconciliation

#### Aggregates

Each bounded context has one or more aggregate roots:

- **Customer Aggregate**: Manages customer information and business rules
- **Invoice Aggregate**: Manages invoice lifecycle, line items, and status transitions
- **Payment Aggregate**: Manages payment recording and validation

#### Value Objects

Immutable value objects represent domain concepts:

- **Email**: Validates and encapsulates email addresses
- **Money**: Handles monetary amounts with proper arithmetic operations

### CQRS Pattern

Commands and queries are explicitly separated:

#### Commands (Write Operations)
- `CreateCustomerCommand` → `CreateCustomerHandler`
- `UpdateCustomerCommand` → `UpdateCustomerHandler`
- `CreateInvoiceCommand` → `CreateInvoiceHandler`
- `RecordPaymentCommand` → `RecordPaymentHandler`

#### Queries (Read Operations)
- `GetCustomerByIdQuery` → `GetCustomerByIdQueryHandler`
- `ListCustomersQuery` → `ListCustomersQueryHandler`
- `GetInvoiceByIdQuery` → `GetInvoiceByIdQueryHandler`
- `ListInvoicesQuery` → `ListInvoicesQueryHandler`

### Vertical Slice Architecture

Features are organized by business capability, not technical layers:

```
application/
├── customer/
│   ├── command/
│   │   ├── CreateCustomerCommand.java
│   │   ├── CreateCustomerHandler.java
│   │   ├── UpdateCustomerCommand.java
│   │   └── UpdateCustomerHandler.java
│   ├── query/
│   │   ├── GetCustomerByIdQuery.java
│   │   ├── GetCustomerByIdQueryHandler.java
│   │   ├── ListCustomersQuery.java
│   │   └── ListCustomersQueryHandler.java
│   └── dto/
│       ├── CustomerResponse.java
│       ├── CreateCustomerRequest.java
│       └── UpdateCustomerRequest.java
├── invoice/
└── payment/
```

## Domain Model

### Customer Aggregate

**Purpose**: Manage customer information and relationships

**Invariants**:
- Name must not be empty and max 200 characters
- Email must be unique and valid format
- Phone and address are optional

**Key Methods**:
- `Customer.create()`: Factory method for creating customers
- `update()`: Updates customer information with validation

### Invoice Aggregate

**Purpose**: Manage invoice lifecycle from draft to payment

**Status Flow**: Draft → Sent → Paid

**Invariants**:
- Invoice must have at least one line item before sending
- Line items can only be modified in Draft status
- Due date must be after issue date
- Payments can only be applied to Sent invoices
- Payment amount cannot exceed balance due

**Key Methods**:
- `Invoice.create()`: Factory method for creating invoices
- `addLineItem()`: Add line item (Draft only)
- `removeLineItem()`: Remove line item (Draft only)
- `send()`: Transition to Sent status
- `applyPayment()`: Apply payment and update balance
- `recalculateTotals()`: Calculate subtotal, tax, and total

**Calculations**:
```
Subtotal = Sum of (Quantity × Unit Price) for all line items
Tax = Subtotal × 10%
Total = Subtotal + Tax
Balance Due = Total - Sum of Payments
```

### Payment Aggregate

**Purpose**: Track and apply payments against invoices

**Invariants**:
- Amount must be positive
- Amount cannot exceed invoice balance
- Payment can only be applied to Sent invoices

**Key Methods**:
- `Payment.create()`: Factory method for creating payments
- Automatically updates invoice balance when recorded

## Technology Stack

### Core Frameworks
- **Spring Boot 3.2.0**: Application framework
- **Spring Web**: REST API support
- **Spring Data JPA**: Data persistence
- **Spring Security**: Authentication and authorization
- **Hibernate**: ORM implementation

### Database
- **PostgreSQL 15**: Production database
- **HikariCP**: Connection pooling
- **Flyway**: Database migrations (optional)

### Security
- **JWT (jjwt 0.12.3)**: Token-based authentication
- **BCrypt**: Password hashing

### Development Tools
- **Lombok**: Boilerplate reduction
- **Springdoc OpenAPI**: API documentation
- **Maven**: Build tool

### Testing
- **JUnit 5**: Testing framework
- **Spring Boot Test**: Integration testing
- **TestContainers**: Database integration tests
- **H2**: In-memory database for tests

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/osgiliath/
│   │   │   ├── api/                        # REST API Layer
│   │   │   │   ├── auth/
│   │   │   │   │   └── AuthController.java
│   │   │   │   ├── customer/
│   │   │   │   │   └── CustomerController.java
│   │   │   │   ├── invoice/
│   │   │   │   │   └── InvoiceController.java
│   │   │   │   ├── payment/
│   │   │   │   │   ├── PaymentController.java
│   │   │   │   │   └── PaymentExceptionHandler.java
│   │   │   │   └── error/
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── ErrorResponse.java
│   │   │   │
│   │   │   ├── application/                # Application Layer (CQRS)
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   └── dto/
│   │   │   │   ├── customer/
│   │   │   │   │   ├── command/
│   │   │   │   │   │   ├── CreateCustomerCommand.java
│   │   │   │   │   │   ├── CreateCustomerHandler.java
│   │   │   │   │   │   ├── UpdateCustomerCommand.java
│   │   │   │   │   │   ├── UpdateCustomerHandler.java
│   │   │   │   │   │   ├── DeleteCustomerCommand.java
│   │   │   │   │   │   └── DeleteCustomerHandler.java
│   │   │   │   │   ├── query/
│   │   │   │   │   │   ├── GetCustomerByIdQuery.java
│   │   │   │   │   │   ├── GetCustomerByIdQueryHandler.java
│   │   │   │   │   │   ├── ListCustomersQuery.java
│   │   │   │   │   │   └── ListCustomersQueryHandler.java
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── CustomerResponse.java
│   │   │   │   │       ├── CreateCustomerRequest.java
│   │   │   │   │       └── UpdateCustomerRequest.java
│   │   │   │   ├── invoice/
│   │   │   │   └── payment/
│   │   │   │
│   │   │   ├── domain/                     # Domain Layer (DDD)
│   │   │   │   ├── customer/
│   │   │   │   │   ├── Customer.java           # Aggregate Root
│   │   │   │   │   └── CustomerRepository.java # Repository Interface
│   │   │   │   ├── invoice/
│   │   │   │   │   ├── Invoice.java            # Aggregate Root
│   │   │   │   │   ├── LineItem.java           # Entity
│   │   │   │   │   ├── InvoiceStatus.java      # Enum
│   │   │   │   │   └── InvoiceRepository.java  # Repository Interface
│   │   │   │   ├── payment/
│   │   │   │   │   ├── Payment.java            # Aggregate Root
│   │   │   │   │   ├── PaymentMethod.java      # Enum
│   │   │   │   │   └── PaymentRepository.java  # Repository Interface
│   │   │   │   └── shared/
│   │   │   │       ├── BaseEntity.java         # Base class with ID
│   │   │   │       ├── DomainException.java    # Domain exception
│   │   │   │       ├── Email.java              # Value Object
│   │   │   │       └── Money.java              # Value Object
│   │   │   │
│   │   │   ├── infrastructure/             # Infrastructure Layer
│   │   │   │   ├── auth/
│   │   │   │   │   └── JpaUserRepository.java
│   │   │   │   ├── customer/
│   │   │   │   │   └── JpaCustomerRepository.java
│   │   │   │   ├── invoice/
│   │   │   │   │   ├── JpaInvoiceRepository.java
│   │   │   │   │   └── JpaLineItemRepository.java
│   │   │   │   └── payment/
│   │   │   │       └── JpaPaymentRepository.java
│   │   │   │
│   │   │   ├── config/                     # Configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── DataSeeder.java
│   │   │   │
│   │   │   └── OsgiliathApplication.java   # Main application class
│   │   │
│   │   └── resources/
│   │       ├── application.yml             # Main configuration
│   │       └── application-test.yml        # Test configuration
│   │
│   └── test/
│       └── java/com/osgiliath/
│           └── (test files)
│
└── pom.xml                                 # Maven dependencies
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.9+
- PostgreSQL 15+ (or Docker)

### 1. Start PostgreSQL

Using Docker Compose (recommended):
```bash
cd ..
docker-compose up -d
```

Or install PostgreSQL locally and create database:
```sql
CREATE DATABASE osgiliath;
CREATE USER osgiliath WITH PASSWORD 'osgiliath_password';
GRANT ALL PRIVILEGES ON DATABASE osgiliath TO osgiliath;
```

### 2. Build and Run

```bash
# Clean and build
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Or build JAR and run
./mvnw clean package
java -jar target/osgiliath-backend-1.0.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080/api`

### 3. Verify Installation

Check health endpoint:
```bash
curl http://localhost:8080/api/actuator/health
```

Access Swagger UI:
```
http://localhost:8080/api/swagger-ui.html
```

## Configuration

### Application Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/osgiliath
    username: osgiliath
    password: osgiliath_password

  jpa:
    hibernate:
      ddl-auto: update  # Change to 'validate' in production
    show-sql: true

server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: ${JWT_SECRET}  # Use environment variable in production
  expiration: 86400000   # 24 hours
```

### Environment Variables

For production, use environment variables:

```bash
export JWT_SECRET="your-256-bit-secret-key-here"
export SPRING_DATASOURCE_URL="jdbc:postgresql://prod-host:5432/osgiliath"
export SPRING_DATASOURCE_USERNAME="osgiliath"
export SPRING_DATASOURCE_PASSWORD="secure-password"
```

### Security Configuration

JWT authentication is configured in `SecurityConfig.java`:

- Public endpoints: `/auth/login`, `/auth/register`, `/swagger-ui.html`, `/v3/api-docs`
- Protected endpoints: All others require valid JWT token
- CORS: Configured for frontend origin (http://localhost:3000)

## API Endpoints

### Authentication

```
POST   /auth/register          Register new user
POST   /auth/login             Login and get JWT token
```

### Customers

```
POST   /customers              Create customer
GET    /customers              List customers (paginated, sortable)
GET    /customers/{id}         Get customer by ID
PUT    /customers/{id}         Update customer
DELETE /customers/{id}         Delete customer
```

### Invoices

```
POST   /invoices                       Create invoice (draft)
GET    /invoices                       List invoices (with filters)
GET    /invoices/{id}                  Get invoice by ID
PUT    /invoices/{id}                  Update invoice
DELETE /invoices/{id}                  Delete invoice
POST   /invoices/{id}/line-items       Add line item
DELETE /invoices/{id}/line-items/{lid} Remove line item
POST   /invoices/{id}/send             Send invoice
GET    /invoices/{id}/balance          Get invoice balance
```

### Payments

```
POST   /invoices/{invoiceId}/payments  Record payment
GET    /invoices/{invoiceId}/payments  List payments for invoice
GET    /payments/{id}                  Get payment by ID
```

For detailed API documentation with request/response examples, see [/docs/API.md](../docs/API.md).

## Database Schema

### Tables

#### customers
```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email_address VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_customer_email ON customers(email_address);
```

#### invoices
```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'PAID')),
    subtotal DECIMAL(19, 2) NOT NULL,
    tax_amount DECIMAL(19, 2) NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    balance_due DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_customer ON invoices(customer_id);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_issue_date ON invoices(issue_date);
```

#### line_items
```sql
CREATE TABLE line_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19, 2) NOT NULL CHECK (unit_price > 0),
    line_total DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_line_items_invoice ON line_items(invoice_id);
```

#### payments
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_date DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_invoice ON payments(invoice_id);
```

#### users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL
);
```

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Integration Tests

```bash
./mvnw verify -Pintegration-test
```

### Test Coverage

```bash
./mvnw test jacoco:report
```

Coverage report will be in `target/site/jacoco/index.html`

### Test Structure

- **Unit Tests**: Test individual domain methods and business logic
- **Integration Tests**: Test complete flows end-to-end with TestContainers

### Example Test

```java
@SpringBootTest
@Testcontainers
class InvoiceFlowIntegrationTest {

    @Test
    void shouldCreateInvoiceAndApplyPayment() {
        // Create customer
        Customer customer = customerRepository.save(
            Customer.create("John Doe", "john@example.com", null, null)
        );

        // Create invoice
        Invoice invoice = Invoice.create(
            customer.getId(),
            "INV-001",
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        );
        invoice.addLineItem("Service", BigDecimal.valueOf(2), Money.of(100));
        invoiceRepository.save(invoice);

        // Send invoice
        invoice.send();
        invoiceRepository.save(invoice);

        // Apply payment
        Payment payment = Payment.create(
            invoice.getId(),
            LocalDate.now(),
            Money.of(220),
            PaymentMethod.BANK_TRANSFER,
            "REF-123"
        );
        invoice.applyPayment(payment.getAmount());

        // Verify
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalanceDue().isZero()).isTrue();
    }
}
```

## Code Examples

### Creating a Customer

```java
// Domain layer - Customer aggregate
Customer customer = Customer.create(
    "Acme Corporation",
    "contact@acme.com",
    "+1-555-0123",
    "123 Main St, City, State 12345"
);

// Validation is enforced in the factory method
// Throws DomainException if invalid
```

### Creating an Invoice

```java
// Create invoice in draft status
Invoice invoice = Invoice.create(
    customerId,
    "INV-2024-001",
    LocalDate.now(),
    LocalDate.now().plusDays(30)
);

// Add line items
invoice.addLineItem(
    "Consulting Services",
    BigDecimal.valueOf(10),  // quantity
    Money.of(150)            // unit price
);

invoice.addLineItem(
    "Development Work",
    BigDecimal.valueOf(20),
    Money.of(200)
);

// Totals are calculated automatically
// subtotal = 1500 + 4000 = 5500
// tax = 550 (10%)
// total = 6050
```

### Sending an Invoice

```java
// Validate and send
invoice.send();

// Status: DRAFT → SENT
// Balance due is now equal to total amount
// Line items can no longer be modified
```

### Recording a Payment

```java
// Create payment
Payment payment = Payment.create(
    invoiceId,
    LocalDate.now(),
    Money.of(3000),
    PaymentMethod.CREDIT_CARD,
    "CC-REF-12345"
);

// Apply to invoice
invoice.applyPayment(payment.getAmount());

// Balance due is reduced
// If balance reaches zero, status changes to PAID
```

### Using CQRS

```java
// Command (Write)
@Service
@RequiredArgsConstructor
public class CreateCustomerHandler {
    private final CustomerRepository repository;

    public CustomerResponse handle(CreateCustomerCommand command) {
        Customer customer = Customer.create(
            command.getName(),
            command.getEmail(),
            command.getPhone(),
            command.getAddress()
        );

        Customer saved = repository.save(customer);

        return CustomerResponse.from(saved);
    }
}

// Query (Read)
@Service
@RequiredArgsConstructor
public class GetCustomerByIdQueryHandler {
    private final CustomerRepository repository;

    public CustomerResponse handle(GetCustomerByIdQuery query) {
        Customer customer = repository.findById(query.getId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));

        return CustomerResponse.from(customer);
    }
}
```

## Notes

- **Rich Domain Models**: Business logic lives in domain entities, not in services
- **Immutable Value Objects**: Email and Money are immutable and validated on creation
- **Transaction Management**: @Transactional on handlers ensures data consistency
- **Exception Handling**: Domain exceptions are caught and translated to HTTP responses
- **Soft Deletes**: Not implemented (hard deletes used for simplicity)
- **Optimistic Locking**: Can be added via @Version annotation if needed
- **Auditing**: CreatedAt and UpdatedAt tracked in BaseEntity

## Further Reading

- [Architecture Documentation](../docs/ARCHITECTURE.md)
- [API Reference](../docs/API.md)
- [Development Guide](../docs/DEVELOPMENT.md)
- [Deployment Guide](../docs/DEPLOYMENT.md)
