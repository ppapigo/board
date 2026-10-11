package com.sbs.board.live;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("liveSecurity")
@RequiredArgsConstructor
public class LiveSecurity {
    private final LiveEventRepository eventRepository;
    private final QuestionRepository questionRepository;
    private final PollRepository pollRepository;

    public boolean isHost(String code, CustomUserDetails user) {
        Long hostId = eventRepository.findHostIdByCode(LiveCodeGenerator.normalize(code))
                .orElseThrow(()->new NotFoundException(ErrorCode.LIVE_EVENT_NOT_FOUND));

        return hostId.equals(user.getId());
    }

    public boolean canDeleteQuestion(Long questionId, CustomUserDetails user){
        return questionRepository.findOwnershipById(questionId)
                .orElseThrow(()->new NotFoundException(ErrorCode.QUESTION_NOT_FOUND))
                .allows(user.getId());
    }

    public boolean isPollHost(Long pollId, CustomUserDetails user){
        Long hostId = pollRepository.findHostIdById(pollId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POLL_NOT_FOUND));
        return hostId.equals( user.getId() );
    }
}
