package com.ahadporkar.pruefpilot.auth;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LoginLimiterTest {
 @Test void twelveAttemptsPerMinutePerAddress(){var limiter=new LoginLimiter(Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"),ZoneOffset.UTC));for(int i=0;i<12;i++)assertTrue(limiter.allow("a"));assertFalse(limiter.allow("a"));assertTrue(limiter.allow("b"));}
 @Test void limiterHasBoundedKeySpace(){var limiter=new LoginLimiter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));for(int i=0;i<10000;i++)assertTrue(limiter.allow("ip-"+i));assertFalse(limiter.allow("overflow"));}
}
