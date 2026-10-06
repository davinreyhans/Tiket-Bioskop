package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.usecase.users.UserRequest;
import com.example.tiketbioskop.usecase.users.UsersUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {
    private final UsersUseCase usersUseCase;

    // Register (public)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Users register(@Valid @RequestBody UserRequest request) {
        return usersUseCase.addUser(request);
    }

    // Logged-in user's own account
    @GetMapping("/me")
    public Users getMe(Principal principal) {
        return usersUseCase.getUserByUsername(principal.getName());
    }

    @PutMapping("/me")
    public Users updateMe(Principal principal, @Valid @RequestBody UserRequest request) {
        return usersUseCase.updateUser(principal.getName(), request);
    }

    // Admin only
    @GetMapping
    public Page<Users> getAllUsers(@SortDefault(sort = "userId") Pageable pageable) {
        return usersUseCase.getAllUsers(pageable);
    }

    @GetMapping("/{username}")
    public Users getUserByUsername(@PathVariable String username) {
        return usersUseCase.getUserByUsername(username);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer userId) {
        usersUseCase.deleteUser(userId);
    }
}
