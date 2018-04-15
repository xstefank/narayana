package io.narayana.lra.coordinator.execution;

import io.narayana.lra.client.Current;
import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.coordinator.api.Coordinator;
import io.narayana.lra.coordinator.domain.service.LRAService;
import io.narayana.lra.logging.LRALogger;
import io.narayana.lra.coordinator.util.Util;
import org.jboss.logging.Logger;
import org.xstefank.lra.definition.Action;
import org.xstefank.lra.definition.LRADefinition;
import org.xstefank.lra.definition.rest.RESTAction;
import org.xstefank.lra.execution.AbstractLRAExecutor;
import org.xstefank.lra.execution.model.LRAResult;
import org.xstefank.lra.model.ActionResult;
import org.xstefank.lra.model.LRAData;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URL;

import static io.narayana.lra.client.NarayanaLRAClient.COORDINATOR_PATH_NAME;

@ApplicationScoped
public class RESTLRAExecutor extends AbstractLRAExecutor {

    private static final Logger log = Logger.getLogger(RESTLRAExecutor.class);

    @Inject
    private Coordinator coordinator;

    @Inject
    private LRAService lraService;

    @Override
    public URL startLRA(LRADefinition lra) {
        //TODO check if coordinator.startLRA works
        URL parentLRAUrl = null;

        LRALogger.logger.info("LRA definition received " + lra);

        if (lra.getParentLRA() != null && !lra.getParentLRA().isEmpty())
            parentLRAUrl = NarayanaLRAClient.lraToURL(lra.getParentLRA(), "Invalid parent LRA id");

        String coordinatorUrl = String.format("%s%s", "http://lra-coordinator:8080/", COORDINATOR_PATH_NAME);
        URL lraId = lraService.startLRA(coordinatorUrl, parentLRAUrl, lra.getClientId(), lra.getTimeout());

//        if (parentLRAUrl != null) {
//            // register with the parentLRA as a participant
//            Client client = ClientBuilder.newClient();
//            String compensatorUrl = String.format("%s/%s", coordinatorUrl,
//                    NarayanaLRAClient.encodeURL(lraId, "Invalid parent LRA id"));
//            Response response;
//
//            if (lraService.hasTransaction(parentLRAUrl))
//                response = coordinator.joinLRAViaBody(parentLRAUrl.toExternalForm(), lra.getTimeout(), null, compensatorUrl);
//            else
//                response = client.target(lra.getParentLRA()).request().put(Entity.text(compensatorUrl));
//
//            if (response.getStatus() != Response.Status.OK.getStatusCode())
//                return response;
//        }

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
        } catch (Exception ex) {
            //TODO change to MalformedURL
            return ActionResult.failure(ex);
        }

        log.info("linkHeader - " + linkHeader);

        final String recoveryUrlBase = String.format("http://%s/%s/",
                "lra-coordinator:8080", NarayanaLRAClient.RECOVERY_COORDINATOR_PATH_NAME);


        lraService.joinLRA(new StringBuilder(), data.getLraId(), 0L,
                null, linkHeader, recoveryUrlBase, null);

        return super.executeAction(action, data);
    }

    @Override
    protected void compensateLRA(LRAResult lraResult) {
        log.info("compensating LRA: " + lraResult.getLraId());
        coordinator.cancelLRA(lraResult.getLraId().toString());
    }

    @Override
    protected void completeLRA(LRAResult lraResult) {
        log.info("completing LRA: " + lraResult.getLraId());
        coordinator.closeLRA(lraResult.getLraId().toString());
    }
}
