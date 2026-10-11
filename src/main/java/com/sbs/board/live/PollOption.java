package com.sbs.board.live;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;


@Builder
@Entity
@Table(name = "live_poll_options")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 50)
    private String text;

    @Column(nullable = false)
    private int sortOrder;

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at")
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    public PollOption(Poll poll, String text, int sortOrder) {
        this.poll=poll;
        this.text=text;
        this.sortOrder=sortOrder;
    }
}
