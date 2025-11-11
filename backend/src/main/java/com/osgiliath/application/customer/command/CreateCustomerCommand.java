package com.osgiliath.application.customer.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command to create a new customer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerCommand {
    private String name;
    private String email;
    private String phone;
    private String address;
}
