package io.narayana.lra.spi;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.tck.service.spi.LraRecoveryService;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;

@ApplicationScoped
public class NarayanaLraRecovery implements LraRecoveryService {

    @Override
    public void triggerRecovery() {
        doTriggerRecovery(System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_HOST_KEY),
            Integer.parseInt(System.getProperty(NarayanaLRAClient.LRA_COORDINATOR_PORT_KEY)));
    }

    @Override
    public void triggerRecovery(URI lraId) {
        doTriggerRecovery(lraId.getHost(), lraId.getPort());
    }

    private void doTriggerRecovery(String host, int port) {
        // trigger a recovery scan
        Client recoveryCoordinatorClient = ClientBuilder.newClient();

        try {
            String recoveryCoordinatorUrl = String.format("http://%s:%d/%s/recovery",
                host, port, LRAConstants.RECOVERY_COORDINATOR_PATH_NAME);
            WebTarget recoveryTarget = recoveryCoordinatorClient.target(URI.create(recoveryCoordinatorUrl));

            // send the request to the recovery coordinator
            Response response = recoveryTarget.request().get();
            LRALogger.logger.error("XXXXXX " + response.readEntity(String.class));
            response.close();
        } finally {
            recoveryCoordinatorClient.close();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
