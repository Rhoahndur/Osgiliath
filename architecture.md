# Osgiliath Architecture Diagrams

**Project:** Osgiliath - AI-Assisted Full-Stack ERP Assessment  
**Date:** November 7, 2025  
**Version:** 1.0
 
---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Clean Architecture Layers](#2-clean-architecture-layers)
3. [Domain-Driven Design (DDD) Bounded Contexts](#3-domain-driven-design-bounded-contexts)
4. [CQRS Pattern Implementation](#4-cqrs-pattern-implementation)
5. [Vertical Slice Architecture](#5-vertical-slice-architecture)
6. [Database Schema](#6-database-schema)
7. [API Architecture](#7-api-architecture)
8. [Frontend Architecture (MVVM)](#8-frontend-architecture-mvvm)
9. [Request Flow Diagrams](#9-request-flow-diagrams)
10. [Deployment Architecture](#10-deployment-architecture)

---

## 1. System Overview

### High-Level System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        UI[React/Next.js Frontend<br/>TypeScript]
    end
    
    subgraph "API Gateway"
        AG[Spring Boot REST API<br/>Java 17+]
    end
    
    subgraph "Application Core"
        APP[Application Layer<br/>Commands & Queries]
        DOM[Domain Layer<br/>Business Logic]
    end
    
    subgraph "Infrastructure"
        INFRA[Infrastructure Layer<br/>Repositories & Services]
        DB[(PostgreSQL Database)]
    end
    
    subgraph "Security"
        AUTH[Spring Security<br/>Authentication]
    end
    
    UI -->|HTTPS/REST| AG
    AG -->|Authentication| AUTH
    AUTH -->|Authorize| APP
    AG -->|Route Requests| APP
    APP -->|Business Logic| DOM
    APP -->|Data Access| INFRA
    INFRA -->|SQL| DB
    
    style UI fill:#e1f5ff
    style AG fill:#fff4e1
    style APP fill:#e8f5e9
    style DOM fill:#f3e5f5
    style INFRA fill:#fce4ec
    style DB fill:#e0e0e0
    style AUTH fill:#fff9c4
```

---

## 2. Clean Architecture Layers

### Dependency Flow and Layer Separation

```mermaid
graph TB
    subgraph "API Layer (Controllers)"
        C1[Customer Controller]
        C2[Invoice Controller]
        C3[Payment Controller]
    end
    
    subgraph "Application Layer (Use Cases)"
        subgraph "Commands (Write)"
            CMD1[Create Customer]
            CMD2[Create Invoice]
            CMD3[Record Payment]
        end
        
        subgraph "Queries (Read)"
            QRY1[Get Customer]
            QRY2[List Invoices]
            QRY3[Get Balance]
        end
        
        subgraph "Handlers"
            H1[Command Handlers]
            H2[Query Handlers]
        end
    end
    
    subgraph "Domain Layer (Business Logic)"
        AGG1[Customer Aggregate]
        AGG2[Invoice Aggregate]
        AGG3[Payment Aggregate]
        VO[Value Objects]
        DR[Domain Rules]
        DE[Domain Events]
    end
    
    subgraph "Infrastructure Layer (Technical Details)"
        REPO1[JPA Repositories]
        REPO2[Database Entities]
        EXT[External Services]
    end
    
    DB[(PostgreSQL)]
    
    C1 & C2 & C3 -->|DTOs| CMD1 & CMD2 & CMD3 & QRY1 & QRY2 & QRY3
    CMD1 & CMD2 & CMD3 --> H1
    QRY1 & QRY2 & QRY3 --> H2
    H1 & H2 -->|Use| AGG1 & AGG2 & AGG3
    AGG1 & AGG2 & AGG3 -->|Contains| VO
    AGG1 & AGG2 & AGG3 -->|Enforce| DR
    AGG1 & AGG2 & AGG3 -->|Publish| DE
    H1 & H2 -->|Access via Interfaces| REPO1
    REPO1 -->|Implement| REPO2
    REPO2 -->|Persist| DB
    
    style C1 fill:#e1f5ff
    style C2 fill:#e1f5ff
    style C3 fill:#e1f5ff
    style CMD1 fill:#e8f5e9
    style CMD2 fill:#e8f5e9
    style CMD3 fill:#e8f5e9
    style QRY1 fill:#fff9c4
    style QRY2 fill:#fff9c4
    style QRY3 fill:#fff9c4
    style AGG1 fill:#f3e5f5
    style AGG2 fill:#f3e5f5
    style AGG3 fill:#f3e5f5
    style REPO1 fill:#fce4ec
    style REPO2 fill:#fce4ec
```

### Dependency Rule Visualization

```mermaid
graph LR
    A[API Layer] -->|depends on| B[Application Layer]
    B -->|depends on| C[Domain Layer]
    D[Infrastructure Layer] -->|implements| C
    D -->|implements| B
    
    style A fill:#e1f5ff
    style B fill:#e8f5e9
    style C fill:#f3e5f5
    style D fill:#fce4ec
    
    Note1[Dependencies point INWARD<br/>Domain has NO dependencies]
```

---

## 3. Domain-Driven Design (DDD) Bounded Contexts

### Bounded Contexts and Aggregates

```mermaid
graph TB
    subgraph "Customer Context"
        CA[Customer Aggregate Root]
        CA --> CID[Customer ID]
        CA --> CNAME[Name]
        CA --> CEMAIL[Email - Value Object]
        CA --> CADDR[Address - Value Object]
        CA --> CMETHODS[Business Methods:<br/>- Create<br/>- Update<br/>- Validate]
    end
    
    subgraph "Invoice Context"
        IA[Invoice Aggregate Root]
        IA --> IID[Invoice ID]
        IA --> INUM[Invoice Number]
        IA --> ISTAT[Status Enum:<br/>DRAFT/SENT/PAID]
        IA --> IAMT[Amounts]
        IA --> LI[Line Items Collection]
        LI --> LI1[Line Item Entity]
        LI --> LI2[Line Item Entity]
        IA --> IMETHODS[Business Methods:<br/>- AddLineItem<br/>- CalculateTotal<br/>- Send<br/>- ApplyPayment]
    end
    
    subgraph "Payment Context"
        PA[Payment Aggregate Root]
        PA --> PID[Payment ID]
        PA --> PAMT[Amount - Value Object]
        PA --> PMETH[Payment Method]
        PA --> PDATE[Payment Date]
        PA --> PMETHODS[Business Methods:<br/>- Create<br/>- Validate]
    end
    
    CA -.->|Reference by ID| IA
    IA -.->|Reference by ID| PA
    
    subgraph "Shared Kernel"
        SK[Common Value Objects:<br/>- Money<br/>- Email<br/>- Date Range]
    end
    
    CA --> SK
    IA --> SK
    PA --> SK
    
    style CA fill:#bbdefb
    style IA fill:#c8e6c9
    style PA fill:#f8bbd0
    style SK fill:#fff9c4
```

### Aggregate Relationships

```mermaid
erDiagram
    CUSTOMER ||--o{ INVOICE : "has many"
    INVOICE ||--o{ LINE-ITEM : "contains"
    INVOICE ||--o{ PAYMENT : "receives"
    
    CUSTOMER {
        UUID id PK
        string name
        string email
        string phone
        string address
    }
    
    INVOICE {
        UUID id PK
        UUID customerId FK
        string invoiceNumber
        date issueDate
        date dueDate
        enum status
        decimal total
        decimal balance
    }
    
    LINE-ITEM {
        UUID id PK
        UUID invoiceId FK
        string description
        decimal quantity
        decimal unitPrice
        decimal lineTotal
    }
    
    PAYMENT {
        UUID id PK
        UUID invoiceId FK
        date paymentDate
        decimal amount
        string paymentMethod
    }
```

---

## 4. CQRS Pattern Implementation

### Command and Query Separation

```mermaid
graph TB
    subgraph "Client"
        CLIENT[React Frontend]
    end
    
    subgraph "API Gateway"
        API[REST Controllers]
    end
    
    subgraph "Write Side (Commands)"
        direction TB
        CMD[Commands]
        CMDH[Command Handlers]
        CMDREPO[Write Repository]
        CMDDB[(Write DB)]
        
        CMD -->|Execute| CMDH
        CMDH -->|Validate & Apply| DOM
        CMDH -->|Persist| CMDREPO
        CMDREPO -->|Write| CMDDB
    end
    
    subgraph "Read Side (Queries)"
        direction TB
        QRY[Queries]
        QRYH[Query Handlers]
        QRYREPO[Read Repository]
        QRYDB[(Read DB)]
        
        QRY -->|Execute| QRYH
        QRYH -->|Fetch| QRYREPO
        QRYREPO -->|Read| QRYDB
    end
    
    subgraph "Domain Layer"
        DOM[Domain Models<br/>Business Logic]
        EVT[Domain Events]
    end
    
    CLIENT -->|POST/PUT/DELETE| API
    CLIENT -->|GET| API
    
    API -->|Dispatch| CMD
    API -->|Dispatch| QRY
    
    DOM -.->|Publish| EVT
    EVT -.->|Update| QRYDB
    
    style CMD fill:#ffcdd2
    style CMDH fill:#ffcdd2
    style CMDREPO fill:#ffcdd2
    style QRY fill:#c8e6c9
    style QRYH fill:#c8e6c9
    style QRYREPO fill:#c8e6c9
    style DOM fill:#bbdefb
    style EVT fill:#fff9c4
```

### Command Flow Example: Create Invoice

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Command
    participant Handler
    participant Domain
    participant Repository
    participant Database
    
    Client->>Controller: POST /api/invoices
    Controller->>Controller: Validate Request DTO
    Controller->>Command: CreateInvoiceCommand
    Command->>Handler: Execute Command
    Handler->>Domain: Validate Customer Exists
    Handler->>Domain: Create Invoice Aggregate
    Domain->>Domain: Apply Business Rules
    Domain->>Domain: Calculate Totals
    Handler->>Repository: Save Invoice
    Repository->>Database: INSERT
    Database-->>Repository: Success
    Repository-->>Handler: Invoice Entity
    Handler-->>Controller: InvoiceResponse DTO
    Controller-->>Client: 201 Created + Invoice Data
```

### Query Flow Example: Get Invoice

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Query
    participant Handler
    participant Repository
    participant Database
    
    Client->>Controller: GET /api/invoices/{id}
    Controller->>Query: GetInvoiceByIdQuery
    Query->>Handler: Execute Query
    Handler->>Repository: FindById
    Repository->>Database: SELECT with JOINs
    Database-->>Repository: Result Set
    Repository-->>Handler: Invoice Entity
    Handler->>Handler: Map to DTO
    Handler-->>Controller: InvoiceResponse DTO
    Controller-->>Client: 200 OK + Invoice Data
```

---

## 5. Vertical Slice Architecture

### Feature-Based Organization

```mermaid
graph TB
    subgraph "Vertical Slices by Feature"
        subgraph "Customer Feature"
            direction TB
            CC[Customer Controller]
            CCMD[Create Customer Command]
            CQRY[Get Customer Query]
            CCH[Command Handler]
            CQH[Query Handler]
            CREPO[Customer Repository]
            CDOM[Customer Domain]
            
            CC --> CCMD & CQRY
            CCMD --> CCH
            CQRY --> CQH
            CCH --> CDOM
            CQH --> CDOM
            CCH & CQH --> CREPO
        end
        
        subgraph "Invoice Feature"
            direction TB
            IC[Invoice Controller]
            ICMD[Create Invoice Command]
            IQRY[List Invoices Query]
            ICH[Command Handler]
            IQH[Query Handler]
            IREPO[Invoice Repository]
            IDOM[Invoice Domain]
            
            IC --> ICMD & IQRY
            ICMD --> ICH
            IQRY --> IQH
            ICH --> IDOM
            IQH --> IDOM
            ICH & IQH --> IREPO
        end
        
        subgraph "Payment Feature"
            direction TB
            PC[Payment Controller]
            PCMD[Record Payment Command]
            PQRY[Get Payments Query]
            PCH[Command Handler]
            PQH[Query Handler]
            PREPO[Payment Repository]
            PDOM[Payment Domain]
            
            PC --> PCMD & PQRY
            PCMD --> PCH
            PQRY --> PQH
            PCH --> PDOM
            PQH --> PDOM
            PCH & PQH --> PREPO
        end
    end
    
    DB[(Shared Database)]
    
    CREPO --> DB
    IREPO --> DB
    PREPO --> DB
    
    style CC fill:#e1f5ff
    style IC fill:#e1f5ff
    style PC fill:#e1f5ff
    style CCMD fill:#ffcdd2
    style ICMD fill:#ffcdd2
    style PCMD fill:#ffcdd2
    style CQRY fill:#c8e6c9
    style IQRY fill:#c8e6c9
    style PQRY fill:#c8e6c9
```

### Directory Structure (Vertical Slice)

```mermaid
graph LR
    ROOT[src/main/java/com/osgiliath]
    
    ROOT --> FEATURES[features/]
    ROOT --> DOMAIN[domain/]
    ROOT --> INFRA[infrastructure/]
    ROOT --> COMMON[common/]
    
    FEATURES --> CUST[customers/]
    FEATURES --> INV[invoices/]
    FEATURES --> PAY[payments/]
    
    CUST --> CCREATE[CreateCustomer/]
    CUST --> CGET[GetCustomer/]
    CUST --> CLIST[ListCustomers/]
    
    CCREATE --> CCMD[CreateCustomerCommand.java]
    CCREATE --> CHDL[CreateCustomerHandler.java]
    CCREATE --> CCTL[CreateCustomerController.java]
    
    INV --> ICREATE[CreateInvoice/]
    INV --> ISEND[SendInvoice/]
    INV --> ILIST[ListInvoices/]
    
    PAY --> PRECORD[RecordPayment/]
    PAY --> PLIST[ListPayments/]
    
    style ROOT fill:#e1f5ff
    style FEATURES fill:#c8e6c9
    style DOMAIN fill:#f3e5f5
    style INFRA fill:#fce4ec
```

---

## 6. Database Schema

### Entity Relationship Diagram

```mermaid
erDiagram
    CUSTOMERS ||--o{ INVOICES : "has"
    INVOICES ||--|{ LINE_ITEMS : "contains"
    INVOICES ||--o{ PAYMENTS : "receives"
    USERS ||--o{ CUSTOMERS : "creates"
    USERS ||--o{ INVOICES : "creates"
    
    CUSTOMERS {
        uuid id PK
        varchar name
        varchar email UK
        varchar phone
        text address
        timestamp created_at
        timestamp updated_at
    }
    
    INVOICES {
        uuid id PK
        uuid customer_id FK
        varchar invoice_number UK
        date issue_date
        date due_date
        varchar status
        decimal subtotal
        decimal tax_amount
        decimal total_amount
        decimal balance_due
        timestamp created_at
        timestamp updated_at
    }
    
    LINE_ITEMS {
        uuid id PK
        uuid invoice_id FK
        varchar description
        decimal quantity
        decimal unit_price
        decimal line_total
        timestamp created_at
    }
    
    PAYMENTS {
        uuid id PK
        uuid invoice_id FK
        date payment_date
        decimal amount
        varchar payment_method
        varchar reference_number
        timestamp created_at
    }
    
    USERS {
        uuid id PK
        varchar username UK
        varchar password_hash
        varchar email
        timestamp created_at
        timestamp last_login
    }
```

### Database Indexes

```mermaid
graph TB
    subgraph "Customers Table"
        C[customers]
        C --> C1[PRIMARY KEY: id]
        C --> C2[UNIQUE INDEX: email]
    end
    
    subgraph "Invoices Table"
        I[invoices]
        I --> I1[PRIMARY KEY: id]
        I --> I2[UNIQUE INDEX: invoice_number]
        I --> I3[INDEX: customer_id]
        I --> I4[INDEX: status]
        I --> I5[INDEX: issue_date]
    end
    
    subgraph "Line Items Table"
        L[line_items]
        L --> L1[PRIMARY KEY: id]
        L --> L2[INDEX: invoice_id]
    end
    
    subgraph "Payments Table"
        P[payments]
        P --> P1[PRIMARY KEY: id]
        P --> P2[INDEX: invoice_id]
        P --> P3[INDEX: payment_date]
    end
    
    style C fill:#bbdefb
    style I fill:#c8e6c9
    style L fill:#fff9c4
    style P fill:#f8bbd0
```

---

## 7. API Architecture

### REST API Endpoints

```mermaid
graph LR
    subgraph "Customer API"
        C1[POST /api/customers]
        C2[GET /api/customers]
        C3[GET /api/customers/:id]
        C4[PUT /api/customers/:id]
        C5[DELETE /api/customers/:id]
    end
    
    subgraph "Invoice API"
        I1[POST /api/invoices]
        I2[GET /api/invoices]
        I3[GET /api/invoices/:id]
        I4[PUT /api/invoices/:id]
        I5[POST /api/invoices/:id/line-items]
        I6[DELETE /api/invoices/:id/line-items/:itemId]
        I7[POST /api/invoices/:id/send]
        I8[GET /api/invoices/:id/balance]
    end
    
    subgraph "Payment API"
        P1[POST /api/invoices/:id/payments]
        P2[GET /api/payments/:id]
        P3[GET /api/invoices/:id/payments]
    end
    
    subgraph "Auth API"
        A1[POST /api/auth/login]
        A2[POST /api/auth/logout]
        A3[GET /api/auth/me]
    end
    
    style C1 fill:#ffcdd2
    style C4 fill:#ffcdd2
    style C5 fill:#ffcdd2
    style C2 fill:#c8e6c9
    style C3 fill:#c8e6c9
    
    style I1 fill:#ffcdd2
    style I4 fill:#ffcdd2
    style I5 fill:#ffcdd2
    style I6 fill:#ffcdd2
    style I7 fill:#ffcdd2
    style I2 fill:#c8e6c9
    style I3 fill:#c8e6c9
    style I8 fill:#c8e6c9
    
    style P1 fill:#ffcdd2
    style P2 fill:#c8e6c9
    style P3 fill:#c8e6c9
```

### API Request/Response Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway<br/>(Spring Boot)
    participant Auth as Spring Security
    participant Controller
    participant Handler
    participant Domain
    participant Repository
    participant DB
    
    Client->>Gateway: HTTP Request + JWT/Session
    Gateway->>Auth: Authenticate
    
    alt Authentication Failed
        Auth-->>Client: 401 Unauthorized
    else Authentication Success
        Auth->>Gateway: Authorized
        Gateway->>Controller: Route Request
        Controller->>Controller: Validate DTO
        
        alt Validation Failed
            Controller-->>Client: 400 Bad Request
        else Validation Success
            Controller->>Handler: Execute Command/Query
            Handler->>Domain: Apply Business Logic
            
            alt Business Rule Violation
                Domain-->>Handler: Domain Exception
                Handler-->>Controller: Error
                Controller-->>Client: 422 Unprocessable Entity
            else Business Logic Success
                Domain->>Repository: Persist/Retrieve
                Repository->>DB: SQL Operation
                DB-->>Repository: Result
                Repository-->>Handler: Entity/Entities
                Handler->>Handler: Map to DTO
                Handler-->>Controller: Response DTO
                Controller-->>Client: 200/201 Success
            end
        end
    end
```

---

## 8. Frontend Architecture (MVVM)

### MVVM Pattern Structure

```mermaid
graph TB
    subgraph "View Layer"
        V1[CustomerListView.tsx]
        V2[CustomerFormView.tsx]
        V3[InvoiceDetailView.tsx]
        V4[PaymentFormView.tsx]
    end
    
    subgraph "ViewModel Layer"
        VM1[CustomerListViewModel]
        VM2[CustomerFormViewModel]
        VM3[InvoiceDetailViewModel]
        VM4[PaymentFormViewModel]
    end
    
    subgraph "Model Layer"
        M1[Customer Model]
        M2[Invoice Model]
        M3[Payment Model]
        M4[LineItem Model]
    end
    
    subgraph "Service Layer"
        S1[CustomerService]
        S2[InvoiceService]
        S3[PaymentService]
        S4[AuthService]
    end
    
    subgraph "API Client"
        API[Axios/Fetch Client]
    end
    
    V1 -->|Binds to| VM1
    V2 -->|Binds to| VM2
    V3 -->|Binds to| VM3
    V4 -->|Binds to| VM4
    
    VM1 -->|Uses| M1
    VM2 -->|Uses| M1
    VM3 -->|Uses| M2 & M4
    VM4 -->|Uses| M3
    
    VM1 & VM2 -->|Calls| S1
    VM3 -->|Calls| S2
    VM4 -->|Calls| S3
    
    S1 & S2 & S3 & S4 -->|HTTP| API
    
    API -->|REST API| BACKEND[Spring Boot Backend]
    
    style V1 fill:#e1f5ff
    style V2 fill:#e1f5ff
    style V3 fill:#e1f5ff
    style V4 fill:#e1f5ff
    style VM1 fill:#c8e6c9
    style VM2 fill:#c8e6c9
    style VM3 fill:#c8e6c9
    style VM4 fill:#c8e6c9
    style M1 fill:#f3e5f5
    style M2 fill:#f3e5f5
    style M3 fill:#f3e5f5
    style M4 fill:#f3e5f5
```

### Frontend Directory Structure

```mermaid
graph LR
    ROOT[src/]
    
    ROOT --> MODELS[models/]
    ROOT --> VM[viewmodels/]
    ROOT --> VIEWS[views/]
    ROOT --> SERVICES[services/]
    ROOT --> UTILS[utils/]
    ROOT --> COMPONENTS[components/]
    
    MODELS --> M1[Customer.ts]
    MODELS --> M2[Invoice.ts]
    MODELS --> M3[Payment.ts]
    
    VM --> VM1[CustomerListViewModel.ts]
    VM --> VM2[InvoiceDetailViewModel.ts]
    
    VIEWS --> V1[customers/]
    VIEWS --> V2[invoices/]
    VIEWS --> V3[payments/]
    
    V1 --> V1A[CustomerListView.tsx]
    V1 --> V1B[CustomerFormView.tsx]
    
    SERVICES --> S1[CustomerService.ts]
    SERVICES --> S2[InvoiceService.ts]
    SERVICES --> S3[ApiClient.ts]
    
    COMPONENTS --> C1[shared/]
    COMPONENTS --> C2[layout/]
    
    style ROOT fill:#e1f5ff
    style MODELS fill:#f3e5f5
    style VM fill:#c8e6c9
    style VIEWS fill:#fff9c4
    style SERVICES fill:#ffcdd2
```

### React Component Data Flow

```mermaid
sequenceDiagram
    participant User
    participant View as React View<br/>(Component)
    participant VM as ViewModel<br/>(State Management)
    participant Service
    participant API as API Client
    participant Backend
    
    User->>View: User Action (e.g., Submit Form)
    View->>VM: Call ViewModel Method
    VM->>VM: Update Loading State
    VM->>Service: Call Service Method
    Service->>API: HTTP Request
    API->>Backend: REST API Call
    Backend-->>API: Response
    API-->>Service: Data/Error
    Service-->>VM: Processed Data
    VM->>VM: Update State
    VM-->>View: Trigger Re-render
    View-->>User: Display Updated UI
```

---

## 9. Request Flow Diagrams

### Complete Request Flow: Create Invoice with Payment

```mermaid
sequenceDiagram
    participant U as User
    participant UI as React Frontend
    participant API as REST API
    participant CH as Command Handler
    participant D as Domain Model
    participant R as Repository
    participant DB as Database
    
    Note over U,DB: Step 1: Create Customer
    U->>UI: Enter Customer Info
    UI->>API: POST /api/customers
    API->>CH: CreateCustomerCommand
    CH->>D: new Customer()
    D->>D: Validate Business Rules
    CH->>R: save(customer)
    R->>DB: INSERT INTO customers
    DB-->>R: Customer Created
    R-->>CH: Customer Entity
    CH-->>API: CustomerResponse
    API-->>UI: 201 Created
    UI-->>U: Show Success
    
    Note over U,DB: Step 2: Create Invoice (Draft)
    U->>UI: Create Invoice + Line Items
    UI->>API: POST /api/invoices
    API->>CH: CreateInvoiceCommand
    CH->>D: Validate Customer Exists
    CH->>D: new Invoice()
    D->>D: Add Line Items
    D->>D: Calculate Totals
    D->>D: Set Status = DRAFT
    CH->>R: save(invoice)
    R->>DB: INSERT INTO invoices, line_items
    DB-->>R: Invoice Created
    R-->>CH: Invoice Entity
    CH-->>API: InvoiceResponse
    API-->>UI: 201 Created
    UI-->>U: Show Invoice
    
    Note over U,DB: Step 3: Send Invoice
    U->>UI: Click "Send Invoice"
    UI->>API: POST /api/invoices/:id/send
    API->>CH: SendInvoiceCommand
    CH->>R: findById(invoiceId)
    R->>DB: SELECT invoice
    DB-->>R: Invoice Entity
    R-->>CH: Invoice
    CH->>D: invoice.send()
    D->>D: Validate Status = DRAFT
    D->>D: Set Status = SENT
    CH->>R: save(invoice)
    R->>DB: UPDATE invoices
    DB-->>R: Updated
    R-->>CH: Invoice Entity
    CH-->>API: InvoiceResponse
    API-->>UI: 200 OK
    UI-->>U: Status Updated
    
    Note over U,DB: Step 4: Record Payment
    U->>UI: Enter Payment Amount
    UI->>API: POST /api/invoices/:id/payments
    API->>CH: RecordPaymentCommand
    CH->>R: findById(invoiceId)
    R->>DB: SELECT invoice
    DB-->>R: Invoice Entity
    R-->>CH: Invoice
    CH->>D: invoice.applyPayment(amount)
    D->>D: Validate Status = SENT
    D->>D: Validate Amount <= Balance
    D->>D: Update Balance
    D->>D: Check if Paid (balance = 0)
    D->>D: Set Status = PAID (if applicable)
    CH->>D: new Payment()
    CH->>R: save(payment, invoice)
    R->>DB: INSERT INTO payments<br/>UPDATE invoices
    DB-->>R: Success
    R-->>CH: Payment Entity
    CH-->>API: PaymentResponse
    API-->>UI: 201 Created
    UI-->>U: Payment Recorded
```

### Invoice Lifecycle State Machine

```mermaid
stateDiagram-v2
    [*] --> Draft: Create Invoice
    
    Draft --> Draft: Add/Remove Line Items
    Draft --> Draft: Update Details
    Draft --> Sent: Mark as Sent
    Draft --> [*]: Delete Invoice
    
    Sent --> Sent: Record Payment<br/>(if balance > 0)
    Sent --> Paid: Record Payment<br/>(balance becomes 0)
    
    Paid --> [*]: Archive/Complete
    
    note right of Draft
        Can be edited
        Cannot receive payments
        Must have ≥1 line item to send
    end note
    
    note right of Sent
        Cannot be edited
        Can receive payments
        Balance tracked
    end note
    
    note right of Paid
        Fully paid (balance = 0)
        No further changes
        Historical record
    end note
```

---

## 10. Deployment Architecture

### Local Development Environment

```mermaid
graph TB
    subgraph "Developer Machine"
        IDE[IDE with AI Tools<br/>Cursor/Copilot]
        
        subgraph "Backend"
            SPRING[Spring Boot<br/>:8080]
            H2[H2/PostgreSQL<br/>:5432]
        end
        
        subgraph "Frontend"
            REACT[React Dev Server<br/>:3000]
        end
    end
    
    IDE -->|Develops| SPRING
    IDE -->|Develops| REACT
    SPRING -->|Connects| H2
    REACT -->|API Calls| SPRING
    
    style IDE fill:#e1f5ff
    style SPRING fill:#c8e6c9
    style REACT fill:#fff9c4
    style H2 fill:#e0e0e0
```

### Production Deployment (AWS Example)

```mermaid
graph TB
    subgraph "Client"
        USER[Web Browser]
    end
    
    subgraph "AWS Cloud"
        subgraph "Edge"
            CF[CloudFront CDN]
        end
        
        subgraph "Compute"
            ALB[Application Load Balancer]
            EC2A[EC2 Instance 1<br/>Spring Boot]
            EC2B[EC2 Instance 2<br/>Spring Boot]
        end
        
        subgraph "Storage"
            RDS[(RDS PostgreSQL<br/>Multi-AZ)]
            S3[S3 Bucket<br/>Static Assets]
        end
        
        subgraph "Security"
            SG[Security Groups]
            IAM[IAM Roles]
        end
    end
    
    USER -->|HTTPS| CF
    CF -->|Static Content| S3
    CF -->|API Requests| ALB
    ALB -->|Load Balance| EC2A
    ALB -->|Load Balance| EC2B
    EC2A -->|SQL| RDS
    EC2B -->|SQL| RDS
    
    SG -.->|Protect| EC2A
    SG -.->|Protect| EC2B
    SG -.->|Protect| RDS
    IAM -.->|Authorize| EC2A
    IAM -.->|Authorize| EC2B
    
    style USER fill:#e1f5ff
    style CF fill:#fff9c4
    style ALB fill:#c8e6c9
    style EC2A fill:#ffcdd2
    style EC2B fill:#ffcdd2
    style RDS fill:#e0e0e0
    style S3 fill:#f3e5f5
```

### Production Deployment (Azure Example)

```mermaid
graph TB
    subgraph "Client"
        USER[Web Browser]
    end
    
    subgraph "Azure Cloud"
        subgraph "Edge"
            CDN[Azure CDN]
        end
        
        subgraph "Compute"
            APPGW[Application Gateway]
            APP1[App Service 1<br/>Spring Boot]
            APP2[App Service 2<br/>Spring Boot]
        end
        
        subgraph "Storage"
            SQLDB[(Azure SQL Database)]
            BLOB[Blob Storage<br/>Static Assets]
        end
        
        subgraph "Security"
            NSG[Network Security Groups]
            KV[Key Vault]
        end
    end
    
    USER -->|HTTPS| CDN
    CDN -->|Static Content| BLOB
    CDN -->|API Requests| APPGW
    APPGW -->|Route| APP1
    APPGW -->|Route| APP2
    APP1 -->|SQL| SQLDB
    APP2 -->|SQL| SQLDB
    APP1 -->|Secrets| KV
    APP2 -->|Secrets| KV
    
    NSG -.->|Protect| APP1
    NSG -.->|Protect| APP2
    
    style USER fill:#e1f5ff
    style CDN fill:#fff9c4
    style APPGW fill:#c8e6c9
    style APP1 fill:#ffcdd2
    style APP2 fill:#ffcdd2
    style SQLDB fill:#e0e0e0
    style BLOB fill:#f3e5f5
```

### Container-Based Deployment (Docker)

```mermaid
graph TB
    subgraph "Docker Compose Environment"
        subgraph "Frontend Container"
            NGINX[Nginx<br/>React Build]
        end
        
        subgraph "Backend Container"
            SPRING[Spring Boot App<br/>Java 17]
        end
        
        subgraph "Database Container"
            POSTGRES[(PostgreSQL 15)]
        end
        
        subgraph "Network"
            NET[app-network]
        end
    end
    
    NGINX -->|Proxy API| SPRING
    SPRING -->|JDBC| POSTGRES
    
    NGINX -.->|Connected| NET
    SPRING -.->|Connected| NET
    POSTGRES -.->|Connected| NET
    
    style NGINX fill:#fff9c4
    style SPRING fill:#c8e6c9
    style POSTGRES fill:#e0e0e0
    style NET fill:#e1f5ff
```

---

## Architecture Decision Records (ADR)

### ADR-001: Clean Architecture with DDD

**Decision**: Implement Clean Architecture with Domain-Driven Design

**Rationale**:
- Separation of concerns enables independent testing and maintenance
- Domain logic isolation protects business rules
- Infrastructure independence allows technology swaps
- Aligns with enterprise-grade system requirements

**Consequences**:
- More initial setup complexity
- Steeper learning curve
- Better long-term maintainability
- Clear architectural boundaries

---

### ADR-002: CQRS Pattern

**Decision**: Separate Commands (writes) from Queries (reads)

**Rationale**:
- Clear separation of responsibilities
- Optimized read and write models
- Easier to scale independently
- Explicit intent in code

**Consequences**:
- More boilerplate code initially
- Clearer codebase
- Easier to optimize specific operations
- Better alignment with business operations

---

### ADR-003: Vertical Slice Architecture

**Decision**: Organize code by features rather than technical layers

**Rationale**:
- Features are cohesive units
- Easier to locate related code
- Reduces coupling between features
- Aligns with business capabilities

**Consequences**:
- Less traditional structure
- Better feature isolation
- Easier to assign work by feature
- Reduced merge conflicts

---

## Diagram Legend

### Color Coding

- 🔵 **Light Blue** (#e1f5ff): API/Controller Layer, UI Components
- 🟢 **Light Green** (#c8e6c9/#e8f5e9): Application Layer, Queries, ViewModels
- 🔴 **Light Red** (#ffcdd2): Commands, Write Operations
- 🟣 **Light Purple** (#f3e5f5): Domain Layer, Models
- 🟡 **Light Yellow** (#fff9c4): Infrastructure, Static Assets
- ⚫ **Gray** (#e0e0e0): Database, Storage

### Symbols

- **Solid Arrows** (→): Direct dependencies or data flow
- **Dashed Arrows** (⇢): Indirect dependencies or events
- **Bold Boxes**: Aggregate roots or major components
- **Subgraphs**: Logical groupings or bounded contexts

---

**Document Version:** 1.0  
**Last Updated:** November 7, 2025  
**Maintained By:** Osgiliath Development Team
