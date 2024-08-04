package com.innercircle.project_one.survey.api.repository;

import com.innercircle.project_one.survey.domain.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {
}
