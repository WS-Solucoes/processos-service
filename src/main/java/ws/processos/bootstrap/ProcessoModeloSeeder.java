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
        if (forceDefaults || modelo.getRequerAprovacaoSuperior() == null) {
            modelo.setRequerAprovacaoSuperior(seed.requerAprovacaoSuperior());
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
                    FROM common.unidade_gestora
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
                                new EtapaSeed("Aprovação do Superior", "Superior imediato aprova ou rejeita", 3, TipoResponsavel.SUPERIOR, null, 5)
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
                                new EtapaSeed("Aprovacao do Superior", "Superior valida as informacoes", 3, TipoResponsavel.SUPERIOR, null, 3)
                        ),
                        List.of(
                                new CampoSeed("endereco", "Endereco Residencial", TipoCampo.TEXT, true, null, "Endereco completo", null, 1),
                                new CampoSeed("meioTransporte", "Meio de Transporte", TipoCampo.SELECT, true, "Onibus|Metro|Trem|Van|Misto", "Selecione", null, 2),
                                new CampoSeed("valorDiario", "Valor Diario Estimado (R$)", TipoCampo.NUMBER, true, null, "Ex: 15.00", "Valor estimado em reais", 3),
                                new CampoSeed("diasUteis", "Dias Uteis por Mes", TipoCampo.NUMBER, false, null, "22", null, 4)
                        )
                ),
                // ── V017: 7 novos modelos ──────────────────────────────────────────────
                new ProcessoModeloSeed(
                        "PROC_LIC_PATERNIDADE",
                        List.of(),
                        "Licenca Paternidade",
                        "Licenca ao servidor pelo nascimento de filho (5 dias, prorrogavel para 20 dias)",
                        "Solicite a licenca em ate 2 dias apos o nascimento. Anexe a certidao de nascimento assim que disponivel. A licenca padrao e de 5 dias. Se o orgao aderiu ao Programa Empresa Cidada, pode ser de 20 dias.",
                        CategoriaProcesso.LICENCA,
                        "FamilyRestroom",
                        "#06B6D4",
                        3,
                        false,
                        true,
                        true,
                        true,
                        9,
                        List.of(
                                new DocumentoSeed("Certidao de Nascimento", "Certidao de nascimento da crianca", true, "PDF,JPG,PNG", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Registro", "Verificacao da certidao e registro da licenca", 1, TipoResponsavel.RH, null, 2)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio da Licenca", TipoCampo.DATE, true, null, "", "Data do nascimento ou primeiro dia de licenca", 1),
                                new CampoSeed("dias_afastamento", "Duracao da Licenca (dias)", TipoCampo.SELECT, true, "5|20", "", "5 dias (padrao) ou 20 dias se aderiu ao Programa Empresa Cidada", 2),
                                new CampoSeed("nome_crianca", "Nome da Crianca", TipoCampo.TEXT, false, null, "", "Se ja registrada na certidao de nascimento", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_LUTO",
                        List.of(),
                        "Licenca por Falecimento",
                        "Licenca em razao de falecimento de conjuge, pais, filhos ou dependentes (8 dias)",
                        "Solicite a licenca em ate 24 horas do falecimento. Anexe a certidao de obito e, se necessario, comprovante do grau de parentesco. A licenca e de 8 dias consecutivos.",
                        CategoriaProcesso.LICENCA,
                        "Article",
                        "#6B7280",
                        2,
                        false,
                        true,
                        true,
                        true,
                        10,
                        List.of(
                                new DocumentoSeed("Certidao de Obito", "Certidao de obito do familiar falecido", true, "PDF,JPG,PNG", 10, null, 1),
                                new DocumentoSeed("Comprovante de Parentesco", "Certidao de casamento, nascimento ou documento que comprove o grau de parentesco", false, "PDF,JPG,PNG", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Registro", "Verificacao dos documentos e registro da licenca", 1, TipoResponsavel.RH, null, 1)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data do Falecimento", TipoCampo.DATE, true, null, "", "Data em que ocorreu o falecimento", 1),
                                new CampoSeed("grau_parentesco", "Grau de Parentesco", TipoCampo.SELECT, true, "Conjuge / Companheiro(a)|Pai|Mae|Filho(a)|Irmao / Irma|Sogro / Sogra|Dependente declarado", "", "Selecione o grau de parentesco", 2),
                                new CampoSeed("nome_falecido", "Nome do Falecido", TipoCampo.TEXT, true, null, "", "Nome completo do familiar falecido", 3),
                                new CampoSeed("dias_afastamento", "Dias de Licenca", TipoCampo.NUMBER, true, null, "8", "Conforme legislacao do ente publico (geralmente 8 dias)", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_GALA",
                        List.of(),
                        "Licenca Gala (Casamento)",
                        "Licenca por motivo de casamento — 8 dias consecutivos",
                        "Solicite a licenca em ate 2 dias do casamento. A certidao de casamento podera ser enviada em ate 10 dias apos a cerimonia. A licenca e de 8 dias consecutivos a partir da data do casamento.",
                        CategoriaProcesso.LICENCA,
                        "FamilyRestroom",
                        "#EC4899",
                        2,
                        false,
                        true,
                        true,
                        true,
                        11,
                        List.of(
                                new DocumentoSeed("Certidao de Casamento", "Certidao de casamento ou declaracao do cartorio", true, "PDF,JPG,PNG", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Registro", "Verificacao da certidao e registro da licenca", 1, TipoResponsavel.RH, null, 1)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data do Casamento", TipoCampo.DATE, true, null, "", "Data da cerimonia de casamento", 1),
                                new CampoSeed("dias_afastamento", "Dias de Licenca", TipoCampo.NUMBER, true, null, "8", "Conforme legislacao vigente (padrao 8 dias)", 2),
                                new CampoSeed("nome_conjuge", "Nome do Conjuge", TipoCampo.TEXT, true, null, "", "Nome completo do conjuge", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_CAPACITACAO",
                        List.of(),
                        "Licenca para Capacitacao",
                        "Afastamento para participacao em programa de treinamento regularmente instituido (a cada 5 anos, ate 3 meses)",
                        "Solicite com antecedencia minima de 30 dias. Informe o curso/programa, a instituicao e o periodo. A chefia imediata deve aprovar. A licenca nao implica interrupcao do vencimento.",
                        CategoriaProcesso.AFASTAMENTO,
                        "School",
                        "#7C3AED",
                        20,
                        true,
                        true,
                        true,
                        true,
                        12,
                        List.of(
                                new DocumentoSeed("Comprovante de Matricula ou Convocacao", "Documento da instituicao comprovando a matricula ou convocacao para o curso", true, "PDF", 10, null, 1),
                                new DocumentoSeed("Plano do Curso / Ementa", "Programa detalhado do treinamento ou curso", false, "PDF", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de elegibilidade (quinquenio e prioridade)", 1, TipoResponsavel.RH, null, 5),
                                new EtapaSeed("Aprovacao do Superior", "Aprovacao do superior imediato pelo interesse do servico", 2, TipoResponsavel.SUPERIOR, null, 5),
                                new EtapaSeed("Publicacao e Registro", "Publicacao da portaria e registro do afastamento", 3, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Data de inicio do curso/afastamento", 1),
                                new CampoSeed("dias_afastamento", "Duracao (dias)", TipoCampo.NUMBER, true, null, "", "Maximo de 90 dias por quinquenio", 2),
                                new CampoSeed("nome_curso", "Nome do Curso / Programa", TipoCampo.TEXT, true, null, "", "Denominacao oficial do treinamento", 3),
                                new CampoSeed("instituicao", "Instituicao Promotora", TipoCampo.TEXT, true, null, "", "Nome da escola, universidade ou entidade promotora", 4),
                                new CampoSeed("modalidade", "Modalidade", TipoCampo.SELECT, true, "Presencial|EAD / Online|Semi-presencial", "", "Modalidade de realizacao do curso", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_PROGRESSAO_FUNCIONAL",
                        List.of(),
                        "Progressao Funcional",
                        "Solicitacao de progressao por merito ou antiguidade conforme plano de cargos e carreiras",
                        "Verifique se voce completou o intersticio necessario (geralmente 2 anos) e se atende os criterios de avaliacao de desempenho. Anexe os documentos comprobatorios.",
                        CategoriaProcesso.CADASTRAL,
                        "HowToReg",
                        "#059669",
                        30,
                        true,
                        false,
                        true,
                        true,
                        13,
                        List.of(
                                new DocumentoSeed("Avaliacao de Desempenho", "Ultima avaliacao de desempenho realizada", true, "PDF", 5, null, 1),
                                new DocumentoSeed("Requerimento de Progressao", "Formulario de requerimento assinado", true, "PDF", 5, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de intersticio e documentacao", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Aprovacao do Superior", "Validacao do superior imediato sobre merito e frequencia", 2, TipoResponsavel.SUPERIOR, null, 5),
                                new EtapaSeed("Publicacao do Ato", "Emissao da portaria e atualizacao cadastral", 3, TipoResponsavel.RH, null, 15)
                        ),
                        List.of(
                                new CampoSeed("tipo_progressao", "Tipo de Progressao", TipoCampo.SELECT, true, "Por merito (avaliacao de desempenho)|Por antiguidade (intersticio)|Por qualificacao (titulacao)", "", "Selecione o fundamento legal da progressao", 1),
                                new CampoSeed("nivel_atual", "Nivel/Referencia Atual", TipoCampo.TEXT, true, null, "Ex: A-I, B-II, Nivel 3", "Seu posicionamento atual na tabela de vencimentos", 2),
                                new CampoSeed("data_ultimo_progresso", "Data da Ultima Progressao", TipoCampo.DATE, true, null, "", "Data da Portaria da ultima progressao", 3),
                                new CampoSeed("observacoes", "Informacoes Adicionais", TipoCampo.TEXTAREA, false, null, "", "Cursos, titulos ou outros fatores relevantes", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_DADOS_BANCARIOS",
                        List.of(),
                        "Alteracao de Dados Bancarios",
                        "Solicitar alteracao da conta-corrente ou poupanca para recebimento de vencimentos",
                        "Informe os novos dados bancarios e envie o comprovante. A alteracao sera aplicada na proxima folha de pagamento. Certifique-se que a conta esta ativa e em seu nome.",
                        CategoriaProcesso.FINANCEIRO,
                        "AccountBalance",
                        "#0EA5E9",
                        5,
                        false,
                        false,
                        true,
                        true,
                        14,
                        List.of(
                                new DocumentoSeed("Comprovante Bancario", "Cartao, extrato ou comprovante de conta (com nome, banco e agencia)", true, "PDF,JPG,PNG", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Atualizacao", "Verificacao do comprovante e atualizacao dos dados bancarios", 1, TipoResponsavel.RH, null, 3)
                        ),
                        List.of(
                                new CampoSeed("banco", "Banco", TipoCampo.SELECT, true, "Banco do Brasil|Caixa Economica Federal|Bradesco|Itau|Santander|Sicoob|Sicredi|Nubank|Inter|Outro", "", "Selecione o banco", 1),
                                new CampoSeed("agencia", "Agencia", TipoCampo.TEXT, true, null, "Ex: 0001", "Numero da agencia (sem digito)", 2),
                                new CampoSeed("conta", "Numero da Conta", TipoCampo.TEXT, true, null, "Ex: 12345-6", "Numero da conta com digito verificador", 3),
                                new CampoSeed("tipo_conta", "Tipo de Conta", TipoCampo.SELECT, true, "Conta-Corrente|Conta Poupanca", "", "Selecione o tipo de conta", 4),
                                new CampoSeed("chave_pix", "Chave PIX (opcional)", TipoCampo.TEXT, false, null, "CPF, e-mail, telefone ou chave aleatoria", "Informar caso deseje cadastrar tambem como opcao de pagamento", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_DECLARACAO_FUNCIONAL",
                        List.of(),
                        "Declaracao de Vinculo Funcional",
                        "Solicitacao de declaracao para comprovacao de vinculo empregaticio, cargo e vencimentos",
                        "Informe a finalidade da declaracao. O documento sera emitido pelo Departamento de Recursos Humanos em papel timbrado com assinatura do responsavel.",
                        CategoriaProcesso.DOCUMENTAL,
                        "Badge",
                        "#6366F1",
                        10,
                        false,
                        false,
                        true,
                        true,
                        15,
                        List.of(),
                        List.of(
                                new EtapaSeed("Emissao da Declaracao", "Elaboracao e assinatura da declaracao", 1, TipoResponsavel.RH, null, 8)
                        ),
                        List.of(
                                new CampoSeed("finalidade", "Finalidade da Declaracao", TipoCampo.SELECT, true, "Financiamento imobiliario|Abertura de conta bancaria|Comprovacao de renda — geral|Processo seletivo / Concurso|Vistos e documentos internacionais|Plano de saude / seguro|Outro", "", "Para que sera utilizada a declaracao?", 1),
                                new CampoSeed("informacoes_adicionais", "Informacoes Especificas Necessarias", TipoCampo.TEXTAREA, false, null, "Ex: Declaracao deve constar o cargo efetivo e a remuneracao bruta", "Se a declaracao precisa de informacoes especificas nao padronizadas", 2),
                                new CampoSeed("quantidade_vias", "Quantidade de Vias", TipoCampo.NUMBER, true, null, "1", "Numero de copias necessarias (maximo 3)", 3)
                        )
                ),
                // ── V018: 19 novos modelos ─────────────────────────────────────────────
                new ProcessoModeloSeed(
                        "PROC_LIC_ACOMPANHAMENTO",
                        List.of(),
                        "Licenca — Acompanhamento de Familiar",
                        "Licenca para acompanhar conjuge ou companheiro deslocado por interesse de servico para outro ponto do territorio nacional ou exterior (sem vencimento)",
                        "Apresente a copia do ato de remocao/designacao do conjuge ou companheiro. A licenca pode ser concedida por ate 8 anos, consecutivos ou nao. Durante este periodo voce ficara sem remuneracao. Solicite com antecedencia de 30 dias.",
                        CategoriaProcesso.LICENCA,
                        "FamilyRestroom",
                        "#0891B2",
                        20,
                        true,
                        true,
                        true,
                        true,
                        16,
                        List.of(
                                new DocumentoSeed("Ato de Remocao / Designacao do Conjuge", "Portaria ou ato oficial que comprova o deslocamento do conjuge por interesse de servico", true, "PDF", 10, null, 1),
                                new DocumentoSeed("Comprovante de Uniao Estavel / Certidao de Casamento", "Documento que comprova o vinculo conjugal", true, "PDF,JPG,PNG", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de elegibilidade e conformidade documental", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Aprovacao e Publicacao", "Emissao da portaria de concessao", 2, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Data prevista para inicio da licenca", 1),
                                new CampoSeed("dias_afastamento", "Duracao Solicitada (dias)", TipoCampo.NUMBER, true, null, "", "Maximo de 2.920 dias (8 anos) consecutivos ou nao", 2),
                                new CampoSeed("nome_conjuge", "Nome do Conjuge / Companheiro(a)", TipoCampo.TEXT, true, null, "", "Nome completo do conjuge ou companheiro que foi deslocado", 3),
                                new CampoSeed("orgao_destino", "Orgao de Destino do Conjuge", TipoCampo.TEXT, true, null, "", "Orgao/entidade para onde o conjuge foi designado", 4),
                                new CampoSeed("cidade_destino", "Cidade / Pais de Destino", TipoCampo.TEXT, true, null, "", "Local para onde o conjuge se deslocou", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_SAUDE_FAMILIAR",
                        List.of(),
                        "Licenca — Saude de Familiar",
                        "Licenca para tratamento de saude de pessoa da familia, mediante comprovacao medica (sem vencimento, max 30 dias por ano)",
                        "Apresente o atestado medico do familiar indicando o tratamento necessario e o periodo. A licenca e concedida sem remuneracao por ate 30 dias por ano. Pode ser prorrogada mediante nova comprovacao.",
                        CategoriaProcesso.LICENCA,
                        "LocalHospital",
                        "#DC2626",
                        5,
                        false,
                        true,
                        true,
                        true,
                        17,
                        List.of(
                                new DocumentoSeed("Atestado Medico do Familiar", "Atestado original do medico responsavel pelo tratamento da pessoa da familia", true, "PDF,JPG,PNG", 10, null, 1),
                                new DocumentoSeed("Comprovante de Parentesco", "Certidao de nascimento, casamento ou outro documento que prove o vinculo familiar", true, "PDF,JPG,PNG", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Concessao", "Avaliacao dos documentos e registro do afastamento", 1, TipoResponsavel.RH, null, 3)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Inicio do tratamento / afastamento", 1),
                                new CampoSeed("dias_afastamento", "Dias Solicitados", TipoCampo.NUMBER, true, null, "", "Max. 30 dias por exercicio", 2),
                                new CampoSeed("nome_familiar", "Nome do Familiar", TipoCampo.TEXT, true, null, "", "Nome completo da pessoa que necessita do tratamento", 3),
                                new CampoSeed("grau_parentesco", "Grau de Parentesco", TipoCampo.SELECT, true, "Conjuge / Companheiro(a)|Pai|Mae|Filho(a)|Irmao / Irma|Sogro / Sogra|Dependente declarado", "", "Selecione o grau de parentesco", 4),
                                new CampoSeed("cid", "CID (Codigo da Doenca)", TipoCampo.TEXT, false, null, "Ex: C50.9", "Codigo CID informado no atestado", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_ADOCAO",
                        List.of(),
                        "Licenca por Adocao",
                        "Licenca a servidora adotante ou ao adotante solteiro sob guarda judicial de crianca (90 a 120 dias conforme idade)",
                        "Apresente o termo de guarda/adocao emitido pela Vara da Infancia e Juventude. A licenca e de 90 dias para criancas entre 1 e 4 anos, ou 120 dias para menores de 1 ano. Pode haver prorrogacao para 180 dias se o orgao aderiu ao Empresa Cidada.",
                        CategoriaProcesso.LICENCA,
                        "ChildCare",
                        "#D97706",
                        5,
                        false,
                        true,
                        true,
                        true,
                        18,
                        List.of(
                                new DocumentoSeed("Termo de Guarda / Adocao", "Termo judicial de guarda provisoria ou definitiva", true, "PDF", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Registro", "Verificacao do termo e concessao da licenca", 1, TipoResponsavel.RH, null, 3)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio da Licenca", TipoCampo.DATE, true, null, "", "Data da guarda / entrega da crianca", 1),
                                new CampoSeed("dias_afastamento", "Duracao da Licenca (dias)", TipoCampo.SELECT, true, "90|120|180", "", "90 dias (1-4 anos) / 120 dias (< 1 ano) / 180 dias (Empresa Cidada)", 2),
                                new CampoSeed("nome_crianca", "Nome da Crianca", TipoCampo.TEXT, true, null, "", "Nome da crianca adotada ou sob guarda", 3),
                                new CampoSeed("data_nascimento_crianca", "Data de Nascimento da Crianca", TipoCampo.DATE, true, null, "", "Usada para determinar a duracao da licenca", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_MANDATO_CLASSISTA",
                        List.of(),
                        "Licenca Mandato Classista / Sindical",
                        "Licenca para exercicio de mandato em entidade representativa de classe (sindicato, associacao, federacao). Sem remuneracao, pelo tempo do mandato.",
                        "Apresente a ata de eleicao ou documento que comprove sua eleicao para o mandato. A licenca dura pelo periodo do mandato. Seu vinculo com o orgao e mantido sem remuneracao.",
                        CategoriaProcesso.LICENCA,
                        "Gavel",
                        "#7C3AED",
                        10,
                        false,
                        true,
                        true,
                        true,
                        19,
                        List.of(
                                new DocumentoSeed("Ata de Eleicao / Posse", "Documento que comprova a eleicao para o mandato na entidade representativa", true, "PDF", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao da documentacao e conformidade legal", 1, TipoResponsavel.RH, null, 5),
                                new EtapaSeed("Publicacao do Ato", "Emissao da portaria de concessao da licenca", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio do Mandato", TipoCampo.DATE, true, null, "", "Data de posse ou inicio do mandato", 1),
                                new CampoSeed("dias_afastamento", "Duracao do Mandato (dias)", TipoCampo.NUMBER, true, null, "", "Informe a duracao prevista em dias", 2),
                                new CampoSeed("entidade_representativa", "Entidade Representativa", TipoCampo.TEXT, true, null, "Ex: Sindicato dos Servidores do Estado", "Nome do sindicato, federacao, confederacao ou associacao", 3),
                                new CampoSeed("cargo_mandato", "Cargo no Mandato", TipoCampo.TEXT, true, null, "Ex: Presidente, Diretor, Conselheiro", "Cargo para o qual foi eleito", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_SEM_VENCIMENTO",
                        List.of(),
                        "Licenca Sem Vencimento",
                        "Licenca por interesse particular, sem remuneracao, para servidor com mais de 5 anos de efetivo exercicio (max. 3 anos)",
                        "Voce deve ter no minimo 5 anos de efetivo exercicio no servico publico. A licenca pode ser concedida por ate 3 anos (nao prorrogavel no mesmo cargo). Neste periodo nao ha remuneracao nem computo de tempo de servico para aposentadoria.",
                        CategoriaProcesso.LICENCA,
                        "WorkOutline",
                        "#475569",
                        20,
                        true,
                        true,
                        true,
                        true,
                        20,
                        List.of(
                                new DocumentoSeed("Requerimento Fundamentado", "Requerimento detalhando o motivo do interesse particular", true, "PDF", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de elegibilidade (quinquenio de exercicio)", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Aprovacao do Superior", "Parecer do superior sobre o interesse do servico", 2, TipoResponsavel.SUPERIOR, null, 5),
                                new EtapaSeed("Publicacao", "Emissao e publicacao do ato de concessao", 3, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Data prevista para o inicio da licenca", 1),
                                new CampoSeed("dias_afastamento", "Duracao Solicitada (dias)", TipoCampo.NUMBER, true, null, "", "Maximo de 1.095 dias (3 anos)", 2),
                                new CampoSeed("motivo", "Motivo da Solicitacao", TipoCampo.TEXTAREA, true, null, "", "Descreva o interesse particular que justifica a licenca", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_LIC_SERVICO_MILITAR",
                        List.of(),
                        "Licenca para Servico Militar",
                        "Licenca em razao de convocacao ou designacao para servico militar obrigatorio",
                        "Apresente o documento de convocacao das Forcas Armadas. Voce mantera todos os direitos e vantagens e o tempo sera computado como de efetivo exercicio.",
                        CategoriaProcesso.LICENCA,
                        "LocalPolice",
                        "#1D4ED8",
                        5,
                        false,
                        true,
                        true,
                        true,
                        21,
                        List.of(
                                new DocumentoSeed("Documento de Convocacao Militar", "Ordem de convocacao ou designacao das Forcas Armadas", true, "PDF,JPG", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Registro e Concessao", "Registro da convocacao e concessao automatica da licenca", 1, TipoResponsavel.RH, null, 3)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Incorporacao", TipoCampo.DATE, true, null, "", "Data de inicio do servico militar", 1),
                                new CampoSeed("dias_afastamento", "Duracao Prevista (dias)", TipoCampo.NUMBER, true, null, "", "Duracao conforme o documento de convocacao", 2),
                                new CampoSeed("forca_armada", "Forca Armada", TipoCampo.SELECT, true, "Exercito Brasileiro|Marinha do Brasil|Forca Aerea Brasileira|Policia Militar Estadual|Corpo de Bombeiros", "", "Selecione a forca que realizou a convocacao", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_AFASTAMENTO_MANDATO_ELETIVO",
                        List.of(),
                        "Afastamento — Mandato Eletivo",
                        "Afastamento para exercicio de mandato em cargo eletivo federal, estadual, distrital ou municipal",
                        "Apresente o diploma ou certidao de diplomacao expedida pelo TRE/TSE. O afastamento e pelo prazo do mandato. Verifique com o RH as condicoes de remuneracao conforme a legislacao vigente.",
                        CategoriaProcesso.AFASTAMENTO,
                        "Gavel",
                        "#1E40AF",
                        15,
                        false,
                        true,
                        true,
                        true,
                        22,
                        List.of(
                                new DocumentoSeed("Diploma / Certidao de Diplomacao", "Documento expedido pela Justica Eleitoral (TRE/TSE)", true, "PDF", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise Juridica / RH", "Conferencia do diploma e analise das condicoes do afastamento", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Publicacao do Ato", "Publicacao da portaria de afastamento", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Posse no Cargo Eletivo", TipoCampo.DATE, true, null, "", "Data de inicio do exercicio do mandato", 1),
                                new CampoSeed("dias_afastamento", "Duracao do Mandato (dias)", TipoCampo.NUMBER, true, null, "", "Duracao total do mandato em dias", 2),
                                new CampoSeed("nivel_mandato", "Esfera do Mandato", TipoCampo.SELECT, true, "Federal (Deputado Federal / Senador)|Estadual / Distrital (Deputado Estadual)|Municipal (Vereador)|Executivo Municipal (Prefeito / Vice)", "", "Selecione a esfera do mandato eletivo", 3),
                                new CampoSeed("municipio_uf", "Municipio / UF", TipoCampo.TEXT, true, null, "Ex: Florianopolis/SC", "Local de exercicio do mandato", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_AFASTAMENTO_ESTUDO_EXTERIOR",
                        List.of(),
                        "Afastamento — Estudo no Exterior",
                        "Afastamento para estudo, missao oficial ou pos-graduacao no exterior com ou sem onus para o erario",
                        "Solicite com antecedencia minima de 60 dias. Informe o programa, a instituicao no exterior e o periodo. A aprovacao depende do interesse do servico e disponibilidade orcamentaria para os casos com onus.",
                        CategoriaProcesso.AFASTAMENTO,
                        "School",
                        "#065F46",
                        30,
                        true,
                        true,
                        true,
                        true,
                        23,
                        List.of(
                                new DocumentoSeed("Carta de Aceitacao / Convite Institucional", "Documento da instituicao estrangeira comprovando aceitacao no programa", true, "PDF", 10, null, 1),
                                new DocumentoSeed("Plano de Trabalho / Pesquisa", "Descricao das atividades a serem desenvolvidas no exterior", true, "PDF", 10, null, 2),
                                new DocumentoSeed("Comprovante de Bolsa / Financiamento (se houver)", "Carta de concessao de bolsa do CNPq, CAPES ou outra agencia", false, "PDF", 10, null, 3)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de requisitos (tempo de servico, cargo, elegibilidade)", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Aprovacao da Direcao / Superior", "Autorizacao pelo superior imediato e direcao do orgao", 2, TipoResponsavel.SUPERIOR, null, 10),
                                new EtapaSeed("Publicacao e Registro", "Publicacao da portaria e abertura do processo", 3, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Data de saida para o exterior", 1),
                                new CampoSeed("dias_afastamento", "Duracao (dias)", TipoCampo.NUMBER, true, null, "", "Duracao total do afastamento em dias", 2),
                                new CampoSeed("pais_destino", "Pais de Destino", TipoCampo.TEXT, true, null, "Ex: Portugal", "Pais onde serao realizadas as atividades", 3),
                                new CampoSeed("instituicao_destino", "Instituicao no Exterior", TipoCampo.TEXT, true, null, "Ex: Universidade de Lisboa", "Nome da universidade, laboratorio ou instituicao anfitria", 4),
                                new CampoSeed("tipo_afastamento", "Tipo", TipoCampo.SELECT, true, "Doutorado|Pos-doutorado|Mestrado|Especializacao|Missao Oficial|Intercambio / Visita tecnica", "", "Selecione o tipo de afastamento", 5),
                                new CampoSeed("onus", "Custeio", TipoCampo.SELECT, true, "Com onus (orgao custeia)|Com onus limitado (bolsa externa)|Sem onus para o erario", "", "Quem custeia o afastamento?", 6)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_AFASTAMENTO_COOPERACAO",
                        List.of(),
                        "Afastamento — Cooperacao Tecnica",
                        "Afastamento para prestacao de servicos a outro orgao ou entidade (cessao ou cooperacao tecnica)",
                        "Apresente o termo de cooperacao, convenio ou requisicao do orgao tomador. Informe as condicoes (com ou sem onus para o orgao cedente). A aprovacao depende do interesse do servico.",
                        CategoriaProcesso.AFASTAMENTO,
                        "BusinessCenter",
                        "#0369A1",
                        20,
                        true,
                        true,
                        true,
                        true,
                        24,
                        List.of(
                                new DocumentoSeed("Requisicao / Termo de Cooperacao", "Documento formalizado pelo orgao tomador solicitando os servicos", true, "PDF", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Avaliacao RH / Direcao", "Analise do interesse do servico e condicoes da cessao", 1, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Publicacao", "Emissao do ato de cessao e comunicado ao orgao tomador", 2, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("data_inicio_afastamento", "Data de Inicio", TipoCampo.DATE, true, null, "", "Data de inicio do afastamento", 1),
                                new CampoSeed("dias_afastamento", "Duracao (dias)", TipoCampo.NUMBER, true, null, "", "Periodo de cooperacao em dias", 2),
                                new CampoSeed("orgao_destino", "Orgao Tomador", TipoCampo.TEXT, true, null, "Ex: Secretaria de Saude do Estado de SP", "Nome do orgao ou entidade que vai receber o servidor", 3),
                                new CampoSeed("onus", "Onus Remuneratorio", TipoCampo.SELECT, true, "Com onus para o cedente (orgao de origem paga)|Com onus para o tomador (orgao destino paga)|Sem onus (sem vencimentos)", "", "Quem sera responsavel pela remuneracao", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_CAT",
                        List.of(),
                        "Comunicado de Acidente de Trabalho (CAT)",
                        "Registro de acidente de trabalho ou doenca profissional/do trabalho para fins de afastamento e previdencia",
                        "Registre imediatamente. Em caso de acidente, relate detalhadamente o ocorrido. Para doenca ocupacional, informe os sintomas e o nexo com as atividades. A CAT deve ser emitida em ate 24h (acidente fatal) ou 1 dia util seguinte.",
                        CategoriaProcesso.AFASTAMENTO,
                        "MedicalServices",
                        "#B91C1C",
                        3,
                        false,
                        false,
                        true,
                        true,
                        25,
                        List.of(
                                new DocumentoSeed("Atestado Medico", "Atestado do medico que realizou o primeiro atendimento", true, "PDF,JPG,PNG", 10, null, 1),
                                new DocumentoSeed("Boletim de Ocorrencia (se aplicavel)", "B.O. em caso de acidente com terceiros ou violencia", false, "PDF", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Registro da CAT", "Emissao da CAT pelo setor de RH/SSO e comunicacao ao INSS", 1, TipoResponsavel.RH, null, 1),
                                new EtapaSeed("Acompanhamento Medico", "Acompanhar o afastamento e evolucao do tratamento", 2, TipoResponsavel.RH, null, 30)
                        ),
                        List.of(
                                new CampoSeed("data_acidente", "Data do Acidente / Inicio dos Sintomas", TipoCampo.DATE, true, null, "", "Data em que ocorreu o acidente ou os primeiros sintomas da doenca", 1),
                                new CampoSeed("tipo_ocorrencia", "Tipo de Ocorrencia", TipoCampo.SELECT, true, "Acidente de trabalho tipico|Acidente de trajeto|Doenca profissional / Ocupacional|Doenca do trabalho", "", "Selecione o tipo de ocorrencia", 2),
                                new CampoSeed("descricao_acidente", "Descricao do Acidente / Doenca", TipoCampo.TEXTAREA, true, null, "", "Descreva detalhadamente o que aconteceu ou os sintomas apresentados", 3),
                                new CampoSeed("parte_corpo_atingida", "Parte do Corpo Atingida", TipoCampo.TEXT, true, null, "Ex: Mao direita, Lombar", "Parte do corpo afetada pelo acidente ou doenca", 4),
                                new CampoSeed("houve_afastamento", "Houve Afastamento do Trabalho?", TipoCampo.BOOLEAN, true, null, "", "Informe se o acidente resultou em afastamento", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_ABONO_PECUNIARIO",
                        List.of(),
                        "Abono Pecuniario de Ferias",
                        "Conversao de 10 dias de ferias em pecunia (dinheiro), correspondente a 1/3 do periodo",
                        "Solicite antes do inicio do periodo de ferias. O pagamento do abono ocorre na folha do mes das ferias. Voce continuara gozando 20 dias de descanso. So e possivel solicitar o abono uma vez a cada periodo aquisitivo.",
                        CategoriaProcesso.FINANCEIRO,
                        "MonetizationOn",
                        "#CA8A04",
                        10,
                        false,
                        false,
                        true,
                        true,
                        26,
                        List.of(),
                        List.of(
                                new EtapaSeed("Analise e Calculo", "Verificacao do periodo aquisitivo e calculo do abono", 1, TipoResponsavel.RH, null, 5),
                                new EtapaSeed("Inclusao em Folha", "Inclusao do abono na folha de pagamento do mes das ferias", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("periodo_aquisitivo", "Periodo Aquisitivo das Ferias", TipoCampo.TEXT, true, null, "Ex: 01/2024 a 12/2024", "Informe o periodo aquisitivo ao qual as ferias se referem", 1),
                                new CampoSeed("data_inicio_ferias", "Data de Inicio das Ferias", TipoCampo.DATE, true, null, "", "Primeiro dia do gozo das ferias (20 dias restantes apos o abono)", 2)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_DEPENDENTES",
                        List.of(),
                        "Inclusao / Exclusao de Dependente",
                        "Cadastro, atualizacao ou exclusao de dependente para fins de IR, plano de saude e pensao",
                        "Inclua o dependente em ate 30 dias do evento que gerou o direito (nascimento, adocao, casamento). Para exclusao, informe o motivo. A alteracao afeta o calculo do IR na folha a partir do mes seguinte.",
                        CategoriaProcesso.CADASTRAL,
                        "FamilyRestroom",
                        "#7C3AED",
                        10,
                        false,
                        false,
                        true,
                        true,
                        27,
                        List.of(
                                new DocumentoSeed("Documento do Dependente", "CPF + certidao de nascimento / casamento / declaracao de uniao estavel", true, "PDF,JPG,PNG", 10, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Atualizacao Cadastral", "Verificacao dos documentos e atualizacao no sistema", 1, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("operacao", "Operacao", TipoCampo.SELECT, true, "Incluir dependente|Excluir dependente|Atualizar dados do dependente", "", "Selecione o tipo de atualizacao", 1),
                                new CampoSeed("nome_dependente", "Nome do Dependente", TipoCampo.TEXT, true, null, "", "Nome completo do dependente", 2),
                                new CampoSeed("cpf_dependente", "CPF do Dependente", TipoCampo.TEXT, true, null, "000.000.000-00", "CPF do dependente (obrigatorio para maiores de 12 anos)", 3),
                                new CampoSeed("data_nascimento_dependente", "Data de Nascimento", TipoCampo.DATE, true, null, "", "Data de nascimento do dependente", 4),
                                new CampoSeed("grau_dependencia", "Grau de Dependencia", TipoCampo.SELECT, true, "Conjuge / Companheiro(a)|Filho(a) menor de 21 anos|Filho(a) invalido(a) (qualquer idade)|Filho(a) universitario(a) ate 24 anos|Pai / Mae|Enteado(a)|Tutelado(a)", "", "Selecione o grau de dependencia para fins de IR", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_DIARIAS_PASSAGENS",
                        List.of(),
                        "Solicitacao de Diarias e Passagens",
                        "Solicitacao de pagamento antecipado de diarias e/ou passagens para deslocamento a servico",
                        "Solicite com pelo menos 5 dias uteis de antecedencia. Informe o destino, periodo e objetivo da missao. Apos o retorno, apresente a prestacao de contas em ate 5 dias.",
                        CategoriaProcesso.FINANCEIRO,
                        "DirectionsCar",
                        "#0891B2",
                        5,
                        true,
                        false,
                        true,
                        true,
                        28,
                        List.of(
                                new DocumentoSeed("Convocacao / Autorizacao da Viagem", "Documento que justifica e autoriza o deslocamento a servico", true, "PDF", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Aprovacao do Superior", "Autorizacao pelo superior para o afastamento e os custos", 1, TipoResponsavel.SUPERIOR, null, 2),
                                new EtapaSeed("Processamento Financeiro", "Emissao das diarias e/ou compra das passagens", 2, TipoResponsavel.RH, null, 3)
                        ),
                        List.of(
                                new CampoSeed("destino", "Destino", TipoCampo.TEXT, true, null, "Ex: Brasilia/DF", "Cidade e UF de destino", 1),
                                new CampoSeed("data_saida", "Data de Saida", TipoCampo.DATE, true, null, "", "Data do primeiro dia fora da sede", 2),
                                new CampoSeed("data_retorno", "Data de Retorno", TipoCampo.DATE, true, null, "", "Data do retorno a sede de trabalho", 3),
                                new CampoSeed("objetivo_viagem", "Objetivo da Viagem", TipoCampo.TEXTAREA, true, null, "", "Descreva o motivo e as atividades a serem realizadas", 4),
                                new CampoSeed("meio_transporte", "Meio de Transporte", TipoCampo.SELECT, true, "Aereo|Rodoviario (onibus)|Veiculo proprio|Veiculo oficial|Ferroviario", "", "Selecione o principal meio de transporte", 5),
                                new CampoSeed("solicita_passagem", "Solicitar Compra de Passagem?", TipoCampo.BOOLEAN, true, null, "", "Informe se precisa que o orgao adquira a passagem", 6)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_VALE_TRANSPORTE",
                        List.of(),
                        "Solicitacao / Atualizacao de Vale-Transporte",
                        "Solicitacao de beneficio de vale-transporte para deslocamento residencia-trabalho, ou atualizacao em caso de mudanca de itinerario",
                        "Informe os itinerarios de ida e volta, os modais utilizados e os valores de cada trecho. Em caso de atualizacao, informe apenas o que mudou. O beneficio e descontado em 6% dos vencimentos.",
                        CategoriaProcesso.FINANCEIRO,
                        "DirectionsCar",
                        "#065F46",
                        10,
                        false,
                        false,
                        true,
                        true,
                        29,
                        List.of(
                                new DocumentoSeed("Comprovante de Endereco Residencial", "Conta de agua, luz, telefone ou contrato de locacao com CEP", true, "PDF,JPG,PNG", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Cadastro", "Verificacao do endereco e cadastro do beneficio", 1, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("operacao", "Tipo de Solicitacao", TipoCampo.SELECT, true, "Inclusao (primeira solicitacao)|Atualizacao de itinerario|Cancelamento do beneficio", "", "Selecione o tipo de operacao", 1),
                                new CampoSeed("cep_residencia", "CEP da Residencia", TipoCampo.TEXT, true, null, "00000-000", "CEP do endereco residencial atual", 2),
                                new CampoSeed("descricao_itinerario", "Descricao do Itinerario", TipoCampo.TEXTAREA, true, null, "Ex: Metro linha 2-Verde, Onibus linha 507", "Descreva o trajeto completo de ida, com os modais e linhas utilizados", 3),
                                new CampoSeed("valor_diario_ida", "Valor Diario (Ida)", TipoCampo.NUMBER, true, null, "0.00", "Valor total das passagens de ida em um dia", 4),
                                new CampoSeed("valor_diario_volta", "Valor Diario (Volta)", TipoCampo.NUMBER, true, null, "0.00", "Valor total das passagens de volta em um dia", 5)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_AVERBACAO_TEMPO",
                        List.of(),
                        "Averbacao de Tempo de Servico",
                        "Solicitacao de computo de tempo de servico anterior a investidura no cargo atual (INSS, outros entes, RPPS)",
                        "Apresente a certidao de tempo de contribuicao (CTC) emitida pelo INSS ou orgao competente. O tempo averbado conta para aposentadoria e progressao por antiguidade.",
                        CategoriaProcesso.DOCUMENTAL,
                        "Assignment",
                        "#057A55",
                        30,
                        false,
                        false,
                        true,
                        true,
                        30,
                        List.of(
                                new DocumentoSeed("Certidao de Tempo de Contribuicao (CTC)", "CTC emitida pelo INSS ou RPPS de origem", true, "PDF", 10, null, 1),
                                new DocumentoSeed("CNIS — Extrato Previdenciario", "Extrato do Cadastro Nacional de Informacoes Sociais", false, "PDF", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise Documental", "Verificacao da autenticidade e validade da CTC", 1, TipoResponsavel.RH, null, 15),
                                new EtapaSeed("Registro e Publicacao", "Emissao do ato de averbacao e atualizacao do cadastro funcional", 2, TipoResponsavel.RH, null, 15)
                        ),
                        List.of(
                                new CampoSeed("origem_tempo", "Origem do Tempo a Averbar", TipoCampo.SELECT, true, "INSS (regime geral)|RPPS de outro ente (Estado/Municipio)|CLT — empresa privada|Servico militar|Atividade rural|Outro regime", "", "Selecione de qual regime/orgao vira o tempo", 1),
                                new CampoSeed("periodo_averbado", "Periodo a Averbar", TipoCampo.TEXT, true, null, "Ex: 01/01/2010 a 31/12/2015", "Informe o periodo de inicio e fim do tempo a ser averbado", 2),
                                new CampoSeed("orgao_origem", "Orgao / Empresa de Origem", TipoCampo.TEXT, true, null, "", "Nome do empregador ou orgao onde o tempo foi trabalhado", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_EXONERACAO",
                        List.of(),
                        "Exoneracao a Pedido",
                        "Solicitacao de exoneracao voluntaria do cargo publico (pedido de demissao)",
                        "Preencha o requerimento com data desejada para o desligamento. Confira se ha vinculo com bolsas, contratos ou mandatos que exijam tramite diferenciado. Apos publicacao do ato, nao e possivel reverter.",
                        CategoriaProcesso.RESCISAO,
                        "WorkOutline",
                        "#B91C1C",
                        30,
                        true,
                        true,
                        true,
                        true,
                        31,
                        List.of(
                                new DocumentoSeed("Requerimento de Exoneracao", "Carta ou requerimento formal assinado pelo servidor solicitando a exoneracao", true, "PDF", 5, null, 1)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH", "Verificacao de pendencias, dividas e condicoes para exoneracao", 1, TipoResponsavel.RH, null, 15),
                                new EtapaSeed("Aprovacao do Superior", "Ciencia e anuencia do superior imediato", 2, TipoResponsavel.SUPERIOR, null, 5),
                                new EtapaSeed("Publicacao do Ato", "Emissao e publicacao da portaria de exoneracao", 3, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("data_desligamento", "Data Desejada para o Desligamento", TipoCampo.DATE, true, null, "", "Data a partir da qual nao mais exercera o cargo", 1),
                                new CampoSeed("motivo_resumo", "Motivo do Desligamento", TipoCampo.TEXTAREA, false, null, "", "Opcional — relate brevemente o motivo da exoneracao", 2),
                                new CampoSeed("possui_pendencias", "Possui pendencias conhecidas?", TipoCampo.SELECT, true, "Nao|Sim — divida financeira com o orgao|Sim — bolsa de estudos com clausula de retorno|Sim — mandato ou funcao de confianca ativo|Sim — outros", "", "Informe se ha alguma pendencia que possa impedir a exoneracao imediata", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_REMOCAO",
                        List.of(),
                        "Remocao / Mudanca de Lotacao",
                        "Solicitacao de remocao para outra unidade, municipio ou sede dentro do mesmo orgao",
                        "Informe a unidade de destino desejada e o motivo da solicitacao. Remocoes a pedido dependem de vaga e interesse do servico. Remocoes por motivo de saude exigem laudo medico. Aguarde deliberacao da direcao.",
                        CategoriaProcesso.CADASTRAL,
                        "HomeWork",
                        "#0369A1",
                        30,
                        true,
                        false,
                        true,
                        true,
                        32,
                        List.of(
                                new DocumentoSeed("Requerimento de Remocao", "Requerimento fundamentando o pedido de remocao", true, "PDF", 5, null, 1),
                                new DocumentoSeed("Laudo Medico (se remocao por saude)", "Laudo medico que justifica a necessidade de mudanca (quando o motivo for de saude)", false, "PDF", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise RH / Direcao", "Avaliacao da disponibilidade de vaga e interesse do servico", 1, TipoResponsavel.RH, null, 20),
                                new EtapaSeed("Publicacao e Realocacao", "Publicacao da portaria de remocao e atualizacao do cadastro", 2, TipoResponsavel.RH, null, 10)
                        ),
                        List.of(
                                new CampoSeed("tipo_remocao", "Tipo de Remocao", TipoCampo.SELECT, true, "A pedido (interesse proprio)|Por permuta (troca com outro servidor)|Por motivo de saude|A pedido — conjuge/companheiro deslocado|De oficio (interesse do servico)", "", "Selecione o fundamento do pedido de remocao", 1),
                                new CampoSeed("unidade_destino", "Unidade de Destino Desejada", TipoCampo.TEXT, true, null, "Ex: Secretaria de Educacao — sede Campinas", "Nome da unidade para onde deseja ser removido", 2),
                                new CampoSeed("cidade_destino", "Municipio de Destino", TipoCampo.TEXT, true, null, "", "Municipio da unidade de destino", 3),
                                new CampoSeed("justificativa", "Justificativa", TipoCampo.TEXTAREA, true, null, "", "Descreva os motivos que fundamentam seu pedido de remocao", 4)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_APOSTILAMENTO",
                        List.of(),
                        "Apostilamento de Cargo",
                        "Inclusao de anotacao no assentamento funcional decorrente de aprovacao em novo concurso publico ou transposicao de cargo no mesmo orgao",
                        "Apresente o ato de nomeacao e o termo de posse do cargo a ser apostilado. O apostilamento nao configura acumulacao — e apenas a anotacao formal do novo enquadramento.",
                        CategoriaProcesso.CADASTRAL,
                        "Assignment",
                        "#0C4A6E",
                        20,
                        false,
                        false,
                        true,
                        true,
                        33,
                        List.of(
                                new DocumentoSeed("Ato de Nomeacao", "Publicacao oficial (DOU/DOE/DOM) do ato de nomeacao no novo cargo", true, "PDF", 10, null, 1),
                                new DocumentoSeed("Termo de Posse", "Termo de posse assinado no novo cargo", true, "PDF", 10, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Analise e Registro", "Conferencia dos documentos e apostilamento no cadastro funcional", 1, TipoResponsavel.RH, null, 15),
                                new EtapaSeed("Publicacao do Apostilamento", "Publicacao do ato de apostilamento no diario oficial", 2, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("cargo_anterior", "Cargo Anterior", TipoCampo.TEXT, true, null, "Ex: Tecnico Administrativo — Classe B", "Cargo que o servidor ocupa atualmente", 1),
                                new CampoSeed("cargo_novo", "Novo Cargo / Enquadramento", TipoCampo.TEXT, true, null, "Ex: Analista Administrativo — Classe A, Nivel I", "Cargo ou enquadramento a ser apostilado", 2),
                                new CampoSeed("data_posse_novo_cargo", "Data da Posse no Novo Cargo", TipoCampo.DATE, true, null, "", "Data em que tomou posse no cargo a ser apostilado", 3)
                        )
                ),
                new ProcessoModeloSeed(
                        "PROC_ESTAGIO_PROBATORIO",
                        List.of(),
                        "Avaliacao de Estagio Probatorio",
                        "Registro de avaliacao periodica do estagio probatorio (aptidao, disciplina, assiduidade, produtividade e responsabilidade)",
                        "Submeta o formulario de autoavaliacao. Seu superior imediato realizara a avaliacao do superior. O RH consolidara as avaliacoes. O estagio probatorio dura 3 anos (1.095 dias de efetivo exercicio). Servidores que nao obtiverem conceito suficiente podem ser exonerados.",
                        CategoriaProcesso.CADASTRAL,
                        "HowToReg",
                        "#7C3AED",
                        30,
                        true,
                        false,
                        true,
                        true,
                        34,
                        List.of(
                                new DocumentoSeed("Formulario de Autoavaliacao", "Formulario preenchido pelo proprio servidor", true, "PDF", 5, null, 1),
                                new DocumentoSeed("Formulario de Avaliacao do Superior", "Formulario de avaliacao preenchido pelo gestor imediato", true, "PDF", 5, null, 2)
                        ),
                        List.of(
                                new EtapaSeed("Autoavaliacao", "Servidor preenche e envia sua autoavaliacao", 1, TipoResponsavel.SERVIDOR, null, 5),
                                new EtapaSeed("Avaliacao do Superior", "Superior imediato realiza e envia sua avaliacao", 2, TipoResponsavel.SUPERIOR, null, 10),
                                new EtapaSeed("Consolidacao RH", "RH consolida avaliacoes e registra o resultado no processo", 3, TipoResponsavel.RH, null, 10),
                                new EtapaSeed("Homologacao / Publicacao", "Publicacao do resultado no Diario Oficial", 4, TipoResponsavel.RH, null, 5)
                        ),
                        List.of(
                                new CampoSeed("periodo_avaliacao", "Periodo de Avaliacao", TipoCampo.SELECT, true, "1 Avaliacao (1 ano — 365 dias)|2 Avaliacao (2 ano — 730 dias)|3 Avaliacao Final (3 ano — 1.095 dias)", "", "Selecione qual avaliacao esta sendo realizada", 1),
                                new CampoSeed("data_nomeacao", "Data de Nomeacao", TipoCampo.DATE, true, null, "", "Data de publicacao do ato de nomeacao no diario oficial", 2),
                                new CampoSeed("data_posse", "Data de Posse", TipoCampo.DATE, true, null, "", "Data de posse e inicio do exercicio", 3),
                                new CampoSeed("observacoes_servidor", "Observacoes do Servidor", TipoCampo.TEXTAREA, false, null, "", "Atividades realizadas, dificuldades encontradas ou metas atingidas no periodo", 4)
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
                                      Boolean requerAprovacaoSuperior,
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
