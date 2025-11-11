package com.osgiliath.application.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing customer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing customer")
public class UpdateCustomerRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    @Schema(description = "Customer name", example = "John Doe", required = true)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Customer email address", example = "john.doe@example.com", required = true)
    private String email;

    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    @Schema(description = "Customer phone number", example = "+1-555-0123")
    private String phone;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    @Schema(description = "Customer address", example = "123 Main St, Anytown, ST 12345")
    private String address;
}
