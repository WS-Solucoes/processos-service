package ws.erh.cadastro.processo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.dto.ProcessoDetalheResponse;
import ws.erh.cadastro.processo.dto.ProcessoDocumentoResponse;
import ws.erh.cadastro.processo.dto.ProcessoHistoricoResponse;
import ws.erh.cadastro.processo.dto.ProcessoMensagemResponse;
import ws.erh.cadastro.processo.dto.ProcessoPendenciaDocumentalResponse;
import ws.erh.cadastro.processo.repository.ProcessoComplementacaoRepository;
import ws.erh.core.enums.processo.StatusComplementacaoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoResponsavel;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoComplementacao;
import ws.erh.model.cadastro.processo.ProcessoComplementacaoItem;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProcessoDetalheService {

    private final ProcessoWorkflowService workflowService;
    private final ProcessoValidacaoService validacaoService;
    private final ProcessoComplementacaoRepository complementacaoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessoDetalheService(ProcessoWorkflowService workflowService,
                                  ProcessoValidacaoService validacaoService,
                                  ProcessoComplementacaoRepository complementacaoRepository) {
        this.workflowService = workflowService;
        this.validacaoService = validacaoService;
        this.complementacaoRepository = complementacaoRepository;
    }

    public ProcessoDetalheResponse build(Processo processo) {
        ProcessoDetalheResponse response = new ProcessoDetalheResponse();
        if (processo == null) {
            return response;
        }

        populateHeader(processo, response);

        List<ProcessoEtapaModelo> etapas = getEtapasOrdenadas(processo.getProcessoModelo());
        List<ProcessoComplementacao> complementacoes = complementacaoRepository.findByProcessoId(processo.getId());
        Map<String, Object> dadosFormulario = parseFormulario(processo.getDadosFormulario());
        List<ProcessoPendenciaDocumentalResponse> pendencias = validacaoService.calcularPendencias(processo);
        Map<Long, ProcessoPendenciaDocumentalResponse> pendenciaPorDocumentoModelo = pendencias.stream()
                .filter(p -> p.getDocumentoModeloId() != null)
                .collect(Collectors.toMap(ProcessoPendenciaDocumentalResponse::getDocumentoModeloId, p -> p, (a, b) -> b));
        Map<Long, ProcessoDocumento> ultimoDocumentoPorModelo = buildUltimoDocumentoPorModelo(processo);
        ProcessoEtapaModelo etapaFallback = resolveEtapaFallback(processo.getProcessoModelo());

        Map<String, ProcessoDetalheResponse.FaseResponse> fasesPorChave = new LinkedHashMap<>();
        ProcessoDetalheResponse.FaseResponse abertura = createFase("ABERTURA", null, 0, "Abertura", processo.getOrigemAbertura() != null
                ? processo.getOrigemAbertura().getDescricao()
                : "Abertura do processo", null);
        abertura.setStatus("CONCLUIDA");
        fasesPorChave.put(abertura.getChave(), abertura);

        for (ProcessoEtapaModelo etapa : etapas) {
            ProcessoDetalheResponse.FaseResponse fase = createFase(
                    buildEtapaKey(etapa),
                    etapa.getId(),
                    etapa.getOrdem(),
                    etapa.getNome(),
                    etapa.getDescricao(),
                    etapa.getTipoResponsavel() != null ? etapa.getTipoResponsavel().name() : null);
            fase.setStatus(resolveFaseStatus(processo, etapa.getOrdem()));
            fase.setAtual(Objects.equals(processo.getEtapaAtual(), etapa.getOrdem())
                    && !isSituacaoFinal(processo.getSituacao()));
            fasesPorChave.put(fase.getChave(), fase);
        }

        ProcessoDetalheResponse.FaseResponse conclusao = createFase(
                "CONCLUSAO",
                null,
                etapas.size() + 1,
                "Conclusão",
                "Resultado, execução e encerramento do processo",
                "SISTEMA");
        conclusao.setStatus(resolveStatusConclusao(processo.getSituacao()));
        conclusao.setAtual(isSituacaoConclusaoAtual(processo.getSituacao()));
        fasesPorChave.put(conclusao.getChave(), conclusao);

        if (processo.getProcessoModelo() != null && processo.getProcessoModelo().getCamposAdicionais() != null) {
            for (ProcessoCampoModelo campo : processo.getProcessoModelo().getCamposAdicionais()) {
                ProcessoDetalheResponse.FormularioCampoResponse campoResponse = new ProcessoDetalheResponse.FormularioCampoResponse();
                campoResponse.setCampoModeloId(campo.getId());
                campoResponse.setNomeCampo(campo.getNomeCampo());
                campoResponse.setLabel(campo.getLabel());
                campoResponse.setTipoCampo(campo.getTipoCampo() != null ? campo.getTipoCampo().name() : null);
                campoResponse.setObrigatorio(Boolean.TRUE.equals(campo.getObrigatorio()));
                campoResponse.setPlaceholder(campo.getPlaceholder());
                campoResponse.setAjuda(campo.getAjuda());
                campoResponse.setOrdem(campo.getOrdem());
                Object valor = dadosFormulario.get(campo.getNomeCampo());
                campoResponse.setValor(valor == null ? null : String.valueOf(valor));

                ProcessoEtapaModelo etapa = campo.getEtapaModelo() != null ? campo.getEtapaModelo() : etapaFallback;
                if (etapa != null) {
                    campoResponse.setEtapaModeloId(etapa.getId());
                    campoResponse.setEtapaOrdem(etapa.getOrdem());
                    campoResponse.setEtapaNome(etapa.getNome());
                }

                ProcessoDetalheResponse.FaseResponse fase = resolveFaseParaCampoOuDocumento(fasesPorChave, etapa);
                fase.getFormulario().add(campoResponse);
                response.getFormularioEstruturado().add(campoResponse);
            }
        }

        if (processo.getProcessoModelo() != null && processo.getProcessoModelo().getDocumentosExigidos() != null) {
            for (ProcessoDocumentoModelo documentoModelo : processo.getProcessoModelo().getDocumentosExigidos()) {
                ProcessoDetalheResponse.DocumentoFaseResponse documentoResponse = new ProcessoDetalheResponse.DocumentoFaseResponse();
                documentoResponse.setDocumentoModeloId(documentoModelo.getId());
                documentoResponse.setNomeDocumento(documentoModelo.getNome());
                documentoResponse.setDescricao(documentoModelo.getDescricao());
                documentoResponse.setObrigatorio(Boolean.TRUE.equals(documentoModelo.getObrigatorio()));
                documentoResponse.setOrdem(documentoModelo.getOrdem());
                documentoResponse.setPendencia(pendenciaPorDocumentoModelo.get(documentoModelo.getId()));

                ProcessoDocumento ultimoDocumento = ultimoDocumentoPorModelo.get(documentoModelo.getId());
                if (ultimoDocumento != null) {
                    documentoResponse.setUltimoDocumento(new ProcessoDocumentoResponse(ultimoDocumento));
                }

                ProcessoEtapaModelo etapa = documentoModelo.getEtapaModelo() != null ? documentoModelo.getEtapaModelo() : etapaFallback;
                if (etapa != null) {
                    documentoResponse.setEtapaModeloId(etapa.getId());
                    documentoResponse.setEtapaOrdem(etapa.getOrdem());
                    documentoResponse.setEtapaNome(etapa.getNome());
                }

                ProcessoDetalheResponse.FaseResponse fase = resolveFaseParaCampoOuDocumento(fasesPorChave, etapa);
                fase.getDocumentos().add(documentoResponse);
                response.getDocumentosPorFase().add(documentoResponse);
            }
        }

        for (ProcessoComplementacao complementacao : complementacoes) {
            ProcessoDetalheResponse.ComplementacaoResponse complementacaoResponse = toComplementacaoResponse(complementacao);
            response.getComplementacoes().add(complementacaoResponse);
            ProcessoDetalheResponse.FaseResponse fase = resolveFaseParaComplementacao(fasesPorChave, complementacao.getEtapaReferencia());
            if (fase != null && complementacao.getStatus() == StatusComplementacaoProcesso.ABERTA) {
                fase.setComplementacaoAtiva(complementacaoResponse);
                fase.setStatus("AGUARDANDO_SERVIDOR");
                fase.setAtual(true);
            }
        }

        response.setFases(new ArrayList<>(fasesPorChave.values()));
        response.setTimeline(buildTimeline(response.getFases()));
        response.setFaseAtualDetalhe(resolveFaseAtual(response.getFases()));

        if (processo.getMensagens() != null) {
            response.setMensagens(processo.getMensagens().stream()
                    .map(ProcessoMensagemResponse::new)
                    .collect(Collectors.toList()));
        }
        if (processo.getHistorico() != null) {
            response.setHistorico(processo.getHistorico().stream()
                    .map(ProcessoHistoricoResponse::new)
                    .collect(Collectors.toList()));
        }

        response.setPendenciasDocumentais(pendencias);
        response.setAcoesDisponiveis(new ArrayList<>(workflowService.resolveAcoesDisponiveis(processo)));
        return response;
    }

    private void populateHeader(Processo processo, ProcessoDetalheResponse response) {
        response.setId(processo.getId());
        response.setProtocolo(processo.getProtocolo());
        if (processo.getProcessoModelo() != null) {
            response.setProcessoModeloNome(processo.getProcessoModelo().getNome());
            response.setProcessoModeloIcone(processo.getProcessoModelo().getIcone());
            response.setProcessoModeloCor(processo.getProcessoModelo().getCor());
            if (processo.getProcessoModelo().getCategoria() != null) {
                response.setProcessoModeloCategoria(processo.getProcessoModelo().getCategoria().name());
                response.setProcessoModeloCategoriaDescricao(processo.getProcessoModelo().getCategoria().getDescricao());
            }
        }
        response.setServidorId(processo.getServidorId());
        response.setServidorNome(processo.getServidorNome());
        response.setServidorCpf(processo.getServidorCpf());
        response.setVinculoFuncionalId(processo.getVinculoFuncionalId());
        response.setVinculoFuncionalMatricula(processo.getVinculoFuncionalMatricula());
        if (processo.getSituacao() != null) {
            response.setSituacao(processo.getSituacao().name());
            response.setSituacaoDescricao(processo.getSituacao().getDescricao());
        }
        if (processo.getOrigemAbertura() != null) {
            response.setOrigemAbertura(processo.getOrigemAbertura().name());
            response.setOrigemAberturaDescricao(processo.getOrigemAbertura().getDescricao());
        }
        if (processo.getResultado() != null) {
            response.setResultado(processo.getResultado().name());
            response.setResultadoDescricao(processo.getResultado().getDescricao());
        }
        if (processo.getIntegracaoStatus() != null) {
            response.setIntegracaoStatus(processo.getIntegracaoStatus().name());
            response.setIntegracaoStatusDescricao(processo.getIntegracaoStatus().getDescricao());
        }
        if (processo.getPrioridade() != null) {
            response.setPrioridade(processo.getPrioridade().name());
            response.setPrioridadeDescricao(processo.getPrioridade().getDescricao());
        }
        response.setEtapaAtual(processo.getEtapaAtual());
        response.setEtapaAtualNome(workflowService.resolveEtapaAtualNome(processo));
        response.setTotalEtapas(workflowService.resolveTotalEtapas(processo));
        response.setResponsavelAtual(workflowService.resolveResponsavelAtual(processo));
        response.setPodeComplementar(workflowService.podeComplementar(processo));
        response.setAtribuidoPara(processo.getAtribuidoPara());
        response.setDepartamentoAtribuido(processo.getDepartamentoAtribuido());
        response.setDadosFormulario(processo.getDadosFormulario());
        response.setObservacaoServidor(processo.getObservacaoServidor());
        response.setJustificativaResultado(processo.getJustificativaResultado());
        response.setReferenciaTipo(processo.getReferenciaTipo());
        response.setReferenciaId(processo.getReferenciaId());
        response.setIntegracaoErro(processo.getIntegracaoErro());
        if (processo.getDataAbertura() != null) {
            response.setDataAbertura(processo.getDataAbertura().toString());
        }
        if (processo.getDataUltimaAtualizacao() != null) {
            response.setDataUltimaAtualizacao(processo.getDataUltimaAtualizacao().toString());
        }
        if (processo.getDataConclusao() != null) {
            response.setDataConclusao(processo.getDataConclusao().toString());
        }
        if (processo.getPrazoLimite() != null) {
            response.setPrazoLimite(processo.getPrazoLimite().toString());
        }
    }

    private ProcessoDetalheResponse.FaseResponse createFase(String chave,
                                                            Long etapaModeloId,
                                                            Integer ordem,
                                                            String nome,
                                                            String descricao,
                                                            String tipoResponsavel) {
        ProcessoDetalheResponse.FaseResponse fase = new ProcessoDetalheResponse.FaseResponse();
        fase.setChave(chave);
        fase.setEtapaModeloId(etapaModeloId);
        fase.setOrdem(ordem);
        fase.setNome(nome);
        fase.setDescricao(descricao);
        fase.setTipoResponsavel(tipoResponsavel);
        fase.setStatus("PENDENTE");
        return fase;
    }

    private Map<Long, ProcessoDocumento> buildUltimoDocumentoPorModelo(Processo processo) {
        if (processo.getDocumentos() == null) {
            return Map.of();
        }
        return processo.getDocumentos().stream()
                .filter(doc -> doc.getDocumentoModelo() != null && doc.getDocumentoModelo().getId() != null)
                .collect(Collectors.toMap(
                        doc -> doc.getDocumentoModelo().getId(),
                        doc -> doc,
                        (left, right) -> left.getDataEnvio() != null && right.getDataEnvio() != null
                                && left.getDataEnvio().isAfter(right.getDataEnvio()) ? left : right));
    }

    private List<ProcessoEtapaModelo> getEtapasOrdenadas(ProcessoModelo modelo) {
        if (modelo == null || modelo.getEtapas() == null) {
            return List.of();
        }
        return modelo.getEtapas().stream()
                .sorted(Comparator.comparing(ProcessoEtapaModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private ProcessoEtapaModelo resolveEtapaFallback(ProcessoModelo modelo) {
        List<ProcessoEtapaModelo> etapas = getEtapasOrdenadas(modelo);
        Optional<ProcessoEtapaModelo> servidor = etapas.stream()
                .filter(etapa -> etapa.getTipoResponsavel() == TipoResponsavel.SERVIDOR)
                .findFirst();
        return servidor.orElse(etapas.isEmpty() ? null : etapas.get(0));
    }

    private ProcessoDetalheResponse.FaseResponse resolveFaseParaCampoOuDocumento(Map<String, ProcessoDetalheResponse.FaseResponse> fasesPorChave,
                                                                                 ProcessoEtapaModelo etapa) {
        if (etapa == null) {
            return fasesPorChave.get("ABERTURA");
        }
        return fasesPorChave.getOrDefault(buildEtapaKey(etapa), fasesPorChave.get("ABERTURA"));
    }

    private ProcessoDetalheResponse.FaseResponse resolveFaseParaComplementacao(Map<String, ProcessoDetalheResponse.FaseResponse> fasesPorChave,
                                                                               Integer etapaReferencia) {
        if (etapaReferencia == null) {
            return fasesPorChave.get("ABERTURA");
        }
        return fasesPorChave.values().stream()
                .filter(fase -> Objects.equals(fase.getOrdem(), etapaReferencia))
                .findFirst()
                .orElse(fasesPorChave.get("ABERTURA"));
    }

    private ProcessoDetalheResponse.ComplementacaoResponse toComplementacaoResponse(ProcessoComplementacao complementacao) {
        ProcessoDetalheResponse.ComplementacaoResponse response = new ProcessoDetalheResponse.ComplementacaoResponse();
        response.setId(complementacao.getId());
        response.setEtapaReferencia(complementacao.getEtapaReferencia());
        response.setEtapaNome(complementacao.getEtapaNomeSnapshot());
        if (complementacao.getSituacaoRetorno() != null) {
            response.setSituacaoRetorno(complementacao.getSituacaoRetorno().name());
            response.setSituacaoRetornoDescricao(complementacao.getSituacaoRetorno().getDescricao());
        }
        if (complementacao.getStatus() != null) {
            response.setStatus(complementacao.getStatus().name());
            response.setStatusDescricao(complementacao.getStatus().getDescricao());
            response.setAtiva(complementacao.getStatus() == StatusComplementacaoProcesso.ABERTA);
        }
        if (complementacao.getPrazoLimite() != null) {
            response.setPrazoLimite(complementacao.getPrazoLimite().toString());
        }
        if (complementacao.getDataSolicitacao() != null) {
            response.setDataSolicitacao(complementacao.getDataSolicitacao().toString());
        }
        if (complementacao.getDataResposta() != null) {
            response.setDataResposta(complementacao.getDataResposta().toString());
        }
        if (complementacao.getDataEncerramento() != null) {
            response.setDataEncerramento(complementacao.getDataEncerramento().toString());
        }
        response.setMotivoConsolidado(complementacao.getMotivoConsolidado());
        response.setSolicitadoPor(complementacao.getSolicitadoPor());
        response.setRespondidoPor(complementacao.getRespondidoPor());
        response.setTipoSolicitante(complementacao.getTipoSolicitante() != null
                ? complementacao.getTipoSolicitante().name()
                : null);
        if (complementacao.getItens() != null) {
            response.setItens(complementacao.getItens().stream()
                    .sorted(Comparator.comparing(ProcessoComplementacaoItem::getOrdem, Comparator.nullsLast(Integer::compareTo)))
                    .map(this::toComplementacaoItemResponse)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private ProcessoDetalheResponse.ComplementacaoItemResponse toComplementacaoItemResponse(ProcessoComplementacaoItem item) {
        ProcessoDetalheResponse.ComplementacaoItemResponse response = new ProcessoDetalheResponse.ComplementacaoItemResponse();
        response.setId(item.getId());
        if (item.getTipoItem() != null) {
            response.setTipoItem(item.getTipoItem().name());
            response.setTipoItemDescricao(item.getTipoItem().getDescricao());
        }
        response.setLabel(item.getLabel());
        response.setObrigatorio(Boolean.TRUE.equals(item.getObrigatorio()));
        response.setMotivo(item.getMotivo());
        response.setOrdem(item.getOrdem());
        if (item.getDocumentoModelo() != null) {
            response.setDocumentoModeloId(item.getDocumentoModelo().getId());
        }
        if (item.getCampoModelo() != null) {
            response.setCampoModeloId(item.getCampoModelo().getId());
        }
        if (item.getDocumentoRespondido() != null) {
            response.setDocumentoRespondidoId(item.getDocumentoRespondido().getId());
            response.setDocumentoRespondidoNome(item.getDocumentoRespondido().getNomeArquivo());
        }
        return response;
    }

    private List<ProcessoDetalheResponse.TimelineItemResponse> buildTimeline(List<ProcessoDetalheResponse.FaseResponse> fases) {
        List<ProcessoDetalheResponse.TimelineItemResponse> timeline = new ArrayList<>();
        for (ProcessoDetalheResponse.FaseResponse fase : fases) {
            ProcessoDetalheResponse.TimelineItemResponse item = new ProcessoDetalheResponse.TimelineItemResponse();
            item.setChave(fase.getChave());
            item.setOrdem(fase.getOrdem());
            item.setNome(fase.getNome());
            item.setTipoResponsavel(fase.getTipoResponsavel());
            item.setStatus(fase.getStatus());
            item.setAtual(Boolean.TRUE.equals(fase.getAtual()));
            if (fase.getComplementacaoAtiva() != null) {
                item.setResumo("Complementação aguardando servidor");
            } else if ("CONCLUSAO".equals(fase.getChave())) {
                item.setResumo("Resultado e encerramento");
            } else {
                item.setResumo(fase.getDescricao());
            }
            timeline.add(item);
        }
        return timeline;
    }

    private ProcessoDetalheResponse.FaseResponse resolveFaseAtual(List<ProcessoDetalheResponse.FaseResponse> fases) {
        return fases.stream()
                .filter(fase -> Boolean.TRUE.equals(fase.getAtual()))
                .findFirst()
                .orElseGet(() -> fases.stream()
                        .filter(fase -> "ATUAL".equals(fase.getStatus()) || "AGUARDANDO_SERVIDOR".equals(fase.getStatus())
                                || "AGUARDANDO_RH".equals(fase.getStatus()) || "AGUARDANDO_CHEFIA".equals(fase.getStatus())
                                || "EM_EXECUCAO".equals(fase.getStatus()) || "FINALIZADA".equals(fase.getStatus()))
                        .findFirst()
                        .orElse(fases.isEmpty() ? null : fases.get(0)));
    }

    private String resolveFaseStatus(Processo processo, Integer ordemEtapa) {
        Integer etapaAtual = processo.getEtapaAtual();
        if (etapaAtual == null || ordemEtapa == null) {
            return "PENDENTE";
        }
        if (isSituacaoFinal(processo.getSituacao())) {
            return ordemEtapa <= etapaAtual ? "CONCLUIDA" : "PENDENTE";
        }
        if (ordemEtapa < etapaAtual) {
            return "CONCLUIDA";
        }
        if (ordemEtapa > etapaAtual) {
            return "PENDENTE";
        }
        if (processo.getSituacao() == SituacaoProcesso.PENDENTE_DOCUMENTACAO
                || processo.getSituacao() == SituacaoProcesso.RASCUNHO) {
            return "AGUARDANDO_SERVIDOR";
        }
        if (processo.getSituacao() == SituacaoProcesso.AGUARDANDO_CHEFIA) {
            return "AGUARDANDO_CHEFIA";
        }
        if (processo.getSituacao() == SituacaoProcesso.EM_EXECUCAO || processo.getSituacao() == SituacaoProcesso.DEFERIDO) {
            return "EM_EXECUCAO";
        }
        return "ATUAL";
    }

    private String resolveStatusConclusao(SituacaoProcesso situacao) {
        if (situacao == null) {
            return "PENDENTE";
        }
        return switch (situacao) {
            case DEFERIDO, INDEFERIDO, CONCLUIDO, CANCELADO, ARQUIVADO -> "FINALIZADA";
            case EM_EXECUCAO -> "EM_EXECUCAO";
            default -> "PENDENTE";
        };
    }

    private boolean isSituacaoConclusaoAtual(SituacaoProcesso situacao) {
        return situacao == SituacaoProcesso.EM_EXECUCAO
                || situacao == SituacaoProcesso.DEFERIDO
                || situacao == SituacaoProcesso.INDEFERIDO
                || situacao == SituacaoProcesso.CONCLUIDO
                || situacao == SituacaoProcesso.CANCELADO
                || situacao == SituacaoProcesso.ARQUIVADO;
    }

    private boolean isSituacaoFinal(SituacaoProcesso situacao) {
        return situacao == SituacaoProcesso.DEFERIDO
                || situacao == SituacaoProcesso.INDEFERIDO
                || situacao == SituacaoProcesso.CONCLUIDO
                || situacao == SituacaoProcesso.CANCELADO
                || situacao == SituacaoProcesso.ARQUIVADO
                || situacao == SituacaoProcesso.EM_EXECUCAO;
    }

    private Map<String, Object> parseFormulario(String dadosFormulario) {
        if (dadosFormulario == null || dadosFormulario.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(dadosFormulario, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String buildEtapaKey(ProcessoEtapaModelo etapa) {
        return "ETAPA_" + etapa.getOrdem();
    }
}
