package com.projeto.codeinsights.domain.pesquisa.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reduz o log append-only de decisoes a uma pergunta so: <b>quem esta dentro da coorte agora</b>.
 * <p>
 * Existe como tipo de dominio, e nao como consulta SQL, porque "a decisao vigente e a mais recente"
 * e regra de negocio. Escrita em JPQL ela viraria uma subconsulta correlacionada que ninguem revisa;
 * aqui ela cabe em tres linhas legiveis e tem teste. Na escala do piloto (dezenas de participantes,
 * uma linha por decisao) dobrar o historico em memoria e irrelevante.
 * <p>
 * O historico e sempre <b>de uma versao do termo</b>. Consentimento dado a v1 nao vale para v2 —
 * quem publica versao nova esta dizendo que o texto aprovado mudou.
 */
public final class HistoricoDeConsentimento {

    private final Map<UUID, ConsentimentoDePesquisa> vigentePorParticipante;

    /**
     * Descarta decisoes de outras versoes do termo. O filtro mora aqui, e nao em cada chamador,
     * porque "consentimento vale para a versao a que foi dado" e regra, e regra repetida em tres
     * lugares e regra que um dia vai valer em dois.
     */
    public HistoricoDeConsentimento(List<ConsentimentoDePesquisa> decisoes, String versaoDoTermo) {
        Map<UUID, ConsentimentoDePesquisa> vigentes = new HashMap<>();
        for (ConsentimentoDePesquisa decisao : decisoes) {
            if (decisao.getVersaoDoTermo().equals(versaoDoTermo)) {
                vigentes.merge(decisao.getParticipanteId(), decisao, HistoricoDeConsentimento::maisRecente);
            }
        }
        this.vigentePorParticipante = Map.copyOf(vigentes);
    }

    /**
     * Desempate no mesmo instante fica com a RECUSA, e nao com a ordem em que as linhas chegaram.
     * Empate exato so aconteceria por registro duplicado, e nesse caso incluir alguem na pesquisa
     * seria a escolha errada — na duvida sobre consentimento, fica de fora.
     */
    private static ConsentimentoDePesquisa maisRecente(ConsentimentoDePesquisa atual,
            ConsentimentoDePesquisa nova) {
        if (nova.getRegistradoEm().isAfter(atual.getRegistradoEm())) {
            return nova;
        }
        if (nova.getRegistradoEm().isEqual(atual.getRegistradoEm()) && !nova.autoriza()) {
            return nova;
        }
        return atual;
    }

    public Optional<ConsentimentoDePesquisa> vigenteDe(UUID participanteId) {
        return Optional.ofNullable(vigentePorParticipante.get(participanteId));
    }

    /** Os unicos participantes cujas submissoes podem entrar na coorte. */
    public Set<UUID> participantesQueAutorizam() {
        return filtrar(true);
    }

    /** Quem respondeu "nao" — distinto de quem nunca respondeu, que nao aparece aqui. */
    public Set<UUID> participantesQueRecusam() {
        return filtrar(false);
    }

    private Set<UUID> filtrar(boolean autoriza) {
        return vigentePorParticipante.entrySet().stream()
                .filter(entrada -> entrada.getValue().autoriza() == autoriza)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
