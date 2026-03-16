package com.osgiliath.application.customer.command;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Command to update an existing customer */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerCommand {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
}
