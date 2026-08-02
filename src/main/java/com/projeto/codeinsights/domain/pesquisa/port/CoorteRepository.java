package com.projeto.codeinsights.domain.pesquisa.port;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;

/**
 * Porta de leitura do corpus de pesquisa: as resolucoes dos participantes que autorizaram, com as
 * tres metricas ja pivotadas.
 * <p>
 * <b>O filtro de consentimento e parametro obrigatorio, e nao um passo posterior.</b> Nao existe
 * metodo que devolva a coorte inteira sem ele: assim, esquecer de aplicar o consentimento nao e um
 * descuido possivel, e sim um codigo que nao compila. Dado de quem nao autorizou nunca chega a sair
 * do banco.
 * <p>
 * Nao ha paginacao aqui de proposito. Tanto o relatorio de qualidade quanto o export CSV precisam
 * da <b>amostra inteira</b> para significar alguma coisa — uma taxa de cobertura calculada sobre a
 * primeira pagina nao e uma taxa de cobertura. A escala do piloto (dezenas de alunos, centenas de
 * resolucoes) cabe folgadamente em memoria; se um dia deixar de caber, a mudanca e trocar esta
 * porta por uma que agregue no banco, nao paginar o que precisa ser inteiro.
 */
public interface CoorteRepository {

    /**
     * As resolucoes dos participantes informados, mais antigas primeiro.
     *
     * @param participantesQueAutorizam quem consentiu com a versao vigente do termo. Conjunto vazio
     *        devolve lista vazia — ninguem autorizou, logo nao ha coorte.
     */
    List<ResolucaoDaCoorte> listarCoorte(Set<UUID> participantesQueAutorizam);

    /**
     * Quem ja submeteu ao menos uma resolucao, tenha consentido ou nao. E o denominador honesto da
     * cobertura de consentimento: quem nunca submetiu nao tem dado a autorizar, e infla a estatistica
     * sem significar nada.
     */
    Set<UUID> autoresComResolucao();

    /** Total de resolucoes na plataforma, para medir quanto da base fica fora da coorte. */
    long totalDeResolucoes();
}
