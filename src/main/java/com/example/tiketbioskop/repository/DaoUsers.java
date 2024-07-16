package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface DaoUsers extends JpaRepository<Users, Integer> {
    List<Users> findAllByOrderByUserId();

    Users findByUserId(Integer userId);

    Users findByUsername(String username);

    Users findByEmail(String email);
}
