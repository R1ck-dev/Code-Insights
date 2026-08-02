package com.projeto.codeinsights.application.pesquisa.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Retrato da <b>usabilidade da amostra</b>, nao dos seus resultados. Responde "o dado que estou
 * coletando serve?" antes de responder "o que ele diz?".
 * <p>
 * As quatro contagens de cobertura ({@code comMetrica}, {@code aguardandoAnalise},
 * {@code falhaDeAnalise}, {@code semAnalisadorDeLinguagem}) <b>particionam</b> {@code resolucoes}:
 * toda resolucao cai em exatamente uma. A separacao entre as duas ultimas e a que importa —
 * "linguagem sem analisador" e escopo conhecido e esperado, "falha de analise" e defeito do motor
 * a investigar. As duas produzem o mesmo estado no banco (analisada, sem metrica) e so a porta
 * {@code AnalisadorMetricas.suporta(...)} as distingue.
 *
 * Tudo aqui descreve <b>a coorte consentida</b>, e nao a plataforma: quem nao autorizou nao entra em
 * nenhuma contagem, nem nas distribuicoes. As unicas excecoes sao os quatro campos de consentimento,
 * que existem justamente para mostrar o tamanho do que ficou de fora — cobertura sem saber o
 * denominador nao e cobertura.
 *
 * @param participantesComUmaResolucao quantos alunos submeteram uma vez so — nao contribuem para
 *        nenhuma analise de evolucao, e descobrir isso durante o piloto ainda da tempo de agir.
 * @param versaoDoTermo a versao do TCLE que autoriza esta amostra. Viaja junto do numero porque
 *        "35 resolucoes" significa coisas diferentes conforme o termo que as autorizou.
 * @param termoAprovadoPeloComite quando falso, nada disto e dado de pesquisa — e ensaio.
 * @param participantesSemResposta quem submeteu e ainda nao respondeu ao termo. Silencio nao e
 *        recusa, e a diferenca e acionavel: da para convidar de novo.
 * @param resolucoesForaDaCoorte quanto da plataforma o pesquisador nao esta vendo.
 */
public record QualidadeDaCoorteDTO(
        int participantes,
        int resolucoes,
        int comMetrica,
        int aguardandoAnalise,
        int falhaDeAnalise,
        int semAnalisadorDeLinguagem,
        int participantesComUmaResolucao,
        OffsetDateTime primeiraSubmissao,
        OffsetDateTime ultimaSubmissao,
        List<ContagemPorLinguagemDTO> porLinguagem,
        List<ContagemDTO> porConfiancaDoTempo,
        List<ContagemDTO> porAutonomia,
        String versaoDoTermo,
        boolean termoAprovadoPeloComite,
        int participantesQueConsentiram,
        int participantesQueRecusaram,
        int participantesSemResposta,
        int resolucoesForaDaCoorte) {
}
