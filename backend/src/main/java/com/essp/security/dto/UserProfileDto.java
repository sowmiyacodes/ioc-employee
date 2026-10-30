package com.essp.security.dto;

public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private boolean active;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String username, String email, String role, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
