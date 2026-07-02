package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.RegisterRequest;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
