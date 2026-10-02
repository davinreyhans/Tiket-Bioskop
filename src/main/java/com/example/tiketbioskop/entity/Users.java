package com.example.tiketbioskop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity(name = "Users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @JsonIgnore // BCrypt hash, never sent back to the client
    @Column(name = "password")
    private String password;

    // "USER" or "ADMIN"; admins are promoted by hand in the DB (see README)
    @Column(name = "role")
    private String role;
}
