package dtm.plugins.services.remote;

import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;

import java.util.Set;

public interface RemoteConnectionManagerService {
    Set<ProcessOrchestratorRemoteConnection> getRemoteConnections();
    ProcessOrchestratorRemoteConnection saveRemoteConnection(ProcessOrchestratorRemoteConnection connection);
    ProcessOrchestratorRemoteConnection deleteRemoteConnection(ProcessOrchestratorRemoteConnection connection);
}
