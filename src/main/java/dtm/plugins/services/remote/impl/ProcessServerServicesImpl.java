package dtm.plugins.services.remote.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.apps.annotations.PluginReference;
import dtm.apps.core.extension.Context;
import dtm.apps.exceptions.DisplayException;
import dtm.di.annotations.aop.DisableAop;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.dto.ProcessDTO;
import dtm.plugins.context.LockContext;
import dtm.plugins.models.Constants;
import dtm.plugins.models.remote.ProcessRemoteServer;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.models.remote.impl.ProcessRemoteServerImpl;
import dtm.plugins.exceptions.DisconnectedException;
import dtm.plugins.models.remote.res.ErrorResponse;
import dtm.plugins.services.remote.ProcessServerServices;
import dtm.plugins.services.remote.attacher.ProcessAttachListenerService;
import dtm.plugins.services.remote.attacher.impl.SimpleProcessAttach;
import dtm.plugins.services.remote.base.BaseHttpRemoteService;
import dtm.request_actions.exceptions.HttpException;
import dtm.request_actions.http.simple.core.HttpAction;
import dtm.request_actions.http.simple.core.result.HttpRequestResult;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DisableAop
@RequiredArgsConstructor
@PluginReference(id = "ProcessServerServices", singleton = true)
public class ProcessServerServicesImpl extends BaseHttpRemoteService implements ProcessServerServices {
    private final HttpAction httpAction;
    private final Context context;

    @Override
    public List<ProcessRemoteServer> getAllProcesses() {
        RemoteAuthentication authentication = getRemoteAuthentication();
        String url = getUrlFormated(authentication.getBaseUrl(), "/process");

        HttpRequestResult<JsonNode> requestResult = httpAction.get(url, getHeaderAuthentication(authentication, true));
        JsonNode rootNode = requestResult.getBody(JsonNode.class).orElseThrow(DisconnectedException::new);
        JsonNode processesNode = rootNode.get("proceses");
        if (processesNode == null || !processesNode.isArray()) {
            return List.of();
        }


        return new ArrayList<>(Constants.OBJECT_MAPPER.convertValue(
                processesNode,
                new TypeReference<List<ProcessRemoteServerImpl>>(){}
        ));
    }

    @Override
    public ProcessRemoteServer start(String processId) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/start/"+processId);
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.post(url, "", headers);

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        } catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (Exception e) {
            throw new DisplayException(e.getMessage()).title("Erro ao iniciar o processo");
        }
    }

    @Override
    public ProcessRemoteServer stop(String processId) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/kill/"+processId);
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.post(url, "", headers);

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        } catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (Exception e) {
            throw new DisplayException(e.getMessage()).title("Erro ao iniciar o processo");
        }
    }

    @Override
    public ProcessRemoteServer restart(String processId) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/restart/"+processId);
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.post(url, "", headers);

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        } catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (Exception e) {
            throw new DisplayException(e.getMessage()).title("Erro ao reiniciar o processo");
        }
    }

    @Override
    public ProcessRemoteServer save(ProcessDTO processDTO) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/create");
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.post(url, processDTO, headers);

            if(!requestResult.isRequestSucess()){
                ErrorResponse errorResponse = requestResult.ifErrorGet(ErrorResponse.class).orElse(new ErrorResponse(500, "Ocorreu um erro inesperado", List.of("Ocorreu um erro inesperado")));

                StringBuilder html = new StringBuilder("<html><body>");

                boolean errorInList = errorResponse.getErrorsList() != null
                        && errorResponse.getErrorsList().contains(errorResponse.getError());

                if (!errorInList) {
                    html.append("<b>").append(errorResponse.getError()).append("</b><br><br>");
                }

                html.append("<ul>");
                for (String err : errorResponse.getErrorsList()) {
                    html.append("<li>").append(err).append("</li>");
                }
                html.append("</ul></body></html>");

                throw new DisplayException(html.toString()).title("Erro ao Criar Processo");
            }

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        } catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (DisplayException displayException){
            throw displayException;
        } catch (Exception e) {
            throw new DisplayException(e.getMessage(), e)
                    .title("Erro ao finalizar a criação do processo")
                    .stackInMessage();
        }
    }

    @Override
    public ProcessRemoteServer save(String processId, ProcessDTO processDTO) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/update/"+processId);
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.put(url, processDTO, headers);

            if(!requestResult.isRequestSucess()){
                ErrorResponse errorResponse = requestResult.ifErrorGet(ErrorResponse.class).orElse(new ErrorResponse(500, "Ocorreu um erro inesperado", List.of("Ocorreu um erro inesperado")));

                StringBuilder html = new StringBuilder("<html><body>");

                boolean errorInList = errorResponse.getErrorsList() != null
                        && errorResponse.getErrorsList().contains(errorResponse.getError());

                if (!errorInList) {
                    html.append("<b>").append(errorResponse.getError()).append("</b><br><br>");
                }

                html.append("<ul>");
                for (String err : errorResponse.getErrorsList()) {
                    html.append("<li>").append(err).append("</li>");
                }
                html.append("</ul></body></html>");

                throw new DisplayException(html.toString()).title("Erro ao Atualizar Processo");
            }

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        }catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (DisplayException displayException){
            throw displayException;
        } catch (Exception e) {
            throw new DisplayException(e.getMessage(), e)
                    .title("Erro ao finalizar a atualização do processo")
                    .stackInMessage();
        }
    }

    @Override
    public ProcessRemoteServer delete(String processId) {
        try{
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/"+processId);
            Map<String, String> headers = getHeaderAuthentication(authentication, true);
            HttpRequestResult<ProcessRemoteServerImpl> requestResult = httpAction.delete(url, headers);

            if(!requestResult.isRequestSucess()){
                ErrorResponse errorResponse = requestResult.ifErrorGet(ErrorResponse.class).orElse(new ErrorResponse(500, "Ocorreu um erro inesperado", List.of("Ocorreu um erro inesperado")));

                StringBuilder html = new StringBuilder("<html><body>");
                boolean errorInList = errorResponse.getErrorsList() != null
                        && errorResponse.getErrorsList().contains(errorResponse.getError());

                if (!errorInList) {
                    html.append("<b>").append(errorResponse.getError()).append("</b><br><br>");
                }

                html.append("<ul>");
                for (String err : errorResponse.getErrorsList()) {
                    html.append("<li>").append(err).append("</li>");
                }
                html.append("</ul></body></html>");

                throw new DisplayException(html.toString()).title("Erro ao remover Processo");
            }

            return requestResult.getBody(ProcessRemoteServerImpl.class).orElseThrow();
        }catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (DisplayException displayException){
            throw displayException;
        } catch (Exception e) {
            throw new DisplayException(e.getMessage(), e)
                    .title("Erro ao remover processo")
                    .stackInMessage();
        }
    }

    @Override
    public ProcessAttachListenerService newAttachProcess(ProcessDefinition processDefinition, boolean sendHistory) {
        return new SimpleProcessAttach(httpAction, processDefinition, this::getRemoteAuthentication, sendHistory);
    }

    @Override
    public void writeToStdin(String processId, String input) {
        try {
            RemoteAuthentication authentication = getRemoteAuthentication();
            String url = getUrlFormated(authentication.getBaseUrl(), "/process/" + processId + "/stdin");
            Map<String, String> headers = getHeaderAuthentication(authentication, false);
            httpAction.post(url, input, headers);
        } catch (HttpException e) {
            throw new DisplayException("Erro ao se comunicar com o servidor remoto").title("Conexão Interrompida");
        } catch (Exception e) {
            throw new DisplayException(e.getMessage()).title("Erro ao enviar input");
        }
    }


    private RemoteAuthentication getRemoteAuthentication() {
        return context
                .getMemory(LockContext.INSTANCE)
                .get(Constants.PROCESS_ORCHESTRATOR_REMOTE_KEY);
    }


}
