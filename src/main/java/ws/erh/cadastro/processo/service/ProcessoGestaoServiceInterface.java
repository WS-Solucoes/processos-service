package ws.erh.cadastro.processo.service;

import ws.erh.model.cadastro.processo.Processo;

public interface ProcessoGestaoServiceInterface {

    Processo atribuir(Long processoId, String atribuidoPara, String departamento, String usuario);
    Processo iniciarAnalise(Long processoId, String usuario);
    Processo solicitarDocumentacao(Long processoId, String descricao, String usuario);
    Processo encaminharChefia(Long processoId, String usuario);
    Processo deferir(Long processoId, String justificativa, String usuario);
    Processo indeferir(Long processoId, String justificativa, String usuario);
    Processo devolver(Long processoId, String justificativa, String usuario);
    Processo executar(Long processoId, String usuario);
    Processo concluir(Long processoId, String usuario);
    Processo arquivar(Long processoId, String usuario);
}
