package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    User create(User user);
    User update(Integer id, User user);
    Boolean delete(Integer id);
    List<User> findAll();
    User findById(Integer id);
}
