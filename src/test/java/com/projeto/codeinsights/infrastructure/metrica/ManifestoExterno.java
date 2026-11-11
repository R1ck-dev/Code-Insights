package com.projeto.codeinsights.infrastructure.metrica;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * O manifesto de proveniencia do corpus externo
 * ({@code src/test/resources/externo/manifesto-proveniencia.json}): para cada caso aceito, o hash
 * SHA-256 do arquivo coletado, e as contagens do funil de coleta.
 * <p>
 * O hash vem so de {@code <linguagem>.casos[].solucao.sha256}, sem busca recursiva: o caso que
 * trocou de solucao guarda um {@code sha256} a mais, o da solucao descartada, e esse nao vale.
 */
final class ManifestoExterno {

    static final String RECURSO = "/externo/manifesto-proveniencia.json";

    /** O funil de uma linguagem: quantos candidatos se percorreu, quantos entraram e por que os outros sairam. */
    record Contagens(int percorridos, int aceitos, Map<String, Integer> exclusoesPorMotivo) {
    }

    private final Map<LinguagemProgramacao, Map<String, String>> hashes = new EnumMap<>(LinguagemProgramacao.class);
    private final Map<LinguagemProgramacao, Contagens> contagens = new EnumMap<>(LinguagemProgramacao.class);

    private ManifestoExterno(JsonNode raiz) {
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            JsonNode no = raiz.path(AnotacaoJson.chave(linguagem));
            hashes.put(linguagem, lerHashes(no.path("casos")));
            contagens.put(linguagem, lerContagens(no));
        }
    }

    static ManifestoExterno dosResources() {
        return de(AnotacaoJson.ler(RECURSO));
    }

    static ManifestoExterno de(JsonNode raiz) {
        return new ManifestoExterno(raiz);
    }

    /** {@code arquivoLocal -> sha256} dos casos aceitos da linguagem. */
    Map<String, String> sha256PorArquivo(LinguagemProgramacao linguagem) {
        return hashes.getOrDefault(linguagem, Map.of());
    }

    Contagens contagens(LinguagemProgramacao linguagem) {
        return contagens.get(linguagem);
    }

    private static Map<String, String> lerHashes(JsonNode casos) {
        Map<String, String> porArquivo = new LinkedHashMap<>();
        for (JsonNode caso : casos) {
            JsonNode solucao = caso.path("solucao");
            String arquivo = AnotacaoJson.texto(solucao, "arquivoLocal");
            if (porArquivo.put(arquivo, AnotacaoJson.texto(solucao, "sha256")) != null) {
                throw new IllegalStateException("Arquivo repetido no manifesto: " + arquivo);
            }
        }
        return Map.copyOf(porArquivo);
    }

    private static Contagens lerContagens(JsonNode linguagem) {
        Map<String, Integer> exclusoes = new LinkedHashMap<>();
        linguagem.path("exclusoesPorMotivo").fields()
                .forEachRemaining(entrada -> exclusoes.put(entrada.getKey(), entrada.getValue().asInt()));
        return new Contagens(linguagem.path("percorridos").asInt(), linguagem.path("aceitos").asInt(),
                Map.copyOf(exclusoes));
    }
}
