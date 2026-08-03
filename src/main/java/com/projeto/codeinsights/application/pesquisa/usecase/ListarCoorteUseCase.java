package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.ResolucaoDaCoorteDTO;
import com.projeto.codeinsights.domain.pesquisa.model.PseudonimoDeAluno;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

import lombok.RequiredArgsConstructor;

/**
 * O corpus dos participantes que consentiram, pseudonimizado, como linhas comparaveis entre si. E a
 * leitura crua da qual saem tanto a tela quanto o CSV — uma fonte so, para que o que o pesquisador
 * ve e o que ele analisa nao possam divergir.
 * <p>
 * <b>O consentimento e resolvido aqui, antes da consulta.</b> Quem nao autorizou nao aparece porque
 * seu id nunca entra no {@code in} da JPQL — e nao porque alguem lembrou de filtrar depois. A
 * diferenca importa: filtro posterior e um passo que se esquece, e o esquecimento so apareceria
 * quando o dado ja tivesse ido para a analise.
 */
@Service
@RequiredArgsConstructor
public class ListarCoorteUseCase {

    private final CoorteRepository coorteRepository;
    private final ObterParticipantesConsentidosUseCase obterParticipantesConsentidosUseCase;

    @Transactional(readOnly = true)
    public List<ResolucaoDaCoorteDTO> execute() {
        List<ResolucaoDaCoorte> coorte =
                coorteRepository.listarCoorte(obterParticipantesConsentidosUseCase.execute());
        Map<UUID, String> pseudonimos = pseudonimizar(coorte);

        return coorte.stream()
                .map(resolucao -> paraDTO(resolucao, pseudonimos.get(resolucao.autorId())))
                .toList();
    }

    /**
     * Mapeia cada autor ao seu pseudonimo e <b>falha alto</b> se dois autores distintos caírem no
     * mesmo codigo. Uma colisao aqui fundiria dois participantes num so, e o dado sairia corrompido
     * sem nenhum sinal — o tipo de defeito que so aparece na hora de escrever o artigo. A chance e
     * remota com 6 digitos hexadecimais; a verificacao custa uma passada e remove a duvida.
     */
    private Map<UUID, String> pseudonimizar(List<ResolucaoDaCoorte> coorte) {
        Map<UUID, String> porAutor = new HashMap<>();
        Map<String, UUID> porPseudonimo = new HashMap<>();

        for (ResolucaoDaCoorte resolucao : coorte) {
            UUID autorId = resolucao.autorId();
            if (porAutor.containsKey(autorId)) {
                continue;
            }
            String pseudonimo = PseudonimoDeAluno.de(autorId);
            UUID jaUsadoPor = porPseudonimo.putIfAbsent(pseudonimo, autorId);
            if (jaUsadoPor != null) {
                throw new NegocioException(
                        "Colisao de pseudonimo (%s) entre dois participantes. O export foi interrompido para nao fundir os dois."
                                .formatted(pseudonimo));
            }
            porAutor.put(autorId, pseudonimo);
        }
        return porAutor;
    }

    private ResolucaoDaCoorteDTO paraDTO(ResolucaoDaCoorte resolucao, String pseudonimo) {
        return new ResolucaoDaCoorteDTO(
                resolucao.resolucaoId(),
                pseudonimo,
                resolucao.desafioId(),
                resolucao.desafioTitulo(),
                resolucao.linguagem(),
                resolucao.indiceAutonomiaIA(),
                resolucao.analisada(),
                resolucao.tempoRotulo(),
                resolucao.tempoOrdem(),
                resolucao.confiancaTempo(),
                resolucao.espacoRotulo(),
                resolucao.espacoOrdem(),
                resolucao.confiancaEspaco(),
                resolucao.ciclomatica(),
                resolucao.submetidaEm(),
                resolucao.analisadoEm());
    }
}
