package com.projeto.codeinsights.domain.knowledge.enums;

/**
 * Confianca do motor de analise estatica no valor que ele proprio calculou.
 * <p>
 * Existe porque inferir a complexidade de codigo arbitrario e indecidivel: em
 * varios pontos o motor precisa assumir um default ("este laco roda n vezes",
 * "esta chamada externa custa O(1)"). Sem registrar isso, um chute entraria no
 * dataset da pesquisa com a mesma autoridade de uma medicao.
 * <ul>
 *   <li>{@link #ALTA} - todo construto foi reconhecido; nenhuma suposicao.</li>
 *   <li>{@link #MEDIA} - ao menos um default conservador foi aplicado; o valor e
 *       plausivel, mas nao deve pesar igual numa analise estatistica fina.</li>
 *   <li>{@link #BAIXA} - o motor nao conseguiu classificar (resultado
 *       {@code DESCONHECIDO}); use para filtrar a amostra, nao para agregar.</li>
 * </ul>
 * Uma metrica exata declara {@link #ALTA} — mas o nivel tambem depende de <b>como o codigo foi
 * lido</b>: a ciclomatica de Java sai de uma AST e e ALTA, enquanto a de C sai de uma leitura por
 * forma e nao passa de {@link #MEDIA}, mesmo sendo a mesma contagem.
 */
public enum NivelConfianca {
    ALTA,
    MEDIA,
    BAIXA;

    /** A menor confianca entre esta e {@code outra} (a incerteza se propaga para cima). */
    public NivelConfianca menor(NivelConfianca outra) {
        return this.ordinal() >= outra.ordinal() ? this : outra;
    }
}
