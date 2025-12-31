package com.college.bookmyslot.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private String usn;
    private Long clubId;

}
