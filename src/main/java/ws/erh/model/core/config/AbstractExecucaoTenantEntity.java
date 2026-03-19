package ws.erh.model.core.config;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ws.common.model.UnidadeGestora;
import ws.common.model.Usuario;
import ws.erh.model.core.config.AbstractTenantEntity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collection;

@MappedSuperclass
@Getter
@Setter
@ToString(callSuper = true)
public abstract class AbstractExecucaoTenantEntity extends AbstractTenantEntity {

    @Column(name = "log_usuario", nullable = false)
    private String usuarioLog;

    @Column(name = "log_data", nullable = false)
    private LocalDateTime dtLog;

    @Column(name = "unidade_gestora_id")
    private Long unidadeGestoraId;

    @Transient
    @ToString.Exclude
    private UnidadeGestora unidadeGestora;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Transient
    @ToString.Exclude
    private Usuario usuario;

    @PrePersist
    protected void onAuditPersist() {
        if (dtLog == null) {
            dtLog = LocalDateTime.now();
        }
        // Propagar usuarioLog automaticamente para entidades filhas
        propagarAuditoria();
    }

    @PreUpdate
    protected void onAuditUpdate() {
        dtLog = LocalDateTime.now();
        // Propagar usuarioLog automaticamente para entidades filhas
        propagarAuditoria();
    }

    /**
     * Propaga automaticamente o usuarioLog para todas as entidades filhas
     * que estendem AbstractExecucaoTenantEntity
     */
    private void propagarAuditoria() {
        if (this.usuarioLog == null) {
            return;
        }

        try {
            Class<?> clazz = this.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(this);

                    if (value == null) {
                        continue;
                    }

                    // Propagar para entidade única
                    if (value instanceof AbstractExecucaoTenantEntity) {
                        AbstractExecucaoTenantEntity child = (AbstractExecucaoTenantEntity) value;
                        if (child.getUsuarioLog() == null) {
                            child.setUsuarioLog(this.usuarioLog);
                        }
                    }
                    // Propagar para coleções de entidades
                    else if (value instanceof Collection<?>) {
                        Collection<?> collection = (Collection<?>) value;
                        for (Object item : collection) {
                            if (item instanceof AbstractExecucaoTenantEntity) {
                                AbstractExecucaoTenantEntity child = (AbstractExecucaoTenantEntity) item;
                                if (child.getUsuarioLog() == null) {
                                    child.setUsuarioLog(this.usuarioLog);
                                }
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Ignora erros de reflexão
        }
    }

    public void setUnidadeGestora(UnidadeGestora unidadeGestora) {
        this.unidadeGestora = unidadeGestora;
        this.unidadeGestoraId = unidadeGestora != null ? unidadeGestora.getId() : null;
    }

    public void setUnidadeGestoraId(Long unidadeGestoraId) {
        this.unidadeGestoraId = unidadeGestoraId;
        if (unidadeGestoraId == null) {
            this.unidadeGestora = null;
        }
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        this.usuarioId = usuario != null ? usuario.getId() : null;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
        if (usuarioId == null) {
            this.usuario = null;
        }
    }

    /**
     * Mantém compatibilidade com serviços que configuram o usuário de auditoria como "logUsuario".
     */
    public void setLogUsuario(String logUsuario) {
        this.usuarioLog = logUsuario;
    }
}
