package dtm.plugins.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserNotificationContent {
    private String title;

    @JsonRawValue
    private String content;
}