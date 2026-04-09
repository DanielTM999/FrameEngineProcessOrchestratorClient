package dtm.plugins.models.remote.res;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import dtm.plugins.models.Constants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;


@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerInitialStateEvent {
    private String timestampLocal;
    private String state;
    private String sessionId;

    @JsonProperty("timestamp")
    private String timestampString;

    @JsonIgnore
    public LocalDateTime getTimestamp() {
        if (timestampString == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestampString);
        } catch (Exception e) {
            log.warn("Erro ao fazer o parse da data: {}", timestampString, e);
            return null;
        }
    }


    public static ServerInitialStateEvent ofString(String contentString) throws JsonProcessingException {
        return Constants.OBJECT_MAPPER.readValue(contentString, ServerInitialStateEvent.class);
    }
}
