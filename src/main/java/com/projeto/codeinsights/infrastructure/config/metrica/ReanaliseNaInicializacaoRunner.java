package com.projeto.codeinsights.infrastructure.config.metrica;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.projeto.codeinsights.application.knowledge.dto.RelatorioReanaliseDTO;
import com.projeto.codeinsights.application.knowledge.usecase.ReanalisarResolucoesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reanalisa o corpus inteiro no boot. <b>Desligado por padrao</b>: so roda com
 * {@code app.reanalisar-na-inicializacao=true} (env {@code REANALISAR_NA_INICIALIZACAO}), porque e
 * uma reescrita em massa de dado de pesquisa e precisa ser deliberada.
 * <p>
 * Alternativa ao {@code POST /api/admin/metricas/reanalisar} para quando nao ha conta admin semeada
 * (ADMIN_PASSWORD ausente) ou para um deploy que ja deve subir com o corpus atualizado. Delega ao
 * mesmo caso de uso — nao existe uma segunda implementacao da reanalise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReanaliseNaInicializacaoRunner implements CommandLineRunner {

    private final ReanalisarResolucoesUseCase reanalisarResolucoesUseCase;

    @Value("${app.reanalisar-na-inicializacao:false}")
    private boolean habilitado;

    @Override
    public void run(String... args) {
        if (!habilitado) {
            return;
        }

        log.warn("app.reanalisar-na-inicializacao=true: reprocessando as metricas do corpus com o motor atual...");
        RelatorioReanaliseDTO relatorio = reanalisarResolucoesUseCase.execute(null);

        log.warn("Reanalise de inicializacao: {} resolucao(oes), {} reprocessada(s), {} pulada(s), "
                + "{} falha(s), {} com mudanca de rotulo/confianca.",
                relatorio.total(), relatorio.reprocessadas(), relatorio.puladas(),
                relatorio.falhas(), relatorio.comMudanca());

        relatorio.alteracoes().forEach(alterada -> alterada.mudancas().forEach(mudanca -> log.info(
                "  resolucao {} | {}: {} ({}) -> {} ({})",
                alterada.resolucaoId(), mudanca.tipo(),
                mudanca.rotuloAntes(), mudanca.confiancaAntes(),
                mudanca.rotuloDepois(), mudanca.confiancaDepois())));
    }
}
