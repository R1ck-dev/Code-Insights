package com.projeto.codeinsights.domain.shared;

import java.util.List;

public record Pagina<T>(
        List<T> itens,
        int paginaAtual,
        int totalPaginas,
        long totalItens) {
}
