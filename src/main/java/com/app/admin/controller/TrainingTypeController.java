package com.app.admin.controller;
import org.springframework.web.bind.annotation.*;
import com.app.admin.model.TrainingType;
import com.app.admin.service.TrainingTypeService;
@RestController @RequestMapping("/api/training-types")
public class TrainingTypeController extends MaintenanceMasterController<TrainingType> { public TrainingTypeController(TrainingTypeService service) { super(service); } }
