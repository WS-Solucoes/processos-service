package ws.erh.cadastro.portal.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PortalJwtExtractor - autenticação do portal")
class PortalJwtExtractorTest {

    private PortalJwtExtractor extractor;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        extractor = new PortalJwtExtractor();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        ReflectionTestUtils.setField(extractor, "publicKey", keyPair.getPublic());
    }

    @Test
    @DisplayName("Deve retornar 401 quando token não for enviado")
    void deveRetornar401QuandoTokenNaoForEnviado() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> extractor.extrairServidorId(request));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Token de autenticação não fornecido.", ex.getReason());
    }

    @Test
    @DisplayName("Deve retornar 401 quando token for inválido")
    void deveRetornar401QuandoTokenForInvalido() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> extractor.extrairServidorId(request));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Token inválido ou expirado.", ex.getReason());
    }

    @Test
    @DisplayName("Deve extrair claims quando token do portal for válido")
    void deveExtrairClaimsQuandoTokenDoPortalForValido() {
        String token = Jwts.builder()
                .claim("type", "portal_access")
                .claim("servidorId", 42L)
                .claim("currentUnidadeGestora", 7L)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        assertEquals(42L, extractor.extrairServidorId(request));
        assertEquals(7L, extractor.extrairUnidadeGestoraId(request));
    }
}
