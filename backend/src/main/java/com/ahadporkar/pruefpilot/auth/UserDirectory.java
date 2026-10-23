package com.ahadporkar.pruefpilot.auth;

import java.util.Locale;
import com.ahadporkar.pruefpilot.shared.Db;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import static com.ahadporkar.pruefpilot.shared.Db.*;

@Service
public class UserDirectory implements UserDetailsService {
    private final Db db;
    public UserDirectory(Db db) {this.db=db;}
    @Override public Actor loadUserByUsername(String username) {
        var row=db.optional("SELECT id,organization_id,email,display_name,role,password_hash,active FROM app_user WHERE email=?",
            username.strip().toLowerCase(Locale.ROOT)).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
        return new Actor(uuid(row,"id"),uuid(row,"organizationId"),str(row,"email"),str(row,"displayName"),str(row,"role"),str(row,"passwordHash"),bool(row,"active"));
    }
}
