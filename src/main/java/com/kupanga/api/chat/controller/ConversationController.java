package com.kupanga.api.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Conversation" , description = " gestion des conversations au niveau du chat")
public class ConversationController {
}
