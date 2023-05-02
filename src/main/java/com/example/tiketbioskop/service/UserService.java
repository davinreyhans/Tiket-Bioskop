package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    User getUserByUsername(String username);

    User getUserByEmail(String email);

    User addUser(User user);

    User updateUserById(User user);

    String deleteUser(Integer userId);


}
