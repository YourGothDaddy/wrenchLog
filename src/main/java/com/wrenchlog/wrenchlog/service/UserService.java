package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.LoginRequest;
import com.wrenchlog.wrenchlog.dto.LoginResponse;
import com.wrenchlog.wrenchlog.dto.RegisterRequest;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.repository.UserRepository;
import com.wrenchlog.wrenchlog.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void registerNewUser(RegisterRequest request){
        if(userRepository.existsByUsername(request.username())){
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User newUser = new User(
                request.username(),
                request.email(),
                hashedPassword
        );

        userRepository.save(newUser);
    }

    public LoginResponse loginUser(LoginRequest request){
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new SecurityException("Invalid username or password"));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new SecurityException("Invalid username or password.");
        }

        List<String> roles = user.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();

        String token = jwtService.generateToken(user.getId(), user.getUsername(), roles);

        return new LoginResponse(user.getId(), user.getUsername(), user.getEmail(), token);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new SecurityException("User not found"));
    }
}
