package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.model.Users;
import com.example.tiketbioskop.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")    //untuk custom path API nya
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @Operation(summary = "This API method function to read existing user information by inputting the username.")

    // Get All User by username
    @GetMapping("/get-users")
    public ResponseEntity<List<Users>> getAllUsers() {
        List<Users> users = usersService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // Get User by username
    @GetMapping("/get-user-by-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@Schema(example = "Fill in name") @PathVariable String username) {
        Users users = usersService.getUserByUsername(username);

        return getMapResponseEntity(users);
    }

    // Get User by email
    @Operation(summary = "This API method function to read existing user information by inputting the email.")
    @GetMapping("/get-user-by-email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@Schema(example = "Fill in email")@PathVariable String email) {
        Users users = usersService.getUserByEmail(email);

        return getMapResponseEntity(users);
    }

    private ResponseEntity<Map<String, Object>> getMapResponseEntity(Users users) {
        Map<String, Object> respBody = new HashMap<>();
        respBody.put("ID User", users.getUserId());
        respBody.put("Nama Lengkap", users.getUsername());
        respBody.put("Email", users.getEmail());

        return new ResponseEntity<>(respBody, HttpStatus.FOUND);
    }

    // Custom response to add user
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully added a new user!",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(example = "{" +
                                    "\"userId\":\"1\","+
                                    "\"username\":\"Davin\","+
                                    "\"password\":\"passDavin\"," +
                                    "\"email\":\"davin@email.com\"}"))
                    }),
            @ApiResponse(responseCode = "400", description = "Failed to add new user.",
                    content = { @Content})})
    @Operation(summary = "This API method function to add a user by inputting a username, email, password and will be stored in the Users entity.")

    // Add User with postman
    @PostMapping("/add-user")
    public ResponseEntity<Users> addUser(@Schema(example = "{" +
            "\"userId\":\"1\","+
            "\"username\":\"Davin\","+
            "\"password\":\"passDavin\"," +
            "\"email\":\"davin@email.com\"}")@RequestBody Users users) {
        usersService.addUser(users);
        return new ResponseEntity<>(users, HttpStatus.CREATED);
    }

    // Custom response to update user
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully update user!",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(example = "{" +
                                    "\"userId\":\"1\","+
                                    "\"username\":\"Davin\","+
                                    "\"password\":\"passDavin\"," +
                                    "\"email\":\"davin@email.com\"}"))
                    }),
            @ApiResponse(responseCode = "400", description = "Failed to update user.",
                    content = { @Content})})
    @Operation(summary = "This API method function to update the user that has been inputted by re-entering the userId, username, password, and email that you want to update.")

    // Update User
    @PutMapping("/update-user")
    public ResponseEntity<Users> updateUser(@Schema(example = "{" +
            "\"userId\":\"1\","+
            "\"username\":\"Davin\","+
            "\"password\":\"passDavin\"," +
            "\"email\":\"ivan@email.com\"}") @RequestBody Users users) {
        usersService.updateUserById(users);
        return new ResponseEntity<>(users, HttpStatus.ACCEPTED);
    }

    // Custom response to delete user
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted the user!"),
            @ApiResponse(responseCode = "404", description = "Failed to delete the user. User not found.",
                    content = { @Content})})

    // Delete User
    @Operation(summary = "This API method function to delete the user you want by inputting the user id.")
    @DeleteMapping("/delete-user/{userId}")
    public ResponseEntity<String> deleteUser(@Schema(example = "Fill in user id") @PathVariable Integer userId) {
        usersService.deleteUser(userId);
        return new ResponseEntity<>("Successfully deleted user " + userId + "!", HttpStatus.ACCEPTED);
    }
}