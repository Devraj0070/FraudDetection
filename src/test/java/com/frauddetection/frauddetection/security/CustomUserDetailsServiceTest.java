package com.frauddetection.frauddetection.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Maps 'ADMIN' role to ROLE_ADMIN authority")
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
    @DisplayName("Maps 'ROLE_ADMIN' role to ROLE_ADMIN authority without duplicate prefix")
    void mapsRoleAdminWithPrefixToRoleAdminAuthority() {
        User user = userWithRole("admin2", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin2")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin2");

        assertEquals("ROLE_ADMIN",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Maps 'USER' role to ROLE_USER authority")
    void mapsUserRoleToRoleUserAuthority() {
        User user = userWithRole("alice", "USER");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Maps 'ROLE_USER' role to ROLE_USER authority without duplicate prefix")
    void mapsRoleUserWithPrefixToRoleUserAuthority() {
        User user = userWithRole("bob", "ROLE_USER");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bob");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Safely handles null role by defaulting to ROLE_USER without throwing or creating ROLE_null")
    void mapsNullRoleSafelyToDefaultRoleUser() {
        User user = userWithRole("charlie", null);
        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("charlie");

        String authority = details.getAuthorities().iterator().next().getAuthority();
        assertEquals("ROLE_USER", authority);
        assertFalse(authority.contains("null"), "Authority must never contain 'null'");
    }

    @Test
    @DisplayName("Safely handles empty string role by defaulting to ROLE_USER")
    void mapsEmptyRoleSafelyToDefaultRoleUser() {
        User user = userWithRole("dan", "");
        when(userRepository.findByUsername("dan")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("dan");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Safely handles whitespace-only role by defaulting to ROLE_USER")
    void mapsWhitespaceRoleSafelyToDefaultRoleUser() {
        User user = userWithRole("eva", "   ");
        when(userRepository.findByUsername("eva")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("eva");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Safely handles malformed 'ROLE_' role by defaulting to ROLE_USER")
    void mapsMalformedRolePrefixOnlySafelyToDefaultRoleUser() {
        User user = userWithRole("frank", "ROLE_");
        when(userRepository.findByUsername("frank")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("frank");

        assertEquals("ROLE_USER",
                details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("Normalizes duplicate ROLE_ prefixes (e.g. ROLE_ROLE_ADMIN) to single ROLE_ADMIN")
    void normalizesDuplicateRolePrefixes() {
        User user = userWithRole("grace", "ROLE_ROLE_ADMIN");
        when(userRepository.findByUsername("grace")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("grace");

        assertEquals("ROLE_ADMIN",
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
