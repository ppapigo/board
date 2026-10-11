package com.sbs.board.live.dto;

import com.sbs.board.live.LiveEvent;
import com.sbs.board.live.LiveStatus;

import java.time.LocalDateTime;

public record LiveEventResponse(
        Long id,
        String code,
        String title,
        LiveStatus status,
        String hostUsername,
        boolean host,
        LocalDateTime createdAt
) {
    public static LiveEventResponse from(LiveEvent event, Long viewerId){
        return new LiveEventResponse(
            event.getId(),
                event.getCode(),
                event.getTitle(),
                event.getStatus(),
                event.getHost().getEmail(),
                event.isHostedBy(viewerId),
                event.getCreatedAt()
        );
    }
}
