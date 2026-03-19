package ws.processos.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoMensagemRepository;
import ws.erh.cadastro.processo.repository.ProcessoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.cadastro.processo.service.ProcessoServiceInterface;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.core.enums.processo.OrigemAberturaProcesso;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.ResultadoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoHistorico;
import ws.erh.model.cadastro.processo.ProcessoMensagem;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Order(60)
@ConditionalOnProperty(prefix = "processos.demo-seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ProcessoDemoSeeder implements ApplicationRunner {

    private static final String SEED_USER = "SEEDER";
    private static final String PREFERRED_PORTAL_CPF = "11234455778";

    private final JdbcTemplate jdbcTemplate;
    private final ProcessoRepository processoRepository;
    private final ProcessoModeloRepository processoModeloRepository;
    private final ProcessoHistoricoRepository historicoRepository;
    private final ProcessoMensagemRepository mensagemRepository;
    private final ProcessoServiceInterface processoService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!tableExists("processos", "processo") || !tableExists("public", "servidor")) {
            log.info("Seed demo de processos ignorado: estruturas de banco ainda nao disponiveis.");
            return;
        }

        for (TenantTarget tenant : loadTenantTargets()) {
            seedTenant(tenant);
        }
    }

    private void seedTenant(TenantTarget tenant) {
        List<ServidorSeedTarget> servidores = loadServidores(tenant.id());
        if (servidores.isEmpty()) {
            log.info("Seed demo de processos ignorado para UG {}: nenhum servidor elegivel encontrado.", tenant.id());
            return;
        }

        ServidorSeedTarget principal = servidores.get(0);
        ServidorSeedTarget secundario = servidores.size() > 1 ? servidores.get(1) : principal;
        ServidorSeedTarget terciario = servidores.size() > 2 ? servidores.get(2) : secundario;

        seedRascunhoAtualizacao(tenant, principal);
        seedSolicitacaoRhFerias(tenant, principal);
        seedProcessoEmAnalise(tenant, principal);
        seedProcessoAguardandoChefia(tenant, secundario);
        seedProcessoConcluido(tenant, terciario);
    }

    private void seedRascunhoAtualizacao(TenantTarget tenant, ServidorSeedTarget servidor) {
        String marker = "[DEMO] Atualizacao cadastral em rascunho para validar o portal do servidor.";
        if (demoProcessoExists(tenant.id(), marker)) {
            return;
        }

        ProcessoModelo modelo = findModelo(tenant.id(), "ATUALIZACAO_CADASTRAL").orElse(null);
        if (modelo == null) {
            return;
        }

        LocalDateTime abertura = LocalDateTime.now().minusHours(12);
        Processo processo = buildBaseProcesso(tenant, servidor, modelo, marker, abertura);
        processo.setSituacao(SituacaoProcesso.RASCUNHO);
        processo.setEtapaAtual(1);
        processo.setPrioridade(Prioridade.NORMAL);
        processo.setDadosFormulario("""
                {"tipoDado":"Endereco","novoValor":"Rua das Flores, 100 - Boa Vista"}
                """);
        processo.setOrigemAbertura(OrigemAberturaProcesso.PORTAL_SERVIDOR);
        processo.setPrazoLimite(abertura.toLocalDate().plusDays(10));
        processo.setDataUltimaAtualizacao(abertura.plusMinutes(20));

        Processo saved = processoRepository.save(processo);

        saveHistorico(saved, abertura, AcaoProcesso.CRIADO, null, SituacaoProcesso.RASCUNHO.name(),
                null, 1, servidor.nome(), TipoAutor.SERVIDOR,
                "Rascunho iniciado pelo servidor no portal.");
        saveMensagem(saved, abertura.plusMinutes(2), servidor.nome(), TipoAutor.SERVIDOR,
                "Vou revisar meus dados antes de enviar a solicitacao.", true);
    }

    private void seedSolicitacaoRhFerias(TenantTarget tenant, ServidorSeedTarget servidor) {
        String marker = "[DEMO] Solicitacao do RH aguardando complemento documental do servidor.";
        if (demoProcessoExists(tenant.id(), marker)) {
            return;
        }

        ProcessoModelo modelo = findModelo(tenant.id(), "FERIAS").orElse(null);
        if (modelo == null) {
            return;
        }

        LocalDateTime abertura = LocalDateTime.now().minusDays(6);
        Processo processo = buildBaseProcesso(tenant, servidor, modelo, marker, abertura);
        processo.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
        processo.setEtapaAtual(1);
        processo.setPrioridade(Prioridade.ALTA);
        processo.setOrigemAbertura(OrigemAberturaProcesso.RH_SOLICITACAO);
        processo.setDadosFormulario("""
                {"dataInicio":"2026-04-07","dataFim":"2026-04-21","diasGozo":"15","abonoPecuniario":"false"}
                """);
        processo.setAtribuidoPara("Analista RH");
        processo.setDepartamentoAtribuido("Departamento de Recursos Humanos");
        processo.setPrazoLimite(LocalDate.now().plusDays(4));
        processo.setDataUltimaAtualizacao(abertura.plusHours(3));

        Processo saved = processoRepository.save(processo);

        saveHistorico(saved, abertura, AcaoProcesso.CRIADO, null, SituacaoProcesso.PENDENTE_DOCUMENTACAO.name(),
                null, 1, "Analista RH", TipoAutor.RH,
                "Processo iniciado pelo RH para envio complementar do servidor.");
        saveHistorico(saved, abertura.plusHours(2), AcaoProcesso.DOCUMENTACAO_SOLICITADA,
                SituacaoProcesso.PENDENTE_DOCUMENTACAO.name(), SituacaoProcesso.PENDENTE_DOCUMENTACAO.name(),
                1, 1, "Analista RH", TipoAutor.RH,
                "Necessario anexar o formulario de ferias e a escala do setor.");
        saveMensagem(saved, abertura.plusHours(2), "Analista RH", TipoAutor.RH,
                "Favor anexar a documentacao inicial para seguirmos com a analise da solicitacao de ferias.", false);
    }

    private void seedProcessoEmAnalise(TenantTarget tenant, ServidorSeedTarget servidor) {
        String marker = "[DEMO] Processo em analise pelo RH para validar a fila administrativa.";
        if (demoProcessoExists(tenant.id(), marker)) {
            return;
        }

        ProcessoModelo modelo = findModelo(tenant.id(), "DECLARACAO").orElse(null);
        if (modelo == null) {
            return;
        }

        LocalDateTime abertura = LocalDateTime.now().minusDays(3);
        Processo processo = buildBaseProcesso(tenant, servidor, modelo, marker, abertura);
        processo.setSituacao(SituacaoProcesso.EM_ANALISE);
        processo.setEtapaAtual(2);
        processo.setPrioridade(Prioridade.NORMAL);
        processo.setOrigemAbertura(OrigemAberturaProcesso.PORTAL_SERVIDOR);
        processo.setDadosFormulario("""
                {"tipoDeclaracao":"Vinculo Funcional","finalidade":"Comprovacao bancaria","quantidadeVias":"1"}
                """);
        processo.setAtribuidoPara("Mariana RH");
        processo.setDepartamentoAtribuido("Atendimento ao Servidor");
        processo.setPrazoLimite(LocalDate.now().plusDays(2));
        processo.setDataUltimaAtualizacao(abertura.plusHours(6));

        Processo saved = processoRepository.save(processo);

        saveHistorico(saved, abertura, AcaoProcesso.CRIADO, null, SituacaoProcesso.ABERTO.name(),
                null, 1, servidor.nome(), TipoAutor.SERVIDOR,
                "Solicitacao enviada via portal do servidor.");
        saveHistorico(saved, abertura.plusHours(4), AcaoProcesso.EM_ANALISE,
                SituacaoProcesso.ABERTO.name(), SituacaoProcesso.EM_ANALISE.name(),
                1, 2, "Mariana RH", TipoAutor.RH,
                "Processo recebido para emissao da declaracao funcional.");
        saveMensagem(saved, abertura.plusMinutes(10), servidor.nome(), TipoAutor.SERVIDOR,
                "Preciso da declaracao para apresentar ao banco.", true);
        saveMensagem(saved, abertura.plusHours(4), "Mariana RH", TipoAutor.RH,
                "Recebemos sua solicitacao. O documento esta em elaboracao.", false);
    }

    private void seedProcessoAguardandoChefia(TenantTarget tenant, ServidorSeedTarget servidor) {
        String marker = "[DEMO] Processo aguardando chefia e com prazo vencido para validar alertas.";
        if (demoProcessoExists(tenant.id(), marker)) {
            return;
        }

        ProcessoModelo modelo = findModelo(tenant.id(), "AUXILIO_TRANSPORTE").orElse(null);
        if (modelo == null) {
            return;
        }

        LocalDateTime abertura = LocalDateTime.now().minusDays(9);
        Processo processo = buildBaseProcesso(tenant, servidor, modelo, marker, abertura);
        processo.setSituacao(SituacaoProcesso.AGUARDANDO_CHEFIA);
        processo.setEtapaAtual(3);
        processo.setPrioridade(Prioridade.URGENTE);
        processo.setOrigemAbertura(OrigemAberturaProcesso.PORTAL_SERVIDOR);
        processo.setDadosFormulario("""
                {"endereco":"Av. Caxanga, 2200","meioTransporte":"Onibus","valorDiario":"18.40","diasUteis":"22"}
                """);
        processo.setAtribuidoPara("Chefia Imediata");
        processo.setDepartamentoAtribuido("Gabinete do Secretario");
        processo.setPrazoLimite(LocalDate.now().minusDays(2));
        processo.setDataUltimaAtualizacao(abertura.plusDays(2));

        Processo saved = processoRepository.save(processo);

        saveHistorico(saved, abertura, AcaoProcesso.CRIADO, null, SituacaoProcesso.ABERTO.name(),
                null, 1, servidor.nome(), TipoAutor.SERVIDOR,
                "Solicitacao aberta pelo servidor.");
        saveHistorico(saved, abertura.plusHours(5), AcaoProcesso.EM_ANALISE,
                SituacaoProcesso.ABERTO.name(), SituacaoProcesso.EM_ANALISE.name(),
                1, 2, "Analista RH", TipoAutor.RH,
                "Documentacao conferida e em analise pelo RH.");
        saveHistorico(saved, abertura.plusDays(2), AcaoProcesso.ENCAMINHADO_CHEFIA,
                SituacaoProcesso.EM_ANALISE.name(), SituacaoProcesso.AGUARDANDO_CHEFIA.name(),
                2, 3, "Analista RH", TipoAutor.RH,
                "Processo encaminhado para aprovacao da chefia.");
        saveMensagem(saved, abertura.plusDays(2), "Analista RH", TipoAutor.RH,
                "Sua solicitacao foi encaminhada para aprovacao da chefia imediata.", false);
    }

    private void seedProcessoConcluido(TenantTarget tenant, ServidorSeedTarget servidor) {
        String marker = "[DEMO] Processo concluido para validar historico e mensagens finais.";
        if (demoProcessoExists(tenant.id(), marker)) {
            return;
        }

        ProcessoModelo modelo = findModelo(tenant.id(), "LICENCA_MEDICA").orElse(null);
        if (modelo == null) {
            return;
        }

        LocalDateTime abertura = LocalDateTime.now().minusDays(14);
        LocalDateTime conclusao = abertura.plusDays(4);
        Processo processo = buildBaseProcesso(tenant, servidor, modelo, marker, abertura);
        processo.setSituacao(SituacaoProcesso.CONCLUIDO);
        processo.setEtapaAtual(3);
        processo.setPrioridade(Prioridade.NORMAL);
        processo.setOrigemAbertura(OrigemAberturaProcesso.PORTAL_SERVIDOR);
        processo.setDadosFormulario("""
                {"dataInicio":"2026-03-03","diasSolicitados":"5","cid":"J11","nomeMedico":"Dr. Paulo Medeiros","crm":"CRM/PE 12345"}
                """);
        processo.setAtribuidoPara("Equipe de Pericia");
        processo.setDepartamentoAtribuido("Saude Ocupacional");
        processo.setPrazoLimite(abertura.toLocalDate().plusDays(5));
        processo.setDataUltimaAtualizacao(conclusao);
        processo.setDataConclusao(conclusao);
        processo.setResultado(ResultadoProcesso.DEFERIDO);
        processo.setJustificativaResultado("Licenca homologada com base no atestado apresentado.");
        processo.setIntegracaoStatus(IntegracaoStatusProcesso.SUCESSO);

        Processo saved = processoRepository.save(processo);

        saveHistorico(saved, abertura, AcaoProcesso.CRIADO, null, SituacaoProcesso.ABERTO.name(),
                null, 1, servidor.nome(), TipoAutor.SERVIDOR,
                "Solicitacao de licenca enviada pelo portal.");
        saveHistorico(saved, abertura.plusHours(6), AcaoProcesso.EM_ANALISE,
                SituacaoProcesso.ABERTO.name(), SituacaoProcesso.EM_ANALISE.name(),
                1, 2, "Equipe de Pericia", TipoAutor.RH,
                "Atestado recebido para analise tecnica.");
        saveHistorico(saved, conclusao, AcaoProcesso.CONCLUIDO,
                SituacaoProcesso.EM_ANALISE.name(), SituacaoProcesso.CONCLUIDO.name(),
                2, 3, "Equipe de Pericia", TipoAutor.RH,
                "Processo deferido e concluido com sucesso.");
        saveMensagem(saved, abertura.plusMinutes(20), servidor.nome(), TipoAutor.SERVIDOR,
                "Encaminho a licenca medica com o atestado anexo.", true);
        saveMensagem(saved, conclusao, "Equipe de Pericia", TipoAutor.RH,
                "Sua solicitacao foi deferida e a licenca foi registrada com sucesso.", true);
    }

    private Processo buildBaseProcesso(TenantTarget tenant,
                                       ServidorSeedTarget servidor,
                                       ProcessoModelo modelo,
                                       String observacaoServidor,
                                       LocalDateTime dataAbertura) {
        Processo processo = new Processo();
        processo.setProtocolo(processoService.gerarProtocolo());
        processo.setProcessoModelo(modelo);
        processo.setServidorId(servidor.servidorId());
        processo.setServidorNome(servidor.nome());
        processo.setServidorCpf(servidor.cpf());
        processo.setVinculoFuncionalId(servidor.vinculoId());
        processo.setVinculoFuncionalMatricula(servidor.matricula());
        processo.setMunicipioNome(servidor.municipioNome());
        processo.setUnidadeGestoraId(tenant.id());
        processo.setUnidadeGestoraNome(tenant.nome());
        processo.setObservacaoServidor(observacaoServidor);
        processo.setDataAbertura(dataAbertura);
        processo.setDataUltimaAtualizacao(dataAbertura);
        processo.setUsuarioLog(SEED_USER);
        processo.setUsuarioId(1L);
        processo.setExcluido(false);
        processo.setIntegracaoStatus(IntegracaoStatusProcesso.PENDENTE);
        return processo;
    }

    private void saveHistorico(Processo processo,
                               LocalDateTime dataHora,
                               AcaoProcesso acao,
                               String situacaoAnterior,
                               String situacaoNova,
                               Integer etapaAnterior,
                               Integer etapaNova,
                               String usuario,
                               TipoAutor tipoAutor,
                               String descricao) {
        ProcessoHistorico historico = new ProcessoHistorico();
        historico.setProcesso(processo);
        historico.setDataHora(dataHora);
        historico.setAcao(acao);
        historico.setSituacaoAnterior(situacaoAnterior);
        historico.setSituacaoNova(situacaoNova);
        historico.setEtapaAnterior(etapaAnterior);
        historico.setEtapaNova(etapaNova);
        historico.setUsuario(usuario);
        historico.setTipoUsuario(tipoAutor);
        historico.setDescricao(descricao);
        historico.setUnidadeGestoraId(processo.getUnidadeGestoraId());
        historicoRepository.save(historico);
    }

    private void saveMensagem(Processo processo,
                              LocalDateTime dataHora,
                              String autor,
                              TipoAutor tipoAutor,
                              String mensagemTexto,
                              boolean lida) {
        ProcessoMensagem mensagem = new ProcessoMensagem();
        mensagem.setProcesso(processo);
        mensagem.setAutor(autor);
        mensagem.setTipoAutor(tipoAutor);
        mensagem.setMensagem(mensagemTexto);
        mensagem.setDataHora(dataHora);
        mensagem.setLida(lida);
        mensagem.setDataLeitura(lida ? dataHora.plusMinutes(5) : null);
        mensagem.setUnidadeGestoraId(processo.getUnidadeGestoraId());
        mensagem.setUsuarioLog(SEED_USER);
        mensagem.setUsuarioId(1L);
        mensagem.setExcluido(false);
        mensagemRepository.save(mensagem);
    }

    private boolean demoProcessoExists(Long unidadeGestoraId, String observacaoServidor) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                  FROM processos.processo
                 WHERE unidade_gestora_id = ?
                   AND observacao_servidor = ?
                   AND excluido = false
                """,
                Long.class,
                unidadeGestoraId,
                observacaoServidor
        );
        return count != null && count > 0;
    }

    private Optional<ProcessoModelo> findModelo(Long unidadeGestoraId, String codigo) {
        return processoModeloRepository.findByCodigoAndUnidadeGestoraId(codigo, unidadeGestoraId);
    }

    private List<TenantTarget> loadTenantTargets() {
        List<TenantTarget> tenants = jdbcTemplate.query(
                """
                SELECT id, nome
                  FROM unidade_gestora
                 WHERE COALESCE(ativo, true) = true
                   AND COALESCE(excluido, false) = false
                 ORDER BY id
                """,
                (rs, rowNum) -> new TenantTarget(rs.getLong("id"), rs.getString("nome"))
        );

        if (!tenants.isEmpty()) {
            return tenants;
        }

        return List.of(new TenantTarget(1L, "Unidade Gestora Padrao"));
    }

    private List<ServidorSeedTarget> loadServidores(Long unidadeGestoraId) {
        return jdbcTemplate.query(
                """
                SELECT s.id AS servidor_id,
                       s.nome AS servidor_nome,
                       s.cpf AS servidor_cpf,
                       COALESCE(m.nome, ug.nome) AS municipio_nome,
                       vf.id AS vinculo_id,
                       vf.matricula AS vinculo_matricula
                  FROM servidor s
             LEFT JOIN municipio m
                    ON m.id = s.municipio_id
             LEFT JOIN unidade_gestora ug
                    ON ug.id = s.unidade_gestora_id
             LEFT JOIN LATERAL (
                       SELECT v.id, v.matricula
                         FROM vinculo_funcional v
                        WHERE v.servidor_id = s.id
                          AND COALESCE(v.excluido, false) = false
                        ORDER BY CASE WHEN v.situacao = 'ATIVO' THEN 0 ELSE 1 END, v.id
                        LIMIT 1
                   ) vf ON true
                 WHERE s.unidade_gestora_id = ?
                   AND COALESCE(s.excluido, false) = false
                 ORDER BY CASE WHEN s.cpf = ? THEN 0 ELSE 1 END, s.id
                 LIMIT 5
                """,
                (rs, rowNum) -> new ServidorSeedTarget(
                        rs.getLong("servidor_id"),
                        rs.getString("servidor_nome"),
                        rs.getString("servidor_cpf"),
                        nullableLong(rs, "vinculo_id"),
                        rs.getString("vinculo_matricula"),
                        rs.getString("municipio_nome")
                ),
                unidadeGestoraId,
                PREFERRED_PORTAL_CPF
        );
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

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private record TenantTarget(Long id, String nome) {
    }

    private record ServidorSeedTarget(Long servidorId,
                                      String nome,
                                      String cpf,
                                      Long vinculoId,
                                      String matricula,
                                      String municipioNome) {
    }
}
