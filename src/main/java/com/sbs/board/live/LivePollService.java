package com.sbs.board.live;

import com.sbs.board.global.exception.DuplicateException;
import com.sbs.board.global.exception.ErrorCode;
import com.sbs.board.global.exception.NotFoundException;
import com.sbs.board.live.dto.PollCreateRequest;
import com.sbs.board.live.dto.PollResponse;
import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivePollService {
    private final LiveEventService liveEventService;
    private final PollRepository pollRepository;
    private final LiveCounterStore counterStore;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public PollResponse create(String code, PollCreateRequest request){
        LiveEvent event = liveEventService.getEvent(code);
        event.assertOpen();

        List<String> options = request.options().stream().map(String::strip).toList();
        Poll poll = pollRepository.save(new Poll(event, request.title().strip(), options));
        PollResponse response = PollResponse.of(poll, Map.of(), null);
        publisher.publishEvent(new LiveChangedEvent(event.getId(), LiveChangedEvent.POLL_CREATED,response));
        return response;
    }

    @Transactional(readOnly = true)
    public PollResponse vote(Long pollId, Long optionId, Long userId){
        Poll poll = pollRepository.findWithEventAndOptionsById(pollId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POLL_NOT_FOUND));
        poll.getEvent().assertOpen();
        poll.assertOpen();

        if(!poll.hasOption(optionId)){
            throw new NotFoundException(ErrorCode.POLL_OPTION_NOT_FOUND);

        }

        if (!counterStore.vote(pollId, optionId, userId)){
            throw new DuplicateException(ErrorCode.ALREADY_VOTED);
        }

        Map<Long, Long> counts = counterStore.voteCount(pollId);
        publisher.publishEvent(new LiveChangedEvent(
                poll.getEvent().getId(),
                LiveChangedEvent.POLL_VOTED,
                PollResponse.of(poll,counts,null)
        ));

        return PollResponse.of(poll, counts, optionId);
    }

    @Transactional
    public void close(Long pollId){
        Poll poll = pollRepository.findWithEventAndOptionsById(pollId)
                .orElseThrow(()->new NotFoundException(ErrorCode.POLL_NOT_FOUND));
        if (!poll.isOpen()){
            return;
        }

        poll.close();
        publisher.publishEvent(new LiveChangedEvent(
                poll.getEvent().getId(),
                LiveChangedEvent.POLL_CLOSED,
                new LivePayloads.IdRef(pollId)
        ));
    }
}
