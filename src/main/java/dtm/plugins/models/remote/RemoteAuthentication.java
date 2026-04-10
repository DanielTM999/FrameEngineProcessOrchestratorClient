package dtm.plugins.models.remote;

import java.time.Instant;

public interface RemoteAuthentication {
    boolean isAuthenticated();
    String getToken();
    Instant getExpirationDateTime();
    String getBaseUrl();
}
