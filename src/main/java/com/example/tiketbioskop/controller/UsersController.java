package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.model.Users;
import com.example.tiketbioskop.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    // Get All Users
    @GetMapping("/get-all-user")
    public ResponseEntity<List<Users>> getAllUsers() {
        List<Users> users = usersService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // Get User by username
    @GetMapping("/get-user-by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        Users users = usersService.getUserByUsername(username);
        return getMapResponseEntity(users);
    }

    // Get User by email
    @GetMapping("/get-user-by-email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        Users users = usersService.getUserByEmail(email);
        return getMapResponseEntity(users);
    }

    //Map Response Entity for Get User by Username/Email
    private ResponseEntity<Map<String, Object>> getMapResponseEntity(Users users) {
        Map<String, Object> respBody = new HashMap<>();
        respBody.put("ID User", users.getUserId());
        respBody.put("Nama Lengkap", users.getUsername());
        respBody.put("Email", users.getEmail());
        return new ResponseEntity<>(respBody, HttpStatus.FOUND);
    }

    // Add User with Postman
    @PostMapping("/add-user")
    public ResponseEntity<Users> addUser(@RequestBody Users users) {
        usersService.addUser(users);
        return new ResponseEntity<>(users, HttpStatus.CREATED);
    }

    // Update User
    @PutMapping("/update-user")
    public ResponseEntity<Users> updateUser(@RequestBody Users users) {
        usersService.updateUserById(users);
        return new ResponseEntity<>(users, HttpStatus.ACCEPTED);
    }

    // Delete User
    @DeleteMapping("/delete-user/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer userId) {
        usersService.deleteUser(userId);
        return new ResponseEntity<>("Successfully deleted user_id " + userId + "!", HttpStatus.ACCEPTED);
    }

}