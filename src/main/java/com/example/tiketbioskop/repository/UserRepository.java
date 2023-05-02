package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    public User findByUserId(Integer userId);
    public User findByUsername(String username);
    public User findByEmail(String email);


}
