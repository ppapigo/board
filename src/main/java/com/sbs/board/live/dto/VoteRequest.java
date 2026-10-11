package com.sbs.board.live.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull Long optionId) {
}
