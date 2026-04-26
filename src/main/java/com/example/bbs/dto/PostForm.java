package com.example.bbs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostForm {
    @NotBlank(message = "{postForm.title.NotBlank}")
    @Size(max = 50, message = "{postForm.title.Size}")
    private String title;

    @NotBlank(message = "{postForm.content.NotBlank}")
    @Size(max = 1000, message = "{postForm.content.Size}")
    private String content;
}
