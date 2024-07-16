package com.example.tiketbioskop.business.users;

import com.example.tiketbioskop.entity.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UsersService usersService;

    // Get All Users
    @GetMapping("/get-all-user")
    public ResponseEntity<List<Users>> getAllUsers() {
        List<Users> userList = usersService.getAllUsers();
        return new ResponseEntity<>(userList, HttpStatus.OK);
    }

    // Get User by username
    @GetMapping("/get-user-by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        Map<String, Object> rslt = usersService.getUserByUsername(username);
        return new ResponseEntity<>(rslt, HttpStatus.OK);
    }

    // Get User by email
    @GetMapping("/get-user-by-email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        Map<String, Object> rslt = usersService.getUserByEmail(email);
        return new ResponseEntity<>(rslt, HttpStatus.OK);
    }

    // Add User with Postman
    @PostMapping("/add-user")
    public ResponseEntity<Map<String, Object>> addUser(@RequestBody Users users) {
        Map<String, Object> rslt = usersService.addUser(users);
        return new ResponseEntity<>(rslt, HttpStatus.CREATED);
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
        String username = usersService.deleteUser(userId);
        return new ResponseEntity<>("Successfully deleted user " + username + "!", HttpStatus.ACCEPTED);
    }

}