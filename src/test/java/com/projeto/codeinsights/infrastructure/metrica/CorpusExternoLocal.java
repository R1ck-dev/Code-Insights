package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;

/**
 * Carrega da pasta local os fontes do corpus externo (Adendo 6). A pasta so entra como parametro: o
 * codigo de terceiros nunca vai para o git, e esta classe nao sabe onde ela fica.
 * <p>
 * Tudo e verificado <b>antes</b> de devolver qualquer codigo: o conjunto de arquivos de cada
 * linguagem tem de ser exatamente o do gabarito, e o SHA-256 dos bytes de cada um tem de ser o do
 * manifesto. So entao os bytes ja conferidos sao decodificados em UTF-8. Toda mensagem de falha cita
 * so nomes de arquivo e hashes, nunca conteudo.
 */
final class CorpusExternoLocal {

    /** Os bytes de um caso, lidos uma vez e ja conferidos; nao se imprime. */
    private record ArquivoLido(CasoExterno caso, byte[] bytes) {
    }

    private CorpusExternoLocal() {
    }

    /** Os fontes das duas linguagens, na ordem do gabarito. Falha alto, sem devolver nada, se algo nao bater. */
    static List<FonteVerificada> carregar(Path pasta, GabaritoExterno gabarito, ManifestoExterno manifesto) {
        List<ArquivoLido> lidos = new ArrayList<>();
        for (LinguagemProgramacao linguagem : GabaritoExterno.LINGUAGENS) {
            Path dir = pasta.resolve(AnotacaoJson.chave(linguagem));
            lidos.addAll(lerEConferir(dir, linguagem, gabarito, manifesto));
        }
        return lidos.stream().map(CorpusExternoLocal::decodificar).toList();
    }

    private static List<ArquivoLido> lerEConferir(Path dir, LinguagemProgramacao linguagem, GabaritoExterno gabarito,
            ManifestoExterno manifesto) {
        exigirMesmoConjunto(dir, linguagem, gabarito.arquivos(linguagem));
        Map<String, String> hashes = manifesto.sha256PorArquivo(linguagem);
        List<ArquivoLido> lidos = new ArrayList<>();
        List<String> divergencias = new ArrayList<>();
        for (CasoExterno caso : gabarito.casos(linguagem)) {
            String esperado = hashes.get(caso.arquivo());
            if (esperado == null) {
                throw new IllegalStateException("Manifesto sem hash para o arquivo do gabarito: " + caso.arquivo());
            }
            byte[] bytes = ler(dir.resolve(caso.arquivo()), caso.arquivo());
            String obtido = sha256(bytes);
            if (!obtido.equalsIgnoreCase(esperado)) {
                divergencias.add("%s: esperado %s, obtido %s".formatted(caso.arquivo(), esperado, obtido));
            }
            lidos.add(new ArquivoLido(caso, bytes));
        }
        exigirSemDivergencia(linguagem, divergencias);
        return lidos;
    }

    private static void exigirSemDivergencia(LinguagemProgramacao linguagem, List<String> divergencias) {
        if (!divergencias.isEmpty()) {
            throw new IllegalStateException("SHA-256 diferente do manifesto (%s): %s"
                    .formatted(linguagem, String.join("; ", divergencias)));
        }
    }

    private static void exigirMesmoConjunto(Path dir, LinguagemProgramacao linguagem, Set<String> esperados) {
        Set<String> presentes = nomesNaPasta(dir, linguagem);
        Set<String> faltando = new TreeSet<>(esperados);
        faltando.removeAll(presentes);
        Set<String> aMais = new TreeSet<>(presentes);
        aMais.removeAll(esperados);
        if (!faltando.isEmpty() || !aMais.isEmpty()) {
            throw new IllegalStateException("Conjunto da pasta difere do gabarito (%s): faltando %s; a mais %s"
                    .formatted(linguagem, faltando, aMais));
        }
    }

    /** Todo item da pasta conta, inclusive subpasta: o conjunto tem de ser exatamente o do gabarito. */
    private static Set<String> nomesNaPasta(Path dir, LinguagemProgramacao linguagem) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Pasta da linguagem ausente (%s)".formatted(linguagem));
        }
        try (Stream<Path> itens = Files.list(dir)) {
            return itens.map(item -> item.getFileName().toString()).collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel listar a pasta (%s).".formatted(linguagem), e);
        }
    }

    private static byte[] ler(Path arquivo, String nome) {
        try {
            return Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o arquivo " + nome + ".", e);
        }
    }

    /** Decodificacao tolerante: os bytes ja sao os do manifesto, e abortar a medicao por um byte invalido nao ajudaria. */
    private static FonteVerificada decodificar(ArquivoLido lido) {
        return new FonteVerificada(lido.caso(), new String(lido.bytes(), StandardCharsets.UTF_8));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM.", e);
        }
    }
}
