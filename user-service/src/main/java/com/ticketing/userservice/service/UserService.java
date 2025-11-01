package com.ticketing.userservice.service;

import com.ticketing.userservice.dto.LoginDTO;
import com.ticketing.userservice.dto.UserDTO;
import com.ticketing.userservice.dto.UserRegistrationDTO;
import com.ticketing.userservice.exception.ResourceNotFoundException;
import com.ticketing.userservice.exception.UserAlreadyExistsException;
import com.ticketing.userservice.model.User;
import com.ticketing.userservice.model.UserStatus;
import com.ticketing.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) {
        log.info("Registering new user: {}", registrationDTO.getUsername());
        
        if (userRepository.existsByUsername(registrationDTO.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + registrationDTO.getUsername());
        }
        
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + registrationDTO.getEmail());
        }
        
        User user = User.builder()
                .username(registrationDTO.getUsername())
                .email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .firstName(registrationDTO.getFirstName())
                .lastName(registrationDTO.getLastName())
                .phoneNumber(registrationDTO.getPhoneNumber())
                .address(registrationDTO.getAddress())
                .city(registrationDTO.getCity())
                .state(registrationDTO.getState())
                .zipCode(registrationDTO.getZipCode())
                .country(registrationDTO.getCountry())
                .status(UserStatus.ACTIVE)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        
        return convertToDTO(savedUser);
    }
    
    public UserDTO authenticateUser(LoginDTO loginDTO) {
        log.info("Authenticating user: {}", loginDTO.getUsernameOrEmail());
        
        User user = userRepository.findByUsernameOrEmail(
                loginDTO.getUsernameOrEmail(),
                loginDTO.getUsernameOrEmail()
        ).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("User account is not active");
        }
        
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User authenticated successfully: {}", user.getId());
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        log.info("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> getActiveUsers() {
        log.info("Fetching active users");
        return userRepository.findByStatus(UserStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String searchTerm) {
        log.info("Searching users with term: {}", searchTerm);
        return userRepository.searchActiveUsers(searchTerm).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists: " + userDTO.getEmail());
            }
            user.setEmail(userDTO.getEmail());
        }
        
        if (userDTO.getFirstName() != null) user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null) user.setLastName(userDTO.getLastName());
        if (userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if (userDTO.getAddress() != null) user.setAddress(userDTO.getAddress());
        if (userDTO.getCity() != null) user.setCity(userDTO.getCity());
        if (userDTO.getState() != null) user.setState(userDTO.getState());
        if (userDTO.getZipCode() != null) user.setZipCode(userDTO.getZipCode());
        if (userDTO.getCountry() != null) user.setCountry(userDTO.getCountry());
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getId());
        
        return convertToDTO(updatedUser);
    }
    
    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.info("User deleted successfully: {}", id);
    }
    
    public void suspendUser(Long id) {
        log.info("Suspending user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("User suspended successfully: {}", id);
    }
    
    public void activateUser(Long id) {
        log.info("Activating user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User activated successfully: {}", id);
    }
    
    private UserDTO convertToDTO(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
}
