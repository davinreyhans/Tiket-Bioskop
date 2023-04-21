package com.example.tiketbioskop.service;

import org.springframework.stereotype.Service;

@Service
public interface UserService {
    void addUser(String username, String email, String password);

    void updateUser(String username, String newUsername, String newEmail, String newPassword);

    void deleteUser(Integer userId);

}
