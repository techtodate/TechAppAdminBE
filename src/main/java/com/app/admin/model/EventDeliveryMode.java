package com.app.admin.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
@Entity @Table(name = "event_delivery_mode", schema = "public") @NoArgsConstructor
public class EventDeliveryMode extends MaintenanceMaster {}
