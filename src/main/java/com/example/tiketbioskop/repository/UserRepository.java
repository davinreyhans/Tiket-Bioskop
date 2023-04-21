package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    //UPDATE User by username
    User findByUsername(String username);

}
