package com.sbs.board.live;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.live.dto.LikeResponse;
import com.sbs.board.live.dto.QuestionCreateRequest;
import com.sbs.board.live.dto.QuestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LiveQuestionService {
    private final LiveEventService liveEventService;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final LiveCounterStore counterStore;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public QuestionResponse create(String code, Long authorId, QuestionCreateRequest request){
        LiveEvent event = liveEventService.getEvent(code);
        event.assertOpen();
        User author = userRepository.findById(authorId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));
        Question question =questionRepository.save(
                new Question(event, author, request.content())
        );

        counterStore.addQuestion(event.getId(), question.getId());
        publisher.publishEvent(new LiveChangedEvent(event.getId(),
                LiveChangedEvent.QUESTION_CREATED,
                QuestionResponse.of(question,0,false,null)));

        return QuestionResponse.of(question,0,false,authorId);
    }

    @Transactional(readOnly = true)
    public LikeResponse toggleLike(Long questionId, Long userId){
        Question question =questionRepository.findWithEventById(questionId)
                .orElseThrow(()->new NotFoundException(ErrorCode.QUESTION_NOT_FOUND));

        LiveEvent event = question.getEvent();
        event.assertOpen();

        LikeResult result = counterStore.toggleLike(event.getId(),questionId, userId);
        publisher.publishEvent(
                new LiveChangedEvent(event.getId(),
                LiveChangedEvent.QUESTION_LIKED,
                new LivePayloads.QuestionLiked(questionId, result.likeCount())
                )
        );
        return new LikeResponse(questionId, result.liked(), result.likeCount());
    }

    @Transactional
    public void delete(Long questionId){
        Question question =questionRepository.findWithEventById(questionId)
                .orElseThrow(()->new NotFoundException(ErrorCode.QUESTION_NOT_FOUND));

        Long eventId = question.getEvent().getId();
        questionRepository.delete(question);
        counterStore.removeQuestion(eventId,questionId);
        publisher.publishEvent(new LiveChangedEvent(eventId,
                LiveChangedEvent.QUESTION_DELETED, new LivePayloads.IdRef(questionId)));
    }
}
