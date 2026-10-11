package com.sbs.board.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
        @NotBlank @Size(max = 100) String content
) {
}
