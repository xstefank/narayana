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
package io.narayana.lra.client.internal.proxy.nonjaxrs;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.participant.InvalidLRAParticipantDefinitionException;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static io.narayana.lra.LRAConstants.COMPENSATE;
import static io.narayana.lra.LRAConstants.COMPLETE;
import static io.narayana.lra.LRAConstants.FORGET;
import static io.narayana.lra.LRAConstants.STATUS;

/**
 * Keeps references to individual non-JAX-RS paraticipant methods in
 * single LRA participant class.
 */
public class LRAParticipant {

    private Class<?> id;
    private Method compensateMethod;
    private Method completeMethod;
    private Method statusMethod;
    private Method forgetMethod;

    LRAParticipant(Class<?> id) {
        this.id = id;
    }

    Class<?> getId() {
        return id;
    }

    Response compensate(URI lraId, URI parentId) {
        return invokeParticipantMethod(compensateMethod, lraId, parentId, COMPENSATE);
    }

    Response complete(URI lraId, URI parentId) {
        return invokeParticipantMethod(completeMethod, lraId, parentId, COMPLETE);
    }

    Response status(URI lraId, URI parentId) {
        return invokeParticipantMethod(statusMethod, lraId, parentId, STATUS);
    }

    Response forget(URI lraId, URI parentId) {
        return invokeParticipantMethod(forgetMethod, lraId, parentId, FORGET);
    }

    @SuppressWarnings("unchecked")
    private Response invokeParticipantMethod(Method method, URI lraId,
                                             URI parentId, String type) {
        Object participant = CDI.current().select(id).get();
        Object result = null;

        switch (method.getParameterCount()) {
            case 0:
                result = invokeMethod(type, method, participant);
                break;
            case 1:
                result = invokeMethod(type, method, participant, lraId);
                break;
            case 2:
                result = invokeMethod(type, method, participant, lraId, parentId);
                break;
            default:
                throw new InvalidLRAParticipantDefinitionException(
                    method.toGenericString() + ": invalid number of arguments: " + method.getParameterCount());
        }

        Response.ResponseBuilder builder = Response.status(Response.Status.OK);

        if (result instanceof CompletionStage) {
            // TODO optimize this with coordinator
            // wait for result the same way as JAX-RS filters do
            try {
                result = ((CompletionStage) result).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        if (result == null) {
            builder.status(Response.Status.NOT_FOUND);
        } else if (method.getReturnType().equals(Void.TYPE)) {
            // void return type and no exception was thrown
            builder.entity(type.equals(COMPLETE) ? ParticipantStatus.Completed.name() : ParticipantStatus.Compensated.name());
        } else if (result instanceof ParticipantStatus) {
            builder.entity(((ParticipantStatus) result).name());
        } else if (result instanceof Response) {
            return (Response) result;
        } else {
            throw new InvalidLRAParticipantDefinitionException(
                method.toGenericString() + ": invalid type of returned object: " + result.getClass());
        }

        return builder.build();
    }

    private Object invokeMethod(String type, Method method, Object o, Object... args) {
        try {
            return method.invoke(o, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof WebApplicationException) {
                return ((WebApplicationException) e.getTargetException()).getResponse();
            }

            // other exceptions differ based on the participant method type
            if (type.equals(COMPENSATE)) {
                return ParticipantStatus.FailedToCompensate.name();
            } else if (type.equals(COMPLETE)) {
                return ParticipantStatus.FailedToComplete.name();
            } else {
                // @Status and @Forget should return HTTP 500
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    void setCompensateMethod(Method compensateMethod) {
        this.compensateMethod = compensateMethod;
    }

    void setCompleteMethod(Method completeMethod) {
        this.completeMethod = completeMethod;
    }

    void setStatusMethod(Method statusMethod) {
        this.statusMethod = statusMethod;
    }

    void setForgetMethod(Method forgetMethod) {
        this.forgetMethod = forgetMethod;
    }

    Method getCompensateMethod() {
        return compensateMethod;
    }

    Method getCompleteMethod() {
        return completeMethod;
    }

    Method getStatusMethod() {
        return statusMethod;
    }

    Method getForgetMethod() {
        return forgetMethod;
    }

    public void augmentTerminationURIs(Map<String, String> terminateURIs, URI baseUri) {
        String baseURI = UriBuilder.fromUri(baseUri)
            .path(LRAParticipantResource.RESOURCE_PATH)
            .path(id.getName())
            .build().toASCIIString();

        if (!terminateURIs.containsKey(COMPLETE) && completeMethod != null) {
            terminateURIs.put(COMPLETE, getURI(baseURI, COMPLETE));
        }

        if (!terminateURIs.containsKey(COMPENSATE) && compensateMethod != null) {
            terminateURIs.put(COMPENSATE, getURI(baseURI, COMPENSATE));
        }

        if (!terminateURIs.containsKey(STATUS) && statusMethod != null) {
            terminateURIs.put(STATUS, getURI(baseURI, STATUS));
        }

        if (!terminateURIs.containsKey(FORGET) && forgetMethod != null) {
            terminateURIs.put(FORGET, getURI(baseURI, FORGET));
        }
    }

    private String getURI(String baseURI, String path) {
        return String.format("%s/%s", baseURI, path);
    }
}
