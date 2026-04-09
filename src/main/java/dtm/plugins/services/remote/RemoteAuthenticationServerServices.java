package dtm.plugins.services.remote;

import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;

public interface RemoteAuthenticationServerServices {
    RemoteAuthentication executeAuthentication(ProcessOrchestratorRemoteConnection connection);
}
