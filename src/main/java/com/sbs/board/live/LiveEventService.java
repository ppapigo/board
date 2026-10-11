package com.sbs.board.live;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.live.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveEventService {
    private static final int MAX_CODES = 5;

    private final LiveEventRepository eventRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final LiveCounterStore counterStore;
    private final LiveSseRegistry sseRegistry;
    private final ApplicationEventPublisher publisher;
    private final LiveCodeGenerator codeGenerator;
    private final PollRepository pollRepository;

    private String issueCode(){
        for (int attempt=0; attempt<MAX_CODES; attempt++){
            String code = codeGenerator.generate();
            if (!eventRepository.existsByCode(code))
                return code;
            }
            log.error("참여 코드 발행 실패: {}회 연속 충돌",MAX_CODES);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

    @Transactional
    public LiveEventResponse create(Long hostId, LiveEventCreateRequest request){
        User host = userRepository.findById(hostId)
                .orElseThrow(()->new NotFoundException(ErrorCode.USER_NOT_FOUND));

        LiveEvent event = eventRepository.save(new LiveEvent(host, request.title(), issueCode()));
        log.info("라이브 이벤트 생성됨: id={}, code={}, host={}",event.getId(),event.getCode(),event.getHost().getEmail());
        return LiveEventResponse.from(event, hostId);
    }

    @Transactional(readOnly = true)
    public List<LiveEventResponse> getMine(Long hostId){
        return eventRepository.findByHostIdOrderByIdDesc(hostId).stream()
                .map(event -> LiveEventResponse.from(event, hostId))
                .toList();
    }

    @Transactional(readOnly = true)
    public LiveEvent getEvent(String code){
        return eventRepository.findByCode(LiveCodeGenerator.normalize(code))
                .orElseThrow(()->new NotFoundException(ErrorCode.LIVE_EVENT_NOT_FOUND));
    }

    private List<QuestionResponse> rankedQuestions(Long eventId, Long viewerId){
        Map<Long, Long> likeCounts = counterStore.likeCounts(eventId);
        Set<Long> liked = counterStore.likedQuestionIds(eventId, viewerId);
        return questionRepository.findByEventId(eventId).stream()
                .map( question -> QuestionResponse.of(
                        question,
                        likeCounts.getOrDefault(question.getId(), 0L),
                        liked.contains(question.getId()),
                        viewerId
                ))
                .sorted(QuestionResponse.RANKING)
                .toList();
    }

    private List<PollResponse> polls(Long eventId, Long viewerId){
        return pollRepository.findByEventIdOrderByIdDesc(eventId).stream()
                .map(poll->PollResponse.of(poll,
                         counterStore.voteCount(poll.getId()),
                        counterStore.voteOptionId(poll.getId(), viewerId)
                        )).toList();
    }

    @Transactional(readOnly = true)
    public LiveSnapshotResponse getSnapshot(String code, Long viewerId){
        LiveEvent event = getEvent(code);
        return new LiveSnapshotResponse(
                LiveEventResponse.from(event, viewerId),
                rankedQuestions(event.getId(), viewerId),
                polls(event.getId(), viewerId)

        );
    }

    @Transactional
    public void close(String code){
        LiveEvent event = getEvent(code);
        if (!event.isOpen()){
            return;
        }

        event.close();;
        publisher.publishEvent(new LiveChangedEvent(event.getId(),
                LiveChangedEvent.EVENT_CLOSED,
                new LivePayloads.EventRef(event.getId())
                ));

        log.info("라이브 이벤트 종료: id:{}, code:{}", event.getId(),event.getCode());

    }
    @Transactional(readOnly = true)
    public SseEmitter subscribe(String code){
        LiveEvent event = getEvent(code);
        return event.isOpen() ? sseRegistry.subscribe(event.getId()) : sseRegistry.closed(event.getId());
    }
}
