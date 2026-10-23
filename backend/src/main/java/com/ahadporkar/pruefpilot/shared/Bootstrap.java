package com.ahadporkar.pruefpilot.shared;

import java.time.*;
import java.util.*;
import com.ahadporkar.pruefpilot.api.Commands;
import com.ahadporkar.pruefpilot.auth.*;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.inspection.InspectionService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.evidence.EvidenceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import static com.ahadporkar.pruefpilot.shared.Db.*;

/** One-time provisioning. No default passwords, silent resets or production demo accounts. */
@Component
public class Bootstrap implements ApplicationRunner {
    private final Db db;private final PasswordEncoder encoder;private final CatalogService catalog;private final MemberService members;
    private final UserDirectory directory;private final InspectionService inspections;private final ActionService actions;private final EvidenceService evidence;
    private final String email,password,organization,timezone,publicUrl,demoPassword;private final boolean demo,production,secure;
    public Bootstrap(Db db,PasswordEncoder encoder,CatalogService catalog,MemberService members,UserDirectory directory,InspectionService inspections,ActionService actions,EvidenceService evidence,
        @Value("${app.bootstrap-email}") String email,@Value("${app.bootstrap-password}") String password,@Value("${app.organization}") String organization,
        @Value("${app.timezone}") String timezone,@Value("${app.public-url}") String publicUrl,@Value("${app.demo-password}") String demoPassword,
        @Value("${app.demo}") boolean demo,@Value("${app.production}") boolean production,@Value("${server.servlet.session.cookie.secure}") boolean secure) {
        this.db=db;this.encoder=encoder;this.catalog=catalog;this.members=members;this.directory=directory;this.inspections=inspections;this.actions=actions;this.evidence=evidence;
        this.email=email;this.password=password;this.organization=organization;this.timezone=timezone;this.publicUrl=publicUrl;this.demoPassword=demoPassword;this.demo=demo;this.production=production;this.secure=secure;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        ZoneId.of(timezone);
        if(production && (demo || !secure || !publicUrl.startsWith("https://"))) throw new IllegalStateException("Production requires HTTPS, secure cookies and DEMO_DATA=false.");
        db.one("SELECT pg_advisory_xact_lock(775519021) AS locked");
        if(db.count("SELECT count(*) FROM organization")>0) return;
        MemberService.validatePassword(password);
        UUID org=UUID.randomUUID(),admin=UUID.randomUUID();
        db.update("INSERT INTO organization(id,name,timezone) VALUES(?,?,?)",org,organization,timezone);
        db.update("INSERT INTO app_user(id,organization_id,email,display_name,password_hash,role) VALUES(?,?,?,?,?,'ADMIN')",admin,org,email.strip().toLowerCase(Locale.ROOT),"Ahad Porkar",encoder.encode(password));
        if(demo) seed(directory.loadUserByUsername(email));
    }
    private void seed(Actor admin) {
        MemberService.validatePassword(demoPassword);
        members.create(admin,new Commands.User("supervisor@pruefpilot.test","Lena Fischer","SUPERVISOR",demoPassword));
        members.create(admin,new Commands.User("inspector@pruefpilot.test","Jonas Weber","INSPECTOR",demoPassword));
        members.create(admin,new Commands.User("technician@pruefpilot.test","Mara Klein","TECHNICIAN",demoPassword));
        members.create(admin,new Commands.User("viewer@pruefpilot.test","Customer review","VIEWER",demoPassword));
        Actor supervisor=directory.loadUserByUsername("supervisor@pruefpilot.test"),inspector=directory.loadUserByUsername("inspector@pruefpilot.test"),technician=directory.loadUserByUsername("technician@pruefpilot.test");
        var sites=List.of(
            catalog.createSite(admin,new Commands.Site("Berlin / Workshop","BER","Example location, Berlin, Germany")),
            catalog.createSite(admin,new Commands.Site("Hamburg / Logistics","HAM","Example location, Hamburg, Germany")),
            catalog.createSite(admin,new Commands.Site("Leipzig / Operations","LEJ","Example location, Leipzig, Germany")));
        var template=catalog.createTemplate(admin,new Commands.Template("Workshop asset / visual check","Werkstattanlage / Sichtkontrolle","DEMONSTRATION ONLY. Not a statutory inspection template. Qualified domain review required before real use.",1,List.of(
            new Commands.Item("Asset identification is readable","Anlagenkennzeichnung ist lesbar","PASS_FAIL",true,null,null,""),
            new Commands.Item("Work area is clear and accessible","Arbeitsbereich ist frei und zugaenglich","PASS_FAIL",true,null,null,""),
            new Commands.Item("No visible damage or leakage","Keine sichtbaren Schaeden oder Undichtigkeiten","PASS_FAIL",true,null,null,""),
            new Commands.Item("Operator observations","Beobachtungen der Bedienperson","TEXT",false,null,null,""))));
        UUID templateId=uuid(template,"id");catalog.reviewTemplate(supervisor,templateId,"DEMO: workflow reviewed for software demonstration, not technical suitability.");
        List<Map<String,Object>> items=catalog.items(admin,templateId);
        String[] names={"Hydraulic lift / Bay 01","Air compressor / Unit 07","Extraction system / Line 04","Mobile work platform / 02","Loading dock gate / A","Pallet truck / E-12","Warehouse ladder / 03","Ventilation unit / North","Bench grinder / Station 06","Service lift / B","Workshop trolley / 08","Safety cabinet / West"};
        String[] categories={"Lifting equipment","Compressed air","Ventilation","Access equipment","Dock equipment","Material handling","Access equipment","Ventilation","Workshop equipment","Lifting equipment","Workshop equipment","Storage"};
        LocalDate today=LocalDate.now(ZoneId.of(timezone));
        int[] offsets={-2,0,-1,3,1,2,5,7,9,14,18,24};
        for(int index=0;index<names.length;index++) {
            String prefix=index<4?"BER":index<8?"HAM":"LEJ";
            var asset=catalog.createAsset(admin,new Commands.Asset(uuid(sites.get(index/4),"id"),"PP-"+prefix+"-"+String.format("%03d",index+1),names[index],categories[index],"Example manufacturer","Series "+(100+index),"DEMO-"+(4000+index),"Zone "+(index%4+1),index<2?"HIGH":"MEDIUM"));
            UUID assetId=uuid(asset,"id");
            UUID scheduleId=uuid(catalog.createSchedule(supervisor,new Commands.Schedule(assetId,templateId,inspector.id(),30,today.plusDays(offsets[index]),"DEMO ONLY: arbitrary 30-day operational interval. Replace after qualified review.")),"id");
            if(index<5) {
                var inspection=inspections.start(inspector,new Commands.StartInspection(assetId,templateId,scheduleId,UUID.randomUUID()));
                UUID inspectionId=uuid(inspection,"id");
                if(index==4) continue;
                var answers=new ArrayList<Commands.Answer>();
                for(int n=0;n<items.size();n++) answers.add(new Commands.Answer(uuid(items.get(n),"id"),n==3?"Demonstration inspection":n==2 && index<2?"FAIL":"PASS",n==2 && index<2?"DEMO: visible seepage at a connection; investigate before use.":""));
                inspections.submit(inspector,inspectionId,new Commands.SubmitInspection(0,answers),false);
                if(index==3) inspections.review(supervisor,inspectionId,new Commands.Review(1,true,"DEMO: reviewed the recorded observations and evidence."));
                if(index<2) {
                    UUID finding=uuid(db.one("SELECT id FROM finding WHERE organization_id=? AND inspection_id=?",admin.organizationId(),inspectionId),"id");
                    actions.assign(supervisor,finding,new Commands.Assign(0,technician.id(),today.plusDays(index+1),index==0?"HIGH":"MEDIUM"));
                    actions.start(technician,finding,1);
                    if(index==1) {
                        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jFZkAAAAASUVORK5CYII=");
                        evidence.store(technician,finding,true,"DEMO-placeholder-not-repair-evidence.png",png);
                        actions.complete(technician,finding,new Commands.Repair(2,"DEMO ONLY: replaced the sample connection; placeholder evidence must never be used for real inspections."));
                    }
                }
            }
        }
    }
}
