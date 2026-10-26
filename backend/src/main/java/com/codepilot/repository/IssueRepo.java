package com.codepilot.repository;
import com.codepilot.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface IssueRepo extends JpaRepository<Issue, Long> {
    List<Issue> findByRepositoryId(Long repoId);
}