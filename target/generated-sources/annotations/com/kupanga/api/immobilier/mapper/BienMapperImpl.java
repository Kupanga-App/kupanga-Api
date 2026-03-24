package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.BienDTO;
import com.kupanga.api.immobilier.entity.Bien;
import com.kupanga.api.user.dto.readDTO.UserDTO;
import com.kupanga.api.user.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-24T01:07:54+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class BienMapperImpl implements BienMapper {

    @Override
    public BienDTO toPublicDTO(Bien bien) {
        if ( bien == null ) {
            return null;
        }

        BienDTO.BienDTOBuilder bienDTO = BienDTO.builder();

        bienDTO.proprietaire( mapProprietairePublic( bien.getProprietaire() ) );
        bienDTO.images( mapImages( bien.getImages() ) );
        bienDTO.pois( mapPoisFr( bien.getPois() ) );
        bienDTO.id( bien.getId() );
        bienDTO.titre( bien.getTitre() );
        bienDTO.typeBien( bien.getTypeBien() );
        bienDTO.description( bien.getDescription() );
        bienDTO.adresse( bien.getAdresse() );
        bienDTO.ville( bien.getVille() );
        bienDTO.codePostal( bien.getCodePostal() );
        bienDTO.pays( bien.getPays() );
        bienDTO.surfaceHabitable( bien.getSurfaceHabitable() );
        bienDTO.nombrePieces( bien.getNombrePieces() );
        bienDTO.nombreChambres( bien.getNombreChambres() );
        bienDTO.etage( bien.getEtage() );
        bienDTO.ascenseur( bien.getAscenseur() );
        bienDTO.anneeConstruction( bien.getAnneeConstruction() );
        bienDTO.modeChauffage( bien.getModeChauffage() );
        bienDTO.classeEnergie( bien.getClasseEnergie() );
        bienDTO.classeGes( bien.getClasseGes() );
        bienDTO.loyerMensuel( bien.getLoyerMensuel() );
        bienDTO.chargesMensuelles( bien.getChargesMensuelles() );
        bienDTO.depotGarantie( bien.getDepotGarantie() );
        bienDTO.meuble( bien.getMeuble() );
        bienDTO.colocation( bien.getColocation() );
        bienDTO.disponibleDe( bien.getDisponibleDe() );
        bienDTO.createdAt( bien.getCreatedAt() );
        bienDTO.updatedAt( bien.getUpdatedAt() );

        bienDTO.latitude( bien.getLocalisation() != null ? bien.getLocalisation().getY() : null );
        bienDTO.longitude( bien.getLocalisation() != null ? bien.getLocalisation().getX() : null );

        return bienDTO.build();
    }

    @Override
    public UserDTO mapProprietairePublic(User user) {
        if ( user == null ) {
            return null;
        }

        UserDTO.UserDTOBuilder userDTO = UserDTO.builder();

        userDTO.firstName( user.getFirstName() );
        userDTO.lastName( user.getLastName() );
        userDTO.urlProfile( user.getUrlProfile() );

        return userDTO.build();
    }
}
