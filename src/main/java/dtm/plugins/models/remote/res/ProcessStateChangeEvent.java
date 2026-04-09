package dtm.plugins.models.remote.res;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.manager.impl.process.definition.ProcessDefinitionImpl;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.enums.ProcessEvents;
import dtm.plugins.models.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessStateChangeEvent {
    private final ProcessDefinitionImpl processDefinition;
    private final ProcessDefinitionImpl mainProcessDefinition;

    @Getter
    private final ProcessEvents action;
    @Getter
    private final boolean mainProcess;

    @JsonCreator
    public ProcessStateChangeEvent(
            @JsonProperty("processDefinition") ProcessDefinitionImpl processDefinition,
            @JsonProperty("mainProcessDefinition") ProcessDefinitionImpl mainProcessDefinition,
            @JsonProperty("action") ProcessEvents action,
            @JsonProperty("mainProcess") boolean mainProcess
    ) {
        this.processDefinition = processDefinition;
        this.mainProcessDefinition = mainProcessDefinition;
        this.action = action;
        this.mainProcess = mainProcess;
    }


    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public ProcessDefinition getMainProcessDefinition() {
        return mainProcessDefinition;
    }

    public static ProcessStateChangeEvent ofString(String contentString) throws JsonProcessingException {
        return Constants.OBJECT_MAPPER.readValue(contentString, ProcessStateChangeEvent.class);
    }

}
