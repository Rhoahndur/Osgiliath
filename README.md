# Osgiliath

> **🚀 NEW TO THIS PROJECT? [START HERE → SETUP.md](SETUP.md)**
> **💡 NO API KEYS OR EXTERNAL SERVICES REQUIRED!**
> This project runs 100% locally with Docker, Spring Boot, and Next.js.

A modern, full-stack invoice management system built with Spring Boot and Next.js, demonstrating enterprise-grade architectural patterns including Domain-Driven Design (DDD), Command Query Responsibility Segregation (CQRS), and Vertical Slice Architecture (VSA).

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture Highlights](#architecture-highlights)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Overview

Osgiliath is a comprehensive ERP-style invoicing system that manages the complete lifecycle of customer transactions, from invoice creation through payment reconciliation. Built as a technical assessment project, it showcases best practices in modern software architecture and development.

The system consists of three core bounded contexts:
- **Customer Management**: Customer information and relationships
- **Invoice Management**: Invoice lifecycle from draft to payment
- **Payment Management**: Payment tracking and reconciliation

## Key Features

### Customer Management
- Create, read, update, and delete customers
- Email uniqueness validation
- Customer information tracking (name, email, phone, address)
- Audit trail with creation and update timestamps

### Invoice Management
- Multi-stage invoice lifecycle (Draft → Sent → Paid)
- Dynamic line item management
- Automatic calculation of subtotals, taxes, and totals
- Invoice status tracking and business rule enforcement
- Unique invoice numbering system
- Due date tracking

### Payment Management
- Record payments against invoices
- Support for multiple payment methods (Credit Card, Bank Transfer, Cash, Check)
- Partial payment support
- Automatic balance calculation
- Payment reference tracking
- Auto-transition to paid status when balance reaches zero

### Authentication & Security
- JWT-based authentication
- Secure password storage with BCrypt
- Session management
- Protected API endpoints
- User registration and login

## Technology Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with JWT
- **API Documentation**: OpenAPI 3.0 (Springdoc)
- **Testing**: JUnit 5, Spring Boot Test, TestContainers

### Frontend
- **Language**: TypeScript 5.3
- **Framework**: Next.js 14 (App Router)
- **UI Library**: React 18
- **Styling**: Tailwind CSS 3.4
- **HTTP Client**: Axios
- **Form Handling**: React Hook Form with Zod validation
- **State Management**: React Hooks

### DevOps & Infrastructure
- **Database**: PostgreSQL 15 (Docker)
- **Containerization**: Docker & Docker Compose
- **Version Control**: Git

## Architecture Highlights

### Domain-Driven Design (DDD)
- **Bounded Contexts**: Clear separation between Customer, Invoice, and Payment domains
- **Aggregates**: Customer, Invoice (with Line Items), and Payment as aggregate roots
- **Rich Domain Models**: Entities contain business logic and enforce invariants
- **Value Objects**: Email and Money as immutable value objects
- **Repository Pattern**: Abstract data access through repository interfaces

### Command Query Responsibility Segregation (CQRS)
- **Commands**: Separate handlers for write operations (CreateCustomer, RecordPayment, etc.)
- **Queries**: Dedicated handlers for read operations (GetCustomer, ListInvoices, etc.)
- **Clear Separation**: No mixing of read and write logic
- **Explicit Intent**: Command/Query names reflect business operations

### Vertical Slice Architecture (VSA)
- **Feature-Based Organization**: Code organized by business capabilities
- **Self-Contained Slices**: Each feature contains all necessary layers
- **Minimal Coupling**: Features are independent and loosely coupled
- **Business-Aligned Structure**: Easy to understand and maintain

### Clean Architecture / Layered Architecture
- **Domain Layer**: Core business logic, entities, value objects, aggregates
- **Application Layer**: Use cases, commands, queries, DTOs, handlers
- **Infrastructure Layer**: Data persistence (JPA repositories), external integrations
- **API Layer**: REST controllers, request/response models, exception handling

### Frontend MVVM Pattern
- **Models**: TypeScript interfaces defining data structures
- **Views**: React components rendering UI (Next.js pages)
- **ViewModels**: Custom React hooks encapsulating business logic and state management
- **Services**: API communication layer with centralized HTTP client

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17 or higher**: [Download JDK](https://adoptium.net/)
- **Node.js 18+ and npm**: [Download Node.js](https://nodejs.org/)
- **Docker & Docker Compose**: [Download Docker](https://www.docker.com/products/docker-desktop)
- **Maven 3.9+**: [Download Maven](https://maven.apache.org/download.cgi) (or use Maven wrapper)
- **Git**: [Download Git](https://git-scm.com/downloads)

## Quick Start

**For detailed setup instructions with troubleshooting, see [SETUP.md](SETUP.md)**

```bash
# 1. Clone and enter directory
git clone <repository-url>
cd Osgiliath

# 2. Start PostgreSQL (Docker required)
docker-compose up -d

# 3. Start Backend (Java 17+ required)
cd backend
./mvnw spring-boot:run

# 4. Start Frontend (Node 18+ required, in new terminal)
cd frontend
npm install
cp .env.local.example .env.local  # Already configured!
npm run dev

# 5. Open browser
# http://localhost:3000
# Login: testuser / password123
```

**That's it! No API keys needed.** The application creates a test user automatically on first run.

**Having issues?** Check [SETUP.md](SETUP.md) for detailed instructions and troubleshooting.

## Project Structure

```
Osgiliath/
├── backend/                    # Spring Boot backend application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/osgiliath/
│   │   │   │   ├── api/           # REST controllers
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── customer/
│   │   │   │   │   ├── invoice/
│   │   │   │   │   └── payment/
│   │   │   │   ├── application/   # Commands, queries, handlers, DTOs
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── customer/
│   │   │   │   │   │   ├── command/
│   │   │   │   │   │   ├── query/
│   │   │   │   │   │   └── dto/
│   │   │   │   │   ├── invoice/
│   │   │   │   │   └── payment/
│   │   │   │   ├── domain/        # Domain models, aggregates, value objects
│   │   │   │   │   ├── customer/
│   │   │   │   │   ├── invoice/
│   │   │   │   │   ├── payment/
│   │   │   │   │   └── shared/
│   │   │   │   ├── infrastructure/ # JPA repositories, persistence
│   │   │   │   │   ├── customer/
│   │   │   │   │   ├── invoice/
│   │   │   │   │   └── payment/
│   │   │   │   └── config/        # Security, JWT, OpenAPI config
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/                  # Unit and integration tests
│   └── pom.xml
├── frontend/                   # Next.js frontend application
│   ├── src/
│   │   ├── app/               # Next.js App Router pages
│   │   │   ├── login/
│   │   │   ├── dashboard/
│   │   │   ├── customers/
│   │   │   └── invoices/
│   │   ├── components/        # Reusable React components
│   │   │   ├── shared/
│   │   │   └── layout/
│   │   ├── models/            # TypeScript interfaces
│   │   ├── services/          # API service layer
│   │   ├── viewmodels/        # React hooks (MVVM)
│   │   └── utils/             # Utility functions
│   ├── package.json
│   └── README.md
├── docs/                       # Comprehensive documentation
│   ├── SETUP.md               # Detailed setup guide
│   ├── ARCHITECTURE.md        # Architecture deep dive
│   ├── API.md                 # API reference
│   ├── DEVELOPMENT.md         # Development workflow
│   ├── DEPLOYMENT.md          # Deployment guide
│   ├── AI_USAGE.md            # AI tools documentation
│   └── PROJECT_SUMMARY.md     # Project statistics
├── docker-compose.yml         # PostgreSQL setup
└── README.md                  # This file
```

## Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[Setup Guide](docs/SETUP.md)**: Step-by-step setup instructions and troubleshooting
- **[Architecture Guide](docs/ARCHITECTURE.md)**: Deep dive into system architecture, DDD, CQRS, and design patterns
- **[API Documentation](docs/API.md)**: Complete REST API reference with examples
- **[Development Guide](docs/DEVELOPMENT.md)**: Development workflow, coding standards, and best practices
- **[Deployment Guide](docs/DEPLOYMENT.md)**: Production deployment instructions
- **[AI Usage Guide](docs/AI_USAGE.md)**: How AI tools were used in this project
- **[Project Summary](docs/PROJECT_SUMMARY.md)**: Project statistics and achievements
- **[Backend README](backend/README.md)**: Backend-specific documentation
- **[Frontend README](frontend/README.md)**: Frontend-specific documentation

## Testing

### Backend Tests

```bash
cd backend

# Run all tests
./mvnw test

# Run integration tests only
./mvnw verify -Pintegration-test

# Run with coverage
./mvnw test jacoco:report
```

### Frontend Tests

```bash
cd frontend

# Type checking
npm run type-check

# Linting
npm run lint
```

## API Documentation

### Swagger UI

The backend provides interactive API documentation via Swagger UI:

```
http://localhost:8080/api/swagger-ui.html
```

### OpenAPI Specification

Access the raw OpenAPI 3.0 specification:

```
http://localhost:8080/api/v3/api-docs
```

### Key Endpoints

#### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

#### Customers
- `POST /api/customers` - Create customer
- `GET /api/customers` - List customers (paginated)
- `GET /api/customers/{id}` - Get customer by ID
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

#### Invoices
- `POST /api/invoices` - Create invoice (draft)
- `GET /api/invoices` - List invoices (with filters)
- `GET /api/invoices/{id}` - Get invoice by ID
- `PUT /api/invoices/{id}` - Update invoice
- `POST /api/invoices/{id}/line-items` - Add line item
- `DELETE /api/invoices/{id}/line-items/{lineItemId}` - Remove line item
- `POST /api/invoices/{id}/send` - Send invoice
- `GET /api/invoices/{id}/balance` - Get invoice balance

#### Payments
- `POST /api/invoices/{invoiceId}/payments` - Record payment
- `GET /api/invoices/{invoiceId}/payments` - List payments for invoice
- `GET /api/payments/{id}` - Get payment by ID

For detailed API documentation with request/response examples, see [docs/API.md](docs/API.md).

## Deployment

### Development Deployment

The application is designed to run locally with Docker Compose for the database.

### Production Considerations

For production deployment, consider:

1. **Environment Variables**: Configure via environment variables, not application.yml
2. **Database**: Use managed PostgreSQL service (AWS RDS, Azure Database, etc.)
3. **Security**:
   - Use strong JWT secret (256+ bits)
   - Enable HTTPS/TLS
   - Configure CORS properly
   - Use secure password policies
4. **Performance**:
   - Enable connection pooling
   - Configure appropriate JVM heap sizes
   - Use CDN for frontend static assets
5. **Monitoring**:
   - Enable Spring Boot Actuator
   - Configure logging aggregation
   - Set up health checks

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for detailed deployment instructions.

## Contributing

This is a technical assessment project, but contributions for learning purposes are welcome:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes with meaningful messages
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow existing code organization and naming conventions
- Maintain separation of concerns (DDD, CQRS principles)
- Write tests for new features
- Update documentation as needed
- Use meaningful commit messages

## License

This project is created for educational and assessment purposes.

## Acknowledgments

- Built using Claude Code (Anthropic) for AI-assisted development
- Inspired by Domain-Driven Design principles by Eric Evans
- Architecture patterns from Clean Architecture by Robert C. Martin
- CQRS pattern guidance from Microsoft's CQRS Journey

---

**Project Status**: Active Development
**Version**: 1.0.0-SNAPSHOT
**Last Updated**: November 7, 2025

For questions or issues, please refer to the comprehensive documentation in the `docs/` directory or open an issue in the repository.
