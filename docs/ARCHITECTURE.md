# Osgiliath Architecture Documentation

Comprehensive guide to the system architecture, design patterns, and technical decisions in the Osgiliath application.

## Table of Contents

- [Overview](#overview)
- [Architectural Principles](#architectural-principles)
- [Clean Architecture](#clean-architecture)
- [Domain-Driven Design (DDD)](#domain-driven-design-ddd)
- [CQRS Pattern](#cqrs-pattern)
- [Vertical Slice Architecture](#vertical-slice-architecture)
- [Frontend MVVM Architecture](#frontend-mvvm-architecture)
- [Database Design](#database-design)
- [Security Architecture](#security-architecture)
- [Design Decisions](#design-decisions)
- [Trade-offs](#trade-offs)

## Overview

Osgiliath implements a modern, layered architecture combining multiple design patterns to achieve:

- **Maintainability**: Clear separation of concerns and explicit dependencies
- **Testability**: Business logic isolated from infrastructure concerns
- **Scalability**: Stateless design and optimized data access patterns
- **Evolvability**: Easy to extend with new features without impacting existing code

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                             │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  Next.js 14 Frontend (React 18 + TypeScript)         │     │
│  │  MVVM Pattern: Models, Views, ViewModels             │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────┬───────────────────────────────────────────────┘
                  │ HTTPS/REST (JSON)
                  │ JWT Authentication
┌─────────────────▼───────────────────────────────────────────────┐
│                        API Layer                                │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  Spring Boot 3.2 REST Controllers                     │     │
│  │  Request/Response DTOs, Validation                    │     │
│  │  Exception Handling, OpenAPI Documentation            │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────┬───────────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────────┐
│                   Application Layer (CQRS)                      │
│  ┌─────────────────────┐       ┌─────────────────────────┐     │
│  │  Commands (Write)   │       │  Queries (Read)         │     │
│  │  - CreateCustomer   │       │  - GetCustomer          │     │
│  │  - CreateInvoice    │       │  - ListInvoices         │     │
│  │  - RecordPayment    │       │  - GetBalance           │     │
│  │  + Handlers         │       │  + Handlers             │     │
│  └─────────────────────┘       └─────────────────────────┘     │
└─────────────────┬───────────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────────┐
│                     Domain Layer (DDD)                          │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  Aggregates (Business Logic)                          │     │
│  │  - Customer: Manages customer lifecycle               │     │
│  │  - Invoice: Manages invoice & line items              │     │
│  │  - Payment: Manages payment recording                 │     │
│  │                                                        │     │
│  │  Value Objects: Email, Money                          │     │
│  │  Domain Rules: Validation, State Transitions          │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────┬───────────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────────┐
│                  Infrastructure Layer                           │
│  ┌───────────────────────────────────────────────────────┐     │
│  │  JPA Repositories (Spring Data)                       │     │
│  │  Database Mapping, Transaction Management             │     │
│  │  External Service Integrations                        │     │
│  └───────────────────────────────────────────────────────┘     │
└─────────────────┬───────────────────────────────────────────────┘
                  │ SQL/JDBC
┌─────────────────▼───────────────────────────────────────────────┐
│                PostgreSQL 15 Database                           │
│  Tables: customers, invoices, line_items, payments, users      │
└─────────────────────────────────────────────────────────────────┘
```

## Architectural Principles

### 1. Separation of Concerns

Each layer has a specific responsibility:

- **API Layer**: HTTP protocol, serialization, validation
- **Application Layer**: Use case orchestration, DTO mapping
- **Domain Layer**: Business logic, rules, invariants
- **Infrastructure Layer**: Persistence, external services

### 2. Dependency Inversion

Dependencies point inward toward the domain:

```
API → Application → Domain ← Infrastructure
```

- Domain has no dependencies
- Infrastructure implements domain interfaces
- Enables testing without external dependencies

### 3. Explicit Over Implicit

- Commands and queries are explicit classes
- Business operations have clear names (SendInvoice, RecordPayment)
- Validation rules are explicitly coded and tested

### 4. Immutability Where Possible

- Value Objects (Email, Money) are immutable
- DTOs are immutable records or final classes
- Reduces bugs related to unexpected state changes

## Clean Architecture

### Layer Responsibilities

#### API Layer (`com.osgiliath.api`)

**Responsibility**: Handle HTTP concerns

**Components**:
- REST Controllers
- Request/Response DTOs
- Exception Handlers
- OpenAPI Annotations

**Example**:
```java
@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CreateCustomerHandler handler;

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
        @Valid @RequestBody CreateCustomerRequest request) {

        CreateCustomerCommand command = mapper.toCommand(request);
        CustomerResponse response = handler.handle(command);
        return ResponseEntity.status(CREATED).body(response);
    }
}
```

**Key Principles**:
- No business logic in controllers
- DTOs never leak to other layers
- HTTP status codes follow REST conventions

#### Application Layer (`com.osgiliath.application`)

**Responsibility**: Orchestrate use cases

**Components**:
- Commands (write operations)
- Queries (read operations)
- Handlers (execute commands/queries)
- DTOs (data transfer)
- Mappers (domain ↔ DTO conversion)

**Example Command**:
```java
public record CreateCustomerCommand(
    String name,
    String email,
    String phone,
    String address
) {}

@Service
@RequiredArgsConstructor
public class CreateCustomerHandler {
    private final CustomerRepository repository;

    @Transactional
    public CustomerResponse handle(CreateCustomerCommand command) {
        // Validation happens in domain
        Customer customer = Customer.create(
            command.name(),
            command.email(),
            command.phone(),
            command.address()
        );

        Customer saved = repository.save(customer);
        return CustomerResponse.from(saved);
    }
}
```

**Key Principles**:
- Each handler does one thing
- Transactions at handler level
- No business logic, only orchestration

#### Domain Layer (`com.osgiliath.domain`)

**Responsibility**: Encapsulate business logic

**Components**:
- Aggregates (Customer, Invoice, Payment)
- Entities (LineItem)
- Value Objects (Email, Money)
- Repository Interfaces
- Domain Exceptions

**Example Aggregate**:
```java
@Entity
public class Invoice extends BaseEntity {
    private UUID customerId;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private List<LineItem> lineItems;
    private Money totalAmount;
    private Money balanceDue;

    // Factory method
    public static Invoice create(UUID customerId, String number,
                                  LocalDate issue, LocalDate due) {
        validateCustomerId(customerId);
        validateDates(issue, due);
        return new Invoice(customerId, number, issue, due);
    }

    // Business method
    public void send() {
        if (status != DRAFT) {
            throw new DomainException("Only draft invoices can be sent");
        }
        if (lineItems.isEmpty()) {
            throw new DomainException("Cannot send empty invoice");
        }
        this.status = SENT;
        this.balanceDue = this.totalAmount;
    }

    // Business method
    public void applyPayment(Money amount) {
        if (status != SENT) {
            throw new DomainException("Can only pay sent invoices");
        }
        if (amount.isGreaterThan(balanceDue)) {
            throw new DomainException("Payment exceeds balance");
        }
        this.balanceDue = this.balanceDue.subtract(amount);
        if (this.balanceDue.isZero()) {
            this.status = PAID;
        }
    }
}
```

**Key Principles**:
- Business rules in domain, not services
- Factory methods for creation
- Aggregates enforce invariants
- No dependencies on infrastructure

#### Infrastructure Layer (`com.osgiliath.infrastructure`)

**Responsibility**: Implement technical details

**Components**:
- JPA Repository Implementations
- Database Entity Mappings
- External Service Clients

**Example**:
```java
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(UUID id);
    Page<Customer> findAll(Pageable pageable);
    boolean existsByEmail(String email);
}

// Implementation
public interface JpaCustomerRepository
    extends CustomerRepository, JpaRepository<Customer, UUID> {
    // Spring Data provides implementation
}
```

**Key Principles**:
- Implements domain repository interfaces
- Handles persistence concerns
- Translates between domain and storage models

## Domain-Driven Design (DDD)

### Bounded Contexts

The system is organized into three bounded contexts:

#### 1. Customer Context

**Purpose**: Manage customer information and relationships

**Aggregate**: Customer
- **Entities**: Customer (aggregate root)
- **Value Objects**: Email
- **Invariants**:
  - Email must be unique
  - Name cannot be empty
  - Email must be valid format

**Business Operations**:
- Create customer with validation
- Update customer information
- Ensure email uniqueness

#### 2. Invoice Context

**Purpose**: Manage invoice lifecycle from creation to payment

**Aggregate**: Invoice
- **Entities**: Invoice (aggregate root), LineItem
- **Value Objects**: Money, InvoiceStatus (enum)
- **Invariants**:
  - Invoice must have line items before sending
  - Only DRAFT invoices can be edited
  - Due date must be after issue date
  - Balance cannot be negative

**Business Operations**:
- Create invoice in DRAFT status
- Add/remove line items (DRAFT only)
- Calculate totals automatically
- Send invoice (DRAFT → SENT)
- Apply payments (SENT only)
- Auto-transition to PAID when balance = 0

#### 3. Payment Context

**Purpose**: Track and apply payments against invoices

**Aggregate**: Payment
- **Entities**: Payment (aggregate root)
- **Value Objects**: Money, PaymentMethod (enum)
- **Invariants**:
  - Amount must be positive
  - Amount cannot exceed invoice balance
  - Payment can only be applied to SENT invoices

**Business Operations**:
- Record payment with validation
- Update invoice balance
- Track payment method and reference

### Aggregates

#### Design Rules for Aggregates

1. **Single Aggregate Root**: Only root can be referenced from outside
2. **Transaction Boundary**: Changes to aggregate are atomic
3. **Consistency Boundary**: Invariants enforced within aggregate
4. **Small Aggregates**: Invoice includes LineItems (can't exist independently)

#### Customer Aggregate

```
Customer (Aggregate Root)
├── id: UUID
├── name: String
├── email: Email (Value Object)
├── phone: String
├── address: String
└── Methods:
    ├── create(name, email, phone, address)
    ├── update(name, email, phone, address)
    └── validateEmail()
```

#### Invoice Aggregate

```
Invoice (Aggregate Root)
├── id: UUID
├── customerId: UUID (reference, not loaded)
├── invoiceNumber: String
├── issueDate: LocalDate
├── dueDate: LocalDate
├── status: InvoiceStatus
├── lineItems: List<LineItem> (entities within aggregate)
│   └── LineItem
│       ├── id: UUID
│       ├── description: String
│       ├── quantity: BigDecimal
│       ├── unitPrice: Money
│       └── lineTotal: Money
├── subtotal: Money
├── taxAmount: Money
├── totalAmount: Money
├── balanceDue: Money
└── Methods:
    ├── create(customerId, number, issueDate, dueDate)
    ├── addLineItem(desc, qty, price)
    ├── removeLineItem(lineItemId)
    ├── send()
    ├── applyPayment(amount)
    └── recalculateTotals()
```

#### Payment Aggregate

```
Payment (Aggregate Root)
├── id: UUID
├── invoiceId: UUID (reference)
├── paymentDate: LocalDate
├── amount: Money
├── paymentMethod: PaymentMethod
├── referenceNumber: String
└── Methods:
    └── create(invoiceId, date, amount, method, ref)
```

### Value Objects

#### Email Value Object

```java
@Embeddable
public class Email {
    private String address;

    private Email(String address) {
        this.address = address;
    }

    public static Email of(String address) {
        validateFormat(address);
        return new Email(address.toLowerCase());
    }

    private static void validateFormat(String email) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new DomainException("Invalid email format");
        }
    }

    public String getAddress() {
        return address;
    }

    // Equals and hashCode based on address
}
```

#### Money Value Object

```java
@Embeddable
public class Money {
    private BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    // Immutable, equals and hashCode
}
```

**Benefits of Value Objects**:
- Type safety (can't mix up Email and String)
- Validation in one place
- Immutability prevents bugs
- Semantic clarity in domain model

## CQRS Pattern

### Command Query Responsibility Segregation

Separates read operations (queries) from write operations (commands).

### Commands (Write Operations)

Commands mutate state and return minimal data (often just an ID).

**Structure**:
```java
// Command - immutable data holder
public record CreateInvoiceCommand(
    UUID customerId,
    String invoiceNumber,
    LocalDate issueDate,
    LocalDate dueDate,
    List<LineItemData> lineItems
) {}

// Handler - executes the command
@Service
@Transactional
@RequiredArgsConstructor
public class CreateInvoiceHandler {
    private final InvoiceRepository repository;
    private final CustomerRepository customerRepository;

    public UUID handle(CreateInvoiceCommand command) {
        // Validate customer exists
        if (!customerRepository.existsById(command.customerId())) {
            throw new NotFoundException("Customer not found");
        }

        // Create aggregate
        Invoice invoice = Invoice.create(
            command.customerId(),
            command.invoiceNumber(),
            command.issueDate(),
            command.dueDate()
        );

        // Add line items
        for (LineItemData item : command.lineItems()) {
            invoice.addLineItem(
                item.description(),
                item.quantity(),
                Money.of(item.unitPrice())
            );
        }

        // Persist
        Invoice saved = repository.save(invoice);
        return saved.getId();
    }
}
```

### Queries (Read Operations)

Queries retrieve data without mutating state.

**Structure**:
```java
// Query - request for data
public record GetInvoiceByIdQuery(UUID invoiceId) {}

// Handler - retrieves the data
@Service
@RequiredArgsConstructor
public class GetInvoiceByIdQueryHandler {
    private final InvoiceRepository repository;

    public Invoice handle(GetInvoiceByIdQuery query) {
        return repository.findById(query.invoiceId())
            .orElseThrow(() ->
                new NotFoundException("Invoice not found"));
    }
}
```

### Benefits of CQRS

1. **Clear Intent**: Operation name reveals purpose
2. **Optimized Reads**: Queries can use different data models
3. **Validation**: Commands validate before execution
4. **Testability**: Each handler is independently testable
5. **Scalability**: Read and write paths can scale independently

### CQRS in Osgiliath

#### Commands

- `CreateCustomerCommand`
- `UpdateCustomerCommand`
- `DeleteCustomerCommand`
- `CreateInvoiceCommand`
- `UpdateInvoiceCommand`
- `AddLineItemCommand`
- `RemoveLineItemCommand`
- `SendInvoiceCommand`
- `RecordPaymentCommand`

#### Queries

- `GetCustomerByIdQuery`
- `ListCustomersQuery`
- `GetInvoiceByIdQuery`
- `ListInvoicesQuery`
- `GetInvoiceBalanceQuery`
- `GetPaymentByIdQuery`
- `ListPaymentsForInvoiceQuery`

## Vertical Slice Architecture

### Feature-Based Organization

Code is organized by feature (business capability) rather than technical layers.

**Traditional Layered Approach** (Horizontal):
```
controllers/
  CustomerController.java
  InvoiceController.java
  PaymentController.java
services/
  CustomerService.java
  InvoiceService.java
  PaymentService.java
repositories/
  CustomerRepository.java
  InvoiceRepository.java
  PaymentRepository.java
```

**Vertical Slice Approach** (Feature-Based):
```
customer/
  api/
    CustomerController.java
  command/
    CreateCustomerCommand.java
    CreateCustomerHandler.java
    UpdateCustomerCommand.java
    UpdateCustomerHandler.java
  query/
    GetCustomerQuery.java
    GetCustomerQueryHandler.java
  dto/
    CustomerResponse.java
    CreateCustomerRequest.java
  domain/
    Customer.java
    CustomerRepository.java
  infrastructure/
    JpaCustomerRepository.java
```

### Benefits

1. **Feature Cohesion**: Related code stays together
2. **Easy Navigation**: Find all code for a feature in one place
3. **Reduced Coupling**: Features are independent
4. **Parallel Development**: Teams can work on different features
5. **Easy to Delete**: Remove entire feature folder

### In Osgiliath

We use a hybrid approach combining VSA with Clean Architecture:

```
application/
  customer/     # Vertical slice for customer features
    command/    # Write operations
    query/      # Read operations
    dto/        # Data transfer objects
  invoice/      # Vertical slice for invoice features
    command/
    query/
    dto/
  payment/      # Vertical slice for payment features
    command/
    query/
    dto

domain/
  customer/     # Customer domain model
  invoice/      # Invoice domain model
  payment/      # Payment domain model
  shared/       # Shared domain concepts

infrastructure/
  customer/     # Customer persistence
  invoice/      # Invoice persistence
  payment/      # Payment persistence

api/
  customer/     # Customer REST endpoints
  invoice/      # Invoice REST endpoints
  payment/      # Payment REST endpoints
```

## Frontend MVVM Architecture

### Model-View-ViewModel Pattern

The frontend follows MVVM to separate concerns:

```
┌─────────────────────────────────────────────────────┐
│                     Views                           │
│  React Components (Pages & UI Components)          │
│  - Render UI                                        │
│  - Handle user interactions                         │
│  - No business logic                                │
└───────────────┬─────────────────────────────────────┘
                │ Uses
┌───────────────▼─────────────────────────────────────┐
│                 ViewModels                          │
│  Custom React Hooks                                 │
│  - Manage state (loading, error, data)             │
│  - Orchestrate service calls                        │
│  - Form validation                                  │
│  - Business logic                                   │
└───────────────┬─────────────────────────────────────┘
                │ Uses
┌───────────────▼─────────────────────────────────────┐
│                  Services                           │
│  API Client Layer                                   │
│  - HTTP calls to backend                            │
│  - Request/response transformation                  │
│  - Error handling                                   │
└───────────────┬─────────────────────────────────────┘
                │ Uses
┌───────────────▼─────────────────────────────────────┐
│                  Models                             │
│  TypeScript Interfaces                              │
│  - Data structure definitions                       │
│  - Type safety                                      │
│  - No logic                                         │
└─────────────────────────────────────────────────────┘
```

### Example: Customer List Feature

#### Model (TypeScript Interface)

```typescript
// models/Customer.ts
export interface Customer {
  id: string;
  name: string;
  email: string;
  phone?: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}
```

#### Service (API Client)

```typescript
// services/customerService.ts
import apiClient from './apiClient';
import { Customer } from '../models/Customer';

export const customerService = {
  getAll: async (): Promise<Customer[]> => {
    const response = await apiClient.get<Customer[]>('/customers');
    return response.data;
  },

  create: async (data: Omit<Customer, 'id' | 'createdAt' | 'updatedAt'>):
    Promise<Customer> => {
    const response = await apiClient.post<Customer>('/customers', data);
    return response.data;
  }
};
```

#### ViewModel (React Hook)

```typescript
// viewmodels/useCustomerListViewModel.ts
import { useState, useEffect } from 'react';
import { customerService } from '../services/customerService';
import { Customer } from '../models/Customer';

export const useCustomerListViewModel = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadCustomers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await customerService.getAll();
      setCustomers(data);
    } catch (err) {
      setError('Failed to load customers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCustomers();
  }, []);

  return {
    customers,
    loading,
    error,
    refresh: loadCustomers
  };
};
```

#### View (React Component)

```typescript
// app/customers/page.tsx
'use client';
import { useCustomerListViewModel } from '../../viewmodels/useCustomerListViewModel';
import { Table } from '../../components/shared/Table';

export default function CustomersPage() {
  const { customers, loading, error, refresh } = useCustomerListViewModel();

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>Customers</h1>
      <Table data={customers} columns={['name', 'email', 'phone']} />
      <button onClick={refresh}>Refresh</button>
    </div>
  );
}
```

### Benefits of MVVM

1. **Separation**: UI logic separate from business logic
2. **Testability**: ViewModels can be tested without UI
3. **Reusability**: ViewModels can be used by multiple views
4. **Maintainability**: Changes to UI don't affect business logic

## Database Design

### Entity Relationship Diagram

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (PK)         │
│ username        │
│ password        │
│ email           │
│ enabled         │
│ created_at      │
└─────────────────┘

┌──────────────────────────┐
│       customers          │
├──────────────────────────┤
│ id (PK)                  │
│ name                     │
│ email_address (UNIQUE)   │
│ phone                    │
│ address                  │
│ created_at               │
│ updated_at               │
└───────────┬──────────────┘
            │
            │ 1:N
            │
┌───────────▼──────────────┐
│       invoices           │
├──────────────────────────┤
│ id (PK)                  │
│ customer_id (FK)         │
│ invoice_number (UNIQUE)  │
│ issue_date               │
│ due_date                 │
│ status                   │
│ subtotal                 │
│ tax_amount               │
│ total_amount             │
│ balance_due              │
│ created_at               │
│ updated_at               │
└───────────┬──────────────┘
            │
            │ 1:N
            │
┌───────────▼──────────────┐
│      line_items          │
├──────────────────────────┤
│ id (PK)                  │
│ invoice_id (FK)          │
│ description              │
│ quantity                 │
│ unit_price               │
│ line_total               │
│ created_at               │
└──────────────────────────┘

┌──────────────────────────┐
│       payments           │
├──────────────────────────┤
│ id (PK)                  │
│ invoice_id (FK)          │
│ payment_date             │
│ amount                   │
│ payment_method           │
│ reference_number         │
│ created_at               │
└──────────────────────────┘
```

### Indexing Strategy

```sql
-- Customer indexes
CREATE INDEX idx_customer_email ON customers(email_address);

-- Invoice indexes
CREATE UNIQUE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_customer ON invoices(customer_id);
CREATE INDEX idx_invoice_status ON invoices(status);
CREATE INDEX idx_invoice_issue_date ON invoices(issue_date);

-- Line item indexes
CREATE INDEX idx_line_items_invoice ON line_items(invoice_id);

-- Payment indexes
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
```

**Rationale**:
- Email lookups during customer creation (uniqueness check)
- Invoice queries by customer (common use case)
- Invoice filtering by status (dashboard, reporting)
- Date range queries on invoices
- Cascade operations on line items and payments

## Security Architecture

### JWT Authentication Flow

```
1. User Registration
   Client → POST /auth/register → Backend
   Backend → Hash password (BCrypt) → Store in DB
   Backend → Generate JWT → Return to Client

2. User Login
   Client → POST /auth/login → Backend
   Backend → Verify password → Generate JWT
   Backend → Return JWT to Client
   Client → Store JWT in localStorage

3. Authenticated Request
   Client → Add JWT to header → Backend
   Backend → Extract JWT → Validate signature
   Backend → Extract user details → Check permissions
   Backend → Process request → Return response

4. Token Expiration
   Client → Request with expired token → Backend
   Backend → Return 401 Unauthorized
   Client → Redirect to login page
```

### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf().disable()  // Using JWT, not cookies
            .cors()            // Configure CORS
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(STATELESS)  // No server sessions
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### Password Security

- **Algorithm**: BCrypt with strength 12
- **Salting**: Automatic per-password random salt
- **Storage**: Only hashed passwords stored, never plaintext
- **Verification**: Constant-time comparison

## Design Decisions

### Why Clean Architecture?

**Decision**: Organize code by layers with dependency inversion

**Rationale**:
- Business logic (domain) has no external dependencies
- Easy to test domain logic in isolation
- Infrastructure can be swapped without affecting business logic
- Clear boundaries between concerns

**Trade-off**: More files and layers vs. simpler flat structure

### Why CQRS?

**Decision**: Separate read and write operations

**Rationale**:
- Clear intent of each operation
- Can optimize reads and writes independently
- Easy to understand what each operation does
- Aligns with user tasks (command-driven)

**Trade-off**: More boilerplate (handlers) vs. shared service methods

### Why DDD?

**Decision**: Model business domains as aggregates

**Rationale**:
- Business rules enforced in one place (aggregate)
- Impossible to create invalid states
- Clear transaction boundaries
- Aligns with business concepts

**Trade-off**: Learning curve vs. anemic models with service logic

### Why Vertical Slices?

**Decision**: Organize features vertically, not by technical layers

**Rationale**:
- Related code stays together
- Easy to find all code for a feature
- Features are loosely coupled
- Easy to add/remove features

**Trade-off**: Some code duplication vs. excessive abstraction

### Why PostgreSQL?

**Decision**: Use PostgreSQL as primary database

**Rationale**:
- ACID compliance ensures data integrity
- Rich data types (UUID, JSONB if needed)
- Mature, reliable, well-documented
- Good performance for relational data

**Alternative**: H2 for development, but PostgreSQL for production

### Why Next.js?

**Decision**: Use Next.js 14 for frontend

**Rationale**:
- Server-side rendering for better SEO
- App Router for modern routing
- Built-in optimization (images, fonts)
- TypeScript support

**Trade-off**: Learning curve vs. plain React

## Trade-offs

### Complexity vs. Maintainability

**Chosen**: Higher initial complexity for long-term maintainability

- More files (commands, queries, handlers)
- Clear structure pays off as system grows
- Easy to onboard new developers

### Performance vs. Correctness

**Chosen**: Correctness first, optimize when needed

- Domain validation on every operation
- Transactions ensure data consistency
- Can add caching later if needed

### Flexibility vs. Simplicity

**Chosen**: Flexible architecture for future growth

- Abstraction layers allow swapping implementations
- CQRS allows different read/write models later
- Can evolve without major rewrites

### Type Safety vs. Speed

**Chosen**: Strong typing in both backend and frontend

- Java 17 with records for DTOs
- TypeScript for frontend
- Catches errors at compile time
- Worth the extra typing

---

**This architecture balances modern best practices with pragmatic implementation, providing a solid foundation for a production-ready invoice management system.**
