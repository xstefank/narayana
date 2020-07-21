package io.narayana.lra.arquillian.spi;

import io.narayana.lra.client.NarayanaLRAClient;
//import org.eclipse.microprofile.lra.tck.service.spi.LRAManagementService;

import javax.ws.rs.NotFoundException;
import java.net.URI;
import java.net.URISyntaxException;

public class NarayanaLRAManagementService { //implements LRAManagementService {

    //@Override
    public boolean isLRAFinished(URI lraId) {
        String host = lraId.getHost();
        int port = lraId.getPort();

        NarayanaLRAClient lraClient = null;
        try {
            lraClient = new NarayanaLRAClient(host, port);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        try {
            return lraClient.isFinished(lraId);
        } catch (NotFoundException e) {
            // coordinator doesn't know about the LRA id anymore
            return true;
        }
    }
}
