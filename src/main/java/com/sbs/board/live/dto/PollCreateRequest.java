package com.sbs.board.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PollCreateRequest(
        @NotBlank @Size(max=100) String title,
        @NotNull @Size(min = 2, max = 5) List<@NotBlank @Size(max=50)String> options
) {
}
