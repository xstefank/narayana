package io.narayana.lra.coordinator.util;

import io.narayana.lra.client.NarayanaLRAClient;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static String createLinkHeader(String compensatorUrl) throws MalformedURLException {
        compensatorUrl += "/";

        Map<String, String> terminateURIs = new HashMap<>();

        terminateURIs.put(NarayanaLRAClient.COMPENSATE, new URL(compensatorUrl + "compensate").toExternalForm());
        terminateURIs.put(NarayanaLRAClient.COMPLETE, new URL(compensatorUrl + "complete").toExternalForm());
        terminateURIs.put(NarayanaLRAClient.STATUS, new URL(compensatorUrl + "status").toExternalForm());

        // register with the coordinator
        // put the lra id in an http header
        StringBuilder linkHeaderValue = new StringBuilder();

        terminateURIs.forEach((k, v) -> makeLink(linkHeaderValue, "", k, v)); // or use Collectors.joining(",")

        return linkHeaderValue.toString();
    }

    private static StringBuilder makeLink(StringBuilder b, String uriPrefix, String key, String value) {

        if (value == null)
            return b;

        String terminationUri = uriPrefix == null ? value : String.format("%s%s", uriPrefix, value);
        Link link = Link.fromUri(terminationUri).rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

}
