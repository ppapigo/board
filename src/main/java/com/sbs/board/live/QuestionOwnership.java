package com.sbs.board.live;

public record QuestionOwnership(Long authorId, Long hostId) {

    public boolean allows(Long userId){
        return authorId.equals(userId) || hostId.equals(userId);
    }
}
