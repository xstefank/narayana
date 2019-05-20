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

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.participant.InvalidLRAParticipantDefinitionException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

/**
 * This CDI extension collects all LRA participants that contain
 * one or more non-JAX-RS participant methods. The collected classes are stored
 * in {@link LRAParticipantRegistry}.
 */
public class LRACDIExtension implements Extension {

    private String currentMethodGenericString;

    private LRAParticipantRegistry participantRegistry = LRAParticipantRegistry.getInstance();

    public void observe(@Observes @WithAnnotations(Path.class) ProcessAnnotatedType<?> type) {
        Class<?> javaClass = type.getAnnotatedType().getJavaClass();

        if (isNotLRAParticipant(javaClass)) {
            return;
        }

        LRAParticipant participant = new LRAParticipant(javaClass);

        Arrays.stream(javaClass.getDeclaredMethods()).forEach(m -> processParticipantMethod(m, participant));

        boolean shouldRegister = participant.getCompensateMethod() != null ||
            participant.getCompleteMethod() != null ||
            participant.getStatusMethod() != null ||
            participant.getForgetMethod() != null;

        if (shouldRegister) {
            participantRegistry.registerParticipant(participant);
        }
    }

    private boolean isNotLRAParticipant(Class<?> javaClass) {
        return Arrays.stream(javaClass.getDeclaredMethods()).noneMatch(m -> m.isAnnotationPresent(LRA.class));
    }

    /**
     * Process participant method for non JAX-RS related processing
     * defined by the specification and verify its signature
     * @param method method to be processed
     *
     * @throws InvalidLRAParticipantDefinitionException if error is detected
     */
    private void processParticipantMethod(Method method, LRAParticipant participant) {

        if (method.isAnnotationPresent(Path.class)) {
            return;
        }

        Class<? extends Annotation> participantAnnotation = getParticipantAnnotation(method);

        if (participantAnnotation == null) {
            return;
        }

        currentMethodGenericString = method.toGenericString();

        Class<?> returnType = method.getReturnType();
        verifyReturnType(returnType);

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length > 2) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                currentMethodGenericString, "Participant method cannot have more than 2 arguments"));
        }

        switch (parameterTypes.length) {
            case 2:
                verifyParamType(parameterTypes[1]);
            case 1:
                verifyParamType(parameterTypes[0]);
            case 0:
                break;
            default:
        }

        if (participantAnnotation.equals(Compensate.class)) {
            participant.setCompensateMethod(method);
        } else if (participantAnnotation.equals(Complete.class)) {
            participant.setCompleteMethod(method);
        } else if (participantAnnotation.equals(Status.class)) {
            participant.setStatusMethod(method);
        } else if (participantAnnotation.equals(Forget.class)) {
            participant.setForgetMethod(method);
        }

    }

    private void verifyParamType(Class<?> parameterType) {
        if (!parameterType.equals(URI.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                currentMethodGenericString, "Invalid argument type in LRA participant method"));
        }
    }

    private Class<? extends Annotation> getParticipantAnnotation(Method method) {
        if (method.isAnnotationPresent(Compensate.class)) {
            return Compensate.class;
        } else if (method.isAnnotationPresent(Complete.class)) {
            return Complete.class;
        } else if (method.isAnnotationPresent(Status.class)) {
            return Status.class;
        } else if (method.isAnnotationPresent(Forget.class)) {
            return Forget.class;
        }
        return null;
    }

    private void verifyReturnType(Class<?> returnType) {
        if (!returnType.equals(Void.TYPE) &&
            !returnType.equals(ParticipantStatus.class) &&
            !returnType.equals(Response.class) &&
            !returnType.equals(CompletionStage.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                currentMethodGenericString, "Invalid return type for participant method " + returnType));
        }
    }
}
