package dtm.plugins.models.remote.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dtm.plugins.models.remote.RemoteAuthentication;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Data
@Slf4j
public class RemoteAuthenticationImpl implements RemoteAuthentication {
    private boolean authenticated;
    private String token;

    @JsonProperty("expirationInstantTimZone")
    private String expirationDateRaw;

    @JsonProperty("expirationInstant")
    private String expirationInstantRaw;

    @JsonIgnore
    private Instant cachedExpiration;

    @JsonIgnore
    private String baseUrl;


    @Override
    public boolean isAuthenticated() {
        Instant expiration = getExpirationDateTime();
        if (!authenticated || expiration == null) return false;
        return Instant.now().isBefore(expiration);
    }

    @Override
    @JsonIgnore
    public Instant getExpirationDateTime() {
        if (cachedExpiration == null && expirationInstantRaw != null && !expirationInstantRaw.isBlank()) {
            try {
                cachedExpiration = Instant.parse(expirationInstantRaw);
            } catch (DateTimeParseException e) {
                log.error("Erro ao converter expirationInstant: {}", expirationInstantRaw, e);
                return null;
            }
        }
        return cachedExpiration;
    }

    @Override
    @JsonIgnore
    public String getBaseUrl() {
        return this.baseUrl;
    }

}
