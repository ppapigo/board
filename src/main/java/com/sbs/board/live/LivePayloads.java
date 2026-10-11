package com.sbs.board.live;

public class LivePayloads {

    public record EventRef(Long eventId){}

    public record IdRef(Long id){}

    public record QuestionLiked(Long id, Long likeCount){}
}
