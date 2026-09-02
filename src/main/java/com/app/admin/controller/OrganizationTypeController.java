package com.app.admin.controller;
import org.springframework.web.bind.annotation.*;
import com.app.admin.model.OrganizationType;
import com.app.admin.service.OrganizationTypeService;
@RestController @RequestMapping("/api/organization-types")
public class OrganizationTypeController extends MaintenanceMasterController<OrganizationType> { public OrganizationTypeController(OrganizationTypeService service) { super(service); } }
