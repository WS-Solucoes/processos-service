package ws.processos.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ws.common.auth.JwtAuthenticationToken;
import ws.common.auth.TenantContext;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProcessosJwtContextFilter extends OncePerRequestFilter {

    private final PublicKey publicKey;

    public ProcessosJwtContextFilter(@Value("${jwt.public-key}") String publicKeyPath) {
        this.publicKey = loadPublicKey(publicKeyPath);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-doc/")
                || path.startsWith("/error")
                || path.startsWith("/api/v1/processo/resumo/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();
            String tokenType = claims.get("type", String.class);
            Long currentUg = parseLongClaim(claims.get("currentUnidadeGestora"));
            String currentRole = claims.get("currentUnidadeGestoraRole", String.class);
            String modulo = claims.get("modulo", String.class);

            @SuppressWarnings("unchecked")
            Map<Object, Object> rawRoles = claims.get("unidadesGestorasRoles", Map.class);
            Map<Long, String> roles = normalizeRoles(rawRoles);

            if ("portal_access".equals(tokenType)) {
                Long servidorId = parseLongClaim(claims.get("servidorId"));
                if (servidorId == null || currentUg == null) {
                    sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Token do portal sem claims obrigatorios.", request.getRequestURI());
                    return;
                }
                if (roles.isEmpty()) {
                    roles.put(currentUg, currentRole != null ? currentRole : "SERVIDOR");
                }
            }

            TenantContext.setCurrentUnidadeGestoraId(currentUg);
            TenantContext.setCurrentUnidadeGestoraRole(currentRole);
            TenantContext.setCurrentModulo(modulo != null ? modulo : "ERH");
            TenantContext.setUnidadesGestoras(roles);
            SecurityContextHolder.getContext().setAuthentication(
                    new JwtAuthenticationToken(claims.getSubject(), roles));

            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token invalido: " + ex.getMessage(), request.getRequestURI());
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private Map<Long, String> normalizeRoles(Map<Object, Object> rawRoles) {
        Map<Long, String> result = new HashMap<>();
        if (rawRoles == null) {
            return result;
        }
        rawRoles.forEach((key, value) -> {
            Long parsedKey = parseLongClaim(key);
            if (parsedKey != null && value != null) {
                result.put(parsedKey, String.valueOf(value));
            }
        });
        return result;
    }

    private Long parseLongClaim(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer.longValue();
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        return null;
    }

    private PublicKey loadPublicKey(String path) {
        try (InputStream in = new ClassPathResource(path.replace("classpath:", "")).getInputStream()) {
            byte[] pem = in.readAllBytes();
            String key = new String(pem)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar chave publica", e);
        }
    }

    private void sendJson(HttpServletResponse response, int status, String message, String path) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status);
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", status == 401 ? "Unauthorized" : "Internal Server Error");
        body.put("message", message);
        body.put("path", path);
        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
