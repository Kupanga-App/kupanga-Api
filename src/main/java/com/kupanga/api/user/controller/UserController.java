package com.kupanga.api.user.controller;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.service.BienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final BienService bienService;

    @GetMapping("/biens")
    ResponseEntity<List<BienDTO>> getAllPropertiesAssociateToOwner(){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(bienService.findAllPropertiesAssociateToUser(auth.getName()));
    }


}
