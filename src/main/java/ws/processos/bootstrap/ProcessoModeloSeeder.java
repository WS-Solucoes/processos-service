package ws.processos.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ws.erh.cadastro.processo.repository.ProcessoModeloRepository;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.core.enums.processo.TipoCampo;
import ws.erh.core.enums.processo.TipoResponsavel;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class ProcessoModeloSeeder implements ApplicationRunner {

    private static final String SEED_USER = "SEEDER";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final ProcessoModeloRepository processoModeloRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        alignProcessoModeloConstraint();

        List<TenantTarget> tenants = loadTenantTargets();
        for (TenantTarget tenant : tenants) {
            for (ProcessoModeloSeed seed : defaultSeeds()) {
                upsertSeed(tenant, seed);
            }
        }
    }

    private void upsertSeed(TenantTarget tenant, ProcessoModeloSeed seed) {
        SeedMatch seedMatch = findSeedMatch(tenant.id(), seed);
        ProcessoModelo modelo = seedMatch.modelo().orElseGet(ProcessoModelo::new);
        boolean forceDefaults = seedMatch.legacyMatch() || modelo.getId() == null;

        if (seedMatch.legacyMatch()) {
            log.info("Migrando modelo legado '{}' para '{}' na unidade gestora {}", modelo.getCodigo(), seed.codigo(), tenant.id());
        }

        applyTopLevelDefaults(modelo, tenant, seed, forceDefaults);

        if (forceDefaults) {
            modelo.getEtapas().clear();
            modelo.getEtapas().addAll(buildEtapas(modelo, tenant.id(), seed.etapas()));

            modelo.getDocumentosExigidos().clear();
            modelo.getDocumentosExigidos().addAll(buildDocumentos(modelo, tenant.id(), seed.documentos()));

            modelo.getCamposAdicionais().clear();
            modelo.getCamposAdicionais().addAll(buildCampos(modelo, tenant.id(), seed.campos()));
        } else {
            mergeEtapas(modelo, tenant.id(), seed.etapas());
            mergeDocumentos(modelo, tenant.id(), seed.documentos());
            mergeCampos(modelo, tenant.id(), seed.campos());
        }

        processoModeloRepository.save(modelo);
    }

    private void applyTopLevelDefaults(ProcessoModelo modelo,
                                       TenantTarget tenant,
                                       ProcessoModeloSeed seed,
                                       boolean forceDefaults) {
        modelo.setCodigo(seed.codigo());
        modelo.setUnidadeGestoraId(tenant.id());
        modelo.setUsuarioLog(SEED_USER);
        modelo.setExcluido(false);

        if (forceDefaults || !StringUtils.hasText(modelo.getNome())) {
            modelo.setNome(seed.nome());
        }
        if (forceDefaults || !StringUtils.hasText(modelo.getDescricao())) {
            modelo.setDescricao(seed.descricao());
        }
        if (forceDefaults || !StringUtils.hasText(modelo.getInstrucoes())) {
            modelo.setInstrucoes(seed.instrucoes());
        }
        if (forceDefaults || modelo.getCategoria() == null) {
            modelo.setCategoria(seed.categoria());
        }
        if (forceDefaults || !StringUtils.hasText(modelo.getIcone())) {
            modelo.setIcone(seed.icone());
        }
        if (forceDefaults || !StringUtils.hasText(modelo.getCor())) {
            modelo.setCor(seed.cor());
        }
        if (forceDefaults || modelo.getPrazoAtendimentoDias() == null) {
            modelo.setPrazoAtendimentoDias(seed.prazoAtendimentoDias());
        }
        if (forceDefaults || modelo.getRequerAprovacaoChefia() == null) {
            modelo.setRequerAprovacaoChefia(seed.requerAprovacaoChefia());
        }
        if (forceDefaults || modelo.getGeraAcaoAutomatica() == null) {
            modelo.setGeraAcaoAutomatica(seed.geraAcaoAutomatica());
        }
        if (forceDefaults || modelo.getAtivo() == null) {
            modelo.setAtivo(seed.ativo());
        }
        if (forceDefaults || modelo.getVisivelPortal() == null) {
            modelo.setVisivelPortal(seed.visivelPortal());
        }
        if (forceDefaults || modelo.getOrdemExibicao() == null || modelo.getOrdemExibicao() <= 0) {
            modelo.setOrdemExibicao(seed.ordemExibicao());
        }
    }

    private void mergeEtapas(ProcessoModelo modelo, Long unidadeGestoraId, List<EtapaSeed> seeds) {
        for (EtapaSeed seed : seeds) {
            boolean exists = modelo.getEtapas().stream().anyMatch(etapa ->
                    Objects.equals(etapa.getOrdem(), seed.ordem())
                            || equalsIgnoreCase(etapa.getNome(), seed.nome()));
            if (!exists) {
                modelo.getEtapas().add(buildEtapa(modelo, unidadeGestoraId, seed));
            }
        }

        modelo.getEtapas().sort(Comparator.comparing(ProcessoEtapaModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)));
    }

    private void mergeDocumentos(ProcessoModelo modelo, Long unidadeGestoraId, List<DocumentoSeed> seeds) {
        for (DocumentoSeed seed : seeds) {
            boolean exists = modelo.getDocumentosExigidos().stream().anyMatch(documento ->
                    Objects.equals(documento.getOrdem(), seed.ordem())
                            || equalsIgnoreCase(documento.getNome(), seed.nome()));
            if (!exists) {
                modelo.getDocumentosExigidos().add(buildDocumento(modelo, unidadeGestoraId, seed));
            }
        }

        modelo.getDocumentosExigidos().sort(Comparator.comparing(ProcessoDocumentoModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)));
    }

    private void mergeCampos(ProcessoModelo modelo, Long unidadeGestoraId, List<CampoSeed> seeds) {
        for (CampoSeed seed : seeds) {
            boolean exists = modelo.getCamposAdicionais().stream().anyMatch(campo ->
                    equalsIgnoreCase(campo.getNomeCampo(), seed.nomeCampo()));
            if (!exists) {
                modelo.getCamposAdicionais().add(buildCampo(modelo, unidadeGestoraId, seed));
            }
        }

        modelo.getCamposAdicionais().sort(Comparator.comparing(ProcessoCampoModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)));
    }

    private List<ProcessoEtapaModelo> buildEtapas(ProcessoModelo modelo, Long unidadeGestoraId, List<EtapaSeed> seeds) {
        List<ProcessoEtapaModelo> etapas = new ArrayList<>();
        for (EtapaSeed seed : seeds) {
            etapas.add(buildEtapa(modelo, unidadeGestoraId, seed));
        }
        return etapas;
    }

    private List<ProcessoDocumentoModelo> buildDocumentos(ProcessoModelo modelo, Long unidadeGestoraId, List<DocumentoSeed> seeds) {
        List<ProcessoDocumentoModelo> documentos = new ArrayList<>();
        for (DocumentoSeed seed : seeds) {
            documentos.add(buildDocumento(modelo, unidadeGestoraId, seed));
        }
        return documentos;
    }

    private List<ProcessoCampoModelo> buildCampos(ProcessoModelo modelo, Long unidadeGestoraId, List<CampoSeed> seeds) {
        List<ProcessoCampoModelo> campos = new ArrayList<>();
        for (CampoSeed seed : seeds) {
            campos.add(buildCampo(modelo, unidadeGestoraId, seed));
        }
        return campos;
    }

    private ProcessoEtapaModelo buildEtapa(ProcessoModelo modelo, Long unidadeGestoraId, EtapaSeed seed) {
        ProcessoEtapaModelo etapa = new ProcessoEtapaModelo();
        etapa.setProcessoModelo(modelo);
        etapa.setNome(seed.nome());
        etapa.setDescricao(seed.descricao());
        etapa.setOrdem(seed.ordem());
        etapa.setTipoResponsavel(seed.tipoResponsavel());
        etapa.setAcaoAutomatica(seed.acaoAutomatica());
        etapa.setPrazoDias(seed.prazoDias());
        etapa.setUnidadeGestoraId(unidadeGestoraId);
        etapa.setUsuarioLog(SEED_USER);
        etapa.setExcluido(false);
        return etapa;
    }

    private ProcessoDocumentoModelo buildDocumento(ProcessoModelo modelo, Long unidadeGestoraId, DocumentoSeed seed) {
        ProcessoDocumentoModelo documento = new ProcessoDocumentoModelo();
        documento.setProcessoModelo(modelo);
        documento.setNome(seed.nome());
        documento.setDescricao(seed.descricao());
        documento.setObrigatorio(seed.obrigatorio());
        documento.setTiposPermitidos(seed.tiposPermitidos());
        documento.setTamanhoMaximoMb(seed.tamanhoMaximoMb());
        documento.setModeloUrl(seed.modeloUrl());
        documento.setOrdem(seed.ordem());
        documento.setUnidadeGestoraId(unidadeGestoraId);
        documento.setUsuarioLog(SEED_USER);
        documento.setExcluido(false);
        return documento;
    }

    private ProcessoCampoModelo buildCampo(ProcessoModelo modelo, Long unidadeGestoraId, CampoSeed seed) {
        ProcessoCampoModelo campo = new ProcessoCampoModelo();
        campo.setProcessoModelo(modelo);
        campo.setNomeCampo(seed.nomeCampo());
        campo.setLabel(seed.label());
        campo.setTipoCampo(seed.tipoCampo());
        campo.setObrigatorio(seed.obrigatorio());
        campo.setOpcoesSelect(seed.opcoesSelect());
        campo.setPlaceholder(seed.placeholder());
        campo.setAjuda(seed.ajuda());
        campo.setOrdem(seed.ordem());
        campo.setUnidadeGestoraId(unidadeGestoraId);
        campo.setUsuarioLog(SEED_USER);
        campo.setExcluido(false);
        return campo;
    }

    private SeedMatch findSeedMatch(Long unidadeGestoraId, ProcessoModeloSeed seed) {
        Optional<ProcessoModelo> canonical = processoModeloRepository.findByCodigoAndUnidadeGestoraId(seed.codigo(), unidadeGestoraId);
        if (canonical.isPresent()) {
            return new SeedMatch(canonical, false);
        }

        for (String legacyCodigo : seed.codigosLegados()) {
            Optional<ProcessoModelo> legacy = processoModeloRepository.findByCodigoAndUnidadeGestoraId(legacyCodigo, unidadeGestoraId);
            if (legacy.isPresent()) {
                return new SeedMatch(legacy, true);
            }
        }

        return new SeedMatch(Optional.empty(), false);
    }

    private List<TenantTarget> loadTenantTargets() {
        try {
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
        } catch (Exception ex) {
            log.warn("Nao foi possivel carregar unidades gestoras ativas para seed de processos: {}", ex.getMessage());
        }

        List<TenantTarget> tenantsFromExistingModels = jdbcTemplate.query(
                """
                SELECT DISTINCT unidade_gestora_id
                FROM processos.processo_modelo
                WHERE unidade_gestora_id IS NOT NULL
                ORDER BY unidade_gestora_id
                """,
                (rs, rowNum) -> new TenantTarget(rs.getLong("unidade_gestora_id"), null)
        );

        if (!tenantsFromExistingModels.isEmpty()) {
            return tenantsFromExistingModels;
        }

        return List.of(new TenantTarget(1L, "Default"));
    }

    private void alignProcessoModeloConstraint() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    """
                    DO $$
                    DECLARE constraint_name text;
                    BEGIN
                        SELECT tc.constraint_name
                        INTO constraint_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.table_schema = 'processos'
                          AND tc.table_name = 'processo_modelo'
                          AND tc.constraint_type = 'UNIQUE'
                          AND kcu.column_name = 'codigo'
                        LIMIT 1;

                        IF constraint_name IS NOT NULL THEN
                            EXECUTE format('ALTER TABLE processos.processo_modelo DROP CONSTRAINT IF EXISTS %I', constraint_name);
                        END IF;
                    END $$;
                    """
            );
            statement.execute("DROP INDEX IF EXISTS processos.processo_modelo_codigo_key");
            statement.execute(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_processo_modelo_codigo_unidade_gestora
                    ON processos.processo_modelo (codigo, unidade_gestora_id)
                    WHERE excluido = false
                    """
            );
        } catch (SQLException ex) {
            log.warn("Nao foi possivel alinhar a constraint de processo_modelo.codigo: {}", ex.getMessage());
        }
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private List<ProcessoModeloSeed> defaultSeeds() {
        return List.of(
                new ProcessoModeloSeed(
                        "FERIAS",
                        List.of("PROC_FERIAS"),
                        "Solicitacao de Ferias",
                        "Solicite suas ferias informando o periodo desejado.",
                        "Informe o periodo desejado e anexe os documentos necessarios para avaliacao do RH e da chefia.",
                        CategoriaProcesso.FERIAS,
                        "CalendarDays",
                        "#3B82F6",
                        15,
                        true,
                        false,
                        true,
                        true,
                        1,
                        List.of(
                                new DocumentoSeed("Formulario de Ferias", "Formulario preenchido de solicitacao", true, "application/pdf", 5, null, 1),
                                new DocumentoSeed("Escala do Setor", "Escala de ferias do setor quando aplicavel", false, "application/pdf,image/jpeg", 5, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Solicitacao do Servidor", "Servidor preenche o formulario de ferias", 1, TipoResponsavel.SERVIDOR, null, 0),
                                new EtapaSeed("Analise do RH", "RH verifica elegibilidade e saldo de ferias", 2, TipoResponsavel.RH, null, 5),
                                new EtapaSeed("Aprovacao da Chefia", "Chefia imediata aprova ou rejeita", 3, TipoResponsavel.CHEFIA, null, 5)
                        ),
                        List.of(
                                new CampoSeed("dataInicio", "Data de Inicio", TipoCampo.DATE, true, null, "Selecione a data de inicio", "Minimo de 30 dias de antecedencia", 1),
                                new CampoSeed("dataFim", "Data de Fim", TipoCampo.DATE, true, null, "Selecione a data de fim", null, 2),
                                new CampoSeed("diasGozo", "Dias de Gozo", TipoCampo.NUMBER, true, null, "30", "Minimo de 10 dias por periodo", 3),
                                new CampoSeed("abonoPecuniario", "Abono Pecuniario", TipoCampo.BOOLEAN, false, null, null, "Converter ate um terco das ferias em abono", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "LICENCA_MEDICA",
                        List.of("PROC_LIC_SAUDE"),
                        "Licenca Medica",
                        "Solicite licenca medica anexando o atestado.",
                        "Anexe o atestado com CID e periodo de afastamento. O RH podera solicitar pericia complementar.",
                        CategoriaProcesso.LICENCA,
                        "Heart",
                        "#EF4444",
                        5,
                        false,
                        false,
                        true,
                        true,
                        2,
                        List.of(
                                new DocumentoSeed("Atestado Medico", "Atestado medico com CID e periodo", true, "application/pdf,image/jpeg,image/png", 10, null, 1),
                                new DocumentoSeed("Laudo Pericial", "Laudo da junta medica quando necessario", false, "application/pdf", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Envio da Documentacao", "Servidor envia o atestado medico", 1, TipoResponsavel.SERVIDOR, null, 0),
                                new EtapaSeed("Pericia Medica", "Analise pela junta medica ou perito", 2, TipoResponsavel.RH, null, 3),
                                new EtapaSeed("Parecer Final", "RH emite parecer final da licenca", 3, TipoResponsavel.RH, null, 2)
                        ),
                        List.of(
                                new CampoSeed("dataInicio", "Data de Inicio da Licenca", TipoCampo.DATE, true, null, "Data do atestado", null, 1),
                                new CampoSeed("diasSolicitados", "Dias Solicitados", TipoCampo.NUMBER, true, null, "Conforme atestado", null, 2),
                                new CampoSeed("cid", "CID (Codigo da Doenca)", TipoCampo.TEXT, true, null, "Ex: M54.5", "Codigo Internacional de Doencas", 3),
                                new CampoSeed("nomeMedico", "Nome do Medico", TipoCampo.TEXT, true, null, "Nome completo", null, 4),
                                new CampoSeed("crm", "CRM do Medico", TipoCampo.TEXT, true, null, "Ex: CRM/PE 12345", null, 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "ATUALIZACAO_CADASTRAL",
                        List.of("PROC_ATUALIZACAO_CADASTRAL"),
                        "Atualizacao Cadastral",
                        "Atualize seus dados pessoais e bancarios.",
                        "Informe os dados a serem alterados e anexe os comprovantes para validacao do RH.",
                        CategoriaProcesso.CADASTRAL,
                        "UserPen",
                        "#8B5CF6",
                        10,
                        false,
                        false,
                        true,
                        true,
                        3,
                        List.of(
                                new DocumentoSeed("Documento de Identificacao", "RG, CNH ou documento oficial com foto", true, "application/pdf,image/jpeg,image/png", 10, null, 1),
                                new DocumentoSeed("Comprovante de Residencia", "Conta recente dos ultimos 3 meses", true, "application/pdf,image/jpeg,image/png", 10, null, 2),
                                new DocumentoSeed("Comprovante Bancario", "Comprovante de conta bancaria quando aplicavel", false, "application/pdf,image/jpeg", 10, null, 3)
                        ),
                        List.of(
                                new EtapaSeed("Envio dos Dados", "Servidor informa os novos dados cadastrais", 1, TipoResponsavel.SERVIDOR, null, 0),
                                new EtapaSeed("Validacao pelo RH", "RH valida documentos e atualiza o cadastro", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("tipoDado", "Tipo de Alteracao", TipoCampo.SELECT, true, "Endereco|Telefone|E-mail|Dados Bancarios|Estado Civil|Outros", "Selecione o tipo", null, 1),
                                new CampoSeed("novoValor", "Descricao da Alteracao", TipoCampo.TEXTAREA, true, null, "Descreva os dados a serem alterados", null, 2)
                        )
                ),
                new ProcessoModeloSeed(
                        "DECLARACAO",
                        List.of(),
                        "Solicitacao de Declaracoes",
                        "Solicite declaracoes e certidoes funcionais.",
                        "Informe a declaracao desejada e a finalidade do pedido.",
                        CategoriaProcesso.DOCUMENTAL,
                        "FileText",
                        "#F59E0B",
                        7,
                        false,
                        false,
                        true,
                        true,
                        4,
                        List.of(
                                new DocumentoSeed("Justificativa", "Documento que comprove a necessidade quando exigido", false, "application/pdf", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Solicitacao", "Servidor solicita a declaracao desejada", 1, TipoResponsavel.SERVIDOR, null, 0),
                                new EtapaSeed("Emissao pelo RH", "RH emite a declaracao solicitada", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("tipoDeclaracao", "Tipo de Declaracao", TipoCampo.SELECT, true, "Vinculo Funcional|Tempo de Servico|Nada Consta|Rendimentos|Frequencia|Outros", "Selecione o tipo", null, 1),
                                new CampoSeed("finalidade", "Finalidade", TipoCampo.TEXT, true, null, "Ex: financiamento bancario", "Informe a finalidade da declaracao", 2),
                                new CampoSeed("quantidadeVias", "Quantidade de Vias", TipoCampo.NUMBER, false, null, "1", null, 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "AUXILIO_TRANSPORTE",
                        List.of(),
                        "Auxilio Transporte",
                        "Solicite ou atualize seu auxilio transporte.",
                        "Informe o itinerario e anexe os comprovantes para analise do RH e da chefia.",
                        CategoriaProcesso.FINANCEIRO,
                        "Bus",
                        "#10B981",
                        10,
                        true,
                        false,
                        true,
                        true,
                        5,
                        List.of(
                                new DocumentoSeed("Comprovante de Residencia", "Comprovante atualizado dos ultimos 3 meses", true, "application/pdf,image/jpeg,image/png", 10, null, 1),
                                new DocumentoSeed("Declaracao de Itinerario", "Declaracao com o itinerario residencia-trabalho", true, "application/pdf", 5, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Solicitacao do Servidor", "Servidor informa o itinerario", 1, TipoResponsavel.SERVIDOR, null, 0),
                                new EtapaSeed("Analise do RH", "RH analisa a documentacao e calcula o valor", 2, TipoResponsavel.RH, null, 5),
                                new EtapaSeed("Aprovacao da Chefia", "Chefia valida as informacoes", 3, TipoResponsavel.CHEFIA, null, 3)
                        ),
                        List.of(
                                new CampoSeed("endereco", "Endereco Residencial", TipoCampo.TEXT, true, null, "Endereco completo", null, 1),
                                new CampoSeed("meioTransporte", "Meio de Transporte", TipoCampo.SELECT, true, "Onibus|Metro|Trem|Van|Misto", "Selecione", null, 2),
                                new CampoSeed("valorDiario", "Valor Diario Estimado (R$)", TipoCampo.NUMBER, true, null, "Ex: 15.00", "Valor estimado em reais", 3),
                                new CampoSeed("diasUteis", "Dias Uteis por Mes", TipoCampo.NUMBER, false, null, "22", null, 4)
                        )
                )
        );
    }

    private record TenantTarget(Long id, String nome) {
    }

    private record SeedMatch(Optional<ProcessoModelo> modelo, boolean legacyMatch) {
    }

    private record ProcessoModeloSeed(String codigo,
                                      List<String> codigosLegados,
                                      String nome,
                                      String descricao,
                                      String instrucoes,
                                      CategoriaProcesso categoria,
                                      String icone,
                                      String cor,
                                      Integer prazoAtendimentoDias,
                                      Boolean requerAprovacaoChefia,
                                      Boolean geraAcaoAutomatica,
                                      Boolean ativo,
                                      Boolean visivelPortal,
                                      Integer ordemExibicao,
                                      List<DocumentoSeed> documentos,
                                      List<EtapaSeed> etapas,
                                      List<CampoSeed> campos) {
    }

    private record DocumentoSeed(String nome,
                                 String descricao,
                                 Boolean obrigatorio,
                                 String tiposPermitidos,
                                 Integer tamanhoMaximoMb,
                                 String modeloUrl,
                                 Integer ordem) {
    }

    private record EtapaSeed(String nome,
                             String descricao,
                             Integer ordem,
                             TipoResponsavel tipoResponsavel,
                             String acaoAutomatica,
                             Integer prazoDias) {
    }

    private record CampoSeed(String nomeCampo,
                             String label,
                             TipoCampo tipoCampo,
                             Boolean obrigatorio,
                             String opcoesSelect,
                             String placeholder,
                             String ajuda,
                             Integer ordem) {
    }
}
