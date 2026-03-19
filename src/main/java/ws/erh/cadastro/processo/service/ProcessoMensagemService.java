package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoMensagemRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.model.cadastro.processo.ProcessoMensagem;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ProcessoMensagemService extends AbstractTenantService implements ProcessoMensagemServiceInterface {

    @Autowired
    private ProcessoMensagemRepository mensagemRepository;

    @Override
    public ProcessoMensagem enviarMensagem(ProcessoMensagem mensagem) {
        log.info("Enviando mensagem no processo ID: {}", mensagem.getProcesso().getId());
        mensagem.setDataHora(LocalDateTime.now());
        mensagem.setLida(false);
        configurarDadosTenant(mensagem);
        return mensagemRepository.save(mensagem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessoMensagem> findByProcessoId(Long processoId) {
        return mensagemRepository.findByProcessoId(processoId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countNaoLidas(Long processoId) {
        return mensagemRepository.countNaoLidasByProcessoId(processoId);
    }

    @Override
    public void marcarComoLidas(Long processoId) {
        log.info("Marcando mensagens como lidas no processo ID: {}", processoId);
        mensagemRepository.marcarTodasComoLidas(processoId);
    }
}
