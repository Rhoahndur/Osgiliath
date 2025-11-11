package com.osgiliath.application.customer.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command to delete a customer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCustomerCommand {
    private UUID id;
}
