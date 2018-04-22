package io.narayana.lra.rest;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface ParticipantCallback {

    String LRA_HTTP_HEADER = "Long-Running-Action";
    String COMPLETE = "/complete";
    String COMPENSATE = "/compensate";

    @PUT
    @Path(COMPLETE)
    @Produces(MediaType.APPLICATION_JSON)
    Response complete(@HeaderParam(LRA_HTTP_HEADER) String lraUri);

    @PUT
    @Path(COMPENSATE)
    @Produces(MediaType.APPLICATION_JSON)
    Response compensate(@HeaderParam(LRA_HTTP_HEADER) String lraUri);

}
