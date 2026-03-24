package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.ContratDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.immobilier.entity.Contrat;
import com.kupanga.api.immobilier.entity.StatutContrat;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-24T01:07:54+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class ContratMapperImpl implements ContratMapper {

    @Override
    public ContratDTO toDTO(Contrat contrat) {
        if ( contrat == null ) {
            return null;
        }

        Long bienId = null;
        UserDTO proprietaire = null;
        UserDTO locataire = null;
        Long id = null;
        String adresseBien = null;
        Double loyerMensuel = null;
        Double chargesMensuelles = null;
        Double depotGarantie = null;
        LocalDate dateDebut = null;
        LocalDate dateFin = null;
        Integer dureeBailMois = null;
        LocalDateTime dateSignatureProprietaire = null;
        LocalDateTime dateSignatureLocataire = null;
        String urlPdf = null;
        StatutContrat statut = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        bienId = contratBienId( contrat );
        proprietaire = mapUserSansInfosSensibles( contrat.getProprietaire() );
        locataire = mapUserSansInfosSensibles( contrat.getLocataire() );
        id = contrat.getId();
        adresseBien = contrat.getAdresseBien();
        loyerMensuel = contrat.getLoyerMensuel();
        chargesMensuelles = contrat.getChargesMensuelles();
        depotGarantie = contrat.getDepotGarantie();
        dateDebut = contrat.getDateDebut();
        dateFin = contrat.getDateFin();
        dureeBailMois = contrat.getDureeBailMois();
        dateSignatureProprietaire = contrat.getDateSignatureProprietaire();
        dateSignatureLocataire = contrat.getDateSignatureLocataire();
        urlPdf = contrat.getUrlPdf();
        statut = contrat.getStatut();
        createdAt = contrat.getCreatedAt();
        updatedAt = contrat.getUpdatedAt();

        Boolean proprietaireASigné = contrat.getSignatureProprietaire() != null;
        Boolean locataireASigné = contrat.getSignatureLocataire() != null;

        ContratDTO contratDTO = new ContratDTO( id, bienId, adresseBien, proprietaire, locataire, loyerMensuel, chargesMensuelles, depotGarantie, dateDebut, dateFin, dureeBailMois, proprietaireASigné, locataireASigné, dateSignatureProprietaire, dateSignatureLocataire, urlPdf, statut, createdAt, updatedAt );

        return contratDTO;
    }

    @Override
    public UserDTO mapUserSansInfosSensibles(User user) {
        if ( user == null ) {
            return null;
        }

        UserDTO.UserDTOBuilder userDTO = UserDTO.builder();

        userDTO.id( user.getId() );
        userDTO.firstName( user.getFirstName() );
        userDTO.lastName( user.getLastName() );
        userDTO.mail( user.getMail() );
        userDTO.role( user.getRole() );
        userDTO.urlProfile( user.getUrlProfile() );
        userDTO.hasCompleteProfil( user.getHasCompleteProfil() );

        return userDTO.build();
    }

    private Long contratBienId(Contrat contrat) {
        if ( contrat == null ) {
            return null;
        }
        Bien bien = contrat.getBien();
        if ( bien == null ) {
            return null;
        }
        Long id = bien.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
