package dtm.plugins.models.remote.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import dtm.manager.impl.process.definition.ProcessDefinitionImpl;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.plugins.models.Constants;
import dtm.plugins.models.remote.ProcessRemoteServer;
import dtm.plugins.models.remote.ProcessRemoteServerState;
import dtm.plugins.models.remote.res.ProcessStateChangeEvent;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessRemoteServerImpl implements ProcessRemoteServer {

    private ProcessDefinitionImpl processDefinition;
    private ProcessDefinitionImpl mainProcessDefinition;
    private ProcessRemoteServerStateImpl processState;

    @Override
    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    @Override
    public ProcessDefinition getMainProcessDefinition() {
        return mainProcessDefinition;
    }

    @Override
    public ProcessRemoteServerState getProcessRemoteServerState() {
        return processState;
    }

    public static ProcessRemoteServer ofString(String contentString) throws JsonProcessingException {
        return Constants.OBJECT_MAPPER.readValue(contentString, ProcessRemoteServerImpl.class);
    }

}
