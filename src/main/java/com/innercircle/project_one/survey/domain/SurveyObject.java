package com.innercircle.project_one.survey.domain;

import com.innercircle.project_one.survey.api.common.SurveyObjectDataType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class SurveyObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SurveyObjectDataType type;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "survey_id")
    private Survey survey;

    @ManyToOne
    @JoinColumn(name = "survey_version_id")
    private SurveyVersion surveyVersion;

    @OneToMany(mappedBy = "surveyObject", cascade = CascadeType.ALL)
    private List<ElementObject> elementObjects = new ArrayList<>();

}