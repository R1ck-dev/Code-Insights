package com.projeto.codeinsights.infrastructure.persistence.pesquisa.adapter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.projeto.codeinsights.domain.knowledge.enums.LinguagemProgramacao;
import com.projeto.codeinsights.domain.knowledge.enums.NivelConfianca;
import com.projeto.codeinsights.domain.knowledge.enums.TipoMetrica;
import com.projeto.codeinsights.domain.pesquisa.model.ResolucaoDaCoorte;
import com.projeto.codeinsights.domain.pesquisa.port.CoorteRepository;
import com.projeto.codeinsights.infrastructure.persistence.pesquisa.repository.SpringDataCoorteRepository;

import lombok.RequiredArgsConstructor;

/** Implementa a porta de leitura do corpus traduzindo a projecao posicional da JPQL. */
@Component
@RequiredArgsConstructor
public class CoorteRepositoryAdapter implements CoorteRepository {

    private final SpringDataCoorteRepository springDataCoorteRepository;

    @Override
    public List<ResolucaoDaCoorte> listarCoorte() {
        return springDataCoorteRepository.coorte(
                TipoMetrica.BIG_O_TEMPO, TipoMetrica.COMPLEXIDADE_ESPACO,
                TipoMetrica.COMPLEXIDADE_CICLOMATICA).stream()
                .map(CoorteRepositoryAdapter::paraDominio)
                .toList();
    }

    /**
     * A ordem aqui e a ordem do select, componente a componente. O ternario nos inteiros preserva o
     * {@code null} de "resolucao nao analisada": {@code (Integer) x} direto perderia a distincao
     * entre nao ter metrica e ter {@code O(1)}, cuja ordem e zero.
     */
    private static ResolucaoDaCoorte paraDominio(Object[] linha) {
        return new ResolucaoDaCoorte(
                (UUID) linha[0],
                (UUID) linha[1],
                (UUID) linha[2],
                (String) linha[3],
                (LinguagemProgramacao) linha[4],
                ((Number) linha[5]).intValue(),
                (Boolean) linha[6],
                (String) linha[7],
                linha[8] == null ? null : ((Number) linha[8]).intValue(),
                (NivelConfianca) linha[9],
                (String) linha[10],
                linha[11] == null ? null : ((Number) linha[11]).intValue(),
                (NivelConfianca) linha[12],
                linha[13] == null ? null : ((Number) linha[13]).intValue(),
                (OffsetDateTime) linha[14]);
    }
}
