package com.kupanga.api.immobilier.service.impl;

import com.kupanga.api.exception.business.BienNotFoundException;
import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienResponseDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.mapper.BienMapper;
import com.kupanga.api.immobilier.repository.BienRepository;
import com.kupanga.api.immobilier.service.BienService;
import com.kupanga.api.user.entity.User;
import com.kupanga.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BienServiceImpl implements BienService {

    private final BienRepository bienRepository;
    private final BienMapper bienMapper;
    private final UserService userService;

    @Override
    @Transactional
    public BienResponseDTO createBien(BienFormDTO bienFormDTO) {

        User proprietaire = userService.getUserById(bienFormDTO.proprietaireId());
        userService.verifyIfUserIsOwner(proprietaire.getRole());

        Bien bien = bienMapper.toEntity(bienFormDTO);
        bien.setProprietaire(proprietaire);

        Bien savedBien = bienRepository.save(bien);

        return bienMapper.toResponseDTO(savedBien);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BienResponseDTO> getAllBiens() {
        return bienRepository.findAll()
                .stream()
                .map(bienMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BienResponseDTO getBienById(Long id) {
        Bien bien = bienRepository.findById(id)
                .orElseThrow(() -> new BienNotFoundException(id));

        return bienMapper.toResponseDTO(bien);
    }

    @Override
    @Transactional
    public BienResponseDTO updateBien(Long id, BienFormDTO bienFormDTO) {

        Bien existingBien = bienRepository.findById(id)
                .orElseThrow(() -> new BienNotFoundException(id));

        existingBien.setTitre(bienFormDTO.titre());
        existingBien.setAdresse(bienFormDTO.adresse());
        existingBien.setVille(bienFormDTO.ville());
        existingBien.setCodePostal(bienFormDTO.codePostal());
        existingBien.setLatitude(bienFormDTO.latitude());
        existingBien.setLongitude(bienFormDTO.longitude());
        existingBien.setDescription(bienFormDTO.description());

        Bien updatedBien = bienRepository.save(existingBien);

        return bienMapper.toResponseDTO(updatedBien);
    }

    @Override
    @Transactional
    public void deleteBien(Long id) {
        if (!bienRepository.existsById(id)) {
            throw new BienNotFoundException(id);
        }
        bienRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BienResponseDTO> getBiensByProprietaire(Long proprietaireId) {
        return bienRepository.findByProprietaire_Id(proprietaireId)
                .stream()
                .map(bienMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BienResponseDTO> getAvailableBiens() {
        return bienRepository.findByLocataireIsNull()
                .stream()
                .map(bienMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BienResponseDTO> getBiensByVille(String ville) {
        return bienRepository.findByVille(ville)
                .stream()
                .map(bienMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
