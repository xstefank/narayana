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

import io.narayana.lra.client.internal.proxy.nonjaxrs.jandex.DotNames;
import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.participant.InvalidLRAParticipantDefinitionException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * This CDI extension collects all LRA participants that contain
 * one or more non-JAX-RS participant methods. The collected classes are stored
 * in {@link LRAParticipantRegistry}.
 */
public class LRACDIExtension implements Extension {

    private ClassPathIndexer classPathIndexer = new ClassPathIndexer();
    private final Map<String, LRAParticipant> participants = new HashMap<>();

    public void observe(@Observes AfterBeanDiscovery event, BeanManager beanManager) throws IOException, ClassNotFoundException {
        Index index = classPathIndexer.createIndex();

        List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple("javax.ws.rs.Path"));

        for (AnnotationInstance annotation : annotations) {
            ClassInfo classInfo;
            AnnotationTarget target = annotation.target();

            if (target.kind().equals(AnnotationTarget.Kind.CLASS)) {
                classInfo = target.asClass();
            } else if (target.kind().equals(AnnotationTarget.Kind.METHOD)) {
                classInfo = target.asMethod().declaringClass();
            } else {
                continue;
            }
            
            LRAParticipant participant = processJavaClass(classInfo);
            if (participant != null) {
                Class<?> participantClass = participant.getParticipantClass();
                participants.put(participantClass.getName(), participant);
                Set<Bean<?>> participantBeans = beanManager.getBeans(participantClass, new AnnotationLiteral<Any>() {});
                if (participantBeans.isEmpty()) {
                    // resource is not registered as managed bean so register a custom managed instance
                    try {
                        participant.setInstance(participantClass.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        LRALogger.i18NLogger.error_cannotProcessParticipant(e);
                    }
                }
            }
        }

        event.addBean()
            .read(beanManager.createAnnotatedType(LRAParticipantRegistry.class))
            .beanClass(LRAParticipantRegistry.class)
            .scope(ApplicationScoped.class)
            .createWith(context -> new LRAParticipantRegistry(participants));
    }

    /**
     * Collects all non-JAX-RS participant methods in the defined Java class
     *
     * @param javaClass a class to be scanned
     * @return Collected methods wrapped in {@link LRAParticipant} class or null if no non-JAX-RS methods have been found
     */
    private LRAParticipant processJavaClass(ClassInfo classInfo) {
        if (isNotLRAParticipant(classInfo)) {
            return null;
        }

        Class<?> clazz;
        if ((clazz = loadParticipantClass(classInfo)) == null) {
            return null;
        } 

        LRAParticipant participant = new LRAParticipant(clazz);
        
        // process participant annotations
        Map<DotName, List<AnnotationInstance>> annotations = classInfo.annotations();
        List<AnnotationInstance> annotationInstances = annotations.get(DotNames.COMPENSATE);
        if (!annotationInstances.isEmpty()) {
            AnnotationInstance annotationInstance = annotationInstances.get(0);
            MethodInfo methodInfo = annotationInstance.target().asMethod();
            verifyParticipantMethodSignature(methodInfo);
            Method method = getMethod(methodInfo, clazz);
        }

        Arrays.stream(javaClass.getDeclaredMethods()).forEach(m -> processParticipantMethod(m, participant));

        boolean shouldProcess = participant.getCompensateMethod() != null ||
            participant.getCompleteMethod() != null ||
            participant.getStatusMethod() != null ||
            participant.getForgetMethod() != null;

        return shouldProcess ? participant : null;
    }

    private void verifyParticipantMethodSignature(MethodInfo methodInfo) {
        if (isJaxRsMethod(methodInfo)) {
            return;
        }

        verifyReturnType(methodInfo);

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length > 2) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Participant method cannot have more than 2 arguments"));
        }

        if (parameterTypes.length > 0 && !parameterTypes[0].equals(URI.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[0]));
        }

        if (parameterTypes.length > 1 && !parameterTypes[1].equals(URI.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[1]));
        }
    }

    private Method getMethod(MethodInfo methodInfo, Class<?> clazz) {
        try {
            switch (methodInfo.parameters().size()) {
                case 2:
                    return clazz.getMethod(methodInfo.name(), String.class, String.class);
                case 1:
                    return clazz.getMethod(methodInfo.name(), String.class);
                case 0:
                    return clazz.getMethod(methodInfo.name());
                default:
                    return null;
            }
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Class<?> loadParticipantClass(ClassInfo classInfo) {
        try {
            return getClass().getClassLoader().loadClass(classInfo.name().toString());
        } catch (ClassNotFoundException e) {
            LRALogger.i18NLogger.error_cannotLoadParticipantClass(classInfo.name().toString(), e);
        }
        
        return null;
    }

    private boolean isNotLRAParticipant(ClassInfo classInfo) {
        Map<DotName, List<AnnotationInstance>> annotations = classInfo.annotations();
        return !annotations.containsKey(DotNames.LRA) || !annotations.containsKey(DotNames.COMPENSATE); 
    }

    /**
     * Process participant method for non JAX-RS related processing
     * defined by the specification and verify its signature
     *
     * @param method method to be processed
     * @throws InvalidLRAParticipantDefinitionException if error is detected
     */
    private void processParticipantMethod(Method method, LRAParticipant participant) {

        if (isJaxRsMethod(method)) {
            return;
        }

        if (!setParticipantAnnotation(method, participant)) {
            return;
        }

        verifyReturnType(method);

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length > 2) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Participant method cannot have more than 2 arguments"));
        }

        if (parameterTypes.length > 0 && !parameterTypes[0].equals(URI.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[0]));
        }

        if (parameterTypes.length > 1 && !parameterTypes[1].equals(URI.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                method.toGenericString(), "Invalid argument type in LRA participant method: " + parameterTypes[1]));
        }
    }

    private boolean isJaxRsMethod(MethodInfo methodInfo) {
        return methodInfo.annotations().stream()
            .map(AnnotationInstance::name)
            .anyMatch(name -> name.equals(DotNames.GET) ||
                name.equals(DotNames.POST) ||
                name.equals(DotNames.PUT) ||
                name.equals(DotNames.DELETE) ||
                name.equals(DotNames.HEAD) ||
                name.equals(DotNames.OPTIONS));
    }

    private boolean setParticipantAnnotation(Method method, LRAParticipant participant) {
        if (method.isAnnotationPresent(Compensate.class)) {
            participant.setCompensateMethod(method);
            return true;
        } else if (method.isAnnotationPresent(Complete.class)) {
            participant.setCompleteMethod(method);
            return true;
        } else if (method.isAnnotationPresent(Status.class)) {
            participant.setStatusMethod(method);
            return true;
        } else if (method.isAnnotationPresent(Forget.class)) {
            participant.setForgetMethod(method);
            return true;
        }

        return false;
    }

    private void verifyReturnType(MethodInfo methodInfo) {
        org.jboss.jandex.Type type = methodInfo.returnType();

        // no way how to get this information from method info
        if (returnType.equals(CompletionStage.class)) {
            Type genericReturnType = method.getGenericReturnType();
            ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
            verifyReturnType((Class<?>) parameterizedType.getActualTypeArguments()[0], method.toGenericString(), true);
            return;
        }

        verifyReturnType(returnType, method.toGenericString(), false);
    }

    private void verifyReturnType(Class<?> returnType, String methodGenericString, boolean inCompletionStage) {
        if (!returnType.equals(Void.TYPE) &&
            !returnType.equals(Void.class) &&
            !returnType.equals(ParticipantStatus.class) &&
            !returnType.equals(Response.class)) {
            throw new InvalidLRAParticipantDefinitionException(String.format("%s: %s",
                methodGenericString, "Invalid return type for participant method "
                    + (inCompletionStage ? "CompletionStage<" + returnType + ">" : returnType)));
        }
    }
}
