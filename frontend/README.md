# Osgiliath Frontend

Next.js 14 frontend application with MVVM architecture for the Osgiliath invoice management system.

## Tech Stack

- **Framework:** Next.js 14 (App Router)
- **Language:** TypeScript
- **Architecture:** MVVM (Model-View-ViewModel)
- **Styling:** Tailwind CSS
- **HTTP Client:** Axios
- **Form Handling:** React Hook Form + Zod
- **State Management:** React Hooks

## Project Structure

```
frontend/
├── src/
│   ├── models/              # Data models (interfaces)
│   │   ├── Auth.ts
│   │   ├── Customer.ts
│   │   ├── Invoice.ts
│   │   └── Payment.ts
│   ├── services/            # API service layer
│   │   ├── apiClient.ts
│   │   ├── authService.ts
│   │   ├── customerService.ts
│   │   ├── invoiceService.ts
│   │   └── paymentService.ts
│   ├── viewmodels/          # React hooks for business logic
│   │   ├── useAuthViewModel.ts
│   │   ├── useCustomerListViewModel.ts
│   │   ├── useCustomerFormViewModel.ts
│   │   ├── useInvoiceListViewModel.ts
│   │   ├── useInvoiceFormViewModel.ts
│   │   └── usePaymentFormViewModel.ts
│   ├── components/          # Reusable UI components
│   │   ├── shared/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Table.tsx
│   │   │   └── Modal.tsx
│   │   └── layout/
│   │       ├── Navigation.tsx
│   │       └── Header.tsx
│   └── app/                 # Next.js App Router pages
│       ├── login/
│       ├── dashboard/
│       ├── customers/
│       ├── invoices/
│       ├── layout.tsx
│       └── globals.css
├── package.json
└── tsconfig.json
```

## Setup Instructions

### Prerequisites

- Node.js 18+ and npm
- Backend API running on http://localhost:8080

### Installation

1. Install dependencies:
```bash
npm install
```

2. Create environment file:
```bash
cp .env.local.example .env.local
```

3. Configure environment variables in `.env.local`:
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Running the Application

**Development mode:**
```bash
npm run dev
```

The application will be available at http://localhost:3000

**Production build:**
```bash
npm run build
npm start
```

**Type checking:**
```bash
npm run type-check
```

**Linting:**
```bash
npm run lint
```

## Features

### Authentication
- Login/Register pages
- JWT token management
- Auto-redirect to login if not authenticated
- Session persistence via localStorage

### Customer Management
- List customers with pagination
- Create new customers
- View customer details
- Edit customer information
- Delete customers

### Invoice Management
- List invoices with filtering by status
- Create invoices with multiple line items
- View invoice details
- Send invoices (status change to SENT)
- Dynamic line item management
- Automatic tax calculation
- Invoice status tracking (DRAFT, SENT, PAID, OVERDUE)

### Payment Management
- Record payments against invoices
- View payment history
- Payment method tracking
- Automatic balance calculation
- Payment validation (amount <= balance)

### Dashboard
- Overview statistics
- Recent invoices
- Quick actions for creating customers/invoices

## MVVM Architecture

### Models (src/models/)
Define TypeScript interfaces for data structures. Pure data types with no logic.

### Services (src/services/)
Handle all API communication. Each service corresponds to a backend resource:
- `authService` - Authentication operations
- `customerService` - Customer CRUD operations
- `invoiceService` - Invoice operations including line items
- `paymentService` - Payment recording and retrieval

### ViewModels (src/viewmodels/)
React hooks that manage state and business logic for views:
- Handle loading/error states
- Form validation
- Data transformations
- Orchestrate service calls
- Provide clean API for views

### Views (src/app/)
Next.js pages and components that render UI. They consume ViewModels and remain as simple as possible.

## API Integration

All API calls include JWT authentication via interceptors in `apiClient.ts`:
- Automatically adds `Authorization: Bearer <token>` header
- Handles 401 responses by redirecting to login
- Centralized error handling

## State Management

State is managed through React hooks in ViewModels:
- `useState` for component state
- `useEffect` for side effects (data fetching)
- Custom hooks encapsulate business logic
- No global state library needed for this application

## Styling

Tailwind CSS utility classes are used throughout:
- Responsive design built-in
- Consistent color scheme (blue primary, gray neutrals)
- Pre-built components in `components/shared/`
- Custom config in `tailwind.config.js`

## Authentication Flow

1. User visits any protected route
2. `layout.tsx` checks authentication status via `useAuthViewModel`
3. If not authenticated, redirect to `/login`
4. After successful login, JWT token stored in localStorage
5. All subsequent API calls include token in header
6. On 401 response, token cleared and user redirected to login

## Notes

- Uses Next.js App Router (not Pages Router)
- All components are Client Components (`'use client'`)
- TypeScript strict mode enabled
- Form validation in ViewModels, not in components
- Error messages displayed to users
- Loading states handled in all async operations
