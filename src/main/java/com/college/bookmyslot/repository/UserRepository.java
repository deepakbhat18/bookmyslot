package com.college.bookmyslot.repository;

import com.college.bookmyslot.model.Club;
import com.college.bookmyslot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByClub(Club club);


    boolean existsByEmail(String email);

    boolean existsByUsn(String usn);

    long countByRole(User.Role role);

    Optional<Object> findByUsn(String usn);
}
