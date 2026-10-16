package com.sbs.board.live;

import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "live_polls", indexes = @Index(name = "idx_live_polls_event", columnList = "event_id"))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "event_id",nullable = false)
    private LiveEvent event;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private LiveStatus status = LiveStatus.OPEN;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL,orphanRemoval = true)
    @OrderBy("sortOrder Desc")
    @Builder.Default
    private List<PollOption> options = new ArrayList<>();

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at")
    @LastModifiedDate
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Poll(LiveEvent event,String title, List<String> optionTexts){
        this();
        this.event =event;
        this.title= title;
        for(int i=0; i<optionTexts.size();i++){
            options.add(new PollOption(this, optionTexts.get(i),i));
        }
    }

    public boolean isOpen(){
        return status== LiveStatus.OPEN;
    }

    public void close(){
        this.status = LiveStatus.CLOSED;
    }

    public void assertOpen(){
        if(!isOpen()){
            throw new BusinessException(ErrorCode.POLL_CLOSED);
        }
    }
    public boolean hasOption(Long optionId){
        return options.stream().anyMatch(option ->option.getId().equals(optionId));
    }

}
