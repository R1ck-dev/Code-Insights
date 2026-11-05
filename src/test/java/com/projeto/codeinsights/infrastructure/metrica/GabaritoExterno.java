package com.projeto.codeinsights.infrastructure.metrica;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.ReducaoAoEscala.Metrica;

/**
 * O gabarito do corpus externo ({@code src/test/resources/externo/gabarito-externo.json}), lido como
 * {@link CasoExterno} por linguagem. O arquivo tem campos que o modelo nao usa (as duas leituras
 * cegas, a decisao de consolidacao), por isso a leitura e por {@link JsonNode} e pega so o que a
 * medicao precisa. A reducao do Adendo 6, item 2, e aplicada aqui, na nota, e nao no arquivo.
 */
final class GabaritoExterno {

    static final String RECURSO = "/externo/gabarito-externo.json";

    /** As linguagens do corpus externo; as duas chaves de {@code casos} no JSON. */
    static final List<LinguagemProgramacao> LINGUAGENS =
            List.of(LinguagemProgramacao.JAVA, LinguagemProgramacao.C);

    private final Map<LinguagemProgramacao, List<CasoExterno>> casos;

    private GabaritoExterno(Map<LinguagemProgramacao, List<CasoExterno>> casos) {
        this.casos = casos;
    }

    static GabaritoExterno dosResources() {
        return de(AnotacaoJson.ler(RECURSO));
    }

    static GabaritoExterno de(JsonNode raiz) {
        Map<LinguagemProgramacao, List<CasoExterno>> porLinguagem = new EnumMap<>(LinguagemProgramacao.class);
        for (LinguagemProgramacao linguagem : LINGUAGENS) {
            JsonNode lista = raiz.path("casos").path(AnotacaoJson.chave(linguagem));
            porLinguagem.put(linguagem, lerLinguagem(lista, linguagem));
        }
        return new GabaritoExterno(porLinguagem);
    }

    List<CasoExterno> casos(LinguagemProgramacao linguagem) {
        return casos.getOrDefault(linguagem, List.of());
    }

    /** Nomes dos arquivos da linguagem: o conjunto que a pasta local tem de igualar. */
    Set<String> arquivos(LinguagemProgramacao linguagem) {
        return casos(linguagem).stream().map(CasoExterno::arquivo).collect(Collectors.toUnmodifiableSet());
    }

    private static List<CasoExterno> lerLinguagem(JsonNode lista, LinguagemProgramacao linguagem) {
        List<CasoExterno> lidos = new ArrayList<>();
        for (JsonNode no : lista) {
            lidos.add(lerCaso(no, linguagem));
        }
        return List.copyOf(lidos);
    }

    private static CasoExterno lerCaso(JsonNode no, LinguagemProgramacao linguagem) {
        String arquivo = AnotacaoJson.texto(no, "arquivo");
        String tempo = AnotacaoJson.texto(no, "tempoGabarito");
        String espaco = AnotacaoJson.textoOuNulo(no, "espacoGabarito");
        return new CasoExterno(
                arquivo,
                linguagem,
                ordemDe(arquivo),
                AnotacaoJson.texto(no, "plataforma"),
                AnotacaoJson.texto(no, "problema"),
                tempo,
                ReducaoAoEscala.nota(arquivo, Metrica.TEMPO, tempo),
                espaco,
                espaco == null ? null : ReducaoAoEscala.nota(arquivo, Metrica.ESPACO, espaco),
                AnotacaoJson.texto(no, "tipoTempo"));
    }

    /** Prefixo numerico do nome do arquivo, ate o primeiro hifen. */
    private static String ordemDe(String arquivo) {
        int hifen = arquivo.indexOf('-');
        if (hifen <= 0) {
            throw new IllegalStateException("Nome de arquivo sem prefixo de ordem: " + arquivo);
        }
        return arquivo.substring(0, hifen);
    }
}
