package com.kupanga.api.immobilier.controller;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.dto.formDTO.UserFormDTO;
import com.kupanga.api.user.entity.Role;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/biens")
@RequiredArgsConstructor
public class BienController {

    private final BienService bienService;

    @PostMapping
    public ResponseEntity<Void> createBien(
            @Parameter(
                    description = "JSON contenant les informations obligatoires du bien immobilier.",
                    required = true
            )
            @RequestPart("bienFormDTO") BienFormDTO bienFormDTO,

            @Parameter(
                    description = "Images d'annonce obligatoire.",
                    required = false
            )
            @RequestPart(value = "files" , required = false) List<MultipartFile> files
            )
    {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        bienService.createBien(auth , bienFormDTO , files);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/{bienId}")
    public ResponseEntity<BienDTO> getBienInfos(@PathVariable Long bienId){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(bienService.getBienInfos( auth , bienId));
    }
}
