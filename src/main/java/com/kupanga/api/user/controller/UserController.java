package com.kupanga.api.user.controller;

import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/utilisateur")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


}
