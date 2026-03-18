package com.example.springtemplate.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageOutput {
    private ChatSenderOutput from;
    private String message;
    private String datetime;
}