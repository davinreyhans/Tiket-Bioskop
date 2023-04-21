package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void addUser(String username, String email, String password) {
        User user = new User();
        // Insert Data
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        userRepository.save(user);
    }

    @Override
    public void updateUser(String username, String newUsername, String newEmail, String newPassword) {
        User user = userRepository.findByUsername(username);
        user.setUsername(newUsername);
        user.setEmail(newEmail);
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }


}
