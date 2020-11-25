package io.narayana.lra;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import io.narayana.lra.logging.LRALogger;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.File;
import java.util.Optional;

@ApplicationScoped
public class ObjectStoreInit {

    @ConfigProperty(name = "lra.object-store.dir")
    Optional<String> osPath;

    public void observesStart(@Observes StartupEvent event) {
        String objectStoreDir = osPath.orElse(System.getProperty("user.dir") + File.separator + "ObjectStore");
        LRALogger.logger.infof("LRA OS dir is set to " + objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
    }
}
