package com.app.admin.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
@Entity @Table(name = "opportunity_type", schema = "public") @NoArgsConstructor
public class OpportunityType extends MaintenanceMaster {}
