package com.siddharth.tradesim_backend.auth.services;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.models.User;
import com.siddharth.tradesim_backend.auth.models.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomizeUserDetailsService implements UserDetailsService {
    private final AuthRepository authRepository;

    @Override
    public @lombok.NonNull UserDetails loadUserByUsername(@NonNull String input) throws UsernameNotFoundException {
        User user = authRepository
                .findByUsernameOrEmail(input)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return new UserPrincipal(user);
    }
}