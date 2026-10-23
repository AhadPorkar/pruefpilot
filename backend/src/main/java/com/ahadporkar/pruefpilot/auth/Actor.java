package com.ahadporkar.pruefpilot.auth;

import java.util.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;

/** Organization and role come from authenticated server state, never from request fields. */
public record Actor(UUID id, UUID organizationId, String email, String displayName, String role,
                    String passwordHash, boolean active) implements UserDetails {
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_"+role)); }
    @Override public String getPassword() {return passwordHash;}
    @Override public String getUsername() {return email;}
    @Override public boolean isEnabled() {return active;}
    public void require(String... roles) {if(!Arrays.asList(roles).contains(role)) throw new AccessDeniedException("Role not allowed");}
    public Map<String,Object> publicView() {return Map.of("id",id,"organizationId",organizationId,"email",email,"displayName",displayName,"role",role);}
}
