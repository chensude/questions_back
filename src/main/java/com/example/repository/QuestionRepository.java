package com.example.repository;

import com.example.entity.Question;
import com.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    Page<Question> findByUserAndQuestionType(User user, String questionType, Pageable pageable);
    Page<Question> findByUser(User user, Pageable pageable);
} 