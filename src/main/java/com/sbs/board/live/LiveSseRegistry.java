package com.sbs.board.live;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Slf4j
@Component
public class LiveSseRegistry {
    private static final long TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private SseEmitter createEmitter(){
        return new SseEmitter(TIMEOUT_MILLIS);
    }

    private void remove(Long eventId, SseEmitter emitter){
        emitters.computeIfPresent(eventId, (id, target)->{
            target.remove(emitter);
            return target.isEmpty() ? null : target;
        });
    }

    private void send(Long eventId, SseEmitter emitter, Supplier<SseEmitter.SseEventBuilder> event){
        try{

            emitter.send( event.get());

        }catch (IOException | IllegalStateException e){
            log.debug("SSE 전송실패, eventId={}",eventId);
            remove(eventId, emitter);
        }
    }

    public void completeAll(Long eventId){
        Set<SseEmitter> target = emitters.remove(eventId);
        if( target != null) {
            target.forEach(ResponseBodyEmitter::complete);
        }
    }

    public void broadcast(Long eventId, String name, Object payload){
        Set<SseEmitter> target = emitters.get(eventId);
        if ( target == null){
            return;
        }

        for (SseEmitter emitter : target){
            send(eventId, emitter, ()->SseEmitter.event()
                    .name(name)
                    .data(payload, MediaType.APPLICATION_JSON));
        }
    }

    public SseEmitter closed(Long eventId){
        SseEmitter emitter = createEmitter();
        try{
            emitter.send(SseEmitter.event()
                    .name(LiveChangedEvent.CONNECTED)
                    .data(new LivePayloads.EventRef(eventId), MediaType.APPLICATION_JSON));
            emitter.send(SseEmitter.event()
                    .name(LiveChangedEvent.EVENT_CLOSED)
                    .data(new LivePayloads.EventRef(eventId), MediaType.APPLICATION_JSON));
            emitter.complete();

        }catch (IOException | IllegalStateException e){
            emitter.completeWithError( e );
        }

        return emitter;
    }

    @Scheduled(fixedRate = 25000)
    public void heartbeat(){
        emitters.forEach((eventId, emitterSet)->{
            emitterSet.forEach(emitter -> send(eventId,emitter,()->SseEmitter.event().comment("ping")));

        });


    }

    public SseEmitter subscribe(Long eventId){

        SseEmitter emitter = createEmitter();

        emitters.compute(eventId, (id,current)->{
            Set<SseEmitter> target = current != null ? current : ConcurrentHashMap.newKeySet();
            target.add(emitter);
            return target;
        });

        // 연결 종료 처리
        emitter.onCompletion(()->remove(eventId,emitter));

        // 타임아웃 처리
        emitter.onTimeout(()-> {
            remove(eventId, emitter);
            emitter.complete();
        });

        // 에러 처리
        emitter.onError(e->remove(eventId,emitter));

        // 연결완료 처리
        send(eventId, emitter, ()->SseEmitter.event()
                .name(LiveChangedEvent.CONNECTED)
                .data(new LivePayloads.EventRef(eventId), MediaType.APPLICATION_JSON));

        log.debug("SSE 구독: eventId={}",eventId);
        return emitter;
    }
}
