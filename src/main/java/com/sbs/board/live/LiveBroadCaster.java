package com.sbs.board.live;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LiveBroadCaster {
    private final LiveSseRegistry registry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(LiveChangedEvent event){
        registry.broadcast(event.eventId(), event.name(), event.payload());

        if (LiveChangedEvent.EVENT_CLOSED.equals(event.name())){
            registry.completeAll(event.eventId());
        }
    }

}
