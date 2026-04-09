package dtm.plugins.services.remote.base;

import dtm.di.annotations.Inject;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.request_actions.http.simple.core.HttpAction;

import java.util.HashMap;
import java.util.Map;

public class BaseHttpRemoteService {

    @Inject
    protected HttpAction httpAction;

    protected Map<String, String> getHeaderAuthentication(RemoteAuthentication remoteAuthentication, boolean isJson){
        return new HashMap<>(){{
            put("Authorization", "Bearer "+remoteAuthentication.getToken());
            if(isJson){
                put("Content-Type", "application/json");
            }
        }};
    }

    protected String getUrlFormated(String base, String endpoint){
        if (base == null) base = "";
        if (endpoint == null) endpoint = "";

        String cleanBase = base.trim();
        String cleanEndpoint = endpoint.trim();

        if (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }

        if (cleanEndpoint.startsWith("/")) {
            cleanEndpoint = cleanEndpoint.substring(1);
        }

        return cleanBase + "/" + cleanEndpoint;
    }


}
