package com.example.board.user;

import com.example.board.global.entity.User;
import com.example.board.global.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUser(User user);


    Optional<UserProfile> findByUser(User user);
}
