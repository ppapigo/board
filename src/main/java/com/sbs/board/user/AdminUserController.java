package com.sbs.board.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    @PutMapping("/{userId}/admin")
    public ResponseEntity<Void> grantAdmin(@PathVariable Long userId) {
        adminUserService.grantAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}
