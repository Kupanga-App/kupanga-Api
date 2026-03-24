package com.kupanga.api.immobilier.mapper;

import com.kupanga.api.immobilier.dto.readDTO.QuittanceDTO;
import com.kupanga.api.immobilier.entity.Quittance;
import com.kupanga.api.user.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-24T01:07:54+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Microsoft)"
)
@Component
public class QuittanceMapperImpl implements QuittanceMapper {

    @Override
    public QuittanceDTO toDTO(Quittance q) {
        if ( q == null ) {
            return null;
        }

        QuittanceDTO quittanceDTO = new QuittanceDTO();

        quittanceDTO.setMoisLabel( buildMoisLabel( q ) );
        quittanceDTO.setNomProprietaire( buildNomProprietaire( q ) );
        quittanceDTO.setEmailProprietaire( qProprietaireMail( q ) );
        quittanceDTO.setNomLocataire( buildNomLocataire( q ) );
        quittanceDTO.setEmailLocataire( qLocataireMail( q ) );
        quittanceDTO.setAdresseBien( buildAdresseBien( q.getBien() ) );
        quittanceDTO.setTypeBien( buildTypeBien( q.getBien() ) );
        quittanceDTO.setId( q.getId() );
        if ( q.getMois() != null ) {
            quittanceDTO.setMois( Integer.parseInt( q.getMois() ) );
        }
        quittanceDTO.setAnnee( q.getAnnee() );
        quittanceDTO.setLoyerMensuel( q.getLoyerMensuel() );
        quittanceDTO.setChargesMensuelles( q.getChargesMensuelles() );
        quittanceDTO.setMontantTotal( q.getMontantTotal() );
        quittanceDTO.setDateEcheance( q.getDateEcheance() );
        quittanceDTO.setDatePaiement( q.getDatePaiement() );
        quittanceDTO.setStatut( q.getStatut() );
        quittanceDTO.setUrlPdf( q.getUrlPdf() );
        quittanceDTO.setCreatedAt( q.getCreatedAt() );
        quittanceDTO.setUpdatedAt( q.getUpdatedAt() );

        return quittanceDTO;
    }

    private String qProprietaireMail(Quittance quittance) {
        if ( quittance == null ) {
            return null;
        }
        User proprietaire = quittance.getProprietaire();
        if ( proprietaire == null ) {
            return null;
        }
        String mail = proprietaire.getMail();
        if ( mail == null ) {
            return null;
        }
        return mail;
    }

    private String qLocataireMail(Quittance quittance) {
        if ( quittance == null ) {
            return null;
        }
        User locataire = quittance.getLocataire();
        if ( locataire == null ) {
            return null;
        }
        String mail = locataire.getMail();
        if ( mail == null ) {
            return null;
        }
        return mail;
    }
}
