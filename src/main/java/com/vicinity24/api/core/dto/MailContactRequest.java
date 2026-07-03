package com.vicinity24.api.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailContactRequest {

    @NotBlank(message = "full_name_required")
    private String fullName;

    @NotBlank(message = "email_required")
    @Email(message = "invalid_email")
    private String email;

    @NotBlank(message = "company_required")
    private String company;

    @NotBlank(message = "solution_required")
    private String solution;

    @NotBlank(message = "message_required")
    private String message;
}
