package com.example.bbs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "{userForm.username.NotBlank}")
    @Size(max = 50, message = "{userForm.username.Size}")
    private String username;

    @NotBlank
    @Size(max = 100, message = "{userForm.password.Max}")
    @Pattern(regexp = "^.{4,}$", message = "{userForm.password.Min}")
    private String password;
}
