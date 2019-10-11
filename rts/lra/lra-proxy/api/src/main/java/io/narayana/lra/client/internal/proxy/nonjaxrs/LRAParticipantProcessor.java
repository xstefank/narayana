package io.narayana.lra.client.internal.proxy.nonjaxrs;

import io.narayana.lra.client.internal.proxy.nonjaxrs.jandex.DotNames;
import io.narayana.lra.logging.LRALogger;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LRAParticipantProcessor {

    public Map<String, LRAParticipant> process(IndexView index, Set<String> beanNames) throws ClassNotFoundException {
        Map<String, LRAParticipant> participants = new HashMap<>();

        Collection<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple("javax.ws.rs.Path"));

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

            LRAParticipant participant = getAsParticipant(classInfo);
            if (participant != null) {
                participants.put(participant.getJavaClass().getName(), participant);
                if (!beanNames.contains(participant.getJavaClass().getName())) {
                    // resource is not registered as managed bean so register a custom managed instance
                    try {
                        participant.setInstance(participant.getJavaClass().newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        LRALogger.i18NLogger.error_cannotProcessParticipant(e);
                    }
                }
            }
        }

        return participants;
    }

    /**
     * Collects all non-JAX-RS participant methods in the defined Java class
     *
     * @param classInfo a Jandex class info of the class to be scanned
     * @return Collected methods wrapped in {@link LRAParticipant} class or null if no non-JAX-RS methods have been found
     */
    private LRAParticipant getAsParticipant(ClassInfo classInfo) throws ClassNotFoundException {
        if (isNotLRAParticipant(classInfo)) {
            return null;
        }

        Class<?> javaClass = Thread.currentThread().getContextClassLoader().loadClass(classInfo.name().toString());

        LRAParticipant participant = new LRAParticipant(javaClass);
        return participant.hasNonJaxRsMethods() ? participant : null;
    }

    private boolean isNotLRAParticipant(ClassInfo classInfo) {
        Map<DotName, List<AnnotationInstance>> annotations = classInfo.annotations();
        return !annotations.containsKey(DotNames.LRA) ||
            (!annotations.containsKey(DotNames.COMPENSATE) &&
                !annotations.containsKey(DotNames.AFTER_LRA));
    }
}
