package io.narayana.lra.arquillian.resource;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path(DelayedAfterLRAResource.ROOT_PATH)
@ApplicationScoped
public class DelayedAfterLRAResource {

    public static final String ROOT_PATH = "/delayed-after-lra";
    public static final String LRA_PATH = "/lra";
    public static final String LAST_LRA_ID_PATH = "/last-after-lra-ended-lra-id";
    public static final String LAST_STATUS_PATH = "/last-after-lra-end-status";
    public static final String CLEAR_AFTER_LRA_PATH = "/clear-after-lra";

    private URI lastAfterLRAEndedLRAId = null;
    private LRAStatus lastAfterLRAEndStatus = null;

    private boolean shouldFailAfterLRA = true;

    @Inject
    LRAMetricService lraMetricService;

    @GET
    @Path(LRA_PATH)
    @LRA
    public Response lra(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId.toASCIIString()).build();
    }

    @PUT
    @Path("/after")
    @AfterLRA
    public Response afterLRA(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI endedLraId, LRAStatus status) {
        lraMetricService.incrementMetric(LRAMetricType.AfterLRA, endedLraId);
        lastAfterLRAEndedLRAId = endedLraId;
        lastAfterLRAEndStatus = status;

        return shouldFailAfterLRA ? Response.status(500).build() : Response.ok().build();
    }

    @GET
    @Path(LAST_LRA_ID_PATH)
    public String getLastLraId() {
        return lastAfterLRAEndedLRAId.toASCIIString();
    }

    @GET
    @Path(LAST_STATUS_PATH)
    public String getLastStatus() {
        return lastAfterLRAEndStatus.name();
    }

    @PUT
    @Path(CLEAR_AFTER_LRA_PATH)
    public void clearAfterLRA() {
        shouldFailAfterLRA = false;
    }
}
