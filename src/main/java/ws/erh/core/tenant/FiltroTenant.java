package ws.erh.core.tenant;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FiltroTenant {
    String value() default "unidade_gestora_id";
}
 