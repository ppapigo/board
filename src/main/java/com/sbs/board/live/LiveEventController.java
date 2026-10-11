package com.sbs.board.live;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.live.dto.LiveEventCreateRequest;
import com.sbs.board.live.dto.LiveEventResponse;
import com.sbs.board.live.dto.LiveSnapshotResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveEventController {
    private final LiveEventService liveEventService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public LiveEventResponse create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LiveEventCreateRequest request
            ){
        return liveEventService.create(userDetails.getId(), request);

    }
    @GetMapping("/events/mine")
    public List<LiveEventResponse> mine(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return liveEventService.getMine(userDetails.getId());
    }

    @GetMapping("/events/{code}")
    public LiveSnapshotResponse snapshot(
            @PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return liveEventService.getSnapshot(code, userDetails.getId());
    }

    @PreAuthorize("@liveSecurity.isHost(#code, authentication.principal)")
    @PostMapping("/events/{code}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String code){
        liveEventService.close(code);

    }

    @GetMapping("/events/{code}/stream")
    public SseEmitter stream(@PathVariable String code, HttpServletResponse response){
        response.setHeader("X-Accel-Buffering","no");
        return liveEventService.subscribe(code);

    }
    //SseEmitter를 응답으로 보내주면 브라우저는 커넥션을 닫지 않고 비동기 연결로 열어둔다.
}
