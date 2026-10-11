package com.sbs.board.live;

import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "live_events",indexes = @Index(name = "idx_live_event_host", columnList = "host_id"))
@Getter
@Builder
@AllArgsConstructor
@Setter
public class LiveEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, unique = true,length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false,length = 10)
    private LiveStatus status;

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at")
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public LiveEvent(User host, String title, String code){
        this.host=host;
        this.title=title;
        this.code=code;
    }
    public boolean isOpen(){
        return status == LiveStatus.OPEN;
    }
    public boolean isHostedBy(Long userId){
        return Objects.equals(host.getId(),userId);
    }
    public void close(){
        this.status=LiveStatus.CLOSED;
    }
    public void assertOpen(){
        if(!isOpen()){
            throw new BusinessException(ErrorCode.LIVE_EVENT_CLOSED);
        }
    }


}
