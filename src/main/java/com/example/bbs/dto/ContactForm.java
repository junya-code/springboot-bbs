package com.example.bbs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactForm {
    @NotBlank(message = "{contactForm.name.NotBlank}")
    @Size(max = 30, message = "{contactForm.name.Size}")
    private String name;

    @NotBlank(message = "{contactForm.email.NotBlank}")
    @Size(max = 100, message = "{contactForm.email.Size}")
    @Email(message = "{contactForm.email.Email}")
    private String email;

    @NotBlank(message = "{contactForm.message.NotBlank}")
    @Size(max = 1000, message = "{contactForm.message.Size}")
    private String message;
}
