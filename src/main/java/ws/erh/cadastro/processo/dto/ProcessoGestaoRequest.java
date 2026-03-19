package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para ações de gestão: atribuir, deferir, indeferir, devolver, etc.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProcessoGestaoRequest {

    private Long processoId;

    /** Ação a ser executada: ATRIBUIR, DEFERIR, INDEFERIR, DEVOLVER, SOLICITAR_DOCUMENTACAO, ARQUIVAR */
    private String acao;

    /** Usuário que está executando (preenchido pelo controller se necessário) */
    private String usuario;

    /** Tipo do usuário: RH, CHEFIA */
    private String tipoUsuario;

    /** Para atribuição */
    private String atribuidoPara;
    private String departamentoAtribuido;

    /** Justificativa (obrigatória para indeferir, devolver) */
    private String justificativa;

    /** Para solicitar documentação adicional */
    private String documentoSolicitado;

    /** Prioridade (opcional) */
    private String prioridade;
}
