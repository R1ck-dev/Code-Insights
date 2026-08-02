package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.domain.pesquisa.model.HistoricoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

/**
 * Quem autorizou o uso das proprias submissoes na versao do termo que esta no ar.
 * <p>
 * Existe separado porque tres leituras dependem exatamente da mesma resposta — a listagem da coorte,
 * o relatorio de qualidade e a identificacao de participante. Tres copias da mesma regra e uma regra
 * que um dia vale em duas: a terceira ficaria para tras numa mudanca e passaria a servir dado de
 * quem revogou.
 * <p>
 * <b>Sem cache.</b> Quem revoga hoje sai da proxima leitura; um valor guardado em memoria
 * transformaria "retirei meu consentimento" em algo que so vale depois do proximo deploy.
 */
@Service
public class ObterParticipantesConsentidosUseCase {

    private final ConsentimentoRepository consentimentoRepository;
    private final TermoDeConsentimentoPort termoDeConsentimentoPort;

    public ObterParticipantesConsentidosUseCase(ConsentimentoRepository consentimentoRepository,
            TermoDeConsentimentoPort termoDeConsentimentoPort) {
        this.consentimentoRepository = consentimentoRepository;
        this.termoDeConsentimentoPort = termoDeConsentimentoPort;
    }

    @Transactional(readOnly = true)
    public Set<UUID> execute() {
        return historicoVigente().participantesQueAutorizam();
    }

    /** O relatorio de qualidade precisa tambem de quem recusou, para separar "nao" de silencio. */
    @Transactional(readOnly = true)
    public HistoricoDeConsentimento historicoVigente() {
        String versao = termoDeConsentimentoPort.vigente().versao();
        return new HistoricoDeConsentimento(consentimentoRepository.decisoesDaVersao(versao), versao);
    }
}
