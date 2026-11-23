package com.finsight.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;


    /*
     * ============================================================
     * GENERATE JWT
     * ============================================================
     */
    public String generateToken(String email) {

        Map<String, Object> claims =
                new HashMap<>();

        return Jwts.builder()

                .claims(claims)

                .subject(email)

                .issuedAt(
                        new Date(
                                System.currentTimeMillis()
                        )
                )

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expiration
                        )
                )

                .signWith(
                        getSigningKey()
                )

                .compact();
    }


    /*
     * ============================================================
     * EXTRACT EMAIL
     * ============================================================
     */
    public String extractEmail(
            String token) {

        return extractClaim(
                token,
                claims ->
                        Objects
                                .requireNonNull(
                                        claims
                                )
                                .getSubject()
        );
    }


    /*
     * ============================================================
     * EXTRACT CLAIM
     * ============================================================
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        Claims claims =
                extractAllClaims(token);

        return claimsResolver.apply(
                Objects.requireNonNull(
                        claims
                )
        );
    }


    /*
     * ============================================================
     * VALIDATE TOKEN
     * ============================================================
     */
    public boolean isTokenValid(
            String token,
            String email) {

        try {

            String extractedEmail =
                    extractEmail(token);

            return extractedEmail != null
                    && extractedEmail.equals(email)
                    && !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }


    /*
     * ============================================================
     * CHECK EXPIRATION
     * ============================================================
     */
    private boolean isTokenExpired(
            String token) {

        return extractExpiration(token)
                .before(new Date());
    }


    /*
     * ============================================================
     * EXTRACT EXPIRATION
     * ============================================================
     */
    private Date extractExpiration(
            String token) {

        return extractClaim(
                token,
                claims ->
                        Objects
                                .requireNonNull(
                                        claims
                                )
                                .getExpiration()
        );
    }


    /*
     * ============================================================
     * EXTRACT ALL CLAIMS
     * ============================================================
     */
    private Claims extractAllClaims(
            String token) {

        return Jwts.parser()

                .verifyWith(
                        getSigningKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }


    /*
     * ============================================================
     * GET SIGNING KEY
     * ============================================================
     */
    private SecretKey getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(
                        secret
                );

        return Keys.hmacShaKeyFor(
                keyBytes
        );
    }
}