package io.narayana.lra.client;

import io.narayana.lra.LRAData;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

import static io.narayana.lra.LRAConstants.CLIENT_ID_PARAM_NAME;
import static io.narayana.lra.LRAConstants.PARENT_LRA_PARAM_NAME;
import static io.narayana.lra.LRAConstants.TIMELIMIT_PARAM_NAME;

@RegisterRestClient
@Path("/")
public interface LRACoordinatorClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<LRAData> getAllLRAs();

    @POST
    @Path("/start")
    @Produces(MediaType.TEXT_PLAIN)
    Response startLRA(
        @QueryParam(CLIENT_ID_PARAM_NAME) String clientId,
        @QueryParam(TIMELIMIT_PARAM_NAME) Long timelimit,
        @QueryParam(PARENT_LRA_PARAM_NAME) String parentLRA);

    @PUT
    @Path("/{LraId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response joinLRAViaBody(
        @PathParam("LraId") String lraId,
        @QueryParam(TIMELIMIT_PARAM_NAME) long timeLimit,
        @HeaderParam("Link") String compensatorLink,
        String compensatorData);

    @PUT
    @Path("/{LraId}/close")
    @Produces(MediaType.TEXT_PLAIN)
    Response closeLRA(@PathParam("LraId") String lraId);

    @PUT
    @Path("/{LraId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    Response cancelLRA(@PathParam("LraId") String lraId);

    @PUT
    @Path("/{LraId}/remove")
    @Produces(MediaType.APPLICATION_JSON)
    Response leaveLRA(@PathParam("LraId") String lraId, String compensatorUrl);

    @GET
    @Path("/{LraId}/status")
    @Produces(MediaType.TEXT_PLAIN)
    Response getLRAStatus(@PathParam("LraId") String lraId);
}
