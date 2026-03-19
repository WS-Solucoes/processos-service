package ws.erh.core.base;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ws.common.auth.TenantContext;
import ws.erh.model.core.config.AbstractTenantEntity;

/**
 * Base simplificada para o microservico de processos.
 * O servico persiste apenas os IDs de tenant/usuario e nao depende do cadastro compartilhado.
 */
public abstract class AbstractTenantService {

    protected <T extends AbstractTenantEntity> void configurarDadosTenant(T entity) {
        configurarUnidadeGestora(entity);
        configurarUsuario(entity);
        configurarUsuarioLog(entity);
    }

    private <T extends AbstractTenantEntity> void configurarUnidadeGestora(T entity) {
        Long unidadeGestoraId = TenantContext.getCurrentUnidadeGestoraId();
        if (unidadeGestoraId == null) {
            throw new IllegalStateException("Unidade gestora nao encontrada no contexto da requisicao.");
        }

        try {
            entity.getClass().getMethod("setUnidadeGestoraId", Long.class).invoke(entity, unidadeGestoraId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar unidade gestora em " + entity.getClass().getSimpleName(), e);
        }
    }

    private <T extends AbstractTenantEntity> void configurarUsuario(T entity) {
        Long usuarioId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Long detailAsLong) {
            usuarioId = detailAsLong;
        }

        try {
            entity.getClass().getMethod("setUsuarioId", Long.class).invoke(entity, usuarioId);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar usuario em " + entity.getClass().getSimpleName(), e);
        }
    }

    private <T extends AbstractTenantEntity> void configurarUsuarioLog(T entity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = TenantContext.getCurrentUnidadeGestoraRole();
        String userLog = role;
        if (userLog == null || userLog.isBlank()) {
            userLog = authentication != null ? String.valueOf(authentication.getPrincipal()) : "SISTEMA";
        }

        try {
            entity.getClass().getMethod("setUsuarioLog", String.class).invoke(entity, userLog);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao configurar usuarioLog em " + entity.getClass().getSimpleName(), e);
        }
    }
}
