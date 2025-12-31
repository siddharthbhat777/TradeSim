package com.siddharth.tradesim_backend.dev_only;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevOnlyService {
    private final AuthRepository authRepository;

    public String makeAdmin(UUID userId) {
        User user =  authRepository.findById(userId).orElse(null);
        if(user == null) throw new RuntimeException("User not found");
        user.setRole(Role.ADMIN);
        authRepository.save(user);
        return "Made USER as ADMIN";
    }

    public List<User> fetchUsers() {
        return authRepository.findAll();
    }
}