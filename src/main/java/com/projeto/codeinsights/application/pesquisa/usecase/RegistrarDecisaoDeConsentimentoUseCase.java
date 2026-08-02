package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.MeuConsentimentoDTO;
import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

import lombok.RequiredArgsConstructor;

/**
 * Grava a resposta do participante ao termo. Aceitar e recusar sao a <b>mesma operacao</b>, com
 * valores diferentes: retirar o consentimento nao pode ser mais dificil do que dar, nem passar por
 * um caminho separado que ninguem manteve.
 * <p>
 * O participante e sempre quem esta autenticado — nao ha parametro para decidir em nome de outro.
 * Consentimento por procuracao nao existe nesta pesquisa, e um endpoint que aceitasse um id de
 * participante no corpo seria uma forma de fabricar consentimento.
 */
@Service
@RequiredArgsConstructor
public class RegistrarDecisaoDeConsentimentoUseCase {

    private final ConsentimentoRepository consentimentoRepository;
    private final TermoDeConsentimentoPort termoDeConsentimentoPort;
    private final ObterMeuConsentimentoUseCase obterMeuConsentimentoUseCase;

    @Transactional
    public MeuConsentimentoDTO execute(UUID participanteId, boolean autoriza) {
        TermoDeConsentimento termo = termoDeConsentimentoPort.vigente();
        DecisaoDeConsentimento nova = autoriza
                ? DecisaoDeConsentimento.ACEITE
                : DecisaoDeConsentimento.RECUSA;

        if (mudouDeIdeia(participanteId, termo.versao(), nova)) {
            consentimentoRepository.registrar(
                    new ConsentimentoDePesquisa(participanteId, termo.versao(), nova));
        }

        return obterMeuConsentimentoUseCase.execute(participanteId);
    }

    /**
     * Repetir a decisao que ja vale nao grava nada. O log e uma trilha de auditoria: um clique duplo
     * ou um F5 na tela nao sao mudanca de vontade, e enche-lo de linhas identicas so tornaria mais
     * dificil ler a historia que ele existe para contar.
     */
    private boolean mudouDeIdeia(UUID participanteId, String versao, DecisaoDeConsentimento nova) {
        Optional<ConsentimentoDePesquisa> vigente =
                obterMeuConsentimentoUseCase.decisaoVigente(participanteId, versao);
        return vigente.isEmpty() || vigente.get().getDecisao() != nova;
    }
}
