package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User addUser(User user);

}
