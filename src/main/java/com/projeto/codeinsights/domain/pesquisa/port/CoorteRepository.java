package com.projeto.codeinsights.domain.pesquisa.port;

import java.util.List;

import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;

/**
 * Porta de leitura do corpus de pesquisa: todas as resolucoes da plataforma, de todos os
 * participantes, com as tres metricas ja pivotadas.
 * <p>
 * Nao ha paginacao aqui de proposito. Tanto o relatorio de qualidade quanto o export CSV precisam
 * da <b>amostra inteira</b> para significar alguma coisa — uma taxa de cobertura calculada sobre a
 * primeira pagina nao e uma taxa de cobertura. A escala do piloto (dezenas de alunos, centenas de
 * resolucoes) cabe folgadamente em memoria; se um dia deixar de caber, a mudanca e trocar esta
 * porta por uma que agregue no banco, nao paginar o que precisa ser inteiro.
 */
public interface CoorteRepository {

    /** Todas as resolucoes da plataforma, mais antigas primeiro. */
    List<ResolucaoDaCoorte> listarCoorte();
}
