package com.ticketing.userservice.controller;

import com.ticketing.userservice.dto.*;
import com.ticketing.userservice.security.JwtTokenProvider;
import com.ticketing.userservice.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {
    
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MeterRegistry meterRegistry;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        log.info("Received registration request for username: {}", registrationDTO.getUsername());
        
        UserDTO userDTO = userService.registerUser(registrationDTO);
        String token = jwtTokenProvider.generateToken(userDTO.getUsername());
        
        Counter.builder("user_registrations_total")
                .description("Total number of user registrations")
                .register(meterRegistry)
                .increment();
        
        AuthResponseDTO response = new AuthResponseDTO(token, userDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> loginUser(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("Received login request for: {}", loginDTO.getUsernameOrEmail());
        
        UserDTO userDTO = userService.authenticateUser(loginDTO);
        String token = jwtTokenProvider.generateToken(userDTO.getUsername());
        
        Counter.builder("user_logins_total")
                .description("Total number of user logins")
                .register(meterRegistry)
                .increment();
        
        AuthResponseDTO response = new AuthResponseDTO(token, userDTO);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Fetching user with ID: {}", id);
        UserDTO userDTO = userService.getUserById(id);
        return ResponseEntity.ok(userDTO);
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user with username: {}", username);
        UserDTO userDTO = userService.getUserByUsername(username);
        return ResponseEntity.ok(userDTO);
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        log.info("Fetching user with email: {}", email);
        UserDTO userDTO = userService.getUserByEmail(email);
        return ResponseEntity.ok(userDTO);
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        log.info("Fetching users - search: {}, activeOnly: {}", search, activeOnly);
        
        List<UserDTO> users;
        if (search != null && !search.isEmpty()) {
            users = userService.searchUsers(search);
        } else if (activeOnly) {
            users = userService.getActiveUsers();
        } else {
            users = userService.getAllUsers();
        }
        
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        log.info("Updating user with ID: {}", id);
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable Long id) {
        log.info("Suspending user with ID: {}", id);
        userService.suspendUser(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        log.info("Activating user with ID: {}", id);
        userService.activateUser(id);
        return ResponseEntity.ok().build();
    }
}
