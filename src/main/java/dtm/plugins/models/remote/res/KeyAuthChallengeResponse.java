package dtm.plugins.models.remote.res;

import lombok.Data;

@Data
public class KeyAuthChallengeResponse {
    private String nonce;
    private String algorithm;
}

