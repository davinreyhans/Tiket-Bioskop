package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.dto.UserDTO;
import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")    //untuk custom path API nya
public class UserController {

//    bikin requirement CRUD
    private UserService userService;

    @PostMapping("/create")
    public User create(@RequestBody UserDTO request){
        return null;
    }

    @PutMapping("/update/{id}")
    public User update(@PathVariable Integer id, @RequestBody UserDTO request){
        return null;
    }

    @GetMapping("/all")
    public List<User> findAll(){
        return null;
    }

    @DeleteMapping("/delete/{id}")
    public Boolean delete(@PathVariable Integer id){
        return null;
    }
}
