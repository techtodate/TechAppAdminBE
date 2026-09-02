package com.app.admin.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
@Entity @Table(name = "organization_type", schema = "public") @NoArgsConstructor
public class OrganizationType extends MaintenanceMaster {}
