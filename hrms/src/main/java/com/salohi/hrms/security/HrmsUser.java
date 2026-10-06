package com.salohi.hrms.security;

import com.salohi.hrms.domain.Role;

import java.io.Serializable;

public record HrmsUser(
        Long id,
        String email,
        String password,
        String fullName,
        Role role,
        boolean active
) implements org.springframework.security.core.userdetails.UserDetails, Serializable {

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean admin() {
        return role == Role.ADMIN;
    }
}
