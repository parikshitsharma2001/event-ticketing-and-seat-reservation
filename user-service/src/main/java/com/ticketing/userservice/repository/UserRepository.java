package com.ticketing.userservice.repository;

import com.ticketing.userservice.model.User;
import com.ticketing.userservice.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByCity(String city);
    
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> searchActiveUsers(String searchTerm);
}
