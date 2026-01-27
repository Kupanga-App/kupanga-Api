package com.kupanga.api.user.controller;

import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/completeProfile")
    public ResponseEntity<UserDTO> completeProfile(@Valid @RequestBody UserFormDTO userFormDTO){

        return ResponseEntity.ok(userService.completeProfil(userFormDTO));
    }

}
