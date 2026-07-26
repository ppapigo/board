package com.sbs.board.notification;

import com.sbs.board.auth.CustomUserDetails;
import com.sbs.board.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify")
public class NotificationController {
    private final NotificationService notificationService;

    public record UnreadCountResponse (Long unreadCount) {}

    @GetMapping("/list")
    public Page<NotificationResponse> list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC)Pageable pageable
            ){
        return notificationService.getNotifications(userDetails.getId(),pageable);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> read(
            @PathVariable Long id, // Notification id
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){

        notificationService.read(id, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    //읽지 않은 목록 확인하기
    @GetMapping("/unreads")
    public UnreadCountResponse unreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        return new UnreadCountResponse(
                notificationService.unreadCount(userDetails.getId())
        );
    }
}
