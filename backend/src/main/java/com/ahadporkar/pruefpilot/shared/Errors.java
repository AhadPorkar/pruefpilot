package com.ahadporkar.pruefpilot.shared;

import com.ahadporkar.pruefpilot.domain.RuleViolation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** RFC 9457-compatible errors. No SQL details, passwords or stack traces cross the boundary. */
@RestControllerAdvice
public class Errors {
    private ResponseEntity<ProblemDetail> problem(int status,String code,String detail) {
        ProblemDetail body=ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status),detail);
        body.setProperty("code",code); return ResponseEntity.status(status).body(body);
    }
    @ExceptionHandler(ApiException.class) ResponseEntity<ProblemDetail> api(ApiException ex) {return problem(ex.status,ex.code,ex.getMessage());}
    @ExceptionHandler(RuleViolation.class) ResponseEntity<ProblemDetail> rule(RuleViolation ex) {return problem(ex.code().equals("conflict")?409:422,ex.code(),ex.getMessage());}
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ProblemDetail> denied() {return problem(403,"forbidden","You do not have permission for this action.");}
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ProblemDetail> conflict() {return problem(409,"conflict","Duplicate record or conflicting update. Refresh and retry.");}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,IllegalArgumentException.class,jakarta.validation.ConstraintViolationException.class,org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    ResponseEntity<ProblemDetail> validation() {return problem(400,"validation","Check the submitted fields and data types.");}
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ProblemDetail> size() {return problem(413,"fileSize","Maximum file size is 10 MiB.");}
}
