package com.college.bookmyslot.dto;

public class RegisterRequest {

    private String name;
    private String email;
    private String usn;
    private String password;
    private String role; // STUDENT / TEACHER / ADMIN

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getUsn() { return usn; }

    public void setUsn(String usn) { this.usn = usn; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }
}
