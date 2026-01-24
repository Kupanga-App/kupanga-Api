package com.kupanga.api.login.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>
 * Classe utilitaire pour la gestion des tokens JWT :
 * </p>
 * <ul>
 *     <li>Génération de l'Access Token</li>
 *     <li>Génération du Refresh Token</li>
 *     <li>Validation et lecture des claims</li>
 *     <li>Vérification de l'expiration</li>
 * </ul>
 */
@Component
public class JwtUtils {

    /**
     * Clé secrète utilisée pour signer les tokens JWT
     */
    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * Temps d'expiration de l'Access Token (en millisecondes)
     */
    @Value("${jwt.expiration-time}")
    private Long accessTokenExpirationTime;


    /**
     * Valide un token JWT par rapport à un utilisateur.
     *
     * @param token        JWT à valider
     * @param userDetails  Détails de l'utilisateur
     * @return true si le token est valide et non expiré
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        String userEmail = extractUserEmail(token);
        return userEmail.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extrait l'email (subject) du token.
     *
     * @param token JWT
     * @return Email utilisateur
     */
    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Génère un Access Token JWT.
     *
     * @param email Email de l'utilisateur
     * @return Access Token JWT
     */
    public String generateAccessToken(String email , String role) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email,role, accessTokenExpirationTime);
    }


    /**
     * Crée un token JWT signé.
     *
     * @param claims           Claims personnalisées
     * @param subject          Sujet du token (email)
     * @param expirationTime   Durée de validité du token
     * @return JWT sous forme de String
     */
    private String createToken(Map<String, Object> claims,
                               String subject,
                               String role,
                               Long expirationTime) {

        // Ajouter le rôle aux claims
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Génère la clé de signature à partir de la clé secrète.
     *
     * @return Clé de signature
     */
    private Key getSignKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Vérifie si un token est expiré.
     *
     * @param token JWT
     * @return true si expiré
     */
    private boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

    /**
     * Extrait la date d'expiration du token.
     *
     * @param token JWT
     * @return Date d'expiration
     */
    private Date extractExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrait une claim spécifique à partir d'une fonction.
     *
     * @param token           JWT
     * @param claimsResolver  Fonction d'extraction
     * @param <T>             Type de retour
     * @return Valeur de la claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrait toutes les claims du token.
     * Vérifie automatiquement la signature JWT.
     *
     * @param token JWT
     * @return Claims contenues dans le token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
