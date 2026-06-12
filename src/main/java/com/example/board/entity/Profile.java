package com.example.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

@Entity
@Table(name = "board")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile {

    @Column(name = "user_name",length = 100, nullable = false)
    private String userName;

    @Column(name = "gender" , nullable = false)
    private boolean gender;

    @Column(name = "birth",length = 100, nullable = false)
    private String birth;

    @Column(name = "phone_number",length = 100, nullable = false)
    private String phoneNumber;

    @Column(name = "address",length = 100, nullable = false)
    private String address;
}
