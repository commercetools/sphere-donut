package pactas;

import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pactas.models.Authorization;
import pactas.models.PactasContract;
import pactas.models.PactasCustomer;
import play.Configuration;
import play.libs.ws.WSAPI;
import play.libs.ws.WSAuthScheme;
import play.mvc.Http;
import utils.JsonUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletionStage;

@Singleton
public class PactasImpl implements Pactas {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pactas.class);

    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final String authUrl;
    private final String clientId;
    private final String clientSecret;
    private final String customerEndpoint;
    private final String contractEndpoint;
    private final WSAPI wsApi;
    private final CompletionStage<Authorization> authorizationPromise;

    @Inject
    public PactasImpl(final Configuration configuration, final WSAPI wsApi) {
        this.authUrl = configuration.getString("pactas.auth");
        this.clientId = configuration.getString("pactas.clientId");
        this.clientSecret = configuration.getString("pactas.clientSecret");
        this.customerEndpoint = configuration.getString("pactas.api.customers");
        this.contractEndpoint = configuration.getString("pactas.api.contracts");
        this.wsApi = wsApi;
        authorizationPromise = authenticate();
    }

    public CompletionStage<Authorization> fetchAuthorization() {
        return authorizationPromise;
    }

    @Override
    public CompletionStage<PactasContract> fetchContract(final String contractId) {
        String endpointUrl = contractEndpoint + contractId;
        return executeRequest(endpointUrl, PactasContract.class);
    }

    @Override
    public CompletionStage<PactasCustomer> fetchCustomer(final String customerId) {
        String endpointUrl = customerEndpoint + customerId;
        return executeRequest(endpointUrl, PactasCustomer.class);
    }

    /**
     * Sends the request to the provided Pactas endpoint and parses the response.
     * @param endpointUrl endpoint URL to Pactas.
     * @param clazz class against which the response will be deserialized.
     * @return Promise of response deserialized against the given class.
     * @throws PactasException when the request failed or the response could not be parsed.
     */
    private <T> CompletionStage<T> executeRequest(final String endpointUrl, final Class<T> clazz) {
        LOGGER.debug("Fetching {}", endpointUrl);
        return authorizationPromise.thenCompose(authorization -> wsApi.url(endpointUrl)
                .setContentType(CONTENT_TYPE)
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorization.getAccessToken())
                .get().thenApply(response -> {
                    if (response.getStatus() == Http.Status.OK) {
                        return JsonUtils.readObject(clazz, response.getBody());
                    } else {
                        throw new PactasException(response.getStatus(), response.getBody());
                    }
                }));
    }

    /**
     * Sends the request to the auth Pactas endpoint and parses the access token.
     * @return Promise of the access token.
     */
    private CompletionStage<Authorization> authenticate() {
        LOGGER.debug("Fetching pactas access token");
        return wsApi.url(authUrl)
                .setContentType(CONTENT_TYPE)
                .setAuth(clientId, clientSecret, WSAuthScheme.BASIC)
                .post("grant_type=client_credentials").thenApply(response -> {
                    if (response.getStatus() == Http.Status.OK) {
                        final Authorization authorization = JsonUtils.readObject(Authorization.class, response.getBody());
                        LOGGER.debug("Pactas token received {}", authorization);
                        return authorization;
                    } else {
                        throw new PactasException(response.getStatus(), response.getBody());
                    }
                });
    }
}
