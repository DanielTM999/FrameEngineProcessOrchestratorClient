package dtm.plugins.controller;

import dtm.apps.core.ApplicationProperties;
import dtm.apps.core.WindowFactory;
import dtm.apps.core.extension.PluginContext;
import dtm.apps.core.extension.PluginMemory;
import dtm.apps.exceptions.DisplayException;
import dtm.apps.views.MainFrameWindow;
import dtm.apps.views.popup.LoadingDialog;
import dtm.di.annotations.aop.DisableAop;
import dtm.manager.ProcessOrchestratorManager;
import dtm.manager.impl.ProcessOrchestratorManagerImpl;
import dtm.plugins.context.LockContext;
import dtm.plugins.models.Constants;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;
import dtm.plugins.services.remote.RemoteAuthenticationServerServices;
import dtm.plugins.services.remote.RemoteConnectionManagerService;
import dtm.plugins.views.ProcessOrchestratorConnectionManagerView;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.controllers.component.AbstractViewController;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;


@DisableAop
@RequiredArgsConstructor
public class ProcessOrchestratorConnectionManagerController extends AbstractViewController<BlockingPanel> {

    private final WindowFactory windowFactory;
    private final ApplicationProperties processOrchestratorSettingsProps;
    private final PluginContext pluginContext;
    private final Runnable onSessionCreated;

    private ProcessOrchestratorConnectionManagerView view;

    @Override
    public void onInit(BlockingPanel component) {
        super.onInit(component);
        if(component instanceof ProcessOrchestratorConnectionManagerView v) view = v;
        loadRemoteConnections();
    }


    public void onLocalConnectionClick() {
        PluginMemory pluginMemory = pluginContext.getMemory(LockContext.INSTANCE);
        if(pluginMemory.contains(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY)){
            Object object = pluginMemory.get(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY);
            if(object instanceof ProcessOrchestratorManager processOrchestratorManager){
                if(processOrchestratorManager.isAlive()){
                    setSessionLocal();
                    onSessionCreated.run();
                    return;
                }
            }
        }


        ProcessOrchestratorManager orchestrator = new ProcessOrchestratorManagerImpl();
        orchestrator.enableShutdownHookToStopAllProcesses();
        pluginMemory.put(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY, orchestrator);
        setSessionLocal();
        onSessionCreated.run();
    }

    public void onSaveConnection(ProcessOrchestratorRemoteConnection connection, ProcessOrchestratorRemoteConnection existing){
        RemoteConnectionManagerService remoteConnectionManagerService = pluginContext.getService(RemoteConnectionManagerService.class);
        if(existing == null){
            validNewConnection(connection);
            remoteConnectionManagerService.saveRemoteConnection(connection);
        }else{
            connection.setId(existing.getId());
            validUpdateConnection(connection);
            remoteConnectionManagerService.saveRemoteConnection(connection);
        }

        loadRemoteConnections();
    }

    private void loadRemoteConnections(){
        RemoteConnectionManagerService remoteConnectionManagerService = pluginContext.getService(RemoteConnectionManagerService.class);
        var connections = remoteConnectionManagerService.getRemoteConnections();

        view.renderRemoteConnections(
                connections,
                this::onRemoteConnectionClick,
                this::onEditConnectionClick,
                this::onDeleteRemoteConnection
        );
    }


    private void onEditConnectionClick(ProcessOrchestratorRemoteConnection connection){
        view.showConnectionForm(connection);
    }

    private void onDeleteRemoteConnection(ProcessOrchestratorRemoteConnection connection){
        int option = ModernDialog.builder()
                .type(ModernDialog.Type.QUESTION)
                .accentColor(new Color(220, 53, 69))
                .title("Remover conexão")
                .message("Tem certeza que deseja remover a conexão remota '"
                        + connection.getConnectionName() + "'?\n\nEssa ação não poderá ser desfeita.")
                .option("Cancelar", 0, UIManager.getColor("Button.background"), UIManager.getColor("Button.foreground"))
                .option("Remover", 1, new Color(220, 53, 69), Color.WHITE)
                .show();

        if(option == 1){
            RemoteConnectionManagerService remoteConnectionManagerService = pluginContext.getService(RemoteConnectionManagerService.class);
            remoteConnectionManagerService.deleteRemoteConnection(connection);
            loadRemoteConnections();
        }

    }

    private void onRemoteConnectionClick(ProcessOrchestratorRemoteConnection connection, MouseEvent event){
        final LoadingDialog loadingDialog = windowFactory.newWindow(LoadingDialog.class, false, new Object[]{
                pluginContext.getContextWindowAs(MainFrameWindow.class),
                "Conectando",
                "Executando autenticação com o servidor..."
        });
        RemoteAuthenticationServerServices remoteAuthenticationServerServices = pluginContext.getService(RemoteAuthenticationServerServices.class);


        Thread.ofVirtual().start(() -> {
            try{
                RemoteAuthentication remoteAuthentication = remoteAuthenticationServerServices.executeAuthentication(connection);

                if (remoteAuthentication.isAuthenticated()) {
                    PluginMemory pluginMemory = pluginContext.getMemory(LockContext.INSTANCE);
                    pluginMemory.put(Constants.PROCESS_ORCHESTRATOR_REMOTE_KEY, remoteAuthentication);
                    setSessionRemote();
                    loadingDialog.dispose();
                    onSessionCreated.run();
                }else {
                    throw new DisplayException(
                            "<html>O servidor recusou a autenticação.<br>" +
                                    "Verifique se sua chave privada está correta e autorizada.</html>"
                    ).title("Acesso Negado");
                }
            }catch (RuntimeException e){
                loadingDialog.dispose();
                throw e;
            }
        });

    }


    private void setSessionLocal(){
        PluginMemory pluginMemory = pluginContext.getMemory(LockContext.INSTANCE);
        pluginMemory.put("SessionType", "local");
    }

    private void setSessionRemote(){
        PluginMemory pluginMemory = pluginContext.getMemory(LockContext.INSTANCE);
        pluginMemory.put("SessionType", "remote");
    }


    private void validNewConnection(ProcessOrchestratorRemoteConnection connection){
        if (connection.getConnectionName() == null || connection.getConnectionName().isEmpty()) {
            throw new DisplayException(
                    "O campo 'Nome da Conexão' é obrigatório."
            ).title("Dados Incompletos");
        }

        if (connection.getUrl() == null || connection.getUrl().isEmpty()) {
            throw new DisplayException(
                    "O campo 'Endereço do Servidor' é obrigatório."
            ).title("Dados Incompletos");
        }

        String finalUrl = normalizeAddress(connection.getUrl());

        if (!isValidUrl(finalUrl)) {
            throw new DisplayException(
                    "O formato do endereço está incorreto.\nExemplos: localhost:8080, 192.168.1.10, meusite.com"
            ).title("Dados Incompletos");
        }
        connection.setUrl(finalUrl);

        String keyPath = connection.getPrivateKeyConnection();
        if (keyPath.isEmpty()) {
            throw new DisplayException(
                    "É necessário selecionar um arquivo de 'Chave Privada'."
            ).title("Dados Incompletos");
        }

        File keyFile = new File(connection.getPrivateKeyConnection());
        if (!keyFile.exists() || !keyFile.isFile()) {
            throw new DisplayException(
                    "O arquivo de chave selecionado não foi encontrado no disco:\n" + keyPath
            ).title("Dados Incompletos");
        }
    }

    private void validUpdateConnection(ProcessOrchestratorRemoteConnection connection){
        RemoteConnectionManagerService remoteConnectionManagerService = pluginContext.getService(RemoteConnectionManagerService.class);
        Set<ProcessOrchestratorRemoteConnection> processOrchestratorRemoteConnectionSet = remoteConnectionManagerService.getRemoteConnections();
        validNewConnection(connection);

        boolean containsByName = processOrchestratorRemoteConnectionSet
                .stream()
                .filter(c -> c.getId() != connection.getId())
                .anyMatch(remoteConnection -> remoteConnection.getConnectionName().equals(connection.getConnectionName()));

        if(containsByName) {
            throw new DisplayException(
                    "Já existe uma conexão remota com o nome '" + connection.getConnectionName() + "'.\n" +
                            "Escolha um nome diferente para identificar esta conexão."
            ).title("Nome já utilizado");
        }
    }


    private String normalizeAddress(String address) {
        if (address == null) return "";
        String lower = address.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "http://" + address;
        }
        return address;
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        String regex = "^(http|https)://([a-zA-Z0-9.-]+|localhost)(:[0-9]{1,5})?(/.*)?$";
        boolean regexValid = Pattern.matches(regex, url);

        if (!regexValid) {
            return false;
        }

        try {
            URI.create(url).toURL();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
