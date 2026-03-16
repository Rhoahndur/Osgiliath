package com.osgiliath.application.customer.command;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Command to delete a customer */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteCustomerCommand {
    private UUID id;
}
