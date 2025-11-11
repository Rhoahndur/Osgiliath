# Osgiliath Development Guide

Development workflow, coding standards, and best practices for contributing to the Osgiliath project.

## Table of Contents

- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Adding New Features](#adding-new-features)
- [Testing Best Practices](#testing-best-practices)
- [Code Organization](#code-organization)
- [Git Workflow](#git-workflow)
- [Code Review Guidelines](#code-review-guidelines)
- [Common Patterns](#common-patterns)
- [Troubleshooting Development Issues](#troubleshooting-development-issues)

## Development Workflow

### Daily Development Routine

1. **Pull Latest Changes**
   ```bash
   git checkout main
   git pull origin main
   ```

2. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Start Development Environment**
   ```bash
   # Terminal 1: Start database
   docker-compose up

   # Terminal 2: Start backend
   cd backend
   ./mvnw spring-boot:run

   # Terminal 3: Start frontend
   cd frontend
   npm run dev
   ```

4. **Make Changes**
   - Write code following standards
   - Add tests for new functionality
   - Update documentation if needed

5. **Test Changes**
   ```bash
   # Backend tests
   cd backend
   ./mvnw test

   # Frontend type check
   cd frontend
   npm run type-check
   npm run lint
   ```

6. **Commit Changes**
   ```bash
   git add .
   git commit -m "feat: add customer deletion feature"
   ```

7. **Push and Create PR**
   ```bash
   git push origin feature/your-feature-name
   # Create PR via GitHub UI
   ```

### Development Environment Setup

#### IDE Configuration

**IntelliJ IDEA**:
1. Import backend as Maven project
2. Enable annotation processing: Settings → Build → Compiler → Annotation Processors
3. Install Lombok plugin
4. Install Spring Boot plugin
5. Configure code style: Settings → Editor → Code Style → Java

**VS Code**:
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Install ESLint extension
4. Install Prettier extension
5. Install Tailwind CSS IntelliSense

#### Hot Reload

**Backend** (Spring Boot DevTools):
- Already configured in pom.xml
- Changes to classes automatically reload
- No need to restart server

**Frontend** (Next.js):
- Built-in Fast Refresh
- Changes automatically reflected in browser
- No manual refresh needed

## Coding Standards

### Backend (Java)

#### Naming Conventions

```java
// Classes: PascalCase
public class CustomerService { }
public class CreateCustomerCommand { }

// Methods: camelCase
public void createCustomer() { }
public CustomerResponse handleRequest() { }

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRIES = 3;
private static final String DEFAULT_STATUS = "DRAFT";

// Variables: camelCase
private String customerName;
private UUID customerId;

// Packages: lowercase
package com.osgiliath.application.customer.command;
```

#### Code Style

```java
// Use record for immutable data
public record CreateCustomerCommand(
    String name,
    String email,
    String phone,
    String address
) {}

// Factory methods over constructors
public static Customer create(String name, String email) {
    validateName(name);
    return new Customer(name, Email.of(email));
}

// Clear method names that express intent
public void send() { }  // Better than updateStatus(SENT)
public void applyPayment(Money amount) { }  // Clear action

// Use Optional for nullable returns
public Optional<Customer> findById(UUID id) {
    return repository.findById(id);
}

// Throw domain exceptions for business rule violations
if (status != DRAFT) {
    throw new DomainException("Cannot edit non-draft invoice");
}
```

#### Documentation

```java
/**
 * Customer Aggregate Root
 * Manages customer lifecycle and enforces business rules
 */
public class Customer extends BaseEntity {

    /**
     * Creates a new customer with validation
     *
     * @param name Customer name (required, max 200 chars)
     * @param email Customer email (required, unique, valid format)
     * @param phone Customer phone (optional)
     * @param address Customer address (optional)
     * @return New customer instance
     * @throws DomainException if validation fails
     */
    public static Customer create(String name, String email,
                                   String phone, String address) {
        // Implementation
    }
}
```

### Frontend (TypeScript/React)

#### Naming Conventions

```typescript
// Interfaces: PascalCase with 'I' prefix optional
interface Customer { }
interface InvoiceFormData { }

// Components: PascalCase
function CustomerList() { }
function InvoiceForm() { }

// Hooks: camelCase starting with 'use'
function useCustomerListViewModel() { }
function useAuthViewModel() { }

// Constants: UPPER_SNAKE_CASE
const API_BASE_URL = 'http://localhost:8080/api';
const MAX_RETRY_COUNT = 3;

// Variables/Functions: camelCase
const customerList = [];
const handleSubmit = () => {};
```

#### Component Structure

```typescript
'use client';

import { useState, useEffect } from 'react';
import { useCustomerListViewModel } from '../../viewmodels/useCustomerListViewModel';
import { Table } from '../../components/shared/Table';
import { Button } from '../../components/shared/Button';

// Props interface
interface CustomerListProps {
  onCustomerSelect?: (customerId: string) => void;
}

// Component
export default function CustomerList({ onCustomerSelect }: CustomerListProps) {
  // ViewModel
  const { customers, loading, error, refresh } = useCustomerListViewModel();

  // Local state
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Effects
  useEffect(() => {
    if (selectedId && onCustomerSelect) {
      onCustomerSelect(selectedId);
    }
  }, [selectedId, onCustomerSelect]);

  // Early returns
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  // Event handlers
  const handleRowClick = (id: string) => {
    setSelectedId(id);
  };

  // Render
  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Customers</h1>
      <Table
        data={customers}
        columns={['name', 'email', 'phone']}
        onRowClick={handleRowClick}
      />
      <Button onClick={refresh}>Refresh</Button>
    </div>
  );
}
```

#### Type Safety

```typescript
// Always define types
interface Customer {
  id: string;
  name: string;
  email: string;
  phone?: string;  // Optional with ?
  address?: string;
}

// Use type guards
function isValidCustomer(data: unknown): data is Customer {
  return (
    typeof data === 'object' &&
    data !== null &&
    'id' in data &&
    'name' in data &&
    'email' in data
  );
}

// Avoid 'any'
// Bad
const data: any = await fetchData();

// Good
const data: Customer[] = await fetchData();
```

## Adding New Features

### Backend: Add New Aggregate

Let's add a "Product" aggregate as an example.

#### Step 1: Create Domain Model

```java
// domain/product/Product.java
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price"))
    private Money price;

    private Product(String name, String description, Money price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public static Product create(String name, String description, Money price) {
        validateName(name);
        validatePrice(price);
        return new Product(name, description, price);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Product name cannot be empty");
        }
    }

    private static void validatePrice(Money price) {
        if (price.isNegative()) {
            throw new DomainException("Product price cannot be negative");
        }
    }
}

// domain/product/ProductRepository.java
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    List<Product> findAll();
}
```

#### Step 2: Create Application Layer

```java
// application/product/command/CreateProductCommand.java
public record CreateProductCommand(
    String name,
    String description,
    double price
) {}

// application/product/command/CreateProductHandler.java
@Service
@Transactional
@RequiredArgsConstructor
public class CreateProductHandler {
    private final ProductRepository repository;

    public UUID handle(CreateProductCommand command) {
        Product product = Product.create(
            command.name(),
            command.description(),
            Money.of(command.price())
        );

        Product saved = repository.save(product);
        return saved.getId();
    }
}

// application/product/dto/ProductResponse.java
public record ProductResponse(
    UUID id,
    String name,
    String description,
    double price,
    LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice().getAmount().doubleValue(),
            product.getCreatedAt()
        );
    }
}
```

#### Step 3: Create Infrastructure Layer

```java
// infrastructure/product/JpaProductRepository.java
public interface JpaProductRepository
    extends ProductRepository, JpaRepository<Product, UUID> {
    // Spring Data provides implementation
}
```

#### Step 4: Create API Layer

```java
// api/product/ProductController.java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management API")
public class ProductController {

    private final CreateProductHandler createHandler;

    @PostMapping
    @Operation(summary = "Create product")
    public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request) {

        CreateProductCommand command = new CreateProductCommand(
            request.getName(),
            request.getDescription(),
            request.getPrice()
        );

        UUID id = createHandler.handle(command);
        // ... fetch and return product
    }
}
```

#### Step 5: Add Tests

```java
// Test the domain model
@Test
void shouldCreateProduct() {
    Product product = Product.create(
        "Widget",
        "A useful widget",
        Money.of(19.99)
    );

    assertThat(product.getName()).isEqualTo("Widget");
    assertThat(product.getPrice()).isEqualTo(Money.of(19.99));
}

@Test
void shouldThrowExceptionForInvalidPrice() {
    assertThatThrownBy(() ->
        Product.create("Widget", "Desc", Money.of(-10))
    ).isInstanceOf(DomainException.class)
     .hasMessageContaining("price cannot be negative");
}
```

### Frontend: Add New Feature

Let's add a "Products" page.

#### Step 1: Create Model

```typescript
// models/Product.ts
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  createdAt: string;
}
```

#### Step 2: Create Service

```typescript
// services/productService.ts
import apiClient from './apiClient';
import { Product } from '../models/Product';

export const productService = {
  getAll: async (): Promise<Product[]> => {
    const response = await apiClient.get<Product[]>('/products');
    return response.data;
  },

  create: async (data: Omit<Product, 'id' | 'createdAt'>): Promise<Product> => {
    const response = await apiClient.post<Product>('/products', data);
    return response.data;
  }
};
```

#### Step 3: Create ViewModel

```typescript
// viewmodels/useProductListViewModel.ts
import { useState, useEffect } from 'react';
import { productService } from '../services/productService';
import { Product } from '../models/Product';

export const useProductListViewModel = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await productService.getAll();
      setProducts(data);
    } catch (err) {
      setError('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  return { products, loading, error, refresh: loadProducts };
};
```

#### Step 4: Create View

```typescript
// app/products/page.tsx
'use client';

import { useProductListViewModel } from '../../viewmodels/useProductListViewModel';
import { Table } from '../../components/shared/Table';

export default function ProductsPage() {
  const { products, loading, error, refresh } = useProductListViewModel();

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="container mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Products</h1>
      <Table
        data={products}
        columns={['name', 'description', 'price']}
      />
      <button onClick={refresh} className="btn btn-primary mt-4">
        Refresh
      </button>
    </div>
  );
}
```

## Testing Best Practices

### Backend Testing

#### Unit Tests (Domain Logic)

```java
@Test
@DisplayName("Should calculate invoice total correctly")
void shouldCalculateInvoiceTotal() {
    // Arrange
    Invoice invoice = Invoice.create(
        customerId,
        "INV-001",
        LocalDate.now(),
        LocalDate.now().plusDays(30)
    );

    // Act
    invoice.addLineItem("Service", BigDecimal.valueOf(10), Money.of(100));
    invoice.addLineItem("Product", BigDecimal.valueOf(5), Money.of(50));

    // Assert
    assertThat(invoice.getSubtotal()).isEqualTo(Money.of(1250));
    assertThat(invoice.getTaxAmount()).isEqualTo(Money.of(125));
    assertThat(invoice.getTotalAmount()).isEqualTo(Money.of(1375));
}
```

#### Integration Tests (Full Flow)

```java
@SpringBootTest
@Testcontainers
class InvoicePaymentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CreateCustomerHandler createCustomerHandler;

    @Autowired
    private CreateInvoiceHandler createInvoiceHandler;

    @Autowired
    private RecordPaymentHandler recordPaymentHandler;

    @Test
    @Transactional
    void shouldCompleteFullInvoicePaymentFlow() {
        // Create customer
        CreateCustomerCommand customerCmd = new CreateCustomerCommand(
            "Acme Corp", "contact@acme.com", null, null
        );
        UUID customerId = createCustomerHandler.handle(customerCmd);

        // Create invoice
        CreateInvoiceCommand invoiceCmd = new CreateInvoiceCommand(
            customerId, "INV-001", LocalDate.now(), LocalDate.now().plusDays(30),
            List.of(new LineItemData("Service", 10, 100.0))
        );
        UUID invoiceId = createInvoiceHandler.handle(invoiceCmd);

        // Send invoice
        sendInvoiceHandler.handle(new SendInvoiceCommand(invoiceId));

        // Record payment
        RecordPaymentCommand paymentCmd = new RecordPaymentCommand(
            invoiceId, LocalDate.now(), 1100.0, PaymentMethod.BANK_TRANSFER, "REF-123"
        );
        recordPaymentHandler.handle(paymentCmd);

        // Verify invoice is paid
        Invoice invoice = getInvoiceQueryHandler.handle(new GetInvoiceByIdQuery(invoiceId));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualTo(Money.zero());
    }
}
```

### Frontend Testing

#### Type Checking

```bash
npm run type-check
```

#### Linting

```bash
npm run lint
```

## Code Organization

### Backend Package Structure

```
com.osgiliath/
├── api/                    # REST controllers
│   ├── auth/
│   ├── customer/
│   ├── invoice/
│   ├── payment/
│   └── error/
├── application/            # Use cases (CQRS)
│   ├── auth/
│   ├── customer/
│   │   ├── command/
│   │   ├── query/
│   │   └── dto/
│   ├── invoice/
│   └── payment/
├── domain/                 # Business logic
│   ├── customer/
│   ├── invoice/
│   ├── payment/
│   └── shared/
├── infrastructure/         # Persistence
│   ├── auth/
│   ├── customer/
│   ├── invoice/
│   └── payment/
└── config/                # Configuration
    ├── SecurityConfig.java
    ├── JwtTokenProvider.java
    └── OpenApiConfig.java
```

### Frontend Directory Structure

```
src/
├── app/                   # Next.js pages
│   ├── login/
│   ├── dashboard/
│   ├── customers/
│   ├── invoices/
│   └── layout.tsx
├── components/            # Reusable components
│   ├── shared/
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Table.tsx
│   │   └── Modal.tsx
│   └── layout/
│       ├── Header.tsx
│       └── Navigation.tsx
├── models/               # TypeScript interfaces
│   ├── Customer.ts
│   ├── Invoice.ts
│   └── Payment.ts
├── services/             # API clients
│   ├── apiClient.ts
│   ├── customerService.ts
│   ├── invoiceService.ts
│   └── paymentService.ts
├── viewmodels/           # React hooks (MVVM)
│   ├── useCustomerListViewModel.ts
│   ├── useInvoiceFormViewModel.ts
│   └── usePaymentFormViewModel.ts
└── utils/                # Utility functions
    ├── formatters.ts
    └── validators.ts
```

## Git Workflow

### Branch Naming

```bash
# Features
feature/add-product-catalog
feature/export-invoices

# Bug fixes
fix/customer-email-validation
fix/payment-amount-calculation

# Refactoring
refactor/extract-value-objects
refactor/improve-query-performance

# Documentation
docs/add-api-examples
docs/update-architecture-guide
```

### Commit Messages

Follow Conventional Commits:

```bash
# Format
<type>(<scope>): <subject>

# Types
feat: New feature
fix: Bug fix
docs: Documentation
refactor: Code refactoring
test: Adding tests
chore: Maintenance

# Examples
feat(customer): add customer deletion endpoint
fix(invoice): correct tax calculation for line items
docs(api): add payment endpoint examples
refactor(domain): extract Money value object
test(invoice): add invoice lifecycle integration test
```

### Pull Request Process

1. **Create PR with clear description**
2. **Link related issues**
3. **Request review from team members**
4. **Address review comments**
5. **Ensure all checks pass**
6. **Squash and merge**

## Code Review Guidelines

### What to Look For

#### Functionality
- Does it work as intended?
- Are edge cases handled?
- Are errors handled gracefully?

#### Code Quality
- Is the code readable?
- Are names clear and descriptive?
- Is there unnecessary complexity?

#### Architecture
- Does it follow DDD/CQRS patterns?
- Are dependencies pointing inward?
- Is business logic in domain layer?

#### Testing
- Are there tests for new functionality?
- Do tests cover edge cases?
- Are tests readable and maintainable?

#### Documentation
- Is complex logic documented?
- Are API changes reflected in docs?
- Are breaking changes highlighted?

### Review Checklist

- [ ] Code follows project conventions
- [ ] Business logic is in domain layer
- [ ] DTOs are used for API boundaries
- [ ] Tests are included and passing
- [ ] No hardcoded values
- [ ] Error handling is appropriate
- [ ] Documentation is updated
- [ ] Commit messages are clear
- [ ] No merge conflicts

## Common Patterns

### Error Handling

```java
// Domain exceptions for business rules
throw new DomainException("Cannot send invoice without line items");

// Not found exceptions
throw new NotFoundException("Customer not found: " + id);

// Global exception handler catches and translates
@ExceptionHandler(DomainException.class)
public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
}
```

### Validation

```java
// In domain
private static void validateName(String name) {
    if (name == null || name.isBlank()) {
        throw new DomainException("Name cannot be empty");
    }
}

// In DTOs
public record CreateCustomerRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name too long")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}
```

### Transaction Management

```java
// Use @Transactional on handlers
@Service
@Transactional
public class CreateInvoiceHandler {
    public UUID handle(CreateInvoiceCommand command) {
        // All operations in one transaction
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));

        Invoice invoice = Invoice.create(...);
        return invoiceRepository.save(invoice).getId();
    }
}
```

## Troubleshooting Development Issues

### Backend Build Failures

```bash
# Clean and rebuild
./mvnw clean install

# Skip tests temporarily
./mvnw clean install -DskipTests

# Clear local Maven cache
rm -rf ~/.m2/repository
./mvnw clean install
```

### Frontend Type Errors

```bash
# Check types
npm run type-check

# Clear cache and reinstall
rm -rf node_modules .next
npm install
```

### Database Issues

```bash
# Reset database
docker-compose down -v
docker-compose up -d

# Check logs
docker-compose logs postgres
```

### Hot Reload Not Working

**Backend**:
- Ensure spring-boot-devtools is in pom.xml
- Rebuild project (Ctrl+F9 in IntelliJ)

**Frontend**:
- Restart Next.js dev server
- Clear .next folder: `rm -rf .next`

---

**Remember**: Code is read more than it's written. Prioritize clarity over cleverness.
