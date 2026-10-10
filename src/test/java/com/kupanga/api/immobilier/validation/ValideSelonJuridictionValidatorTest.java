package com.kupanga.api.immobilier.validation;

import com.kupanga.api.immobilier.dto.formDTO.BienFormDTO;
import com.kupanga.api.immobilier.entity.ClasseEnergie;
import com.kupanga.api.immobilier.entity.TypeBien;
import com.kupanga.api.juridiction.Devise;
import com.kupanga.api.juridiction.JuridictionRegistry;
import com.kupanga.api.juridiction.JuridictionsDeTest;
import com.kupanga.api.juridiction.Pays;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * J4 (C1, C3) : {@code @ValideSelonJuridiction} sur {@link BienFormDTO}, avec les vrais profils FR et CD
 * d'{@code application.yml} (une erreur par champ fautif, comme le renverra l'API en 400).
 */
@DisplayName("J4 : formulaire de bien validé selon le pays (FR, CD)")
class ValideSelonJuridictionValidatorTest {

    private static final JuridictionRegistry REGISTRE = JuridictionsDeTest.registre();

    /** Validateur Bean Validation dont la contrainte J4 reçoit le vrai registre (comme sous Spring). */
    private final Validator validator = Validation.byDefaultProvider().configure()
            .constraintValidatorFactory(new ConstraintValidatorFactory() {
                private final ConstraintValidatorFactory parDefaut =
                        Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory();

                @Override
                @SuppressWarnings("unchecked")
                public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> type) {
                    return type == ValideSelonJuridictionValidator.class
                            ? (T) new ValideSelonJuridictionValidator(REGISTRE)
                            : parDefaut.getInstance(type);
                }

                @Override
                public void releaseInstance(ConstraintValidator<?, ?> instance) {
                }
            })
            .buildValidatorFactory().getValidator();

    private static BienFormDTO.BienFormDTOBuilder base() {
        return BienFormDTO.builder()
                .titre("Appartement T3").typeBien(TypeBien.APPARTEMENT)
                .adresse("12 rue des Tests").ville("Nantes")
                .surfaceHabitable(65.0).nombrePieces(3)
                .loyerMensuel(new BigDecimal("850")).chargesMensuelles(new BigDecimal("50"))
                .depotGarantie(new BigDecimal("1700"))
                .meuble(false).colocation(false).disponibleDe(LocalDate.now().plusDays(10));
    }

    private static BienFormDTO.BienFormDTOBuilder france() {
        return base().pays(Pays.FR).codePostal("44000");
    }

    private static BienFormDTO.BienFormDTOBuilder kinshasa() {
        return base().pays(Pays.CD).adresse("N° 12, Av. Kasa-Vubu").ville("Kinshasa")
                .commune("Kalamu").quartier("Matonge");
    }

    /** Champ → message des violations. */
    private Map<String, String> erreurs(BienFormDTO dto) {
        Set<ConstraintViolation<BienFormDTO>> violations = validator.validate(dto);
        return violations.stream().collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a + " | " + b));
    }

    @Test
    @DisplayName("Formulaires complets acceptés : France avec code postal, Kinshasa avec commune et quartier sans code postal")
    void formulairesValides() {
        assertThat(erreurs(france().build())).isEmpty();
        assertThat(erreurs(kinshasa().build())).isEmpty();
        assertThat(erreurs(kinshasa().avenue("Kasa-Vubu").numeroParcelle("1234/B")
                .pointDeRepere("Derrière l'église Saint-Joseph, en face de la station (Total)")
                .devise(Devise.CDF).loyerMensuel(new BigDecimal("1400000")).build())).isEmpty();
    }

    @Test
    @DisplayName("C1 : France sans code postal → 400 sur codePostal")
    void france_codePostalObligatoire() {
        assertThat(erreurs(france().codePostal(null).build()))
                .containsOnlyKeys("codePostal")
                .containsEntry("codePostal", "Ce champ est obligatoire pour un bien situé en France");
        assertThat(erreurs(france().codePostal(" ").build())).containsKey("codePostal");
    }

    @Test
    @DisplayName("C1 : RDC sans commune ni quartier → une erreur par champ ; code postal non exigé")
    void rdc_communeEtQuartierObligatoires() {
        Map<String, String> erreurs = erreurs(kinshasa().commune(null).quartier("").build());

        assertThat(erreurs).containsOnlyKeys("commune", "quartier");
        assertThat(erreurs.get("commune")).contains("obligatoire").contains("République démocratique du Congo");
    }

    @Test
    @DisplayName("Champ masqué envoyé → 400 : code postal et DPE en RDC, adresse congolaise en France")
    void champsMasques_refuses() {
        assertThat(erreurs(kinshasa().codePostal("12345").classeEnergie(ClasseEnergie.C).build()))
                .containsOnlyKeys("codePostal", "classeEnergie")
                .hasEntrySatisfying("codePostal", m -> assertThat(m).contains("ne s'applique pas"));
        assertThat(erreurs(france().quartier("Matonge").pointDeRepere("Près de la gare").build()))
                .containsOnlyKeys("quartier", "pointDeRepere");
    }

    @Test
    @DisplayName("Devise étrangère au pays et plafond de la devise (C5) → erreur sur le champ concerné")
    void deviseEtPlafonds() {
        assertThat(erreurs(france().devise(Devise.USD).build())).containsOnlyKeys("devise");
        assertThat(erreurs(kinshasa().loyerMensuel(new BigDecimal("150000")).build()))
                .containsOnlyKeys("loyerMensuel")
                .hasEntrySatisfying("loyerMensuel", m -> assertThat(m).contains("100000 USD"));
        // 150 000 CDF est sous le plafond du franc congolais
        assertThat(erreurs(kinshasa().devise(Devise.CDF).loyerMensuel(new BigDecimal("150000")).build())).isEmpty();
    }

    @Test
    @DisplayName("C3 : caractères des adresses congolaises acceptés (N°, /, #, &) ; URL et balises refusées")
    void caracteresAdresseCongolaise() {
        assertThat(erreurs(kinshasa().adresse("N° 12, Av. Kasa-Vubu").commune("C/Kalamu").quartier("Q/Matonge")
                .numeroParcelle("N° 45/B").build())).isEmpty();
        assertThat(erreurs(kinshasa().pointDeRepere("<script>alert(1)</script>").build())).containsKey("pointDeRepere");
        assertThat(erreurs(kinshasa().pointDeRepere("voir https://exemple.com").build())).containsKey("pointDeRepere");
        assertThat(erreurs(kinshasa().quartier("x".repeat(101)).build())).containsKey("quartier");
    }

    @Test
    @DisplayName("Pays non pris en charge ou absent : la contrainte J4 n'ajoute rien (refus fait ailleurs)")
    void paysNonPrisEnCharge_ignore() {
        assertThat(erreurs(base().pays(Pays.BE).codePostal("1000").build())).isEmpty();
        assertThat(erreurs(base().pays(null).build())).containsOnlyKeys("pays");
    }

    @Test
    @DisplayName("Sans registre (validateur par défaut hors Spring) : aucune erreur J4, le service refait le contrôle")
    void sansRegistre_neBloquePas() {
        Validator parDefaut = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(parDefaut.validate(kinshasa().commune(null).build())).isEmpty();
    }
}
