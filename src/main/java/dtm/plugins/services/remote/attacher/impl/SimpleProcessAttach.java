package dtm.plugins.services.remote.attacher.impl;


import dtm.manager.process.definition.ProcessDefinition;
import dtm.plugins.exceptions.DisconnectedException;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.services.remote.attacher.ProcessAttachListenerService;
import dtm.plugins.services.remote.base.BaseHttpRemoteService;
import dtm.request_actions.http.simple.core.HttpAction;
import dtm.request_actions.http.simple.core.ServerEventEmiter;
import dtm.request_actions.http.simple.core.StreamReader;
import dtm.request_actions.http.simple.core.result.HttpRequestResult;
import dtm.request_actions.http.simple.implementation.ServerEventEmiterService;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleProcessAttach extends BaseHttpRemoteService implements ProcessAttachListenerService {

    private final ExecutorService executorService;
    private final HttpAction httpAction;
    private final ProcessDefinition processDefinition;
    private final Supplier<RemoteAuthentication> authenticationGetter;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<BiConsumer<String, String>> onEvenRef = new AtomicReference<>();
    private final AtomicReference<Consumer<Throwable>> onErrorRef = new AtomicReference<>();
    private final AtomicReference<StreamReader> streamReaderRef = new AtomicReference<>();
    private final boolean sendHistory;

    public SimpleProcessAttach(
            @NonNull HttpAction httpAction,
            @NonNull ProcessDefinition processDefinition,
            Supplier<RemoteAuthentication> authenticationGetter,
            boolean sendHistory
    ){
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MainProcess-AttachListener-Worker-"+processDefinition.getProcessName());
            t.setDaemon(true);
            return t;
        });
        this.sendHistory = sendHistory;
        this.httpAction = httpAction;
        this.processDefinition = processDefinition;
        this.authenticationGetter = authenticationGetter;
    }

    @Override
    public void attach() {
        RemoteAuthentication authentication = authenticationGetter.get();
        String url = getUrlFormated(authentication.getBaseUrl(), "/process/monitor/output/"+processDefinition.getProcessId()+"?sendHistory="+sendHistory);
        Map<String, String> headers = getHeaderAuthentication(authentication, false);
        executorService.submit(() -> {
            HttpRequestResult<Object> requestResult = httpAction.get(url, headers);
            StreamReader streamReader = requestResult.getStreamReader();
            streamReaderRef.set(streamReader);
            ServerEventEmiterService.defineServerEventEmiter(streamReader, createServerEventEmiter());
        });
    }

    @Override
    public void onEvent(BiConsumer<String, String> onEvent) {
        onEvenRef.set(onEvent);
    }

    @Override
    public void onError(Consumer<Throwable> onError) {
        onErrorRef.set(onError);
    }

    @Override
    public void close() {
        closed.set(true);
        if(streamReaderRef.get() != null){
            try {
                streamReaderRef.get().close();
            } catch (Exception e) {
                if(onErrorRef.get() != null) onErrorRef.get().accept(e);
            }
        }

        if(!executorService.isShutdown()){
            executorService.shutdown();
        }
    }

    @Override
    public void interrupt() {
        onEvenRef.set(null);
        onErrorRef.set(null);
        close();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    private ServerEventEmiter createServerEventEmiter(){
        return new ServerEventEmiter() {
            @Override
            public void onEvent(String s, String s1) {
                if(onEvenRef.get() != null) onEvenRef.get().accept(s, s1);
            }

            @Override
            public void onError(Throwable throwable) {
                if(onErrorRef.get() != null) onErrorRef.get().accept(throwable);
            }

            @Override
            public void onDisconected() {
                if(onErrorRef.get() != null) onErrorRef.get().accept(new DisconnectedException());
            }
        };
    }

}
