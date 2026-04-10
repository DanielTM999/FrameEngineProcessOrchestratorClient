package dtm.plugins.services.remote.impl;


import dtm.apps.annotations.PluginReference;
import dtm.apps.exceptions.DisplayException;
import dtm.di.annotations.aop.DisableAop;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;
import dtm.plugins.models.remote.impl.RemoteAuthenticationImpl;
import dtm.plugins.models.remote.res.KeyAuthChallengeResponse;
import dtm.plugins.services.remote.RemoteAuthenticationServerServices;
import dtm.request_actions.exceptions.HttpException;
import dtm.request_actions.http.simple.core.HttpAction;
import dtm.request_actions.http.simple.core.result.HttpRequestResult;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@DisableAop
@RequiredArgsConstructor
@PluginReference(id = "RemoteAuthenticationServerServices", singleton = true)
public class RemoteAuthenticationServerServicesImpl implements RemoteAuthenticationServerServices {

    private final HttpAction httpAction;

    @Override
    public RemoteAuthentication executeAuthentication(ProcessOrchestratorRemoteConnection connection) {
        try{
            String baseUrl = connection.getUrl();
            String privateKeyPath = connection.getPrivateKeyConnection();
            String privateKey = getPrivateKey(privateKeyPath);
            String keyFingerprint = generatePublicKeyFingerprintFromPrivate(privateKey);
            KeyAuthChallengeResponse keyAuthChallengeResponse = getkeyAuthChallengeResponse(baseUrl, keyFingerprint);
           return authenticate(keyAuthChallengeResponse, keyFingerprint, privateKey, baseUrl);
        }catch (HttpException e){
            throw new DisplayException(
                    "<html>Não foi possível comunicar-se com o servidor remoto.<br><br>" +
                            "Possíveis causas:<br>" +
                            "• O servidor pode estar offline ou inacessível;<br>" +
                            "• O endereço IP/Porta pode estar incorreto;<br>" +
                            "• Bloqueio de Firewall.<br><br>" +
                            "Detalhe técnico: Invalid connection</html>"
            ).title("Falha na Conexão");
        } catch (Exception e) {
            return new RemoteAuthentication() {
                @Override
                public boolean isAuthenticated() {
                    return false;
                }

                @Override
                public String getToken() {
                    return "";
                }

                @Override
                public Instant getExpirationDateTime() {
                    return null;
                }

                @Override
                public String getBaseUrl() {
                    return "";
                }
            };
        }

    }

    private RemoteAuthentication authenticate(KeyAuthChallengeResponse keyAuthChallenge, String keyFingerprint, String privateKey, String baseUrl) throws Exception{
        String challengeSigh = generateNonceSigh(keyAuthChallenge.getNonce(), keyAuthChallenge.getAlgorithm(), privateKey);
        Map<String, String> headers = new HashMap<>(){{
            put("Content-Type", "application/json");
        }};
        Map<String, Object> body = new HashMap<>(){{
            put("keyFingerprint", keyFingerprint);
            put("signature", challengeSigh);
        }};

        HttpRequestResult<RemoteAuthenticationImpl> requestResult =  httpAction.post(getUrlFormated(baseUrl, "/auth/key/validate"), body, headers);

        RemoteAuthenticationImpl remoteAuthentication = requestResult.getBody(RemoteAuthenticationImpl.class).orElseThrow();
        remoteAuthentication.setBaseUrl(baseUrl);
        return remoteAuthentication;
    }

    private KeyAuthChallengeResponse getkeyAuthChallengeResponse(String baseUrl, String keyFingerprint) throws Exception{
        Map<String, String> headers = new HashMap<>(){{
            put("Content-Type", "application/json");
        }};
        Map<String, Object> body = new HashMap<>(){{
            put("keyFingerprint", keyFingerprint);
        }};

        HttpRequestResult<KeyAuthChallengeResponse> requestResult =  httpAction.post(getUrlFormated(baseUrl, "/auth/key/challenge"), body, headers);

        return requestResult.getBody(KeyAuthChallengeResponse.class).orElseThrow();
    }

    private String getUrlFormated(String base, String endpoint){
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

    private String generatePublicKeyFingerprintFromPrivate(String privateKeyStr) throws Exception{
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);

        PrivateKey privateKey = KeyFactory
                .getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        RSAPrivateCrtKey rsaPrivate = (RSAPrivateCrtKey) privateKey;

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                rsaPrivate.getModulus(),
                rsaPrivate.getPublicExponent()
        );

        PublicKey publicKey = KeyFactory
                .getInstance("RSA")
                .generatePublic(publicKeySpec);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey.getEncoded());

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private String generateNonceSigh(String nonce, String alg, String privateKeyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        Signature signature = Signature.getInstance(alg);
        signature.initSign(privateKey);
        signature.update(Base64.getDecoder().decode(nonce));

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String getPrivateKey(String privateKeyStrPath){
        Path path = Paths.get(privateKeyStrPath);
        if (!Files.exists(path)) {
            throw new DisplayException(
                    "O arquivo de chave privada não foi encontrado.\n\n" +
                            "Verifique se o caminho informado está correto e se o arquivo não foi movido ou excluído.\n\n" +
                            "Caminho: " + privateKeyStrPath
            ).title("Chave Privada não Encontrada");
        }
        try {
            return Files.readString(path)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
        } catch (Exception e) {
            throw new DisplayException(
                    "Não foi possível ler o arquivo de chave privada.\n\n" +
                            "Verifique se o arquivo não está corrompido e se a aplicação tem permissão de leitura.\n\n" +
                            "Caminho: " + privateKeyStrPath
            ).title("Erro ao Ler Chave Privada");
        }
    }

}
