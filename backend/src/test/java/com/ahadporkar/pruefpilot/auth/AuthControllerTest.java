package com.ahadporkar.pruefpilot.auth;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import static org.junit.jupiter.api.Assertions.*;
import static com.ahadporkar.pruefpilot.TestActors.*;
class AuthControllerTest {
 @Test void csrfContract(){var result=new AuthController().csrf(new DefaultCsrfToken("X-CSRF-TOKEN","_csrf","masked-token"));assertEquals("X-CSRF-TOKEN",result.get("headerName"));assertEquals("masked-token",result.get("token"));}
 @Test void principalContract(){assertEquals("VIEWER",new AuthController().me(actor("VIEWER")).get("role"));}
}
