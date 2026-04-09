package dtm.plugins.models.remote.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dtm.plugins.models.remote.ProcessRemoteServerState;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessRemoteServerStateImpl implements ProcessRemoteServerState {
    private boolean running;
    private long pid;
}
