package dtm.plugins.models.remote.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dtm.plugins.models.remote.RemoteAuthentication;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Data
@Slf4j
public class RemoteAuthenticationImpl implements RemoteAuthentication {
    private boolean authenticated;

    private String token;

    @JsonProperty("expirationDateTime")
    private String expirationDateRaw;

    @JsonIgnore
    private LocalDateTime cachedExpirationDateTime;

    @JsonIgnore
    private String baseUrl;


    @Override
    public boolean isAuthenticated() {
        LocalDateTime expiration = getExpirationDateTime();

        if (!authenticated || expiration == null) {
            return false;
        }

        return LocalDateTime.now().isBefore(expiration);
    }

    @Override
    @JsonIgnore
    public LocalDateTime getExpirationDateTime() {
        if (cachedExpirationDateTime == null && expirationDateRaw != null && !expirationDateRaw.isBlank()) {
            try {
                cachedExpirationDateTime = LocalDateTime.parse(expirationDateRaw);
            } catch (DateTimeParseException e) {
                log.error("Erro ao converter data de expiração: {}", expirationDateRaw, e);
                return null;
            }
        }
        return cachedExpirationDateTime;
    }

    @Override
    @JsonIgnore
    public String getBaseUrl() {
        return this.baseUrl;
    }

}
