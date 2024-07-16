package com.example.tiketbioskop.business.users;

import com.example.tiketbioskop.entity.Users;
import com.example.tiketbioskop.repository.DaoUsers;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UsersServiceImpl implements UsersService {

    @Autowired
    private DaoUsers daoUsers;

    // GET
    @Override
    public List<Users> getAllUsers() {
        return daoUsers.findAllByOrderByUserId();
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        log.debug("getUserByUsername [{}]", username);

        Users users = daoUsers.findByUsername(username);

        Map<String, Object> rslt = new HashMap<>();
        rslt.put("ID User", users.getUserId());
        rslt.put("Username", users.getUsername());
        rslt.put("Email", users.getEmail());

        return rslt;
    }

    @Override
    public Map<String, Object> getUserByEmail(String email) {
        log.debug("getUserByEmail [{}]", email);

        Users users = daoUsers.findByEmail(email);

        Map<String, Object> rslt = new HashMap<>();
        rslt.put("ID User", users.getUserId());
        rslt.put("Username", users.getUsername());
        rslt.put("Email", users.getEmail());

        return rslt;
    }

    // POST
    @Override
    public Map<String, Object> addUser(Users users) throws Exception {
        log.debug("addUser [{}]", users);

        Map<String, Object> rslt = new HashMap<>();

        if (daoUsers.existsByUsername(users.getUsername())) {
            rslt.put("errorMessage", "The '" + users.getUsername() + "' username already exist.");
            return rslt;
            // throw new Exception("The \"" + users.getUsername() + "\" username already
            // exist.");
        }

        if (daoUsers.existsByEmail(users.getEmail())) {
            rslt.put("errorMessage", "The '" + users.getEmail() + "' email already exist.");
            return rslt;
            // throw new Exception("The \"" + users.getEmail() + "\" email already exist.");
        }

        daoUsers.save(users);

        Users user = daoUsers.findByEmail(users.getEmail());

        rslt.put("message", "Succesfully added new user!");
        rslt.put("user", user);

        return rslt;
    }

    // PUT
    @Override
    public void updateUserById(Users users) {
        Users updateUser = daoUsers.findByUserId(users.getUserId());
        updateUser.setUsername(users.getUsername());
        updateUser.setEmail(users.getEmail());
        updateUser.setPassword(users.getPassword());
        daoUsers.save(updateUser);
    }

    // DELETE
    @Override
    public String deleteUser(int userId) {
        Users users = daoUsers.findByUserId(userId);

        String username = users.getUsername();

        daoUsers.deleteById(userId);

        return username;
    }
}