package com.ahadporkar.pruefpilot;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import com.ahadporkar.pruefpilot.api.*;
import com.ahadporkar.pruefpilot.auth.*;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.inspection.InspectionService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.evidence.EvidenceService;
import com.ahadporkar.pruefpilot.report.ReportService;
import com.ahadporkar.pruefpilot.shared.*;
import com.ahadporkar.pruefpilot.domain.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ahadporkar.pruefpilot.shared.Db.*;

/** Real PostgreSQL + actual HTTP security. Failsafe runs this with `mvn verify`.
 * Each test owns an isolated organization; never run against a production database. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "app.bootstrap-email=bootstrap@integration.test",
 "app.bootstrap-password=Integration-bootstrap-only-2026!",
 "app.demo=false","app.production=false","server.servlet.session.cookie.secure=false"
})
class WorkflowIT {
 static final String PASSWORD="Integration-only-password-2026!";
 @Autowired Db db; @Autowired CatalogService catalog; @Autowired InspectionService inspections;
 @Autowired ActionService actions; @Autowired EvidenceService evidence; @Autowired ReportService reports;
 @Autowired MemberService members; @Autowired PasswordEncoder encoder; @Autowired JsonMapper json;
 @Value("${local.server.port}") int port;
 Actor admin,supervisor,inspector,technician,viewer; UUID assetId,templateId,scheduleId,itemId; LocalDate due;
 @BeforeEach void provision(){
  UUID org=UUID.randomUUID();db.update("INSERT INTO organization(id,name,timezone) VALUES(?,?,?)",org,"Integration workspace","Europe/Berlin");
  admin=user(org,"ADMIN");supervisor=user(org,"SUPERVISOR");inspector=user(org,"INSPECTOR");technician=user(org,"TECHNICIAN");viewer=user(org,"VIEWER");
  UUID site=uuid(catalog.createSite(admin,new Commands.Site("Workshop","TEST","Fictional address")),"id");
  assetId=uuid(catalog.createAsset(admin,new Commands.Asset(site,"ASSET-01","Test asset","Workshop","Example","M1","S1","Bay 1","HIGH")),"id");
  templateId=uuid(catalog.createTemplate(admin,new Commands.Template("Visual check","Sichtkontrolle","TEST ONLY",1,List.of(new Commands.Item("No visible damage","Keine sichtbaren Schaeden","PASS_FAIL",true,null,null,"")))),"id");
  catalog.reviewTemplate(supervisor,templateId,"Reviewed solely for integration testing");itemId=uuid(catalog.items(admin,templateId).getFirst(),"id");
  due=LocalDate.now(ZoneId.of("Europe/Berlin")).minusDays(2);
  scheduleId=uuid(catalog.createSchedule(supervisor,new Commands.Schedule(assetId,templateId,inspector.id(),30,due,"TEST ONLY: configured operator cadence")),"id");
 }
 Actor user(UUID org,String role){UUID id=UUID.randomUUID();String email=id+"@integration.test",hash=encoder.encode(PASSWORD);db.update("INSERT INTO app_user(id,organization_id,email,display_name,password_hash,role) VALUES(?,?,?,?,?,?)",id,org,email,role+" Tester",hash,role);return new Actor(id,org,email,role+" Tester",role,hash,true);}
 Map<String,Object> start(Actor actor,UUID key){return inspections.start(actor,new Commands.StartInspection(assetId,templateId,scheduleId,key));}
 Map<String,Object> submit(Actor actor,UUID id,boolean failed){return inspections.submit(actor,id,new Commands.SubmitInspection(0,List.of(new Commands.Answer(itemId,failed?"FAIL":"PASS",failed?"Damaged test component":""))),false);}
 @Test void completeLifecycleAndManifest() throws Exception {
  var started=start(inspector,UUID.randomUUID());UUID inspectionId=uuid(started,"id");assertEquals("DRAFT",str(started,"status"));
  assertEquals("SUBMITTED",str(submit(inspector,inspectionId,true),"status"));
  assertEquals(due.toString(),str(db.one("SELECT next_due FROM inspection_schedule WHERE id=?",scheduleId),"nextDue"));
  assertEquals("VERIFIED",str(inspections.review(supervisor,inspectionId,new Commands.Review(1,true,"Checked the complete inspection record")),"status"));
  assertEquals(due.plusDays(30).toString(),str(db.one("SELECT next_due FROM inspection_schedule WHERE id=?",scheduleId),"nextDue"));
  UUID findingId=uuid(db.one("SELECT id FROM finding WHERE inspection_id=?",inspectionId),"id");
  actions.assign(supervisor,findingId,new Commands.Assign(0,technician.id(),due.plusDays(5),"HIGH"));actions.start(technician,findingId,1);
  assertEquals("evidenceRequired",assertThrows(RuleViolation.class,()->actions.complete(technician,findingId,new Commands.Repair(2,"Replaced the damaged component"))).code());
  byte[] pdf="%PDF-1.7\nTEST FIXTURE NOT REPAIR EVIDENCE".getBytes(StandardCharsets.UTF_8);
  var file=evidence.store(technician,findingId,true,"../repair.pdf",pdf);assertArrayEquals(pdf,evidence.download(viewer,uuid(file,"id")));
  actions.complete(technician,findingId,new Commands.Repair(2,"Replaced the damaged component"));
  assertEquals("CLOSED",str(actions.verify(supervisor,findingId,new Commands.Review(3,true,"Independently checked operation and evidence")),"status"));
  byte[] bundle=reports.export(viewer,assetId,"de");Map<String,byte[]> files=unzip(bundle);
  assertTrue(files.keySet().containsAll(List.of("report.html","data.json","answers.csv","manifest.json")));
  Map<?,?> manifest=json.readValue(files.get("manifest.json"),Map.class);
  for(Object value:(List<?>)manifest.get("files")){Map<?,?> entry=(Map<?,?>)value;byte[] bytes=files.get(entry.get("path").toString());assertNotNull(bytes);assertEquals(entry.get("sha256"),FileRules.sha256(bytes));assertEquals(((Number)entry.get("bytes")).intValue(),bytes.length);}
  assertTrue(new String(files.get("report.html"),StandardCharsets.UTF_8).contains("lang=\"de\""));
  assertTrue(db.count("SELECT count(*) FROM audit_event WHERE organization_id=?",admin.organizationId())>=12);
 }
 @Test void independentInspectionReviewIsEnforced(){UUID id=uuid(start(admin,UUID.randomUUID()),"id");submit(admin,id,false);assertEquals("independentReview",assertThrows(RuleViolation.class,()->inspections.review(admin,id,new Commands.Review(1,true,"Self review attempt"))).code());assertEquals("SUBMITTED",str(inspections.get(admin,id),"status"));}
 @Test void repairerCannotVerifyOwnRepair(){UUID id=uuid(start(inspector,UUID.randomUUID()),"id");submit(inspector,id,true);UUID findingId=uuid(db.one("SELECT id FROM finding WHERE inspection_id=?",id),"id");actions.assign(supervisor,findingId,new Commands.Assign(0,admin.id(),due,"HIGH"));actions.start(admin,findingId,1);evidence.store(admin,findingId,true,"test.pdf","%PDF-test fixture".getBytes(StandardCharsets.US_ASCII));actions.complete(admin,findingId,new Commands.Repair(2,"Replaced component for test"));assertEquals("independentReview",assertThrows(RuleViolation.class,()->actions.verify(admin,findingId,new Commands.Review(3,true,"Self review attempt"))).code());}
 @Test void invalidAnswerRollsBack(){UUID id=uuid(start(inspector,UUID.randomUUID()),"id");assertThrows(RuleViolation.class,()->inspections.submit(inspector,id,new Commands.SubmitInspection(0,List.of(new Commands.Answer(itemId,"FAIL",""))),false));assertEquals("DRAFT",str(inspections.get(admin,id),"status"));assertEquals(0,db.count("SELECT count(*) FROM inspection_answer WHERE inspection_id=?",id));assertEquals(0,db.count("SELECT count(*) FROM finding WHERE inspection_id=?",id));}
 @Test void idempotentStartAndStaleVersion(){UUID key=UUID.randomUUID();var first=start(inspector,key);assertEquals(str(first,"id"),str(start(inspector,key),"id"));UUID id=uuid(first,"id");submit(inspector,id,false);assertThrows(RuleViolation.class,()->inspections.review(supervisor,id,new Commands.Review(0,true,"Stale browser submission")));}
 @Test void concurrentSameKeyCreatesOneRecord() throws Exception {UUID key=UUID.randomUUID();try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->start(inspector,key));var b=pool.submit(()->start(inspector,key));assertEquals(str(a.get(20,TimeUnit.SECONDS),"id"),str(b.get(20,TimeUnit.SECONDS),"id"));}assertEquals(1,db.count("SELECT count(*) FROM inspection WHERE request_key=?",key));}
 @Test void crossWorkspaceReferencesAndReadsFail(){UUID other=UUID.randomUUID();db.update("INSERT INTO organization(id,name,timezone) VALUES(?,?,?)",other,"Other","UTC");Actor outsider=user(other,"ADMIN");assertEquals(404,assertThrows(ApiException.class,()->catalog.asset(outsider,assetId)).status);assertThrows(DataAccessException.class,()->db.update("INSERT INTO inspection_schedule(id,organization_id,asset_id,template_id,owner_id,interval_days,next_due,reason,reviewed_by) VALUES(?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),other,assetId,templateId,outsider.id(),30,due,"Invalid cross tenant",outsider.id()));}
 @Test void auditIsAppendOnly(){assertThrows(DataAccessException.class,()->db.update("UPDATE audit_event SET detail='changed' WHERE organization_id=?",admin.organizationId()));assertThrows(DataAccessException.class,()->db.update("DELETE FROM audit_event WHERE organization_id=?",admin.organizationId()));}
 @Test void archivedAssetCannotHaveOpenSchedule(){assertThrows(RuleViolation.class,()->catalog.archiveAsset(admin,assetId));catalog.pauseSchedule(supervisor,scheduleId);catalog.archiveAsset(admin,assetId);assertEquals(true,catalog.asset(admin,assetId).get("archived"));assertThrows(RuleViolation.class,()->inspections.start(inspector,new Commands.StartInspection(assetId,templateId,null,UUID.randomUUID())));}
 @Test void actualHttpSessionCsrfRoleAndRevocation() throws Exception {
  var cookieManager=new CookieManager(null,CookiePolicy.ACCEPT_ALL);var client=HttpClient.newBuilder().cookieHandler(cookieManager).build();
  assertEquals(401,request(client,"GET","/api/workspace",null,null,null).statusCode());
  assertEquals(403,request(client,"POST","/api/sites","{}",null,null).statusCode());
  var token=csrf(client);String login="username="+URLEncoder.encode(viewer.email(),StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(PASSWORD,StandardCharsets.UTF_8);
  assertEquals(204,request(client,"POST","/api/auth/login",login,token,"application/x-www-form-urlencoded").statusCode());
  assertEquals(200,request(client,"GET","/api/workspace",null,null,null).statusCode());
  token=csrf(client);assertEquals(403,request(client,"POST","/api/sites","{\"name\":\"No\",\"code\":\"NO\",\"address\":\"No\"}",token,"application/json").statusCode());
  members.setActive(admin,viewer.id(),false);assertEquals(401,request(client,"GET","/api/workspace",null,null,null).statusCode());
 }
 @SuppressWarnings("unchecked") Map<String,String> csrf(HttpClient client) throws Exception {var response=request(client,"GET","/api/auth/csrf",null,null,null);assertEquals(200,response.statusCode());return (Map<String,String>)(Map<?,?>)json.readValue(response.body(),Map.class);}
 HttpResponse<String> request(HttpClient client,String method,String path,String body,Map<String,String> token,String type) throws Exception {var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));if(token!=null)builder.header(token.get("headerName"),token.get("token"));if(type!=null)builder.header("Content-Type",type);builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());}
 static Map<String,byte[]> unzip(byte[] bytes) throws IOException {var files=new LinkedHashMap<String,byte[]>();try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=zip.getNextEntry())!=null)files.put(entry.getName(),zip.readAllBytes());}return files;}
}
