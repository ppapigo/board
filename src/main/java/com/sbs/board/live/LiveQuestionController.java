package com.sbs.board.live;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.live.dto.LikeResponse;
import com.sbs.board.live.dto.QuestionCreateRequest;
import com.sbs.board.live.dto.QuestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveQuestionController {
    private final LiveQuestionService liveQuestionService;

    @PostMapping("/events/{code}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(
            @PathVariable String code,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QuestionCreateRequest request
            ) {
        return liveQuestionService.create(code, userDetails.getId(),request);

    }

    @PostMapping("/questions/{id}/like")
    public LikeResponse like(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return liveQuestionService.toggleLike(id, userDetails.getId());
    }

    @PreAuthorize("@liveSecurity.canDeleteQuestion(#id, authentication.principal)")
    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        liveQuestionService.delete(id);
    }
}
