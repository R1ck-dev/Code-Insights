package com.projeto.codeinsights.application.pesquisa.dto;

import java.util.UUID;

/** Revelacao sob demanda: quem esta por tras de um pseudonimo. */
public record ParticipanteIdentificadoDTO(String pseudonimo, UUID usuarioId, String username, String email) {
}
