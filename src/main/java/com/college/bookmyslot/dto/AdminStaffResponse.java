package com.college.bookmyslot.dto;
import lombok.Data;
@Data
public class AdminStaffResponse {

    private Long id;
    private String name;
    private String email;
    private String clubName;
    private boolean active;

    public AdminStaffResponse(
            Long id,
            String name,
            String email,
            String clubName,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.clubName = clubName;
        this.active = active;
    }

    // getters
}

