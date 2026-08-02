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
 * @param participantesComUmaResolucao quantos alunos submeteram uma vez so — nao contribuem para
 *        nenhuma analise de evolucao, e descobrir isso durante o piloto ainda da tempo de agir.
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
        List<ContagemDTO> porAutonomia) {
}
