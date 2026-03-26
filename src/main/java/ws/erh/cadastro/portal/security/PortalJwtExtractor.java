package ws.erh.cadastro.portal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class PortalJwtExtractor {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PORTAL_ACCESS_TOKEN_TYPE = "portal_access";

    @Value("${jwt.public-key}")
    private String publicKeyPath;

    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        this.publicKey = loadPublicKey(publicKeyPath);
    }

    public Long extrairServidorId(HttpServletRequest request) {
        Claims claims = extrairClaims(request);
        return extrairClaimLong(claims,
                "servidorId",
                "Token do portal sem identificação do servidor.");
    }

    public Long extrairUnidadeGestoraId(HttpServletRequest request) {
        Claims claims = extrairClaims(request);
        return extrairClaimLong(claims,
                "currentUnidadeGestora",
                "Token do portal sem identificação da unidade gestora.");
    }

    private Claims extrairClaims(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw unauthorized("Token de autenticação não fornecido.");
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = claims.get("type", String.class);
            if (!PORTAL_ACCESS_TOKEN_TYPE.equals(tokenType)) {
                throw unauthorized("Token inválido para o portal do servidor.");
            }
            return claims;
        } catch (PortalAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized("Token inválido ou expirado.");
        }
    }

    private Long extrairClaimLong(Claims claims, String claimName, String missingMessage) {
        Object claimValue = claims.get(claimName);
        if (claimValue == null) {
            throw unauthorized(missingMessage);
        }
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        throw unauthorized("Token do portal contém dados inválidos.");
    }

    private PortalAuthenticationException unauthorized(String message) {
        return new PortalAuthenticationException(message);
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path.replace("classpath:", "")).getInputStream()) {
            byte[] keyBytes = in.readAllBytes();
            String keyPEM = new String(keyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(keyPEM);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        }
    }
}
