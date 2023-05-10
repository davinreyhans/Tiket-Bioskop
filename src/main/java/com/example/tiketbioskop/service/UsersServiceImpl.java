package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Users;
import com.example.tiketbioskop.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;

    public UsersServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    //GET
    @Override
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    @Override
    public Users getUserByUsername(String username) {
        return usersRepository.findByUsername(username);
    }

    @Override
    public Users getUserByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    //POST
    @Override
    public Users addUser(Users users) {
        return usersRepository.save(users);
    }

    //PUT
    @Override
    public Users updateUserById(Users users) {
        Users updateUsers = usersRepository.findByUserId(users.getUserId());
        updateUsers.setUsername(users.getUsername());
        updateUsers.setEmail(users.getEmail());
        updateUsers.setPassword(users.getPassword());
        return usersRepository.save(updateUsers);
    }

    //DELETE
    @Override
    public String deleteUser(int userId) {
        usersRepository.deleteById(userId);
        return "Delete user id '" + userId + "' has been successful!";
    }

}