package dtm.plugins.models.remote.connection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessOrchestratorRemoteConnection {
    private long id;
    private String connectionName;
    private String privateKeyConnection;
    private String url;
}
