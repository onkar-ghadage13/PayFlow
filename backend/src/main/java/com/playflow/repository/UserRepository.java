package com.playflow.repository;
import com.playflow.entity.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User,Long> { Optional<User> findByEmailIgnoreCase(String email); Optional<User> findByPhoneNumber(String phoneNumber); java.util.List<User> findAllByNameIgnoreCase(String name); boolean existsByEmailIgnoreCase(String email); boolean existsByPhoneNumber(String phoneNumber); long countByRole(String role); }
