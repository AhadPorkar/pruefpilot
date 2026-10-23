package com.ahadporkar.pruefpilot.shared;
import java.util.*;import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DbTest {
 @Test void typedMapHelpers(){var id=UUID.randomUUID();var m=Map.<String,Object>of("id",id.toString(),"version",3,"active",true);assertEquals(id,Db.uuid(m,"id"));assertEquals(3,Db.number(m,"version"));assertTrue(Db.bool(m,"active"));assertEquals("",Db.str(m,"missing"));assertNull(Db.uuid(m,"missing"));}
 @Test void notFoundIsSafe(){var e=ApiException.missing();assertEquals(404,e.status);assertFalse(e.getMessage().contains("SELECT"));}
}
