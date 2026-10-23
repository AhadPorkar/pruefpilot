package com.ahadporkar.pruefpilot.auth;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static com.ahadporkar.pruefpilot.TestActors.*;
import static org.junit.jupiter.api.Assertions.*;
class ActorTest {
 @Test void principalContainsNoPasswordInPublicView(){var a=actor("ADMIN");assertFalse(a.publicView().containsKey("passwordHash"));assertEquals("ROLE_ADMIN",a.getAuthorities().iterator().next().getAuthority());}
 @Test void roleEnforced(){assertThrows(AccessDeniedException.class,()->actor("VIEWER").require("ADMIN"));assertDoesNotThrow(()->actor("ADMIN").require("ADMIN","SUPERVISOR"));}
}
