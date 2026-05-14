package com.kupanga.api.backoffice.controller;

import com.kupanga.api.backoffice.dto.UserAdminPageDTO;
import com.kupanga.api.backoffice.dto.UserAdminSearchDTO;
import com.kupanga.api.backoffice.service.UserAdminService;
import com.kupanga.api.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/backoffice/users")
@RequiredArgsConstructor
public class BackOfficeUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public String list(
            @RequestParam(required = false)          String firstName,
            @RequestParam(required = false)          String lastName,
            @RequestParam(required = false)          String mail,
            @RequestParam(required = false)          Role   role,
            @RequestParam(defaultValue = "0")        int    page,
            @RequestParam(defaultValue = "10")       int    size,
            Model model,
            Principal principal
    ) {
        UserAdminSearchDTO dto    = new UserAdminSearchDTO(firstName, lastName, mail, role, page, size);
        UserAdminPageDTO   result = userAdminService.rechercher(dto);

        model.addAttribute("adminEmail", principal.getName());
        model.addAttribute("page",       result);
        model.addAttribute("roles",      Role.values());
        model.addAttribute("firstName",  firstName);
        model.addAttribute("lastName",   lastName);
        model.addAttribute("mail",       mail);
        model.addAttribute("role",       role);
        model.addAttribute("size",       size);
        return "backoffice/users/list";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id) {
        userAdminService.supprimer(id);
        return "redirect:/backoffice/users";
    }
}
