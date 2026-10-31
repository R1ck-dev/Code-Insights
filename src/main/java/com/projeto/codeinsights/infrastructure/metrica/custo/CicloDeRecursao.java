package com.projeto.codeinsights.infrastructure.metrica.custo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Recursao;
import com.projeto.codeinsights.infrastructure.metrica.custo.AnalisadorDeRecursao.Reducao;

/**
 * Custo de tempo de um <b>ciclo de recursao mutua</b> ({@code par -> impar -> par}), independente de linguagem.
 * <p>
 * Cada membro do ciclo e analisado com os seus proprios parametros (uma {@link Recursao} por membro) e as
 * recorrencias sao fundidas numa so, {@code f(n) = a f(n-c) + g(n)} ou {@code f(n) = a f(n/b) + g(n)}, onde
 * {@code f} e o custo de <b>um</b> membro qualquer do ciclo. A fusao e conservadora por construcao:
 * <ul>
 *   <li>{@code a} e o <b>maior</b> numero de chamadas ao ciclo por caminho entre os membros;</li>
 *   <li>{@code g(n)} e o <b>maior</b> custo local entre os membros;</li>
 *   <li>reducao mista entre os membros ({@code n-1} num, {@code n/2} noutro): <b>sem</b> recorrencia
 *       unica. Tomar a divisiva "que domina" subestimaria o ciclo (a subtrativa e exponencial), entao
 *       a resposta e {@code ?}. A suposicao de reducao mista da recursao simples nao vale aqui.</li>
 * </ul>
 * Assim o resultado nunca fica abaixo do real; pode ficar acima quando os membros sao assimetricos. O
 * resultado e sempre rebaixado a {@link NivelConfianca#MEDIA}.
 * <p>
 * So a reducao aritmetica entra. Backtracking, percurso de estrutura, reducao nao reconhecida, pivo,
 * reducao mista (dentro de um membro ou entre membros) e qualquer cache num membro devolvem vazio — o chamador responde {@code ?} com {@link #semRecorrenciaUnica}.
 */
public final class CicloDeRecursao {

    private CicloDeRecursao() {
    }

    /** A recorrencia unica do ciclo, ou vazio se algum membro sai do que a fusao sabe tratar. */
    public static Optional<Recursao> combinar(List<Recursao> membros) {
        if (membros.isEmpty() || motivoDaRecusa(membros).isPresent()) {
            return Optional.empty();
        }
        Reducao reducao = membros.get(0).reducao();
        return Optional.of(new Recursao(reducao, maiorNumeroDeChamadas(membros), menorFator(membros),
                UsoDeCache.NENHUM, suposicoes(membros)));
    }

    /**
     * {@code f(n)}, o custo de um membro do ciclo, e a recorrencia resolvida sobre ele.
     *
     * @param nomes   os membros, so para a nota
     * @param locais  o custo do corpo de cada membro <b>sem</b> as chamadas ao ciclo
     */
    public static CustoAvaliado resolver(List<String> nomes, Recursao recursao, List<CustoAvaliado> locais) {
        CustoAvaliado local = locais.stream().reduce(CustoAvaliado::mais).orElseThrow();
        return SolucionadorDeRecorrencia.resolver(recursao, local)
                .rebaixado(NivelConfianca.MEDIA)
                .comNota("recursao mutua entre %s: recorrencia unica sobre o ciclo; custo local = maior dos membros"
                        .formatted(String.join(", ", nomes)));
    }

    /** O {@code ?} do ciclo que {@link #combinar} recusou, com o motivo do primeiro membro recusado. */
    public static CustoAvaliado semRecorrenciaUnica(List<String> nomes, List<Recursao> membros) {
        String motivo = motivoDaRecusa(membros).orElse("nenhum membro analisado");
        return CustoAvaliado.desconhecido("recursao mutua entre %s: %s".formatted(String.join(", ", nomes), motivo));
    }

    /** O primeiro motivo pelo qual o ciclo nao tem recorrencia unica: um membro recusado ou a mistura de reducoes. */
    private static Optional<String> motivoDaRecusa(List<Recursao> membros) {
        Optional<String> deMembro = membros.stream()
                .map(CicloDeRecursao::motivoDaRecusa)
                .flatMap(Optional::stream)
                .findFirst();
        return deMembro.isPresent() ? deMembro : motivoDaMistura(membros);
    }

    /** Subtrativa num membro e divisiva noutro: uma recorrencia so teria de escolher uma e erraria a outra. */
    private static Optional<String> motivoDaMistura(List<Recursao> membros) {
        boolean subtrativa = membros.stream().anyMatch(membro -> membro.reducao() == Reducao.SUBTRATIVA);
        boolean divisiva = membros.stream().anyMatch(membro -> membro.reducao() == Reducao.DIVISIVA);
        return subtrativa && divisiva
                ? Optional.of("reducoes mistas entre membros do ciclo (n-c e n/b); nao ha recorrencia unica")
                : Optional.empty();
    }

    private static Optional<String> motivoDaRecusa(Recursao membro) {
        if (AnalisadorDeRecursao.SUPOSICAO_DE_REDUCAO_MISTA.equals(membro.suposicao())) {
            return Optional.of("um membro mistura n-c e n/b nas chamadas ao ciclo; nao ha recorrencia unica");
        }
        if (membro.cache().papel() != UsoDeCache.Papel.NENHUM) {
            return Optional.of("um membro usa cache ou marcacao de visitados; nao ha recorrencia unica");
        }
        if (AnalisadorDeRecursao.SUPOSICAO_DE_PIVO.equals(membro.suposicao())) {
            return Optional.of("um membro recursa sobre subintervalos por um pivo; nao ha recorrencia unica");
        }
        if (membro.reducao() != Reducao.DIVISIVA && membro.reducao() != Reducao.SUBTRATIVA) {
            return Optional.of("um membro tem reducao %s (so n-c e n/b entram no ciclo); nao ha recorrencia unica"
                    .formatted(membro.reducao()));
        }
        return Optional.empty();
    }

    private static int maiorNumeroDeChamadas(List<Recursao> membros) {
        return membros.stream().mapToInt(Recursao::chamadasPorCaminho).max().orElseThrow();
    }

    /** O menor fator entre os membros (todos de uma mesma reducao, depois de {@link #motivoDaMistura}). */
    private static int menorFator(List<Recursao> membros) {
        return membros.stream()
                .mapToInt(Recursao::fator)
                .min()
                .orElseThrow();
    }

    private static String suposicoes(List<Recursao> membros) {
        Set<String> distintas = new LinkedHashSet<>();
        membros.stream().map(Recursao::suposicao).filter(Objects::nonNull).forEach(distintas::add);
        return distintas.isEmpty() ? null : String.join("; ", distintas);
    }
}
