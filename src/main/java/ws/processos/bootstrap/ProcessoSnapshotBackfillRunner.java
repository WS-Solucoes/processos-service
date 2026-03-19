package ws.processos.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(50)
@RequiredArgsConstructor
@Slf4j
public class ProcessoSnapshotBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("processos", "processo")) {
            log.info("Backfill de snapshots ignorado: tabela processos.processo ainda nao existe.");
            return;
        }

        backfillServidorSnapshot();
        backfillVinculoSnapshot();
        backfillUnidadeGestoraSnapshot();
    }

    private void backfillServidorSnapshot() {
        if (!tableExists("public", "servidor")) {
            return;
        }

        int updated;
        if (tableExists("public", "municipio")) {
            updated = jdbcTemplate.update(
                    """
                    UPDATE processos.processo p
                       SET servidor_nome = COALESCE(p.servidor_nome, s.nome),
                           servidor_cpf = COALESCE(p.servidor_cpf, s.cpf),
                           municipio_nome = COALESCE(p.municipio_nome, m.nome)
                      FROM servidor s
                 LEFT JOIN municipio m
                        ON m.id = s.municipio_id
                     WHERE p.servidor_id = s.id
                       AND (
                            p.servidor_nome IS NULL
                         OR p.servidor_cpf IS NULL
                         OR p.municipio_nome IS NULL
                       )
                    """
            );
        } else {
            updated = jdbcTemplate.update(
                    """
                    UPDATE processos.processo p
                       SET servidor_nome = COALESCE(p.servidor_nome, s.nome),
                           servidor_cpf = COALESCE(p.servidor_cpf, s.cpf)
                      FROM servidor s
                     WHERE p.servidor_id = s.id
                       AND (
                            p.servidor_nome IS NULL
                         OR p.servidor_cpf IS NULL
                       )
                    """
            );
        }

        if (updated > 0) {
            log.info("Backfill de snapshots de servidor concluiu {} processo(s).", updated);
        }
    }

    private void backfillVinculoSnapshot() {
        if (!tableExists("public", "vinculo_funcional")) {
            return;
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE processos.processo p
                   SET vinculo_funcional_matricula = COALESCE(p.vinculo_funcional_matricula, vf.matricula)
                  FROM vinculo_funcional vf
                 WHERE p.vinculo_funcional_id = vf.id
                   AND p.vinculo_funcional_matricula IS NULL
                """
        );

        if (updated > 0) {
            log.info("Backfill de snapshots de vinculo concluiu {} processo(s).", updated);
        }
    }

    private void backfillUnidadeGestoraSnapshot() {
        if (!tableExists("public", "unidade_gestora")) {
            return;
        }

        int updated = jdbcTemplate.update(
                """
                UPDATE processos.processo p
                   SET unidade_gestora_nome = COALESCE(p.unidade_gestora_nome, ug.nome)
                  FROM unidade_gestora ug
                 WHERE p.unidade_gestora_id = ug.id
                   AND p.unidade_gestora_nome IS NULL
                """
        );

        if (updated > 0) {
            log.info("Backfill de snapshots de unidade gestora concluiu {} processo(s).", updated);
        }
    }

    private boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                  FROM information_schema.tables
                 WHERE table_schema = ?
                   AND table_name = ?
                """,
                Integer.class,
                schema,
                table
        );
        return count != null && count > 0;
    }
}
