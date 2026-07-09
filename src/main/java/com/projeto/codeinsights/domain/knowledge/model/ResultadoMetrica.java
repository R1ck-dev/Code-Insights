package com.projeto.codeinsights.domain.knowledge.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * ResultadoMetrica: uma metrica estatica calculada para uma {@code Resolucao}.
 * <p>
 * O modelo e uniforme (uma instancia por resolucao x {@link TipoMetrica}) para
 * que novas metricas entrem por adicao, sem alterar o schema: {@code valor}
 * guarda a magnitude numerica (a contagem, no caso da ciclomatica, ou a
 * {@code ordem} da classe de complexidade, no caso de Big O e espaco) e
 * {@code rotulo} guarda a forma de exibicao ("7", "O(n^2)"). {@code detalhe}
 * registra o raciocinio que produziu o valor, e {@link NivelConfianca} separa o
 * que o motor deduziu do que teve de assumir - sem isso, um chute entraria no
 * dataset da pesquisa com a mesma autoridade de uma medicao.
 */
public class ResultadoMetrica {

    private UUID id;
    private UUID resolucaoId;
    private TipoMetrica tipo;
    private int valor;
    private String rotulo;
    private String detalhe;
    private NivelConfianca confianca;
    private OffsetDateTime analisadoEm;

    /** Construtor de criacao: gera o id e carimba o instante da analise. */
    public ResultadoMetrica(UUID id, UUID resolucaoId, TipoMetrica tipo, int valor,
            String rotulo, String detalhe, NivelConfianca confianca) {
        if (resolucaoId == null) {
            throw new NegocioException("A resolucao do resultado de metrica e obrigatoria.");
        }
        if (tipo == null) {
            throw new NegocioException("O tipo do resultado de metrica e obrigatorio.");
        }
        if (rotulo == null || rotulo.isBlank()) {
            throw new NegocioException("O rotulo do resultado de metrica e obrigatorio.");
        }
        if (confianca == null) {
            throw new NegocioException("A confianca do resultado de metrica e obrigatoria.");
        }

        this.id = (id != null) ? id : UUID.randomUUID();
        this.resolucaoId = resolucaoId;
        this.tipo = tipo;
        this.valor = valor;
        this.rotulo = rotulo;
        this.detalhe = detalhe;
        this.confianca = confianca;
        this.analisadoEm = OffsetDateTime.now();
    }

    /** Construtor de reconstituicao. */
    public ResultadoMetrica(UUID id, UUID resolucaoId, TipoMetrica tipo, int valor,
            String rotulo, String detalhe, NivelConfianca confianca, OffsetDateTime analisadoEm) {
        this.id = id;
        this.resolucaoId = resolucaoId;
        this.tipo = tipo;
        this.valor = valor;
        this.rotulo = rotulo;
        this.detalhe = detalhe;
        this.confianca = confianca;
        this.analisadoEm = analisadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getResolucaoId() {
        return resolucaoId;
    }

    public TipoMetrica getTipo() {
        return tipo;
    }

    public int getValor() {
        return valor;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public NivelConfianca getConfianca() {
        return confianca;
    }

    public OffsetDateTime getAnalisadoEm() {
        return analisadoEm;
    }
}
