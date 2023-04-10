package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public abstract class UserServiceImpl implements UserService{

    @Autowired
    UserRepository userRepository;

    @Override
    public User create(User user) {
        userRepository.save(user);
        return null;
    }

    @Override
    public User update(Integer id, User user) {
        return null;
    }

    @Override
    public Boolean delete(Integer id) {
        return null;
    }

    @Override
    public User findById(Integer id) {
        Optional<User> result = userRepository.findById(id);
        if (result.isPresent()) {
            return result.get();
        }
        return null;
    }
}
