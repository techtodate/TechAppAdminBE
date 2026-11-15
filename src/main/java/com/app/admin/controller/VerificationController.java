package com.app.admin.controller;

import com.app.admin.dto.*;
import com.app.admin.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/verifications")
public class VerificationController {
    private final VerificationService service;
    public VerificationController(VerificationService service) { this.service=service; }

    @GetMapping("/menu") public Map<String,Object> menu(){return service.menu();}

    @GetMapping public Map<String,Object> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
        @RequestParam(required=false) String search,@RequestParam(required=false) String status,
        @RequestParam(name="verificationType",required=false) String verificationType,@RequestParam(name="assignedReviewer",required=false) Long assignedReviewer,
        @RequestParam(defaultValue="submittedAt") String sortBy,@RequestParam(defaultValue="desc") String direction) {
        return service.list(page,size,search,status,verificationType,assignedReviewer,sortBy,direction);
    }
    @GetMapping("/counts") public Map<String,Long> counts(){return service.counts();}
    @GetMapping("/{id}") public Map<String,Object> detail(@PathVariable long id){return service.detail(id);}
    @GetMapping("/{id}/approval-readiness") public Map<String,Object> approvalReadiness(@PathVariable long id){return service.approvalReadiness(id);}
    @PostMapping("/{id}/assign") public Map<String,Object> assign(@PathVariable long id,@Valid @RequestBody VerificationAssignmentRequest request){return service.assign(id,request);}
    @PostMapping("/{id}/start-review") public Map<String,Object> startReview(@PathVariable long id){return service.startReview(id);}
    @PutMapping("/{id}/sections/{sectionCode}") public Map<String,Object> section(@PathVariable long id,@PathVariable String sectionCode,@Valid @RequestBody VerificationSectionReviewRequest request){return service.section(id,sectionCode,request);}
    @PostMapping("/{id}/evidence/{evidenceId}/approve") public Map<String,Object> approveEvidence(@PathVariable long id,@PathVariable long evidenceId,@RequestBody(required=false) Map<String,String> request){return service.evidence(id,evidenceId,"APPROVE",comment(request));}
    @PostMapping("/{id}/evidence/{evidenceId}/reject") public Map<String,Object> rejectEvidence(@PathVariable long id,@PathVariable long evidenceId,@RequestBody(required=false) Map<String,String> request){return service.evidence(id,evidenceId,"REJECT",comment(request));}
    @PostMapping("/{id}/evidence/{evidenceId}/request-changes") public Map<String,Object> changeEvidence(@PathVariable long id,@PathVariable long evidenceId,@RequestBody(required=false) Map<String,String> request){return service.evidence(id,evidenceId,"REQUEST_CHANGES",comment(request));}
    @GetMapping("/{id}/evidence/{evidenceId}/download-url") public Map<String,String> evidenceUrl(@PathVariable long id,@PathVariable long evidenceId){return Map.of("download_url",service.download(id,evidenceId));}
    @GetMapping("/{id}/history") public java.util.List<Map<String,Object>> history(@PathVariable long id){return service.history(id);}
    @PutMapping("/{id}/identity/review") public Map<String,Object> identity(@PathVariable long id,@Valid @RequestBody VerificationSectionReviewRequest request){return service.identity(id,request);}
    @PostMapping("/{id}/approve") public Map<String,Object> approve(@PathVariable long id){return service.approve(id);}
    @PostMapping("/{id}/request-changes") public Map<String,Object> requestChanges(@PathVariable long id,@Valid @RequestBody VerificationChangesRequest request){return service.requestChanges(id,request);}
    @PostMapping("/{id}/reject") public Map<String,Object> reject(@PathVariable long id,@Valid @RequestBody VerificationDecisionRequest request){return service.reject(id,request);}
    private String comment(Map<String,String> request){return request==null?null:request.get("comments");}
}
