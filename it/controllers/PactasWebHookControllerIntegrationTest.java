package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.AbstractModule;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.products.ProductProjection;
import org.junit.Before;
import org.junit.Test;
import pactas.models.webhooks.WebhookAccountCreated;
import play.Application;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.test.WithServer;
import services.PactasWebHookControllerAction;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.OK;
import static utils.JsonUtils.readJsonFromResource;

public class PactasWebHookControllerIntegrationTest extends WithServer {

    private static final JsonNode WEBHOOK = readJsonFromResource("pactas-webhook-account.json");

    private final ProductProjection product = mock(ProductProjection.class);
    private final BlockingSphereClient sphereClient = mock(BlockingSphereClient.class);
    private final PactasWebHookControllerAction controllerAction = mock(PactasWebHookControllerAction.class);

    @Before
    public void setUp() throws Exception {
        when(controllerAction.placeOrder(any())).thenReturn(completedFuture(null));
    }

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder()
                .overrides(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ProductProjection.class).toInstance(product);
                        bind(BlockingSphereClient.class).toInstance(sphereClient);
                        bind(PactasWebHookControllerAction.class).toInstance(controllerAction);
                    }
                }).build();
    }

    @Test
    public void testCreateOrderFromSubscription() throws Exception {
        try (final WSClient wsClient = WS.newClient(testServer.port())) {
            final WSResponse wsResponse = wsClient.url("/order/execute")
                    .post(WEBHOOK.toString())
                    .toCompletableFuture().get();
            assertThat(wsResponse.getStatus()).isEqualTo(OK);
            verify(controllerAction).placeOrder(new WebhookAccountCreated("58e3a4af14aa010f3864eda1"));
        }
    }
}
