package com.innercircle.project_one.survey.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

import java.util.List;

public record SurveySubmitDTO(
        int version,
        List<SurveySubmitObject> objects
) {

    public record SurveySubmitObject(
            String type,
            Long id,

            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "type"
            )
            @JsonSubTypes({
                    @JsonSubTypes.Type(value = SurveySubmitObject.StringContent.class, name = "text"),
                    @JsonSubTypes.Type(value = SurveySubmitObject.StringContent.class, name = "rich_text"),
                    @JsonSubTypes.Type(value = SurveySubmitObject.ElementContent.class, name = "radio"),
                    @JsonSubTypes.Type(value = SurveySubmitObject.CheckBoxContent.class, name = "check_box")
            })
            SurveySubmitContent content
    ) {

        public static abstract class SurveySubmitContent {}

        @Getter
        public static class ElementContent extends SurveySubmitContent {
            private final String selectedElement;

            @JsonCreator
            public ElementContent(@JsonProperty("selected_element") String selectedElement) {
                this.selectedElement = selectedElement;
            }
        }

        @Getter
        public static class CheckBoxContent extends SurveySubmitContent {
            private final List<String> selectedElements;

            @JsonCreator
            public CheckBoxContent(@JsonProperty("checked_elements") List<String> selectedElements) {
                this.selectedElements = selectedElements;
            }
        }

        @Getter
        public static class StringContent extends SurveySubmitContent {
            private final String content;

            public StringContent(String content) {
                this.content = content;
            }
        }


    }
}
