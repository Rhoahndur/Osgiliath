package com.osgiliath.application.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing customer data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer response data")
public class CustomerResponse {

    @Schema(description = "Customer unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Customer name", example = "John Doe")
    private String name;

    @Schema(description = "Customer email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Customer phone number", example = "+1-555-0123")
    private String phone;

    @Schema(description = "Customer address", example = "123 Main St, Anytown, ST 12345")
    private String address;

    @Schema(description = "Timestamp when customer was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when customer was last updated")
    private LocalDateTime updatedAt;

    @Schema(description = "Entity version for optimistic locking")
    private Long version;
}
