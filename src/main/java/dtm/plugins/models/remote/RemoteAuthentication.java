package dtm.plugins.models.remote;

import java.time.LocalDateTime;

public interface RemoteAuthentication {
    boolean isAuthenticated();
    String getToken();
    LocalDateTime getExpirationDateTime();
    String getBaseUrl();
}
