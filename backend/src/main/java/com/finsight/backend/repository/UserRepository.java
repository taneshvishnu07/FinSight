package com.finsight.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.User;

/**
 * Data access for User. Emails are stored lower-cased (see AuthServiceImpl), so look-
 * ups should pass a normalised email.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}