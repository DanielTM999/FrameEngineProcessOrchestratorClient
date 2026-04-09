package dtm.plugins.models.remote;

public interface ProcessRemoteServerState {
    boolean isRunning();
    long getPid();
}
