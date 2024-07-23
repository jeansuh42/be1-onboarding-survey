package com.innercircle.project_one.survey.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class ElementObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int elementOrder;
    private String elementValue;

    @ManyToOne
    @JoinColumn(name = "survey_object_id")
    private SurveyObject surveyObject;

}
