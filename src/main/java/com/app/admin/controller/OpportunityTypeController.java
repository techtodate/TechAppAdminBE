package com.app.admin.controller;
import org.springframework.web.bind.annotation.*;
import com.app.admin.model.OpportunityType;
import com.app.admin.service.OpportunityTypeService;
@RestController @RequestMapping("/api/opportunity-types")
public class OpportunityTypeController extends MaintenanceMasterController<OpportunityType> { public OpportunityTypeController(OpportunityTypeService service) { super(service); } }
