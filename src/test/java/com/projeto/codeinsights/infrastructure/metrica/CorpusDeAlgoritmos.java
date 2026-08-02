package com.projeto.codeinsights.infrastructure.metrica;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projeto.codeinsights.infrastructure.metrica.CasoDeCorpus.Categoria;

/**
 * Carrega o corpus de validacao do motor: os arquivos {@code .java} em
 * {@code src/test/resources/corpus/} mais o gabarito anotado a mao em {@code gabarito.json}.
 * <p>
 * O codigo mora em arquivo de verdade, e nao em text block, por dois motivos praticos: colar
 * uma solucao de terceiro vira copiar-colar puro (sem escapar aspas nem barras), e o arquivo
 * pode ser aberto e lido como codigo. O gabarito fica separado porque e <b>anotacao humana</b>
 * sobre o codigo, nao parte dele.
 *
 * @see CasoDeCorpus para a distincao entre gabarito e esperado-do-motor
 */
final class CorpusDeAlgoritmos {

    private static final String RAIZ = "/corpus/";
    private static final ObjectMapper JSON = new ObjectMapper();

    private CorpusDeAlgoritmos() {
    }

    /** Entrada crua do manifesto. Campos ausentes chegam como {@code null}. */
    private record Entrada(
            String arquivo,
            String nome,
            String origem,
            String nota,
            String tempoGabarito,
            String tempoMotor,
            String espacoGabarito,
            String espacoMotor) {
    }

    private record Manifesto(List<Entrada> casos) {
    }

    static List<CasoDeCorpus> casos() {
        return lerManifesto().casos().stream().map(CorpusDeAlgoritmos::montar).toList();
    }

    private static Manifesto lerManifesto() {
        try (InputStream entrada = abrir(RAIZ + "gabarito.json")) {
            return JSON.readValue(entrada, Manifesto.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o manifesto do corpus.", e);
        }
    }

    private static CasoDeCorpus montar(Entrada entrada) {
        String tempoEsperado = entrada.tempoMotor() != null ? entrada.tempoMotor() : entrada.tempoGabarito();
        String espacoEsperado = entrada.espacoMotor() != null ? entrada.espacoMotor() : entrada.espacoGabarito();

        exigirEsperadoExplicito(entrada, entrada.tempoGabarito(), entrada.tempoMotor(), "tempo");
        exigirEsperadoExplicito(entrada, entrada.espacoGabarito(), entrada.espacoMotor(), "espaco");

        return new CasoDeCorpus(
                entrada.nome(),
                categoriaDe(entrada.arquivo()),
                entrada.arquivo(),
                lerCodigo(entrada.arquivo()),
                entrada.origem(),
                entrada.nota(),
                entrada.tempoGabarito(),
                tempoEsperado,
                entrada.espacoGabarito(),
                espacoEsperado);
    }

    /**
     * Um gabarito fora da escala do motor (ex.: O(raiz de n)) nunca pode ser o valor esperado —
     * o motor e incapaz de emitir esse rotulo. Sem esta guarda o caso entraria com um esperado
     * impossivel e o teste falharia com uma mensagem que nao explica nada.
     */
    private static void exigirEsperadoExplicito(Entrada entrada, String gabarito, String motor, String metrica) {
        if (gabarito != null && !CasoDeCorpus.estaNaEscala(gabarito) && motor == null) {
            throw new IllegalStateException(
                    "%s: o gabarito de %s (%s) esta fora da escala do motor, entao o manifesto precisa dizer o que o motor responde hoje."
                            .formatted(entrada.arquivo(), metrica, gabarito));
        }
    }

    private static Categoria categoriaDe(String arquivo) {
        String pasta = arquivo.substring(0, arquivo.indexOf('/'));
        return Categoria.valueOf(pasta.toUpperCase());
    }

    private static String lerCodigo(String arquivo) {
        try (InputStream entrada = abrir(RAIZ + arquivo)) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler o caso " + arquivo + ".", e);
        }
    }

    private static InputStream abrir(String caminho) {
        InputStream entrada = CorpusDeAlgoritmos.class.getResourceAsStream(caminho);
        if (entrada == null) {
            throw new IllegalStateException("Recurso do corpus nao encontrado: " + caminho);
        }
        return entrada;
    }
}
