package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Corpus externo de mentira para os testes: tres arquivos escritos a mao (dois Java, um C), com o
 * gabarito e o manifesto correspondentes montados em {@link JsonNode}. Cada fonte leva o
 * {@link #TOKEN}, para que um teste prove que ele nao vaza para mensagem nem para {@code toString()}.
 * Nada daqui e codigo de terceiros.
 */
final class CorpusExternoSintetico {

    static final String TOKEN = "SEGREDO_DO_FIXTURE";

    static final String JAVA_VALIDO = "class Soma { // " + TOKEN + " ação\n"
            + "  int f(int[] v) { int s = 0; for (int x : v) { s += x; } return s; }\n}\n";
    static final String JAVA_INVALIDO = "class { " + TOKEN + " ;;; (((\n";
    static final String C_VALIDO = "/* " + TOKEN + " */\n"
            + "int soma(int *v, int n) { int s = 0; for (int i = 0; i < n; i++) { s += v[i]; } return s; }\n";

    /** Um arquivo do corpus sintetico. */
    record Arquivo(LinguagemProgramacao linguagem, String nome, String conteudo) {
    }

    static final Arquivo PRIMEIRO_JAVA = new Arquivo(LinguagemProgramacao.JAVA, "001-leetcode-11.java", JAVA_VALIDO);
    static final Arquivo SEGUNDO_JAVA = new Arquivo(LinguagemProgramacao.JAVA, "002-leetcode-22.java", JAVA_VALIDO);
    static final Arquivo UNICO_C = new Arquivo(LinguagemProgramacao.C, "003-leetcode-33.c", C_VALIDO);

    static final List<Arquivo> PADRAO = List.of(PRIMEIRO_JAVA, SEGUNDO_JAVA, UNICO_C);

    private static final ObjectMapper JSON = new ObjectMapper();

    private CorpusExternoSintetico() {
    }

    /** Grava os arquivos em {@code pasta/java} e {@code pasta/c}, criando as duas subpastas. */
    static void gravar(Path pasta, List<Arquivo> arquivos) {
        try {
            for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
                Files.createDirectories(pasta.resolve(AnotacaoJson.chave(linguagem)));
            }
            for (Arquivo arquivo : arquivos) {
                Path destino = pasta.resolve(AnotacaoJson.chave(arquivo.linguagem())).resolve(arquivo.nome());
                Files.write(destino, arquivo.conteudo().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Gabarito de tempo {@code O(n)} e espaco {@code O(1)} para cada arquivo. */
    static JsonNode gabarito(List<Arquivo> arquivos) {
        ObjectNode casos = JSON.createObjectNode();
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            ArrayNode lista = casos.putArray(AnotacaoJson.chave(linguagem));
            arquivos.stream().filter(a -> a.linguagem() == linguagem).forEach(a -> lista.add(casoDoGabarito(a)));
        }
        return JSON.createObjectNode().set("casos", casos);
    }

    /** Manifesto com o SHA-256 verdadeiro de cada arquivo, exceto os de {@code hashesForcados}. */
    static JsonNode manifesto(List<Arquivo> arquivos, Map<String, String> hashesForcados) {
        ObjectNode raiz = JSON.createObjectNode();
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            ArrayNode lista = raiz.putObject(AnotacaoJson.chave(linguagem)).putArray("casos");
            arquivos.stream().filter(a -> a.linguagem() == linguagem)
                    .forEach(a -> lista.add(casoDoManifesto(a, hashesForcados)));
        }
        return raiz;
    }

    static JsonNode manifesto(List<Arquivo> arquivos) {
        return manifesto(arquivos, Map.of());
    }

    static String sha256(String conteudo) {
        return CorpusExternoLocal.sha256(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    private static ObjectNode casoDoGabarito(Arquivo arquivo) {
        String[] partes = arquivo.nome().split("[-.]");
        ObjectNode caso = JSON.createObjectNode();
        caso.put("arquivo", arquivo.nome());
        caso.put("plataforma", partes[1]);
        caso.put("problema", partes[2]);
        caso.put("tempoGabarito", "O(n)");
        caso.put("espacoGabarito", "O(1)");
        caso.put("tipoTempo", "pior-caso");
        return caso;
    }

    private static ObjectNode casoDoManifesto(Arquivo arquivo, Map<String, String> hashesForcados) {
        ObjectNode solucao = JSON.createObjectNode();
        solucao.put("arquivoLocal", arquivo.nome());
        solucao.put("sha256", hashesForcados.getOrDefault(arquivo.nome(), sha256(arquivo.conteudo())));
        return JSON.createObjectNode().set("solucao", solucao);
    }
}
