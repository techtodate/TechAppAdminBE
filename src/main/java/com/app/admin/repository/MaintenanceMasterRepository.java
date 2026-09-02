package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import com.app.admin.model.MaintenanceMaster;
@NoRepositoryBean
public interface MaintenanceMasterRepository<T extends MaintenanceMaster> extends JpaRepository<T, Long> {}
