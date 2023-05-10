package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Users;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UsersService {
    List<Users> getAllUsers();

    Users getUserByUsername(String username);

    Users getUserByEmail(String email);

    Users addUser(Users users);

    Users updateUserById(Users users);

    String deleteUser(int userId);

}