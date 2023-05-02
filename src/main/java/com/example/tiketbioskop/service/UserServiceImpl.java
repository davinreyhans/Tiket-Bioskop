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
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User addUser(User user) {
        user.getUsername();
        user.getEmail();
        user.getPassword();
        return userRepository.save(user);
    }

    @Override
    public User updateUserById(User user) {
        user.getUsername();
        User updateUser = userRepository.findById(user.getUserId()).get();
        updateUser.setUsername(user.getUsername());
        updateUser.setEmail(user.getEmail());
        updateUser.setPassword(user.getPassword());
        return userRepository.save(updateUser);
    }


    @Override
    public String deleteUser(Integer userId) {
        userRepository.deleteById(userId);
        return "Delete user id " + userId + " has been successful!";
    }

}
