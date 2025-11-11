# Osgiliath - Technical Architecture Writeup

## Executive Summary

Osgiliath is a production-quality ERP invoicing system built with enterprise-grade architectural patterns: Domain-Driven Design (DDD), Command Query Responsibility Segregation (CQRS), and Vertical Slice Architecture (VSA). The system manages the complete lifecycle of customers, invoices, and payments with clean separation of concerns and scalable design.

**Technology Stack**:
- Backend: Java 17, Spring Boot 3.2, Spring Data JPA
- Frontend: TypeScript, Next.js 14 (App Router), React 18
- Database: PostgreSQL 15
- Authentication: JWT (JSON Web Tokens)
- PDF Generation: iText 7
- Testing: JUnit 5, Spring Boot Test

---

## 1. Architectural Principles

### 1.1 Domain-Driven Design (DDD)

The system implements DDD with rich domain entities that encapsulate business logic and enforce invariants.

**Aggregate Roots**:
- **Customer**: Manages customer information with email validation
- **Invoice**: Manages invoice lifecycle, line items, and balance calculations
- **Payment**: Records immutable payment transactions

**Value Objects**:
- **Money**: Encapsulates currency amounts with precision handling
- **Email**: Validates and encapsulates email addresses

**Domain Boundaries**:
Each aggregate root maintains its own consistency boundary. Cross-aggregate references use IDs rather than direct object references, ensuring proper transactional boundaries.

**Rich Domain Logic Examples**:
- `Invoice.send()`: Validates invoice has line items before transitioning from DRAFT to SENT
- `Invoice.recordPayment()`: Validates payment amount, updates balance, auto-transitions to PAID when balance reaches zero
- `Invoice.isOverdue()`: Business logic for determining overdue status based on due date and payment status
- `Customer.updateEmail()`: Validates email format before updating

### 1.2 Command Query Responsibility Segregation (CQRS)

Write operations (Commands) and read operations (Queries) are strictly separated:

**Commands** (Write Operations):
- `CreateCustomerCommand` / `UpdateCustomerCommand` / `DeleteCustomerCommand`
- `CreateInvoiceCommand` / `UpdateInvoiceCommand` / `SendInvoiceCommand` / `MarkInvoiceAsPaidCommand`
- `RecordPaymentCommand`

**Queries** (Read Operations):
- `ListCustomersQuery` / `GetCustomerByIdQuery`
- `ListInvoicesQuery` / `GetInvoiceByIdQuery` / `ExportInvoiceToPdfQuery`
- `ListPaymentsForInvoiceQuery`

**Benefits**:
- Clear separation of concerns
- Optimized read/write models
- Easier to scale reads independently
- Simplified testing (commands test business logic, queries test data retrieval)

### 1.3 Vertical Slice Architecture (VSA)

Code is organized by feature/use case rather than technical layers:

```
backend/src/main/java/com/osgiliath/
├── domain/              # Domain layer (entities, value objects, repositories)
│   ├── customer/
│   ├── invoice/
│   ├── payment/
│   └── shared/
├── application/         # Application layer (commands, queries, handlers)
│   ├── customer/
│   │   ├── command/
│   │   └── query/
│   ├── invoice/
│   │   ├── CreateInvoiceCommand.java
│   │   ├── CreateInvoiceHandler.java
│   │   ├── SendInvoiceCommand.java
│   │   ├── SendInvoiceHandler.java
│   │   └── ...
│   └── payment/
├── api/                 # API layer (REST controllers, DTOs)
│   ├── customer/
│   ├── invoice/
│   └── payment/
├── infrastructure/      # Infrastructure layer (JPA repositories, persistence)
│   ├── customer/
│   ├── invoice/
│   └── payment/
└── config/              # Cross-cutting configuration
```

Each feature slice contains everything needed for that feature: domain logic, application handlers, API endpoints, and infrastructure.

### 1.4 Clean Architecture (Layered)

Dependencies point inward (from infrastructure → application → domain):

1. **Domain Layer** (core business logic, no dependencies):
   - Entities: `Customer`, `Invoice`, `Payment`, `LineItem`
   - Value Objects: `Money`, `Email`
   - Repository Interfaces: `CustomerRepository`, `InvoiceRepository`, `PaymentRepository`

2. **Application Layer** (use cases, depends only on domain):
   - Command Handlers: Execute business operations
   - Query Handlers: Retrieve data for display
   - DTOs: Data transfer objects for API boundaries
   - Mappers: Convert between domain entities and DTOs

3. **Infrastructure Layer** (technical details, implements domain interfaces):
   - JPA Repositories: `JpaCustomerRepository`, `JpaInvoiceRepository`
   - Database Entities: Annotated with `@Entity`, `@Table`, etc.
   - External integrations (none currently, but would go here)

4. **API Layer** (HTTP/REST interface):
   - Controllers: `CustomerController`, `InvoiceController`, `PaymentController`
   - Request/Response DTOs
   - OpenAPI documentation

**Dependency Rule**: The domain layer has zero dependencies on Spring, JPA, or any framework. This ensures the core business logic is portable and testable.

---

## 2. Database Schema

### 2.1 Entity-Relationship Diagram

```
┌─────────────────┐
│     users       │
├─────────────────┤
│ id (UUID) PK    │
│ username        │
│ password_hash   │
│ email           │
│ created_at      │
└─────────────────┘

┌─────────────────────┐
│     customers       │
├─────────────────────┤
│ id (UUID) PK        │
│ name                │
│ email               │
│ phone               │
│ address             │
│ created_at          │
│ updated_at          │
└─────────────────────┘
         │
         │ 1
         │
         │ *
┌─────────────────────┐
│     invoices        │
├─────────────────────┤
│ id (UUID) PK        │
│ invoice_number      │
│ customer_id (UUID)  │────┐
│ issue_date          │    │
│ due_date            │    │
│ status              │    │
│ subtotal            │    │
│ tax_rate            │    │
│ tax_amount          │    │
│ total_amount        │    │
│ balance_due         │    │
│ created_at          │    │
│ updated_at          │    │
└─────────────────────┘    │
         │                 │
         │ 1               │
         │                 │
         │ *               │
┌─────────────────────┐    │
│    line_items       │    │
├─────────────────────┤    │
│ id (BIGINT) PK      │    │
│ invoice_id (UUID)   │────┘
│ description         │
│ quantity            │
│ unit_price          │
│ line_total          │
└─────────────────────┘
         │
         │ 1
         │
         │ *
┌─────────────────────┐
│     payments        │
├─────────────────────┤
│ id (UUID) PK        │
│ invoice_id (UUID)   │────┐
│ amount              │    │
│ payment_date        │    │
│ payment_method      │    │
│ reference_number    │    │
│ created_at          │    │
└─────────────────────┘    │
                           │
                           └───> FK to invoices.id
```

### 2.2 Table Definitions

**users**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**customers**
```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**invoices**
```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    subtotal DECIMAL(19,2) NOT NULL,
    tax_rate DECIMAL(5,2) NOT NULL,
    tax_amount DECIMAL(19,2) NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    balance_due DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
```

**line_items**
```sql
CREATE TABLE line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id UUID NOT NULL,
    description TEXT NOT NULL,
    quantity DECIMAL(19,2) NOT NULL,
    unit_price DECIMAL(19,2) NOT NULL,
    line_total DECIMAL(19,2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE INDEX idx_line_items_invoice_id ON line_items(invoice_id);
```

**payments**
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
CREATE INDEX idx_payments_date ON payments(payment_date);
```

### 2.3 Data Integrity

**Referential Integrity**:
- Invoices reference Customers (cannot delete customer with invoices)
- Line Items cascade delete with Invoice (deleting invoice removes line items)
- Payments reference Invoices (cannot delete invoice with payments)

**Constraints**:
- Unique invoice numbers prevent duplicates
- Unique usernames and emails prevent duplicate accounts
- NOT NULL constraints on critical fields ensure data completeness
- CHECK constraints on enums (enforced at application level via JPA)

**Indexes**:
- Primary keys (automatic)
- Foreign key indexes for join performance
- Status index for filtering invoices
- Due date index for overdue queries
- Payment date index for reporting

---

## 3. Design Decisions

### 3.1 Technology Choices

**Java Spring Boot**:
- **Rationale**: Industry-standard for enterprise applications, excellent ecosystem, strong DDD support
- **Trade-offs**: Heavier than lightweight frameworks, but provides robust infrastructure
- **Benefits**: Built-in dependency injection, transaction management, data access, security

**Next.js 14 with App Router**:
- **Rationale**: Modern React framework with excellent developer experience, server-side rendering capabilities
- **Trade-offs**: Learning curve for App Router, but better performance and SEO
- **Benefits**: File-based routing, API routes, TypeScript support, fast refresh

**PostgreSQL**:
- **Rationale**: Production-ready RDBMS with excellent ACID compliance, JSON support for future extensibility
- **Trade-offs**: More setup than H2, but necessary for production deployment
- **Benefits**: Reliable, scalable, strong transactional support, widely supported

**JWT Authentication**:
- **Rationale**: Stateless authentication suitable for REST APIs, scalable across multiple servers
- **Trade-offs**: Cannot revoke tokens without additional infrastructure
- **Benefits**: No server-side session storage, works well with microservices

### 3.2 Invoice Numbering Strategy

**Implementation**: Sequential numbering with prefix (INV-0001, INV-0002, etc.)

**Decision Factors**:
- Human-readable and sortable
- Generated at invoice creation (DRAFT status)
- Globally unique via database sequence
- Never reused, even if invoice is deleted

**Alternative Considered**: UUID-based numbers (rejected for poor readability)

### 3.3 Money Handling

**Implementation**: `Money` value object using `BigDecimal` for precision

**Decision Factors**:
- No floating-point errors (critical for financial calculations)
- Encapsulates currency logic in one place
- Type-safe (cannot accidentally mix Money with primitives)
- Immutable to prevent accidental modification

**Example**:
```java
public class Money {
    private final BigDecimal amount;

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
}
```

### 3.4 Invoice Status State Machine

**States**: DRAFT → SENT → PAID (with OVERDUE and CANCELLED variations)

**Transitions**:
- DRAFT → SENT: `sendInvoice()` - requires at least one line item
- SENT → PAID: Automatic when `balance_due` reaches zero
- SENT → OVERDUE: Automatic via scheduled job (checks `due_date < today` and status = SENT)
- Any → CANCELLED: Manual cancellation (business requirement: preserve audit trail)

**Design Decision**: Status is stored as enum in database, state transitions enforced in domain entity

**Benefits**:
- Business rules centralized in domain
- Prevents invalid state transitions
- Clear audit trail of invoice lifecycle

### 3.5 Payment Recording Strategy

**Approach**: Immutable payment records with balance tracking on Invoice

**Implementation**:
1. Create immutable `Payment` entity
2. Reduce `Invoice.balance_due` by payment amount
3. Auto-transition invoice to PAID if balance reaches zero
4. Prevent overpayment (payment amount cannot exceed balance due)

**Decision Factors**:
- Audit trail: All payments preserved forever
- Partial payments: Supports multiple payments per invoice
- Refunds: Would create negative payments (not yet implemented)
- Idempotency: Reference numbers prevent duplicate processing

**Alternative Considered**: Ledger-based accounting (rejected as over-engineering for MVP)

### 3.6 CQRS Without Event Sourcing

**Decision**: Implement CQRS pattern without full event sourcing

**Rationale**:
- CQRS provides clear separation of concerns
- Event sourcing adds significant complexity
- Current requirements don't need full event replay
- Can add event sourcing later if needed (domain events already in place)

**Benefits**:
- Simpler implementation
- Easier debugging
- Faster development
- Still maintains CQRS benefits

### 3.7 Frontend MVVM Pattern

**Structure**:
- **Models**: TypeScript interfaces matching API DTOs
- **ViewModels**: Custom React hooks encapsulating business logic
- **Views**: React components (purely presentational)

**Example**:
```typescript
// ViewModel (custom hook)
export const useInvoiceListViewModel = () => {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [filters, setFilters] = useState<Filters>({});

  const fetchInvoices = async () => {
    const data = await invoiceService.getInvoices(page, size, filters);
    setInvoices(data.content);
  };

  return { invoices, filters, setFilters, fetchInvoices };
};

// View (React component)
export const InvoiceList = () => {
  const { invoices, filters, setFilters } = useInvoiceListViewModel();

  return <Table data={invoices} />;
};
```

**Benefits**:
- Business logic testable without rendering components
- Views remain simple and focused on UI
- Clear separation of concerns
- Reusable ViewModels across multiple views

### 3.8 API Design Principles

**RESTful Conventions**:
- `GET /api/customers` - List customers (with pagination)
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer by ID
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

**Command Endpoints** (non-standard REST for domain operations):
- `POST /api/invoices/{id}/send` - Send invoice (state transition)
- `POST /api/invoices/{id}/mark-paid` - Mark as paid
- `POST /api/invoices/{id}/cancel` - Cancel invoice
- `POST /api/invoices/{id}/payments` - Record payment

**Decision**: Use command-style endpoints for domain operations rather than generic PUT

**Rationale**:
- Makes intent explicit (`/send` vs `PUT /invoices/{id}` with status field)
- Prevents accidental state mutations
- Clearer API documentation
- Better aligned with CQRS commands

### 3.9 Pagination Strategy

**Implementation**: Spring Data Page abstraction

**Response Format**:
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0
  }
}
```

**Benefits**:
- Standard Spring Data format
- Frontend can easily implement pagination controls
- Includes metadata for total count
- Supports sorting via `Pageable` parameter

### 3.10 Error Handling Strategy

**Domain Exceptions**:
- `DomainException`: Base exception for business rule violations
- Thrown from domain entities when invariants are violated
- Examples: "Cannot send invoice without line items", "Payment exceeds balance due"

**Exception Handling**:
- `@ControllerAdvice` catches exceptions globally
- Returns appropriate HTTP status codes (400 for validation, 404 for not found, 500 for server errors)
- Consistent error response format

**Benefits**:
- Centralized error handling
- Consistent API error responses
- Domain layer throws business exceptions, infrastructure layer translates to HTTP

### 3.11 Testing Strategy

**Unit Tests**:
- Domain entities (pure business logic, no dependencies)
- Command/Query handlers (mock repositories)
- Value objects (Money, Email validation)

**Integration Tests**:
- End-to-end flows (create customer → create invoice → record payment)
- Repository layer (actual database queries)
- API layer (Spring MockMvc tests)

**Test Coverage**:
- Domain layer: >90% (critical business logic)
- Application layer: >80% (command/query handlers)
- Infrastructure layer: >70% (database integration)

**Design Decision**: Focus on integration tests over mocking

**Rationale**:
- Integration tests catch more real-world issues
- Domain logic is well-isolated and easy to unit test
- Database queries need real database testing
- Spring Boot Test makes integration tests fast

---

## 4. Performance Considerations

### 4.1 Database Optimization

**Indexes**:
- Primary keys (automatic B-tree indexes)
- Foreign keys (for join performance)
- Status field (for filtering by invoice status)
- Due date (for overdue invoice queries)
- Customer ID (for customer invoice lookup)

**N+1 Query Prevention**:
- `@EntityGraph` for eager loading line items with invoice
- Single query fetches invoice + line items
- Prevents multiple queries when displaying invoice details

**Connection Pooling**:
- HikariCP (Spring Boot default)
- Configured for optimal performance

### 4.2 Frontend Optimization

**Code Splitting**:
- Next.js automatic code splitting per route
- Lazy loading of components
- Reduces initial bundle size

**Caching**:
- Browser caching of static assets
- SWR (stale-while-revalidate) pattern for data fetching
- Optimistic UI updates

**Pagination**:
- Server-side pagination (prevents loading all data)
- Default page size: 10 items
- Configurable page size

### 4.3 API Response Optimization

**DTOs**:
- Lightweight DTOs prevent over-fetching
- Only return necessary fields
- Invoice list returns summary (no line items)
- Invoice detail returns full object with line items

**Projection**:
- JPA projections for list queries (select only needed columns)
- Full entities only for detail views

---

## 5. Security Considerations

### 5.1 Authentication

**JWT Token Security**:
- Tokens signed with secret key
- Expiration time: 24 hours (configurable)
- Stored in localStorage (client-side)
- Sent via Authorization header

**Password Security**:
- BCrypt hashing (strength: 10)
- Salted hashes (automatic with BCrypt)
- Never store plaintext passwords

### 5.2 Authorization

**Current Implementation**:
- All authenticated users have full access
- Future: Role-based access control (ADMIN, USER, VIEWER)

**API Security**:
- All endpoints require authentication (except /auth/login, /auth/register)
- JWT filter validates token on every request
- CORS configured to allow only frontend origin

### 5.3 Input Validation

**DTO Validation**:
- `@Valid` annotation on controller parameters
- Bean Validation annotations (`@NotNull`, `@Email`, `@Min`, `@Max`)
- Custom validators for business rules

**Domain Validation**:
- Email format validation in `Email` value object
- Money amount validation (no negative amounts)
- Invoice state transition validation

---

## 6. Scalability Considerations

### 6.1 Horizontal Scaling

**Stateless Design**:
- JWT authentication (no server-side sessions)
- RESTful API (stateless by design)
- No in-memory caches (currently)

**Benefits**:
- Can run multiple backend instances behind load balancer
- No session affinity required
- Easy to scale horizontally

### 6.2 Database Scaling

**Read Replicas**:
- CQRS separates reads from writes
- Queries can be routed to read replicas
- Commands execute on primary database

**Partitioning**:
- Invoices can be partitioned by year
- Archive old invoices to separate tables
- Improves query performance

### 6.3 Caching Strategy (Future)

**Redis Cache**:
- Cache frequently accessed data (customer list, invoice list)
- Cache invalidation on write commands
- Reduces database load

**CDN**:
- Static assets served from CDN
- Reduces server load
- Improves global performance

---

## 7. Future Enhancements

### 7.1 Event Sourcing

**Current**: Traditional CRUD with domain events (not persisted)
**Future**: Full event sourcing with event store
**Benefits**: Complete audit trail, time-travel debugging, event replay

### 7.2 Microservices

**Current**: Monolithic application
**Future**: Separate services for Customer, Invoice, Payment
**Benefits**: Independent deployment, technology diversity, team autonomy

### 7.3 Reporting & Analytics

**Current**: Basic invoice list with filters
**Future**: Dashboard with revenue charts, aging reports, payment trends
**Implementation**: Separate read model (CQRS), potentially using ElasticSearch

### 7.4 Email Notifications

**Current**: Manual invoice sending (status change only)
**Future**: Email invoices to customers, payment reminders, overdue notifications
**Implementation**: Spring Mail + Thymeleaf templates

### 7.5 Multi-Currency Support

**Current**: Single currency (USD assumed)
**Future**: Multi-currency with exchange rates
**Implementation**: Add `currency` field to `Money` value object, integrate exchange rate API

---

## 8. Deployment Architecture

### 8.1 Production Deployment

**Infrastructure** (AWS or Azure):
```
┌─────────────────────────────────────────────────────┐
│                   Load Balancer                     │
│                  (ALB / App Gateway)                │
└───────────────────┬─────────────────────────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
    ┌────▼─────┐         ┌────▼─────┐
    │ Backend  │         │ Backend  │
    │ Instance │         │ Instance │
    │  (EC2)   │         │  (EC2)   │
    └────┬─────┘         └────┬─────┘
         │                     │
         └──────────┬──────────┘
                    │
              ┌─────▼─────┐
              │ PostgreSQL│
              │    RDS    │
              └───────────┘

    ┌─────────────────┐
    │   Frontend      │
    │   (Vercel or    │
    │    S3 + CF)     │
    └─────────────────┘
```

**Components**:
- **Backend**: Spring Boot on EC2 or ECS (containerized)
- **Database**: PostgreSQL RDS (Multi-AZ for high availability)
- **Frontend**: Next.js on Vercel or S3 + CloudFront
- **Load Balancer**: Application Load Balancer or Azure App Gateway

### 8.2 Environment Configuration

**Development**:
- H2 in-memory database (optional)
- Hot reload enabled
- Debug logging

**Staging**:
- PostgreSQL (separate from production)
- Production-like configuration
- Integration testing

**Production**:
- PostgreSQL RDS
- Error logging only
- Performance monitoring
- Automated backups

---

## 9. Conclusion

Osgiliath demonstrates enterprise-grade architecture with clear separation of concerns, robust domain modeling, and scalable design. The implementation adheres to DDD principles with rich domain entities, CQRS for command-query separation, and Vertical Slice Architecture for maintainability.

Key architectural strengths:
- **Domain-centric design**: Business logic encapsulated in domain entities
- **Clean boundaries**: Clear separation between layers with dependency inversion
- **Testability**: Isolated domain logic enables comprehensive testing
- **Scalability**: Stateless design supports horizontal scaling
- **Maintainability**: Feature-based organization simplifies understanding and modification

The system is production-ready and can be deployed to AWS or Azure with minimal configuration changes.
