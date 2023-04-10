package com.example.tiketbioskop.dto;

/* untuk mengirim data antara layer atau komponen aplikasi yang berbeda,
misalnya antara layer presentasi dan layer bisnis
atau antara layer bisnis dan layer data.*/
public record UserDTO(Integer id, String username, String password, String email) {

}
