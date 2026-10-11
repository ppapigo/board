package com.sbs.board.live;

public record LiveChangedEvent(Long eventId, String name, Object payload) {

    public static final String CONNECTED = "connected";
    public static final String QUESTION_CREATED = "question_created";
    public static final String QUESTION_LIKED = "question_liked";
    public static final String QUESTION_DELETED = "question_deleted";
    public static final String EVENT_CLOSED ="event_closed";

    public static final String POLL_CREATED ="poll.created";
    public static final String POLL_VOTED ="poll.voted";
    public static final String POLL_CLOSED ="poll.closed";
}
