package com.projeto.codeinsights.domain.pesquisa.port;

import java.util.List;
import java.util.UUID;

import com.projeto.codeinsights.domain.pesquisa.model.ConsentimentoDePesquisa;

/**
 * Persistencia do registro de consentimento. <b>Nao ha metodo de atualizacao nem de remocao</b>:
 * a tabela e um livro-razao, e apagar uma decisao apagaria a base legal de ter usado o dado.
 */
public interface ConsentimentoRepository {

    ConsentimentoDePesquisa registrar(ConsentimentoDePesquisa consentimento);

    /**
     * Todas as decisoes ja tomadas sobre uma versao do termo, por todos os participantes. Quem
     * reduz isto a "decisao vigente" e {@code HistoricoDeConsentimento}, no dominio — a regra de
     * qual decisao vale nao pode morar numa consulta.
     */
    List<ConsentimentoDePesquisa> decisoesDaVersao(String versaoDoTermo);

    /** O historico de uma pessoa so, em qualquer versao — o que a propria pessoa tem direito de ver. */
    List<ConsentimentoDePesquisa> historicoDoParticipante(UUID participanteId);
}
