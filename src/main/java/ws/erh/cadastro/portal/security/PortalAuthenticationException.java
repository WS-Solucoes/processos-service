package ws.erh.cadastro.portal.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PortalAuthenticationException extends ResponseStatusException {

    public PortalAuthenticationException(String reason) {
        super(HttpStatus.UNAUTHORIZED, reason);
    }
}
