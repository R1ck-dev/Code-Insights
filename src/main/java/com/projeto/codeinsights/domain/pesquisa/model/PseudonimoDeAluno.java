package com.projeto.codeinsights.domain.pesquisa.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Deriva o identificador com que um participante aparece nos dados de pesquisa: {@code A-3F9C21}
 * no lugar do nome e do e-mail.
 * <p>
 * <b>E pseudonimizacao, nao anonimizacao</b>, e a diferenca precisa aparecer em qualquer publicacao
 * dos dados. O codigo e derivado do id do usuario por funcao deterministica e <b>sem segredo</b>:
 * quem tiver a lista de ids consegue recalcular o pseudonimo e refazer a ligacao. Isso e proposital
 * — a pesquisa precisa reconseguir ligar o dado ao aluno para acompanhar o piloto —, mas significa
 * que o CSV exportado nao pode ser tratado como dado anonimo irreversivel.
 * <p>
 * Ser deterministico e o que da <b>estabilidade</b>: o mesmo aluno recebe o mesmo codigo em todo
 * export, hoje e em marco, sem guardar coluna nenhuma no banco. Uma tabela de pseudonimos exigiria
 * migration e poderia divergir; uma funcao pura nao tem como.
 */
public final class PseudonimoDeAluno {

    private static final String PREFIXO = "A-";

    /**
     * Seis digitos hexadecimais dao 16.7 milhoes de codigos. Com quatro seriam 65 mil, e uma coorte
     * de 30 alunos ja teria ~0,7% de chance de colisao — baixo, mas colisao aqui funde dois
     * participantes num so e corrompe o dado em silencio.
     */
    private static final int DIGITOS = 6;

    private PseudonimoDeAluno() {
    }

    public static String de(UUID autorId) {
        if (autorId == null) {
            throw new IllegalArgumentException("Nao ha pseudonimo para autor nulo.");
        }
        byte[] resumo = sha256(autorId.toString());
        return PREFIXO + HexFormat.of().formatHex(resumo).substring(0, DIGITOS).toUpperCase();
    }

    private static byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM.", e);
        }
    }
}
