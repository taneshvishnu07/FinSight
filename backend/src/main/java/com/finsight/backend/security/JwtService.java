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

/**
 * Creates and validates JSON Web Tokens (JWT) for stateless login.
 *
 * A token contains the user's email as its subject, an issued-at time and an expiry
 * time, and is signed with HMAC-SHA using the Base64 secret `jwt.secret`. Token
 * lifetime comes from `jwt.expiration` (milliseconds; default 86,400,000 = 24 hours).
 * Both values are configured in application-dev.yml / application-prod.yml.
 */
@Service
public class JwtService {

    // Base64-encoded signing key (must decode to at least 256 bits). Keep it private
    // and use a different value in production.
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;


    /*
     * ============================================================
     * GENERATE JWT
     * ============================================================
     */
    /**
     * Create a signed token for the given email (called after a successful login).
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
    /**
     * Read the email (token subject). Throws if the signature is invalid or the token
     * is expired/malformed.
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
    /**
     * Generic helper: verify the token, then apply `claimsResolver` to read one claim
     * from it.
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
    /**
     * True only if the token verifies, belongs to `email` and has not expired. Any
     * parsing/verification error returns false instead of throwing.
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
    /**
     * True if the expiry time is in the past.
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
    /**
     * Read the expiry date claim.
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
    /**
     * Verify the signature and return the token payload; throws a JwtException if
     * verification fails.
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
    /**
     * Decode the Base64 secret into an HMAC key.
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