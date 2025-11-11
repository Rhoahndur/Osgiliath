package com.osgiliath.application.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCommand {

    private String username;
    private String password;
    private String email;
}
