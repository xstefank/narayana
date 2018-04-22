package io.narayana.lra.coordinator.util;

import io.narayana.lra.client.NarayanaLRAClient;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Util {

    private Util() {
    }

    public static String createLinkHeader(String compensatorData) throws MalformedURLException {
        compensatorData += "/";

        Map<String, String> terminateURIs = new HashMap<>();

        terminateURIs.put(NarayanaLRAClient.COMPENSATE, new URL(compensatorData + "compensate").toExternalForm());
        terminateURIs.put(NarayanaLRAClient.COMPLETE, new URL(compensatorData + "complete").toExternalForm());
        terminateURIs.put(NarayanaLRAClient.STATUS, new URL(compensatorData + "status").toExternalForm());

        // register with the coordinator
        // put the lra id in an http header
        StringBuilder linkHeaderValue = new StringBuilder();

        terminateURIs.forEach((k, v) -> makeLink(linkHeaderValue, "", k, v)); // or use Collectors.joining(",")

        return linkHeaderValue.toString();
    }

    private static StringBuilder makeLink(StringBuilder builder, String uriPrefix, String key, String value) {

        if (value == null) {
            return builder;
        }

        String terminationUri = uriPrefix == null ? value : String.format("%s%s", uriPrefix, value);
        Link link = Link.fromUri(terminationUri).rel(key).type(MediaType.TEXT_PLAIN).build();

        if (builder.length() != 0) {
            builder.append(',');
        }

        return builder.append(link);
    }

}
