package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")    //untuk custom path API nya
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Add User with unit-test
//    public String addUser(String username, String email, String password) {
//        userService.addUser(username, email, password);
//        return "Add User Success!";
//    }

    // Add User with postman
    @PostMapping("/addUser")
    public String addUser(@RequestBody User user) {
        userService.addUser(user.getUsername(), user.getEmail(), user.getPassword());
        return "Add User Success!";
    }

    // Update User
    @PutMapping("/update/{userId}")
    public String updateUser(@PathVariable Integer userId, @RequestBody User user) {
        userService.updateUser(String.valueOf(userId), user.getUsername(), user.getEmail(), user.getPassword());
        return "Update User Success!";
    }

    // Delete User
    @DeleteMapping("/delete/{userId}")
    public void deleteUser(@PathVariable Integer userId) {
        userService.deleteUser(userId);
    }


}
