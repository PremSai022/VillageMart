package com.villagemart.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.username) = lower(:login) or u.mobile = :login")
    Optional<User> findByLogin(String login);

    Optional<User> findByUsernameIgnoreCase(String username);
}
