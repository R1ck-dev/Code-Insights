package com.projeto.codeinsights.application.pesquisa.usecase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializa linhas em CSV conforme a RFC 4180. Existe separado do caso de uso porque escapar CSV e
 * uma regra que se acerta uma vez: um titulo de desafio com virgula, aspas ou quebra de linha
 * desloca todas as colunas seguintes daquela linha, e o erro so aparece na planilha do pesquisador.
 * <p>
 * Decisoes do formato, todas com um lado ruim assumido:
 * <ul>
 *   <li><b>Virgula</b> como separador — e o padrao que R, pandas e SPSS leem sem parametro. O Excel
 *       em portugues espera ponto-e-virgula e vai precisar do assistente de importacao.</li>
 *   <li><b>BOM UTF-8</b> no inicio — sem ele o Excel exibe titulo acentuado como lixo, em silencio.
 *       Em troca, {@code pandas.read_csv} sem {@code encoding='utf-8-sig'} carrega o BOM no nome da
 *       primeira coluna. O defeito com BOM e visivel na hora; o defeito sem BOM passa batido.</li>
 *   <li><b>CRLF</b> como fim de linha, como manda a RFC; toda ferramenta de analise aceita.</li>
 * </ul>
 */
final class EscritorDeCsv {

    /**
     * Marca de ordem de bytes: e o que faz o Excel reconhecer o arquivo como UTF-8. Escrita como
     * escape, e nao como o caractere literal, porque literal seria invisivel no fonte — e um
     * caractere invisivel que ninguem consegue ver e ninguem consegue revisar.
     */
    private static final String BOM = "\uFEFF";
    private static final String SEPARADOR = ",";
    private static final String FIM_DE_LINHA = "\r\n";

    /** Caracteres que fazem a planilha ler a celula como formula em vez de texto. */
    private static final String PREFIXOS_DE_FORMULA = "=+-@\t\r";

    private EscritorDeCsv() {
    }

    static String escrever(List<String> cabecalho, List<List<String>> linhas) {
        StringBuilder csv = new StringBuilder(BOM);
        csv.append(formatarLinha(cabecalho));
        linhas.forEach(linha -> csv.append(formatarLinha(linha)));
        return csv.toString();
    }

    private static String formatarLinha(List<String> campos) {
        return campos.stream().map(EscritorDeCsv::escapar).collect(Collectors.joining(SEPARADOR)) + FIM_DE_LINHA;
    }

    /** Campo nulo vira vazio — {@code ,,} e como CSV diz "sem dado", e nao a string "null". */
    private static String escapar(String campo) {
        if (campo == null || campo.isEmpty()) {
            return "";
        }
        String neutralizado = neutralizarFormula(campo);
        if (!precisaDeAspas(neutralizado)) {
            return neutralizado;
        }
        return '"' + neutralizado.replace("\"", "\"\"") + '"';
    }

    /**
     * Titulo de desafio e texto livre escrito por um aluno, e a planilha do pesquisador trata
     * {@code =}, {@code +}, {@code -} e {@code @} no inicio da celula como <b>formula</b>: um titulo
     * {@code =1+1} chega ao pesquisador como {@code 2}, e um {@code #NAME?} entra no lugar do dado
     * sem nenhum aviso. O apostrofo a frente e a neutralizacao padrao — a planilha o consome ao
     * exibir, entao o titulo aparece intacto; aspas de CSV nao serviriam, porque o parser as remove
     * antes de decidir se a celula e formula.
     * <p>
     * Custo assumido: quem ler o arquivo com pandas ou R ve o apostrofo, porque para essas
     * ferramentas ele e um caractere comum. Preferimos o caractere visivel ao dado adulterado.
     */
    private static String neutralizarFormula(String campo) {
        return PREFIXOS_DE_FORMULA.indexOf(campo.charAt(0)) >= 0 ? "'" + campo : campo;
    }

    private static boolean precisaDeAspas(String campo) {
        return campo.contains(SEPARADOR) || campo.contains("\"")
                || campo.contains("\n") || campo.contains("\r");
    }
}
