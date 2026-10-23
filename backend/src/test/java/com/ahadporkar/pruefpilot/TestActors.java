package com.ahadporkar.pruefpilot;
import java.util.UUID;
import com.ahadporkar.pruefpilot.auth.Actor;
public final class TestActors {
 private TestActors() {}
 public static final UUID ORG=UUID.fromString("00000000-0000-4000-8000-000000000100");
 public static Actor actor(String role) {return new Actor(UUID.randomUUID(),ORG,"test@example.test","Test User",role,"hash",true);}
}
