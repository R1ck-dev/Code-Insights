package com.projeto.codeinsights.domain.pesquisa.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.codeinsights.domain.pesquisa.enums.DecisaoDeConsentimento;
import com.projeto.codeinsights.domain.shared.exception.NegocioException;

/**
 * Uma decisao do participante sobre o TCLE, num instante, para uma versao do termo.
 * <p>
 * <b>Nao tem metodo de mudanca de estado, e isso e a regra central.</b> O registro e append-only:
 * mudar de ideia grava uma linha nova, nunca altera a anterior. Um {@code revogar()} que sobrescreve
 * o aceite apagaria a prova de que houve aceite — exatamente o que o Comite de Etica precisa ver, e
 * exatamente o que uma auditoria pede quando alguem pergunta "com base em que voce usou este dado?".
 * <p>
 * O participante e guardado como {@code UUID}, e nao como {@code Usuario}: referencia entre
 * agregados e por id, e o contexto {@code pesquisa} nao conhece o modelo de {@code identity}.
 */
public class ConsentimentoDePesquisa {

    private final UUID id;
    private final UUID participanteId;
    private final String versaoDoTermo;
    private final DecisaoDeConsentimento decisao;
    private final OffsetDateTime registradoEm;

    /** Construtor de criacao: gera id, carimba o instante e valida invariantes. */
    public ConsentimentoDePesquisa(UUID participanteId, String versaoDoTermo,
            DecisaoDeConsentimento decisao) {
        if (participanteId == null) {
            throw new NegocioException("O participante e obrigatorio.");
        }
        if (versaoDoTermo == null || versaoDoTermo.isBlank()) {
            throw new NegocioException("A versao do termo e obrigatoria.");
        }
        if (decisao == null) {
            throw new NegocioException("A decisao e obrigatoria.");
        }

        this.id = UUID.randomUUID();
        this.participanteId = participanteId;
        this.versaoDoTermo = versaoDoTermo.trim();
        this.decisao = decisao;
        this.registradoEm = OffsetDateTime.now();
    }

    /** Construtor de reconstituicao: estado completo, usado pelo mapper ao carregar do banco. */
    public ConsentimentoDePesquisa(UUID id, UUID participanteId, String versaoDoTermo,
            DecisaoDeConsentimento decisao, OffsetDateTime registradoEm) {
        this.id = id;
        this.participanteId = participanteId;
        this.versaoDoTermo = versaoDoTermo;
        this.decisao = decisao;
        this.registradoEm = registradoEm;
    }

    public boolean autoriza() {
        return decisao == DecisaoDeConsentimento.ACEITE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getParticipanteId() {
        return participanteId;
    }

    public String getVersaoDoTermo() {
        return versaoDoTermo;
    }

    public DecisaoDeConsentimento getDecisao() {
        return decisao;
    }

    public OffsetDateTime getRegistradoEm() {
        return registradoEm;
    }
}
