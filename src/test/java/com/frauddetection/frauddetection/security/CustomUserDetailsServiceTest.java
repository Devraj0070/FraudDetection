package com.frauddetection.frauddetection.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void mapsAdminRoleToRoleAdminAuthority() {
        User user = userWithRole("admin", "ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertEquals("ROLE_ADMIN",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void mapsUserRoleToRoleUserAuthority() {
        User user = userWithRole("alice", "USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void throwsWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing"));
    }

    private User userWithRole(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("encoded");
        user.setRole(role);
        return user;
    }
}
