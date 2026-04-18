package com.siddharth.tradesim_backend.dev_only;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.user.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevOnlyService {
    private final AuthRepository authRepository;

    public String makeAdmin(UUID userId) {
        User user = authRepository.findById(userId).orElseThrow(() -> UserException.notFound("User not found"));
        user.setRole(Role.ADMIN);
        authRepository.save(user);
        return "Made USER as ADMIN";
    }

    public List<User> fetchUsers() {
        return authRepository.findAll();
    }
}