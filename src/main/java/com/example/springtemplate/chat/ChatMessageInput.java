package com.example.springtemplate.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageInput {

    @NotBlank(message = "Message must not be blank")
    @Size(max = 500, message = "Message must be 500 characters or fewer")
    private String message;
}