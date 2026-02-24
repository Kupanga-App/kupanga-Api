package com.kupanga.api.immobilier.service;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.dto.readDTO.BienResponseDTO;

import java.util.List;

public interface BienService {

    BienResponseDTO createBien(BienFormDTO bienFormDTO);

    List<BienResponseDTO> getAllBiens();

    BienResponseDTO getBienById(Long id);

    BienResponseDTO updateBien(Long id, BienFormDTO bienFormDTO);

    void deleteBien(Long id);

    List<BienResponseDTO> getBiensByProprietaire(Long proprietaireId);

    List<BienResponseDTO> getAvailableBiens();

    List<BienResponseDTO> getBiensByVille(String ville);
}
