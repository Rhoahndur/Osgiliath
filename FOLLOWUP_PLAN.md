# Codebase Health Report & Implementation Plan

**Date:** March 16, 2026
**Scope:** Full codebase audit covering CI/CD, dev tooling, dead code, and code quality
**Status:** ALL TASKS COMPLETE

---

## Task A: Fill Development Setup Gaps

### A1. Add Prettier to frontend -- DONE

Installed `prettier` (v3.8.1), created `.prettierrc.json` and `.prettierignore`, added `format` and `format:check` scripts. Ran `prettier --write` across all 45 frontend source files.

### A2. Add `.editorconfig` to project root -- DONE

Created `.editorconfig` at project root with 2-space indent (general), 4-space indent (Java), and no trailing whitespace trim (Markdown).

### A3. Add Husky + lint-staged pre-commit hook -- DONE

Installed `husky` (v9.1.7) and `lint-staged` (v16.4.0). Pre-commit hook runs `prettier --write` + `eslint --fix` on staged `.ts/.tsx` files, and `prettier --write` on `.json/.css/.md` files.

### A4. Add GitHub Actions CI workflow -- DONE

Created `.github/workflows/ci.yml` with two parallel jobs:
- **Backend:** PostgreSQL 15 service container, Java 17 (Temurin), `./mvnw clean verify`
- **Frontend:** Node 18, type-check, lint, format:check, test:ci, build

Triggers on push to `main` and on pull requests.

### A5. ESLint -- NO CHANGES NEEDED

ESLint works via Next.js defaults. No custom rules required.

---

## Task B: Remove Dead / Redundant Code

### B1. Remove `DataSeeder.java` -- DONE

Deleted `backend/src/main/java/com/osgiliath/config/DataSeeder.java`. Kept `DataInitializer.java` (simpler, rewritten with externalized credentials in C2).

### B2. Remove `backend/remove_duplicates.py` -- DONE

Deleted leftover utility script.

### B3. Remove unused frontend dependencies -- DONE

Removed `react-hook-form`, `zod`, and `@hookform/resolvers` from `frontend/package.json`. Regenerated lock file.

### B4. Remove dead `LineItem.update()` method -- DONE

Deleted unused package-private `update()` method from `LineItem.java`.

### B5. Remove `frontend/FRONTEND_VALIDATION_REPORT.md` -- DONE

Deleted build artifact.

---

## Task C: Backend Code Quality Fixes

### C1. Fix `ddl-auto: update` in prod config -- DONE

Changed to `ddl-auto: validate` in `application-prod.yml`.

### C2. Externalize default dev credentials -- DONE

- Added `app.seed.*` config section to `application.yml` with env-var-backed defaults
- Added `app.seed.enabled: false` to `application-prod.yml` to disable seeding in production
- Rewrote `DataInitializer.java` with `@ConditionalOnProperty`, `@Value`-injected credentials, no password logging

### C3. Fix `RuntimeException` in `AuthController.java` -- DONE

Changed `new RuntimeException("User not found")` to `new DomainException("User not found: " + username)`. Now properly routes to 404 via GlobalExceptionHandler.

### C4. Fix `Invoice.cancel()` accounting -- DONE

SENT invoices now retain their `balanceDue` at cancellation (reflecting outstanding amount). Only DRAFT invoices zero out balance. Captures `wasDraft` flag before status change to avoid sequencing bug.

### C5. Fix exception handling -- DONE

- Added 4 dedicated `@ExceptionHandler` methods to `GlobalExceptionHandler.java`:
  - `CustomerHasInvoicesException` → 409 CONFLICT
  - `InsufficientBalanceException` → 422 UNPROCESSABLE_ENTITY
  - `InvoiceHasNoLineItemsException` → 422 UNPROCESSABLE_ENTITY
  - `InvoiceNotSentException` → 422 UNPROCESSABLE_ENTITY
- Kept `DomainException` handler as fallback
- Deleted `PaymentExceptionHandler.java` (was duplicating global handler with inconsistent response format)

### C6. Fix `Money` class setScale consistency -- DONE

Added `.setScale(2, RoundingMode.HALF_UP)` to `add()` and `subtract()` return values, matching `multiply()`.

---

## Task D: Frontend Code Quality Fixes

### D1. Replace `any` types in catch blocks -- DONE

Fixed 19 catch blocks across 8 files. Removed `: any` annotations and added type-safe narrowing (`err as { response?: ... }` or `err instanceof Error`).

Files: `useCustomerListViewModel.ts`, `usePaymentFormViewModel.ts`, `useCustomerFormViewModel.ts`, `useInvoiceListViewModel.ts`, `useAuthViewModel.ts`, `useInvoiceFormViewModel.ts`, `AuthContext.tsx`, `invoiceService.ts`

### D2. Replace `window.location.reload()` -- DONE

Replaced 3 instances in `invoices/[id]/page.tsx` with `await refreshInvoice()`.

### D3. Replace native `alert()`/`confirm()` -- DONE

Replaced all 14 `alert()` and 6 `confirm()` calls across 3 pages with:
- `confirmDialog` state + Modal component for confirmations
- `errorMessage` state + dismissible banner for error feedback

Files: `invoices/[id]/page.tsx`, `invoices/page.tsx`, `customers/page.tsx`

### D4. Add `error.tsx` and `not-found.tsx` -- DONE

- `frontend/src/app/error.tsx` - Client component error boundary with "Try Again" and "Go to Dashboard" buttons
- `frontend/src/app/not-found.tsx` - 404 page with link back to dashboard

---

## Task E: EmailService Wiring -- DONE

- Added `app.email.enabled` config (default: `false`) to both `application.yml` and `application-prod.yml`
- Added `@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")` to `EmailService.java` so it only instantiates when explicitly enabled
- Added `@PreDestroy close()` method to `EmailService.java` for SesClient cleanup
- Wired `Optional<EmailService>`, `ExportInvoiceToPdfQueryHandler`, and `CustomerRepository` into `SendInvoiceHandler.java`
- After invoice send + save, optionally generates PDF and sends email asynchronously
- Email failures are logged but don't fail the invoice send operation
- Zero impact on existing behavior (email is off by default)
