package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public abstract class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User addUser(User user) {
        user.getUsername();
        user.getEmail();
        user.getPassword();
        return userRepository.save(user);
    }

}
