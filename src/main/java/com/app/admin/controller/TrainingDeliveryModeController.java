package com.app.admin.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.admin.model.TrainingDeliveryMode;
import com.app.admin.service.TrainingDeliveryModeService;
@RestController @RequestMapping("/api/training-delivery-modes")
public class TrainingDeliveryModeController extends MaintenanceMasterController<TrainingDeliveryMode> {
    public TrainingDeliveryModeController(TrainingDeliveryModeService service) { super(service); }
}
