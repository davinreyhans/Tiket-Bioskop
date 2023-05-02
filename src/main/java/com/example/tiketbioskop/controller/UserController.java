package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.model.User;
import com.example.tiketbioskop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")    //untuk custom path API nya
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "This API method function to read existing user information by inputting the username.")
    // Get User by username
    @GetMapping("/get-username/{username}")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@Schema(example = "Fill in name") @PathVariable String username) {
        User users = userService.getUserByUsername(username);

        return getMapResponseEntity(users);
    }

    // Get User by email
    @Operation(summary = "This API method function to read existing user information by inputting the email.")
    @GetMapping("/get-email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@Schema(example = "Fill in email")@PathVariable String email) {
        User user = userService.getUserByEmail(email);

        return getMapResponseEntity(user);
    }

    private ResponseEntity<Map<String, Object>> getMapResponseEntity(User user) {
        Map<String, Object> respBody = new HashMap<>();
        respBody.put("ID User", user.getUserId());
        respBody.put("Nama Lengkap", user.getUsername());
        respBody.put("Email", user.getEmail());
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
    // endpoint: http://localhost:8080/user/add-user
    @PostMapping("/add-user")
    public ResponseEntity<User> addUser(@Schema(example = "{" +
            "\"userId\":\"1\","+
            "\"username\":\"Davin\","+
            "\"password\":\"passDavin\"," +
            "\"email\":\"davin@email.com\"}")@RequestBody User user) {
        userService.addUser(user);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
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
    // endpoint: http://localhost:8080/user/update-user
    @PutMapping("/update-user")
    public ResponseEntity<User> updateUser(@Schema(example = "{" +
            "\"userId\":\"1\","+
            "\"username\":\"Davin\","+
            "\"password\":\"passDavin\"," +
            "\"email\":\"ivan@email.com\"}") @RequestBody User user) {
        userService.updateUserById(user);
        return new ResponseEntity<>(user, HttpStatus.ACCEPTED);
    }

    // Delete User
    @Operation(summary = "This API method function to delete the user you want by inputting the user id.")
    @DeleteMapping("/delete-user/{userId}")
    public ResponseEntity<Integer> deleteUser(@Schema(example = "Fill in user id") @PathVariable Integer userId) {
        userService.deleteUser(userId);
        return new ResponseEntity<>(userId, HttpStatus.ACCEPTED);
    }

}
