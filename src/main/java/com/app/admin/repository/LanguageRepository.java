package com.app.admin.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.app.admin.model.Language;
@Repository public interface LanguageRepository extends JpaRepository<Language, Long> {}
