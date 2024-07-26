package com.innercircle.project_one.survey.api;

import com.innercircle.project_one.survey.api.dto.SurveySubmitDTO;
import com.innercircle.project_one.survey.api.repository.*;
import com.innercircle.project_one.survey.api.dto.SurveyDTO;
import com.innercircle.project_one.survey.api.dto.SurveyObjectDTO;
import com.innercircle.project_one.survey.common.ApiResponse;
import com.innercircle.project_one.survey.common.SuccessResponse;
import com.innercircle.project_one.survey.common.SurveyObjectDataType;
import com.innercircle.project_one.survey.domain.*;
import com.innercircle.project_one.survey.domain.embeddable.SurveyObjectContent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyVersionRepository surveyVersionRepository;
    private final SurveyObjectRepository surveyObjectRepository;
    private final ElementObjectRepository elementObjectRepository;
    private final SurveyObjectAnswerRepository surveyObjectAnswerRepository;


    public SurveyService(
            SurveyRepository surveyRepository,
            SurveyVersionRepository surveyVersionRepository,
            SurveyObjectRepository surveyObjectRepository,
            ElementObjectRepository elementObjectRepository,
            SurveyObjectAnswerRepository surveyObjectAnswerRepository) {
        this.surveyRepository = surveyRepository;
        this.surveyVersionRepository = surveyVersionRepository;
        this.surveyObjectRepository = surveyObjectRepository;
        this.elementObjectRepository = elementObjectRepository;
        this.surveyObjectAnswerRepository = surveyObjectAnswerRepository;
    }

    @Transactional
    public ApiResponse saveSurvey(SurveyDTO surveyDTO) {
        Survey survey = createSurvey(surveyDTO);
        SurveyVersion newSurveyVersion = createSurveyVersion(survey);
        saveSurveyObjects(survey, surveyDTO, newSurveyVersion);
        return new SuccessResponse<>("설문조사 폼이 저장되었습니다.");
    }

    @Transactional
    public ApiResponse updateSurvey(Long surveyId, SurveyDTO surveyDTO) {
        Survey findSurvey = findSurvey(surveyId);
        findSurvey.updateSurveyTitleAndDescription(surveyDTO);
        Survey savedSurvey = surveyRepository.save(findSurvey);

        SurveyVersion latestVersion = surveyVersionRepository.findTopBySurveyOrderByVersionDesc(savedSurvey);
        latestVersion.setVersion(latestVersion.getVersion() + 1);

        saveSurveyObjects(savedSurvey, surveyDTO, latestVersion);
        return new SuccessResponse<>("설문조사 폼이 업데이트되었습니다.");
    }

    private void saveSurveyObjects(Survey savedSurvey, SurveyDTO surveyDTO, SurveyVersion surveyVersion) {

        SurveyVersion savedSurveyVersion = surveyVersionRepository.save(surveyVersion);
        savedSurvey.updateSurveyVersion(savedSurveyVersion);
        surveyRepository.save(savedSurvey);

        List<SurveyObject> surveyObjects = new ArrayList<>();
        List<SurveyObjectDTO> objects = surveyDTO.objects();
        for (int i = 0; i < objects.size(); i++) {
            SurveyObject surveyObject = createSurveyObject(i, objects.get(i), savedSurvey, savedSurveyVersion);
            surveyObjects.add(surveyObject);
        }

        surveyObjectRepository.saveAll(surveyObjects);
    }

    private SurveyObject createSurveyObject(int idx, SurveyObjectDTO objectDTO, Survey survey, SurveyVersion savedSurveyVersion) {

        SurveyObjectContent content   = new SurveyObjectContent(objectDTO);
        SurveyObjectDataType dataType = SurveyObjectDataType.of(objectDTO.type().toUpperCase());

        SurveyObject surveyObject = SurveyObject.builder()
                .elementOrder(idx)
                .type(dataType)
                .surveyObjectContent(content)
                .survey(survey)
                .surveyVersionId(savedSurveyVersion.getVersion())
                .build();

        if(dataType.isElementDataType()) {

            if (objectDTO.elements().isEmpty()) {
                throw new IllegalArgumentException("선택 리스트 요소는 1개 이상 지정되어야 합니다.");
            }

            List<ElementObject> elements = getElementObject(objectDTO, surveyObject);
            elementObjectRepository.saveAll(elements);
        }

        return surveyObject;
    }



    @Transactional
    public ApiResponse submitSurveyResponse(Long surveyId, SurveySubmitDTO surveySubmitDTO) {

        Survey survey = findSurvey(surveyId);
        isSubmitable(survey, surveySubmitDTO);

        List<SurveyObject> surveyObjects = survey.getSurveyObjects();
        survey.sortSurveyObjects();

        List<SurveySubmitDTO.SurveySubmitObject> objects = surveySubmitDTO.objects();

        for (int i = 0; i < objects.size(); i++) {
            SurveySubmitDTO.SurveySubmitObject submitObject = objects.get(i);

            List<SurveyObjectAnswer> answers = createAnswer(surveyObjects.get(i), submitObject);
            for (SurveyObjectAnswer answer : answers) {
                surveyObjectAnswerRepository.save(answer);
            }
        }

        return new SuccessResponse<>("설문조사 응답이 제출되었습니다.");
    }


    private List<SurveyObjectAnswer> createAnswer(SurveyObject surveyObject, SurveySubmitDTO.SurveySubmitObject requestObject) {
        List<SurveyObjectAnswer> answers = new ArrayList<>();
        switch (SurveyObjectDataType.of(requestObject.type())) {
            case TEXT, RICH_TEXT -> {
                String content = ((SurveySubmitDTO.SurveySubmitObject.StringContent) requestObject.content()).getContent();
                answers.add(StringSurveyObjectAnswer.builder()
                        .surveyObject(surveyObject)
                        .answer(content)
                        .build());
            }
            case RADIO -> {
                String selectedElement = ((SurveySubmitDTO.SurveySubmitObject.ElementContent) requestObject.content()).getSelectedElement();
                answers.add(ElementSurveyObjectAnswer.builder()
                        .surveyObject(surveyObject)
                        .elementObject(new ElementObject(1, selectedElement, surveyObject))
                        .build());
            }
            case CHECK_BOX -> {
                List<String> selectedElements = ((SurveySubmitDTO.SurveySubmitObject.CheckBoxContent) requestObject.content()).getSelectedElements();
                for (int i = 0; i < selectedElements.size(); i++) {
                    answers.add(ElementSurveyObjectAnswer.builder()
                            .surveyObject(surveyObject)
                            .elementObject(new ElementObject(i + 1, selectedElements.get(i), surveyObject))
                            .build());
                }
            }
            default -> throw new IllegalArgumentException("적절한 입력 타입이 아닙니다.");
        }
        return answers;
    }

    private Survey createSurvey(SurveyDTO surveyDTO) {
        return surveyRepository.save(new Survey(surveyDTO.title(), surveyDTO.description()));
    }
    private Survey findSurvey(Long surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("설문조사를 찾을 수 없습니다."));
    }

    private SurveyVersion createSurveyVersion(Survey survey) {
        return surveyVersionRepository.save(new SurveyVersion(1L, survey));
    }

    private List<ElementObject> getElementObject(SurveyObjectDTO objectDTO, SurveyObject surveyObject) {
        List<ElementObject> elements = new ArrayList<>();
        int elementStringSize = objectDTO.elements().size();

        for (int i = 0; i < elementStringSize; i++) {
            String elementValue = objectDTO.elements().get(i);
            ElementObject elementObject = new ElementObject(i, elementValue, surveyObject);
            elements.add(elementObject);
          }

        return elements;
    }

    private void isSubmitable(Survey survey, SurveySubmitDTO surveySubmitDTO){

        if(survey.getSurveyVersion().getVersion() != surveySubmitDTO.version()) {
            throw new IllegalArgumentException("버전이 일치하지 않습니다.");
        }

        survey.sortSurveyObjects();
        List<SurveyObject> savedSurveyObjects = survey.getSurveyObjects();
        List<SurveySubmitDTO.SurveySubmitObject> submitSurveyObjects = surveySubmitDTO.objects();

        if (savedSurveyObjects.size() != submitSurveyObjects.size()) {
            throw new IllegalArgumentException("목록이 일치하지 않습니다.");
        }

        for (int i = 0; i < savedSurveyObjects.size(); i++){
            SurveyObject surveyObject = savedSurveyObjects.get(i);
            SurveySubmitDTO.SurveySubmitObject submitSurveyObject = submitSurveyObjects.get(i);
            
//            if(!Objects.equals(surveyObject.getId(), submitSurveyObject.id())){
//                throw new IllegalArgumentException("목록 순서가 일치하지 않습니다.");
//            }
            
            if (surveyObject.getType() != SurveyObjectDataType.of(submitSurveyObject.type())) {
                throw new IllegalArgumentException("목록 타입이 일치하지 않습니다.");
            }

        }

    }

}
