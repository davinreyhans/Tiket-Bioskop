package com.example.tiketbioskop.business.users;

import com.example.tiketbioskop.entity.Users;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UsersService {

    List<Users> getAllUsers();

    Map<String, Object> getUserByUsername(String username);

    Map<String, Object> getUserByEmail(String email);

    Map<String, Object> addUser(Users users) throws Exception;

    void updateUserById(Users users);

    String deleteUser(int userId);

}