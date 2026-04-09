package dtm.plugins.services.remote.impl;

import dtm.apps.annotations.PluginReference;
import dtm.apps.core.ApplicationProperties;
import dtm.apps.core.ApplicationPropertiesManager;
import dtm.apps.exceptions.DisplayException;
import dtm.di.annotations.aop.DisableAop;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;
import dtm.plugins.services.remote.RemoteConnectionManagerService;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@DisableAop
@SuppressWarnings("unchecked")
@PluginReference(id = "RemoteConnectionManagerService", singleton = true)
public class RemoteConnectionManagerServiceImpl implements RemoteConnectionManagerService {

    private final ApplicationProperties connectionsProps;

    public RemoteConnectionManagerServiceImpl(ApplicationPropertiesManager applicationPropertiesManager) {
        this.connectionsProps = applicationPropertiesManager.getProperties("ProcessOrchestratorConnections");
    }


    @Override
    public Set<ProcessOrchestratorRemoteConnection> getRemoteConnections() {
        return connectionsProps.getCollectionProperty("connections", Set.class, ProcessOrchestratorRemoteConnection.class, ConcurrentHashMap.newKeySet());
    }

    @Override
    public ProcessOrchestratorRemoteConnection saveRemoteConnection(ProcessOrchestratorRemoteConnection connection) {
        if(connection == null) throw new DisplayException("connection is null");
        Set<ProcessOrchestratorRemoteConnection> connections = connectionsProps.getCollectionProperty("connections", Set.class, ProcessOrchestratorRemoteConnection.class, ConcurrentHashMap.newKeySet());

        if(connection.getId() <= 0){
            newConnection(connection, connections);
        }else{
            connections.removeIf(existing -> existing.getId() == connection.getId());
            connections.add(connection);
            connectionsProps.setProperty("connections", connections, true);
        }

        return connection;
    }

    @Override
    public ProcessOrchestratorRemoteConnection deleteRemoteConnection(ProcessOrchestratorRemoteConnection connection) {
        Set<ProcessOrchestratorRemoteConnection> connections = connectionsProps.getCollectionProperty("connections", Set.class, ProcessOrchestratorRemoteConnection.class, ConcurrentHashMap.newKeySet());
        connections.removeIf(e -> e.getId() == connection.getId());
        connectionsProps.setProperty("connections", connections, true);
        return connection;
    }


    private void newConnection(ProcessOrchestratorRemoteConnection connection,  Set<ProcessOrchestratorRemoteConnection> connections) {
        AtomicLong idGenerator = new AtomicLong(
                connections.stream()
                        .mapToLong(ProcessOrchestratorRemoteConnection::getId)
                        .max()
                        .orElse(0)
        );

        connection.setId(idGenerator.incrementAndGet());
        connections.add(connection);
        connectionsProps.setProperty("connections", connections, true);
    }

}
