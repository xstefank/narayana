/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package io.narayana.lra.arquillian.resource;

import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

@Path(NestedParticipant.NESTED_PARTICIPANT_PATH)
public class NestedParticipant {

    public static final String NESTED_PARTICIPANT_PATH = "nested-participant";
    public static final String ACTION_PATH = "action";

    private static final AtomicInteger forgetCounter = new AtomicInteger(0);

    @Inject
    private LRAMetricService lraMetricService;

    @GET
    @Path(ACTION_PATH)
    @LRA(LRA.Type.NESTED)
    public String doWorkInLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                              @HeaderParam(LRA.LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentId) {
        LRALogger.logger.error("LRA ID = " + lraId);
        LRALogger.logger.error("PARENT ID = " + parentId);
        lraMetricService.incrementMetric(LRAMetricType.Nested, parentId, NestedParticipant.class.getName());
        return lraId.toASCIIString();
    }

    @PUT
    @Path("compensate")
    @Compensate
    public Response compensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                               @HeaderParam(LRA.LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentId) {
        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, NestedParticipant.class.getName());
        lraMetricService.incrementMetric(LRAMetricType.Nested, parentId, NestedParticipant.class.getName());
        return Response.ok().build();
    }

    @PUT
    @Path("complete")
    @Complete
    public Response complete(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                             @HeaderParam(LRA.LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentId) {
        LRALogger.logger.error("CCCCCCCCCCCCCCCCCCCCCComplete");

        lraMetricService.incrementMetric(LRAMetricType.Completed, lraId, NestedParticipant.class.getName());
        lraMetricService.incrementMetric(LRAMetricType.Nested, parentId, NestedParticipant.class.getName());
        return Response.ok().build();
    }

    @DELETE
    @Path("forget")
    @Forget
    public Response forget(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId,
                           @HeaderParam(LRA.LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentId) {
        LRALogger.logger.error("FFFFFFFFFFFFFFFFForget ");
        lraMetricService.incrementMetric(LRAMetricType.Forget, lraId, NestedParticipant.class.getName());
        lraMetricService.incrementMetric(LRAMetricType.Nested, parentId, NestedParticipant.class.getName());

        return forgetCounter.getAndIncrement() == 0 ? Response.status(Response.Status.INTERNAL_SERVER_ERROR).build() :
            Response.ok().build();
    }
}
