package io.narayana.lra.coordinator.execution;

import io.narayana.lra.client.Current;
import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.coordinator.api.Coordinator;
import io.narayana.lra.coordinator.domain.service.LRAService;
import io.narayana.lra.coordinator.util.Util;
import io.narayana.lra.logging.LRALogger;
import io.narayana.lra.rest.RESTAction;
import io.narayana.lra.rest.RESTLra;
import org.jboss.logging.Logger;
import org.xstefank.lra.definition.Action;
import org.xstefank.lra.definition.LRADefinition;
import org.xstefank.lra.execution.AbstractLRAExecutor;
import org.xstefank.lra.execution.model.LRAResult;
import org.xstefank.lra.model.ActionResult;
import org.xstefank.lra.model.LRAData;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static io.narayana.lra.client.NarayanaLRAClient.COORDINATOR_PATH_NAME;

@ApplicationScoped
public class RESTLRAExecutor extends AbstractLRAExecutor {

    private static final Logger log = Logger.getLogger(RESTLRAExecutor.class);

    @Inject
    private LRAService lraService;

    @Override
    public URL startLRA(LRADefinition lra) {
        LRALogger.logger.info("LRA definition received " + lra);

        String coordinatorUrl = String.format("%s%s", Coordinator.getCoordinatorUri(), COORDINATOR_PATH_NAME);
        URL lraId = lraService.startLRA(coordinatorUrl, null, lra.getClientId(), lra.getTimelimit());

        Current.push(lraId);

        return lraId;
    }

    @Override
    protected ActionResult executeAction(Action action, LRAData data) {
        RESTAction restAction = (RESTAction) action;

        //register participant into LRA
        String linkHeader;
        try {
            linkHeader = Util.createLinkHeader(restAction.getCallbackUrl().toString());
        } catch (MalformedURLException ex) {
            return ActionResult.failure(ex);
        }

        log.info("linkHeader - " + linkHeader);

        final String recoveryUrlBase = String.format("http://%s/%s/",
                Coordinator.getCoordinatorUri(), NarayanaLRAClient.RECOVERY_COORDINATOR_PATH_NAME);


        lraService.joinLRA(new StringBuilder(), data.getLraId(), 0L,
                null, linkHeader, recoveryUrlBase, null);

        return super.executeAction(action, data);
    }

    @Override
    protected void compensateLRA(LRAResult lraResult) {
        log.info("compensating LRA: " + lraResult.getLraId());
        lraService.endLRA(lraResult.getLraId(), true, false);
        invokeCallback(lraResult);
    }

    @Override
    protected void completeLRA(LRAResult lraResult) {
        log.info("completing LRA: " + lraResult.getLraId());
        lraService.endLRA(lraResult.getLraId(), false, false);
        invokeCallback(lraResult);
    }

    private void invokeCallback(LRAResult lraResult) {
        String callbackURL = ((RESTLra) lraResult.getLraDefinition()).getCallbackURL();

        if (callbackURL != null) {
            log.info("invoking callback for " + lraResult.getLraId());
            Client client = ClientBuilder.newClient();
            URI build = UriBuilder
                    .fromUri(callbackURL)
                    .path(lraResult.isSuccess() ? "complete" : "compensate")
                    .build();

            WebTarget target = client.target(build);

            Response response = target.request()
                    .header("Long-Running-Action", lraResult.getLraId())
                    .put(Entity.json(lraResult));
        }
    }
}
