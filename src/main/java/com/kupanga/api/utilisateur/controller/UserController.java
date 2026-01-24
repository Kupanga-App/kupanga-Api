package com.kupanga.api.utilisateur.controller;

import com.kupanga.api.utilisateur.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utilisateur")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


}
