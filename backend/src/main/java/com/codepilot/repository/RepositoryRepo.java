package com.codepilot.repository;
import com.codepilot.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepositoryRepo extends JpaRepository<Repository, Long> {}