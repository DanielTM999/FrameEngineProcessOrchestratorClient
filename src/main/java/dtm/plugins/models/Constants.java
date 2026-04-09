package dtm.plugins.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public final class Constants {
    public static final String PROCESS_ORCHESTRATOR_LOCAL_KEY = "__ProcessLocalOrchestrator__"+ UUID.randomUUID();
    public static final String PROCESS_ORCHESTRATOR_REMOTE_KEY = "__ProcessRemoteOrchestrator__"+ UUID.randomUUID();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
}
