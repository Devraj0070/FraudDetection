package com.frauddetection.frauddetection.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.frauddetection.frauddetection.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}