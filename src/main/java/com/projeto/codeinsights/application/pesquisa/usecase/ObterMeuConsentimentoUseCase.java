package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.MeuConsentimentoDTO;
import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;
import com.projeto.codeinsights.domain.pesquisa.model.HistoricoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

import lombok.RequiredArgsConstructor;

/**
 * O termo em vigor e a resposta que esta pessoa deu a ele — se e que deu.
 * <p>
 * Devolve os dois numa leitura so porque a tela precisa dos dois ao mesmo tempo. Buscar o texto num
 * endpoint e a decisao noutro criaria a janela em que a versao muda entre as duas chamadas e a
 * pessoa acaba respondendo sobre um texto que nao e o que ela leu.
 */
@Service
@RequiredArgsConstructor
public class ObterMeuConsentimentoUseCase {

    private final ConsentimentoRepository consentimentoRepository;
    private final TermoDeConsentimentoPort termoDeConsentimentoPort;

    @Transactional(readOnly = true)
    public MeuConsentimentoDTO execute(UUID participanteId) {
        TermoDeConsentimento termo = termoDeConsentimentoPort.vigente();
        Optional<ConsentimentoDePesquisa> vigente = decisaoVigente(participanteId, termo.versao());

        return new MeuConsentimentoDTO(
                termo.versao(),
                termo.titulo(),
                termo.texto(),
                termo.aprovadoPeloComite(),
                vigente.map(ConsentimentoDePesquisa::getDecisao).orElse(null),
                vigente.map(ConsentimentoDePesquisa::getRegistradoEm).orElse(null));
    }

    /** Visivel ao pacote para que o registro de decisao reaproveite a mesma leitura, sem duplicar a regra. */
    Optional<ConsentimentoDePesquisa> decisaoVigente(UUID participanteId, String versaoDoTermo) {
        return new HistoricoDeConsentimento(
                consentimentoRepository.historicoDoParticipante(participanteId), versaoDoTermo)
                .vigenteDe(participanteId);
    }
}
