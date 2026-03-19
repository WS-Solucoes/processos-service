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

    @Value("${jwt.public-key}")
    private String publicKeyPath;

    private PublicKey publicKey;

    @PostConstruct
    public void init() throws Exception {
        this.publicKey = loadPublicKey(publicKeyPath);
    }

    public Long extrairServidorId(HttpServletRequest request) {
        Claims claims = extrairClaims(request);
        Object servidorIdObj = claims.get("servidorId");
        if (servidorIdObj == null) {
            throw new IllegalStateException("Token do portal sem identificação do servidor.");
        }
        if (servidorIdObj instanceof Integer integer) {
            return integer.longValue();
        }
        return (Long) servidorIdObj;
    }

    public Long extrairUnidadeGestoraId(HttpServletRequest request) {
        Claims claims = extrairClaims(request);
        Object ugIdObj = claims.get("currentUnidadeGestora");
        if (ugIdObj == null) {
            throw new IllegalStateException("Token do portal sem identificação da unidade gestora.");
        }
        if (ugIdObj instanceof Integer integer) {
            return integer.longValue();
        }
        return (Long) ugIdObj;
    }

    private Claims extrairClaims(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Token de autenticação não fornecido.");
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String tokenType = claims.get("type", String.class);
            if (!"portal_access".equals(tokenType)) {
                throw new IllegalStateException("Token inválido para o portal do servidor.");
            }
            return claims;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Token inválido ou expirado.");
        }
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
