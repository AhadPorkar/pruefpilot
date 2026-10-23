import com.ahadporkar.pruefpilot.domain.*;
import com.ahadporkar.pruefpilot.auth.LoginLimiter;
import java.time.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
public final class DomainSmoke {
 static int count=0;
 static void ok(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
 static void rejected(String code,Runnable fn){try{fn.run();throw new AssertionError("Expected "+code);}catch(RuleViolation e){ok(code.equals(e.code()),"reject "+code);}}
 public static void main(String[] args){
  var check=new InspectionRules.Item("PASS_FAIL",true,null,null);
  ok("PASS".equals(InspectionRules.outcome(check,"PASS","")),"pass check");
  for(String value:new String[]{"FAIL","NA"}){rejected("noteRequired",()->InspectionRules.outcome(check,value,""));ok(value.equals(InspectionRules.outcome(check,value,"Documented reason")),"explained "+value);}
  rejected("answerRequired",()->InspectionRules.outcome(check,"",null));
  rejected("validation",()->InspectionRules.outcome(check,"bad",""));
  ok("NA".equals(InspectionRules.outcome(new InspectionRules.Item("TEXT",false,null,null),"",null)),"optional blank");
  var numeric=new InspectionRules.Item("NUMBER",true,new BigDecimal("2"),new BigDecimal("8"));
  for(String n:new String[]{"2","8","5.123456"})ok("PASS".equals(InspectionRules.outcome(numeric,n,"")),"numeric "+n);
  ok("FAIL".equals(InspectionRules.outcome(numeric,"1.99","Below minimum")),"range failure");
  for(String n:new String[]{"NaN","Infinity","1,2","1000000000000","0.1234567"})rejected("invalidNumber",()->InspectionRules.outcome(numeric,n,""));
  rejected("independentReview",()->InspectionRules.assertIndependent("a","a"));
  InspectionRules.assertIndependent("a","b");ok(true,"independent reviewer accepted");
  rejected("conflict",()->InspectionRules.requireVersion(2,1));rejected("invalidTransition",()->InspectionRules.requireState("CLOSED","OPEN"));
  for(int days:new int[]{-1,0,3651})rejected("invalidInterval",()->ScheduleRules.validate(days,"Operator review"));
  rejected("scheduleReason",()->ScheduleRules.validate(30,"short"));
  ok(LocalDate.parse("2026-11-30").equals(ScheduleRules.nextDue(LocalDate.parse("2026-09-01"),30,LocalDate.parse("2026-11-05"))),"anchored overdue cadence");
  ok(LocalDate.parse("2028-02-29").equals(ScheduleRules.nextDue(LocalDate.parse("2028-02-28"),1,LocalDate.parse("2028-02-28"))),"leap day");
  rejected("evidenceRequired",()->ActionRules.mayComplete("IN_PROGRESS","Replaced damaged component",0));
  rejected("repairRequired",()->ActionRules.mayComplete("IN_PROGRESS","fixed",1));
  rejected("invalidTransition",()->ActionRules.mayComplete("OPEN","Replaced damaged component",1));
  ActionRules.mayComplete("IN_PROGRESS","Replaced damaged component",1);ok(true,"repair completed");
  rejected("independentReview",()->ActionRules.mayVerify("AWAITING_VERIFICATION","a","a","Checked function",1));
  ActionRules.mayVerify("AWAITING_VERIFICATION","a","b","Checked function",1);ok(true,"independent closure");
  ok("application/pdf".equals(FileRules.mime("%PDF-1.7".getBytes(StandardCharsets.US_ASCII))),"PDF signature");
  ok("image/png".equals(FileRules.mime(new byte[]{(byte)137,80,78,71,13,10,26,10})),"PNG signature");
  ok("image/jpeg".equals(FileRules.mime(new byte[]{(byte)255,(byte)216,(byte)255})),"JPEG signature");
  rejected("fileType",()->FileRules.mime("<script>".getBytes(StandardCharsets.US_ASCII)));
  rejected("fileSize",()->FileRules.mime(new byte[0]));rejected("fileSize",()->FileRules.mime(new byte[FileRules.MAX_BYTES+1]));
  ok("passwd.pdf".equals(FileRules.filename("../../passwd","application/pdf")),"path traversal removed");
  ok("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad".equals(FileRules.sha256("abc".getBytes(StandardCharsets.US_ASCII))),"SHA-256 known vector");
  ok(!ExportRules.html("<script>").contains("<script>"),"HTML escaped");
  for(String s:new String[]{"=SUM(A1)","+1","-1","@cmd","\t=2"})ok(ExportRules.csv(s).contains("'"),"CSV neutralized "+s);
  var limiter=new LoginLimiter(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));for(int i=0;i<12;i++)ok(limiter.allow("a"),"login allowance "+i);ok(!limiter.allow("a"),"login limit");ok(limiter.allow("b"),"separate address");
  System.out.println("RESULT: "+count+" checks passed; production domain sources; no Spring/PostgreSQL execution.");
 }
}
