package com.ahadporkar.pruefpilot.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.ahadporkar.pruefpilot.auth.*;
import com.ahadporkar.pruefpilot.audit.AuditService;
import com.ahadporkar.pruefpilot.catalog.CatalogService;
import com.ahadporkar.pruefpilot.inspection.InspectionService;
import com.ahadporkar.pruefpilot.action.ActionService;
import com.ahadporkar.pruefpilot.evidence.EvidenceService;
import com.ahadporkar.pruefpilot.report.ReportService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.ahadporkar.pruefpilot.shared.Db.*;

/** Thin transport layer. Authorization, tenant scoping, locking and audit live in services. */
@RestController
@RequestMapping("/api")
@Validated
public class ApiController {
    private final CatalogService catalog;private final InspectionService inspections;private final ActionService actions;
    private final EvidenceService evidence;private final ReportService reports;private final WorkspaceService workspace;
    private final MemberService members;private final AuditService audit;
    public ApiController(CatalogService catalog,InspectionService inspections,ActionService actions,EvidenceService evidence,ReportService reports,WorkspaceService workspace,MemberService members,AuditService audit) {
        this.catalog=catalog;this.inspections=inspections;this.actions=actions;this.evidence=evidence;this.reports=reports;this.workspace=workspace;this.members=members;this.audit=audit;
    }
    @GetMapping("/workspace") public Map<String,Object> workspace(@AuthenticationPrincipal Actor actor) {return workspace.workspace(actor);}
    @GetMapping("/assets/{id}/passport") public Map<String,Object> passport(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page) {return workspace.passport(actor,id,page);}
    @PostMapping("/sites") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> site(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.Site command) {return catalog.createSite(actor,command);}
    @PostMapping("/assets") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> asset(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.Asset command) {return catalog.createAsset(actor,command);}
    @PostMapping("/assets/{id}/archive") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {catalog.archiveAsset(actor,id);}
    @PostMapping("/templates") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> template(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.Template command) {return catalog.createTemplate(actor,command);}
    @PostMapping("/templates/{id}/review") @ResponseStatus(HttpStatus.NO_CONTENT) public void templateReview(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Review command) {catalog.reviewTemplate(actor,id,command.note());}
    @PostMapping("/schedules") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> schedule(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.Schedule command) {return catalog.createSchedule(actor,command);}
    @PostMapping("/schedules/{id}/pause") @ResponseStatus(HttpStatus.NO_CONTENT) public void pause(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {catalog.pauseSchedule(actor,id);}
    @PostMapping("/inspections") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> inspection(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.StartInspection command) {return inspections.start(actor,command);}
    @GetMapping("/inspections/{id}") public Map<String,Object> inspection(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return Map.of("inspection",inspections.get(actor,id),"answers",inspections.answers(actor,id));}
    @PutMapping("/inspections/{id}/draft") public Map<String,Object> draft(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.SubmitInspection command) {return inspections.submit(actor,id,command,true);}
    @PostMapping("/inspections/{id}/submit") public Map<String,Object> submit(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.SubmitInspection command) {return inspections.submit(actor,id,command,false);}
    @PostMapping("/inspections/{id}/review") public Map<String,Object> review(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Review command) {return inspections.review(actor,id,command);}
    @PostMapping("/findings/{id}/assign") public Map<String,Object> assign(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Assign command) {return actions.assign(actor,id,command);}
    @PostMapping("/findings/{id}/start") public Map<String,Object> start(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Version command) {return actions.start(actor,id,command.version());}
    @PostMapping("/findings/{id}/complete") public Map<String,Object> complete(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Repair command) {return actions.complete(actor,id,command);}
    @PostMapping("/findings/{id}/verify") public Map<String,Object> verify(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Review command) {return actions.verify(actor,id,command);}
    @PostMapping("/inspections/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> inspectionEvidence(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam("file") MultipartFile file) throws IOException {return evidence.upload(actor,id,false,file);}
    @PostMapping("/findings/{id}/evidence") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> findingEvidence(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam("file") MultipartFile file) throws IOException {return evidence.upload(actor,id,true,file);}
    @GetMapping("/evidence/{id}/download") public ResponseEntity<byte[]> download(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {
        var metadata=evidence.metadata(actor,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(str(metadata,"mime"))).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(str(metadata,"filename"),StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(evidence.download(actor,id));
    }
    @GetMapping("/assets/{id}/report") public ResponseEntity<byte[]> report(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="en") String lang) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"pruefpilot-"+id+"-"+(lang.equals("de")?"de":"en")+".zip\"").cacheControl(CacheControl.noStore()).body(reports.export(actor,id,lang));
    }
    @GetMapping("/audit") public List<Map<String,Object>> audit(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page) {return audit.list(actor,page);}
    @GetMapping("/members") public List<Map<String,Object>> members(@AuthenticationPrincipal Actor actor) {return members.list(actor);}
    @PostMapping("/members") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> member(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.User command) {return members.create(actor,command);}
    @PatchMapping("/members/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void active(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Commands.Active command) {members.setActive(actor,id,command.active());}
    @PostMapping("/auth/password") @ResponseStatus(HttpStatus.NO_CONTENT) public void password(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Commands.Password command,HttpServletRequest request) {members.changePassword(actor,command);request.getSession().invalidate();}
}
