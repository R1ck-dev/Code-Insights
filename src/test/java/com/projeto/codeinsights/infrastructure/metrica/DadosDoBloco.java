package com.projeto.codeinsights.infrastructure.metrica;

import java.time.LocalDate;
import java.util.List;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.infrastructure.metrica.RelatorioDeAcuracia.Medida;

/**
 * Tudo de que o bloco externo e feito: as medidas do externo, as do corpus proprio para a coluna ao
 * lado, as contagens de exclusao do manifesto e a data. Nada aqui carrega codigo-fonte.
 */
record DadosDoBloco(List<MedidaExterna> externas, List<Medida> proprias, ManifestoExterno manifesto,
        LocalDate data) {

    List<ResumoExterno> resumos() {
        return ResumoExterno.porLinguagem(externas);
    }

    List<Medida> propriasDe(LinguagemProgramacao linguagem) {
        return proprias.stream().filter(m -> m.caso().linguagem() == linguagem).toList();
    }
}
