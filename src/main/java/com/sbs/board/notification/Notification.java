package com.sbs.board.notification;

import com.sbs.board.global.entity.User;
import com.sbs.board.notification.dto.NotificationResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림을 받는 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id" ,nullable = false)
    private User recipient;

    // 알림을 발생시킨 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id" ,nullable = false)
    private User actor;

    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated()
    private NotificationType notificationType;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "is_read" ,nullable = false)
    private boolean read;

    public void markAsRead(){
        this.read = true;
    }

    public boolean isOwnedBy(Long userId){
        return recipient.getId().equals(userId);
    }

    public static NotificationResponse toResponse(Notification notification){
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getNotificationType());
        response.setActorUsername(notification.getActor().getNickName());
        response.setRead(notification.isRead());
        response.setPostId(notification.getPostId());
        response.setCommentId(notification.getCommentId());
        response.setCreatedAt(notification.getCreatedAt());

        String message = notification.getNotificationType() == NotificationType.REPLY_ON_COMMENT
                ? notification.actor.getNickName() + "님이 댓글에 댓글을 달았습니다"
                : notification.actor.getNickName() + "님이 게시글에 댓글을 달았습니다";
        response.setMessage(message);

        return response;
        }

}
