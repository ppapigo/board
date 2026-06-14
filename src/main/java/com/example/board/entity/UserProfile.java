package com.example.board.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //user entity와 1대1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone_number",length = 30)
    private String phoneNumber;

    @Column(name = "birth",length = 100)
    private LocalDate birth;

    @Column(name = "user_name",length = 100)
    private String userName;

    @Column(name = "gender" )
    private int gender;

    @Column(name = "address",length = 100)
    private String address;

    @Column(name="created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at", nullable=false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
