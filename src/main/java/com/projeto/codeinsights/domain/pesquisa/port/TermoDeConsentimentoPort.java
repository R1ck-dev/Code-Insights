package com.projeto.codeinsights.domain.pesquisa.port;

import com.projeto.codeinsights.domain.pesquisa.model.TermoDeConsentimento;

/**
 * De onde sai o texto do TCLE em vigor. E porta de servico externo, e nao repositorio: o termo nao
 * e dado da aplicacao, e um documento aprovado fora dela.
 */
public interface TermoDeConsentimentoPort {

    TermoDeConsentimento vigente();
}
