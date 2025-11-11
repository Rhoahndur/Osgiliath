#!/usr/bin/env python3
import re
import os
import sys

def remove_duplicate_constructors_and_getters(file_path):
    """
    Remove explicit constructors and getters from a Java class that has Lombok annotations.
    """
    with open(file_path, 'r') as f:
        content = f.read()

    original_content = content

    # Extract class name from the file
    class_match = re.search(r'public\s+class\s+(\w+)', content)
    if not class_match:
        return False

    class_name = class_match.group(1)

    # Check if the file has Lombok annotations
    has_lombok = bool(re.search(r'@(RequiredArgsConstructor|AllArgsConstructor|NoArgsConstructor|Data)', content))
    if not has_lombok:
        return False

    # Pattern to match explicit constructors (public ClassName(...) { ... })
    # This pattern matches multi-line constructors
    constructor_pattern = rf'\n\s*public\s+{class_name}\s*\([^)]*\)\s*\{{[^}}]*\}}\n'
    content = re.sub(constructor_pattern, '\n', content)

    # Pattern to match getter methods (public Type getFieldName() { return fieldName; })
    # This pattern matches multi-line getters
    getter_pattern = r'\n\s*public\s+\w+(?:<[^>]+>)?\s+get\w+\s*\(\s*\)\s*\{[^}]*\}\n'
    content = re.sub(getter_pattern, '\n', content)

    # Clean up multiple blank lines
    content = re.sub(r'\n\s*\n\s*\n+', '\n\n', content)

    # Write back if changes were made
    if content != original_content:
        with open(file_path, 'w') as f:
            f.write(content)
        return True

    return False

def main():
    # List of files to process
    files = """src/main/java/com/invoiceme/config/SecurityConfig.java
src/main/java/com/invoiceme/config/DataInitializer.java
src/main/java/com/invoiceme/config/DataSeeder.java
src/main/java/com/invoiceme/config/JwtAuthenticationFilter.java
src/main/java/com/invoiceme/config/CustomUserDetailsService.java
src/main/java/com/invoiceme/api/auth/AuthController.java
src/main/java/com/invoiceme/api/payment/PaymentController.java
src/main/java/com/invoiceme/api/error/ErrorResponse.java
src/main/java/com/invoiceme/api/analytics/AnalyticsController.java
src/main/java/com/invoiceme/api/invoice/InvoiceController.java
src/main/java/com/invoiceme/application/auth/dto/UserResponse.java
src/main/java/com/invoiceme/application/auth/dto/RegisterRequest.java
src/main/java/com/invoiceme/application/auth/dto/LoginRequest.java
src/main/java/com/invoiceme/application/auth/dto/LoginResponse.java
src/main/java/com/invoiceme/application/auth/LoginHandler.java
src/main/java/com/invoiceme/application/auth/RegisterHandler.java
src/main/java/com/invoiceme/application/payment/dto/PaymentResponse.java
src/main/java/com/invoiceme/application/payment/dto/RecordPaymentRequest.java
src/main/java/com/invoiceme/application/payment/command/RecordPaymentHandler.java
src/main/java/com/invoiceme/application/payment/command/RecordPaymentCommand.java
src/main/java/com/invoiceme/application/payment/command/RecordPaymentResult.java
src/main/java/com/invoiceme/application/payment/query/ListPaymentsForInvoiceQueryHandler.java
src/main/java/com/invoiceme/application/payment/query/ListPaymentsForInvoiceQuery.java
src/main/java/com/invoiceme/application/payment/query/GetPaymentByIdQueryHandler.java
src/main/java/com/invoiceme/application/payment/query/GetPaymentByIdQuery.java
src/main/java/com/invoiceme/application/customer/dto/CreateCustomerRequest.java
src/main/java/com/invoiceme/application/customer/dto/UpdateCustomerRequest.java
src/main/java/com/invoiceme/application/customer/dto/CustomerResponse.java
src/main/java/com/invoiceme/application/customer/command/UpdateCustomerHandler.java
src/main/java/com/invoiceme/application/customer/command/CreateCustomerCommand.java
src/main/java/com/invoiceme/application/customer/command/DeleteCustomerCommand.java
src/main/java/com/invoiceme/application/customer/command/UpdateCustomerCommand.java
src/main/java/com/invoiceme/application/customer/command/DeleteCustomerHandler.java
src/main/java/com/invoiceme/application/customer/query/ListCustomersQuery.java
src/main/java/com/invoiceme/application/customer/query/GetCustomerByIdQuery.java
src/main/java/com/invoiceme/application/customer/query/ListCustomersQueryHandler.java
src/main/java/com/invoiceme/application/customer/query/GetCustomerByIdQueryHandler.java
src/main/java/com/invoiceme/application/analytics/TopCustomerDto.java
src/main/java/com/invoiceme/application/analytics/MonthlyRevenueDto.java
src/main/java/com/invoiceme/application/invoice/AddLineItemHandler.java
src/main/java/com/invoiceme/application/invoice/UpdateInvoiceRequest.java
src/main/java/com/invoiceme/application/invoice/DeleteInvoiceCommand.java
src/main/java/com/invoiceme/application/invoice/GetInvoiceBalanceQuery.java
src/main/java/com/invoiceme/application/invoice/CreateInvoiceCommand.java
src/main/java/com/invoiceme/application/invoice/CancelInvoiceCommand.java
src/main/java/com/invoiceme/application/invoice/LineItemResponse.java
src/main/java/com/invoiceme/application/invoice/MarkInvoiceAsPaidCommand.java
src/main/java/com/invoiceme/application/invoice/RemoveLineItemCommand.java
src/main/java/com/invoiceme/application/invoice/CancelInvoiceHandler.java
src/main/java/com/invoiceme/application/invoice/CreateInvoiceHandler.java
src/main/java/com/invoiceme/application/invoice/DeleteInvoiceHandler.java
src/main/java/com/invoiceme/application/invoice/AddLineItemCommand.java
src/main/java/com/invoiceme/application/invoice/RemoveLineItemHandler.java
src/main/java/com/invoiceme/application/invoice/BalanceResponse.java
src/main/java/com/invoiceme/application/invoice/MarkInvoiceAsPaidHandler.java
src/main/java/com/invoiceme/application/invoice/CreateInvoiceRequest.java
src/main/java/com/invoiceme/application/invoice/UpdateInvoiceCommand.java
src/main/java/com/invoiceme/application/invoice/InvoiceNumberGenerator.java
src/main/java/com/invoiceme/application/invoice/InvoiceResponse.java
src/main/java/com/invoiceme/application/invoice/GetInvoiceByIdQuery.java
src/main/java/com/invoiceme/application/invoice/SendInvoiceHandler.java
src/main/java/com/invoiceme/application/invoice/ExportInvoiceToPdfQueryHandler.java
src/main/java/com/invoiceme/application/invoice/GetInvoiceBalanceQueryHandler.java
src/main/java/com/invoiceme/application/invoice/ListInvoicesQuery.java
src/main/java/com/invoiceme/application/invoice/GetInvoiceByIdQueryHandler.java
src/main/java/com/invoiceme/application/invoice/UpdateInvoiceHandler.java
src/main/java/com/invoiceme/application/invoice/ListInvoicesQueryHandler.java
src/main/java/com/invoiceme/application/invoice/ExportInvoiceToPdfQuery.java
src/main/java/com/invoiceme/application/invoice/SendInvoiceCommand.java
src/main/java/com/invoiceme/application/invoice/LineItemRequest.java""".strip().split('\n')

    modified_count = 0

    for file_path in files:
        if os.path.exists(file_path):
            if remove_duplicate_constructors_and_getters(file_path):
                print(f"✓ Modified: {file_path}")
                modified_count += 1
            else:
                print(f"  Skipped: {file_path}")
        else:
            print(f"✗ Not found: {file_path}")

    print(f"\nTotal files modified: {modified_count}/{len(files)}")

if __name__ == '__main__':
    main()
