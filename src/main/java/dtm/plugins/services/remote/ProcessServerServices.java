package dtm.plugins.services.remote;

import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.dto.ProcessDTO;
import dtm.plugins.models.remote.ProcessRemoteServer;
import dtm.plugins.services.remote.attacher.ProcessAttachListenerService;

import java.util.List;

public interface ProcessServerServices {
    List<ProcessRemoteServer> getAllProcesses();

    ProcessRemoteServer start(String processId);
    ProcessRemoteServer stop(String processId);
    ProcessRemoteServer restart(String processId);

    ProcessRemoteServer save(ProcessDTO processDTO);
    ProcessRemoteServer save(String processId, ProcessDTO processDTO);

    ProcessRemoteServer delete(String processId);

    ProcessAttachListenerService newAttachProcess(ProcessDefinition processDefinition, boolean sendHistory);

    void writeToStdin(String processId, String input);

}
