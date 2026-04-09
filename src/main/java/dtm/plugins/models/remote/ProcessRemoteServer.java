package dtm.plugins.models.remote;

import dtm.manager.process.definition.ProcessDefinition;

public interface ProcessRemoteServer {
    ProcessDefinition getProcessDefinition();
    ProcessDefinition getMainProcessDefinition();
    ProcessRemoteServerState getProcessRemoteServerState();
}
