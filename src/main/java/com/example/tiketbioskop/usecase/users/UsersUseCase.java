package com.example.tiketbioskop.usecase.users;

import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoTickets;
import com.example.tiketbioskop.repository.DaoUsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersUseCase {

    private final DaoUsers daoUsers;

    private final DaoTickets daoTickets;

    private final PasswordEncoder passwordEncoder;

    // GET
    public Page<Users> getAllUsers(Pageable pageable) {
        return daoUsers.findAll(pageable);
    }

    public Users getUserByUsername(String username) {
        log.debug("getUserByUsername [{}]", username);
        return daoUsers.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Username '" + username + "' not found."));
    }

    private Users getUserById(Integer userId) {
        return daoUsers.findById(userId)
                .orElseThrow(() -> new NotFoundException("User id '" + userId + "' not found."));
    }

    // POST
    public Users addUser(UserRequest request) {
        log.debug("addUser [{}]", request.username());
        Users users = new Users();
        users.setRole("USER");
        return save(users, request);
    }

    // PUT
    public Users updateUser(String currentUsername, UserRequest request) {
        return save(getUserByUsername(currentUsername), request);
    }

    // DELETE
    public void deleteUser(Integer userId) {
        Users users = getUserById(userId);
        // tickets are kept as history, so a user who ever booked stays
        if (daoTickets.existsByUserUserId(userId)) {
            throw new ConflictException("User id '" + userId + "' still has tickets, cancel them first.");
        }
        daoUsers.delete(users);
    }

    private Users save(Users users, UserRequest request) {
        // the DB unique constraints still catch races; these checks just give a clearer message
        daoUsers.findByUsername(request.username())
                .filter(other -> !other.getUserId().equals(users.getUserId()))
                .ifPresent(other -> {
                    throw new ConflictException("The '" + request.username() + "' username already exist.");
                });
        daoUsers.findByEmail(request.email())
                .filter(other -> !other.getUserId().equals(users.getUserId()))
                .ifPresent(other -> {
                    throw new ConflictException("The '" + request.email() + "' email already exist.");
                });

        users.setUsername(request.username());
        users.setEmail(request.email());
        users.setPassword(passwordEncoder.encode(request.password()));
        return daoUsers.save(users);
    }
}
