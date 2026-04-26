package com.example.bbs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentForm {
    @NotBlank(message = "{commentForm.content.NotBlank}")
    @Size(max = 1000, message = "{commentForm.content.Size}")
    private String content;
}