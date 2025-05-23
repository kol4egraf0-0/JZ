package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) { //логин пользователя
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) { //любое поле из токена, <T>
        // позвоялет вернуть стринг дату и тдд
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails)  { //генерация токена без доп полей
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) { //генерация токена с доп полем extraClaims(role, email)
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    public String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) { //создание токена
        return Jwts
                .builder()
                .setClaims(extraClaims) //кастомные поля дополнительные
                .setSubject(userDetails.getUsername()) //сабжект  обычно логин
                .setIssuedAt(new Date(System.currentTimeMillis())) //дата создания
                .setExpiration(new Date(System.currentTimeMillis()+expiration))//дата истека
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) //getSignInKey() возвращает секретный ключ (EC для ES256)
                //    - ES256 - алгоритм подписи ECDSA с SHA-256
                .compact();
    }

    public boolean isValidateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token); //извелкаем имя и проверям годен ли
        return (username.equals(userDetails.getUsername())&& !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey()) //возращает ключ
                .build()
                .parseClaimsJws(token) //проверяет токен
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
