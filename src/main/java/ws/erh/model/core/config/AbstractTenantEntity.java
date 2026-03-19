package ws.erh.model.core.config;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "unidadeGestoraId", type = Long.class))
@FilterDef(name = "excluidoFilter", parameters = @ParamDef(name = "excluido", type = boolean.class))
@FilterDef(
    name = "municipioFilter",
    parameters = @ParamDef(name = "municipio", type = String.class)
)
@FilterDef(
    name = "competenciaFilter",
    parameters = @ParamDef(name = "competencia", type = String.class)
)
public abstract class AbstractTenantEntity {

    @Column(name = "excluido", nullable = false, columnDefinition = "boolean default false")
    private boolean excluido;

    @PrePersist
    protected void onCreate() {
        this.excluido = false;
    }
    
    public boolean isExcluido() {
        return excluido;
    }
    
    public void setExcluido(boolean excluido) {
        this.excluido = excluido;
    }
}