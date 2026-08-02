package com.projeto.codeinsights.application.pesquisa.dto;

import java.time.OffsetDateTime;

import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;

/**
 * O termo em vigor mais a decisao de quem esta perguntando. Vem junto de proposito: a tela precisa
 * mostrar o texto e o estado atual na mesma leitura, e separa-los abriria a janela em que a pessoa
 * ve um texto e responde sobre outro.
 *
 * @param decisao {@code null} quando a pessoa ainda nao respondeu sobre esta versao. Ausencia de
 *        resposta e diferente de {@code RECUSA}: uma e silencio, a outra e um "nao" deliberado, e a
 *        tela precisa poder dizer coisas diferentes nos dois casos.
 * @param podeSerUsadoEmPesquisa falso enquanto o termo for rascunho — aceite a versao nao aprovada
 *        pelo Comite de Etica nao autoriza nada, e a interface tem de dizer isso a quem aceita.
 */
public record MeuConsentimentoDTO(
        String versao,
        String titulo,
        String texto,
        boolean podeSerUsadoEmPesquisa,
        DecisaoDeConsentimento decisao,
        OffsetDateTime decididoEm) {
}
