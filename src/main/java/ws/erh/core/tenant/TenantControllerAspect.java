package ws.erh.core.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ws.common.auth.TenantContext;

@Aspect
@Component
public class TenantControllerAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Pointcut("@annotation(ws.erh.core.tenant.FiltroTenant)")
    public void tenantFilterPointcut() {
    }

    @Around("tenantFilterPointcut()")
    @Transactional
    public Object applyTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Long unidadeGestoraId = TenantContext.getCurrentUnidadeGestoraId();
        if (unidadeGestoraId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                    .setParameter("unidadeGestoraId", unidadeGestoraId);
            session.enableFilter("excluidoFilter")
                    .setParameter("excluido", false);
        }
        return joinPoint.proceed();
    }
}
