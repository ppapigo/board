package com.sbs.board.live.dto;

import java.util.List;

public record LiveSnapshotResponse(
        LiveEventResponse event,
        List<QuestionResponse> questions,
        List<PollResponse> polls
) {
}
