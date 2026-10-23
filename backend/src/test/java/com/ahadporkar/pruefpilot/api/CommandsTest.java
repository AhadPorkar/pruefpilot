package com.ahadporkar.pruefpilot.api;
import java.util.*;import java.math.BigDecimal;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommandsTest {
 @Test void requestContractsRejectMissingOrInvalidFields(){try(var factory=Validation.buildDefaultValidatorFactory()){var v=factory.getValidator();assertFalse(v.validate(new Commands.Site("","?","Address")).isEmpty());assertFalse(v.validate(new Commands.Schedule(null,null,null,0,null,"")).isEmpty());assertTrue(v.validate(new Commands.Site("Berlin workshop","BER","Berlin")).isEmpty());}}
 @Test void measurementsHaveBoundedPrecision(){try(var f=Validation.buildDefaultValidatorFactory()){assertFalse(f.getValidator().validate(new Commands.Item("Pressure","Druck","NUMBER",true,new BigDecimal("0.1234567"),null,"bar")).isEmpty());}}
}
