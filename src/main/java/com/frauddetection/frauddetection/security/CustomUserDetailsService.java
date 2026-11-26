package com.frauddetection.frauddetection.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    public static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        String authority = normalizeAuthority(user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    /**
     * Safely normalizes user roles into Spring Security authorities.
     * - Null, empty, or whitespace roles safely default to ROLE_USER.
     * - Existing "ROLE_" prefixes are trimmed to avoid duplicate "ROLE_ROLE_".
     * - Malformed roles safely default to ROLE_USER without granting administrative privileges.
     */
    public static String normalizeAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_" + DEFAULT_ROLE;
        }

        String trimmed = role.trim().toUpperCase();
        while (trimmed.startsWith("ROLE_")) {
            trimmed = trimmed.substring(5).trim();
        }

        if (trimmed.isEmpty()) {
            return "ROLE_" + DEFAULT_ROLE;
        }

        return "ROLE_" + trimmed;
    }
}
