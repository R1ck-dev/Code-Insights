package com.projeto.codeinsights.application.pesquisa.usecase;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.codeinsights.application.pesquisa.dto.ContagemDTO;
import com.projeto.codeinsights.application.pesquisa.dto.ContagemPorLinguagemDTO;
import com.projeto.codeinsights.application.pesquisa.dto.QualidadeDaCoorteDTO;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.knowledge.port.AnalisadorMetricas;
import com.projeto.codeinsights.domain.pesquisa.model.HistoricoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;
import com.projeto.codeinsights.domain.pesquisa.port.ConsentimentoRepository;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.domain.pesquisa.port.TermoDeConsentimentoPort;

import lombok.RequiredArgsConstructor;

/**
 * Mede o instrumento, nao o resultado: cobertura da analise, distribuicoes e buracos da amostra.
 * <p>
 * E a leitura que serve <b>durante</b> a coleta. Um aluno que submeteu uma vez so, uma leva de
 * resolucoes que nao parseou, uma linguagem sem analisador — sao coisas que inviabilizam parte da
 * analise e que ainda da tempo de corrigir enquanto o piloto acontece. Depois, nao da.
 * <p>
 * A agregacao e feita em memoria, sobre a coorte inteira, em vez de seis consultas de
 * {@code group by}. Na escala do piloto (dezenas de alunos, centenas de resolucoes) a diferenca e
 * irrelevante e o codigo fica auditavel — da para ler a regra de cada balde. Se a amostra crescer
 * uma ordem de grandeza, isto vira agregacao no banco.
 */
@Service
@RequiredArgsConstructor
public class ObterQualidadeDaCoorteUseCase {

    private final CoorteRepository coorteRepository;
    private final ObterParticipantesConsentidosUseCase obterParticipantesConsentidosUseCase;
    private final TermoDeConsentimentoPort termoDeConsentimentoPort;
    private final AnalisadorMetricas analisadorMetricas;

    @Transactional(readOnly = true)
    public QualidadeDaCoorteDTO execute() {
        TermoDeConsentimento termo = termoDeConsentimentoPort.vigente();
        HistoricoDeConsentimento historico = obterParticipantesConsentidosUseCase.historicoVigente();

        Set<UUID> autorizam = historico.participantesQueAutorizam();
        List<ResolucaoDaCoorte> coorte = coorteRepository.listarCoorte(autorizam);

        Map<UUID, Long> porAutor = coorte.stream()
                .collect(Collectors.groupingBy(ResolucaoDaCoorte::autorId, Collectors.counting()));

        // Denominador: quem submeteu. Quem nunca submeteu nao tem dado a autorizar e so inflaria a
        // taxa de resposta sem mudar nada na amostra.
        Set<UUID> submeteram = coorteRepository.autoresComResolucao();
        int consentiram = quantosDentre(submeteram, autorizam);
        int recusaram = quantosDentre(submeteram, historico.participantesQueRecusam());

        return new QualidadeDaCoorteDTO(
                porAutor.size(),
                coorte.size(),
                (int) coorte.stream().filter(this::comMetrica).count(),
                (int) coorte.stream().filter(this::aguardandoAnalise).count(),
                (int) coorte.stream().filter(this::falhaDeAnalise).count(),
                (int) coorte.stream().filter(this::semAnalisadorDeLinguagem).count(),
                (int) porAutor.values().stream().filter(total -> total == 1L).count(),
                primeiraSubmissao(coorte),
                ultimaSubmissao(coorte),
                porLinguagem(coorte),
                porConfiancaDoTempo(coorte),
                porAutonomia(coorte),
                termo.versao(),
                termo.aprovadoPeloComite(),
                consentiram,
                recusaram,
                submeteram.size() - consentiram - recusaram,
                (int) (coorteRepository.totalDeResolucoes() - coorte.size()));
    }

    private int quantosDentre(Set<UUID> populacao, Set<UUID> subconjunto) {
        return (int) populacao.stream().filter(subconjunto::contains).count();
    }

    // ------------------------------------------------------------------ cobertura
    // Os quatro baldes particionam a coorte: toda resolucao cai em exatamente um. Todos falam da
    // CLASSE DE TEMPO — e ela que `temMetrica()` testa, e e ela que a Carta e a Matriz plotam.

    /**
     * O motor nao produz classe de tempo para esta linguagem — escopo conhecido, nao defeito. Domina
     * os demais baldes.
     * <p>
     * A pergunta e por METRICA, e nao por linguagem, porque o suporte pode ser <b>parcial</b>: uma
     * linguagem nova entra no motor pela ciclomatica — que e contagem — antes de ganhar as
     * estimativas de Big-O e de espaco. Foi assim que C entrou. Com a pergunta antiga
     * ({@code suporta(linguagem)}), toda resolucao numa linguagem nesse estagio cairia em <i>falha
     * de analise</i>: a tela acusaria defeito no motor onde so falta instrumento, e num piloto
     * concentrado nessa linguagem isso seria quase a amostra inteira.
     * <p>
     * Hoje C ja produz as tres, e este balde volta a valer so para linguagem sem analisador nenhum.
     * A pergunta por metrica continua sendo a certa — e o que permite a proxima linguagem entrar
     * pela porta estreita sem que a plataforma a trate como defeito.
     */
    private boolean semAnalisadorDeLinguagem(ResolucaoDaCoorte resolucao) {
        return !analisadorMetricas.produz(TipoMetrica.BIG_O_TEMPO, resolucao.linguagem());
    }

    private boolean comMetrica(ResolucaoDaCoorte resolucao) {
        return !semAnalisadorDeLinguagem(resolucao) && resolucao.temMetrica();
    }

    private boolean aguardandoAnalise(ResolucaoDaCoorte resolucao) {
        return !semAnalisadorDeLinguagem(resolucao) && !resolucao.analisada();
    }

    /**
     * O motor sabia analisar, rodou e nao produziu metrica: o codigo nao parseou. E o unico dos
     * quatro baldes que pede investigacao, e por isso ele existe separado do anterior.
     */
    private boolean falhaDeAnalise(ResolucaoDaCoorte resolucao) {
        return !semAnalisadorDeLinguagem(resolucao) && resolucao.analisada() && !resolucao.temMetrica();
    }

    // ------------------------------------------------------------------ distribuicoes

    private OffsetDateTime primeiraSubmissao(List<ResolucaoDaCoorte> coorte) {
        return coorte.stream().map(ResolucaoDaCoorte::submetidaEm).min(Comparator.naturalOrder()).orElse(null);
    }

    private OffsetDateTime ultimaSubmissao(List<ResolucaoDaCoorte> coorte) {
        return coorte.stream().map(ResolucaoDaCoorte::submetidaEm).max(Comparator.naturalOrder()).orElse(null);
    }

    private List<ContagemPorLinguagemDTO> porLinguagem(List<ResolucaoDaCoorte> coorte) {
        Map<LinguagemProgramacao, Long> contagem = contar(coorte, ResolucaoDaCoorte::linguagem);
        return contagem.entrySet().stream()
                .sorted(Map.Entry.<LinguagemProgramacao, Long>comparingByValue().reversed())
                .map(entrada -> new ContagemPorLinguagemDTO(entrada.getKey(), entrada.getValue().intValue(),
                        analisadorMetricas.metricasSuportadas(entrada.getKey())))
                .toList();
    }

    /**
     * Confianca declarada pelo motor nas resolucoes que <b>tem</b> metrica de tempo. Resolucao sem
     * metrica nao entra: ela nao tem confianca baixa, ela nao tem confianca nenhuma.
     */
    private List<ContagemDTO> porConfiancaDoTempo(List<ResolucaoDaCoorte> coorte) {
        Map<NivelConfianca, Long> contagem = contar(
                coorte.stream().filter(resolucao -> resolucao.confiancaTempo() != null).toList(),
                ResolucaoDaCoorte::confiancaTempo);
        return Arrays.stream(NivelConfianca.values())
                .filter(contagem::containsKey)
                .map(nivel -> new ContagemDTO(nivel.name(), contagem.get(nivel).intValue()))
                .toList();
    }

    /** Escala fixa 1..5, inclusive os niveis com zero — o buraco na distribuicao e informacao. */
    private List<ContagemDTO> porAutonomia(List<ResolucaoDaCoorte> coorte) {
        Map<Integer, Long> contagem = contar(coorte, ResolucaoDaCoorte::indiceAutonomiaIA);
        return IntStream.rangeClosed(1, 5)
                .mapToObj(nivel -> new ContagemDTO(String.valueOf(nivel),
                        contagem.getOrDefault(nivel, 0L).intValue()))
                .toList();
    }

    private <T> Map<T, Long> contar(List<ResolucaoDaCoorte> coorte, Function<ResolucaoDaCoorte, T> chave) {
        return coorte.stream().collect(Collectors.groupingBy(chave, LinkedHashMap::new, Collectors.counting()));
    }
}
