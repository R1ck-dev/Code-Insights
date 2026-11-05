package com.projeto.codeinsights.infrastructure.metrica;

/**
 * Tabela em Markdown, uma linha por chamada. O texto de cada celula perde {@code |} e quebra de
 * linha, que desmontariam a tabela; o relatorio so escreve nomes de arquivo, rotulos e numeros.
 */
final class TabelaMd {

    private final int colunas;
    private final StringBuilder md = new StringBuilder();

    TabelaMd(String... cabecalho) {
        this.colunas = cabecalho.length;
        escrever(cabecalho);
        md.append("|").append("---|".repeat(colunas)).append('\n');
    }

    TabelaMd linha(String... celulas) {
        if (celulas.length != colunas) {
            throw new IllegalArgumentException("A tabela tem %d colunas; a linha trouxe %d."
                    .formatted(colunas, celulas.length));
        }
        escrever(celulas);
        return this;
    }

    /** A tabela inteira, seguida de uma linha em branco. */
    String renderizar() {
        return md + "\n";
    }

    private void escrever(String[] celulas) {
        md.append('|');
        for (String celula : celulas) {
            md.append(' ').append(celula.replace("|", "\\|").replace('\n', ' ')).append(" |");
        }
        md.append('\n');
    }
}
