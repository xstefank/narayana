package io.narayana.lra.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.narayana.lra.rest.action.DeleteRESTAction;
import io.narayana.lra.rest.action.PostRESTAction;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jboss.logging.Logger;
import org.xstefank.lra.definition.Action;
import org.xstefank.lra.model.ActionResult;
import org.xstefank.lra.model.LRAData;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@ToString
@NoArgsConstructor
public abstract class RESTAction implements Action {

    public static final String LRA_HTTP_HEADER = "Long-Running-Action";

    @JsonIgnore
    private Logger log = Logger.getLogger(RESTAction.class);

    @JsonProperty(required = true)
    private URL target;

    @JsonProperty
    private URL callbackUrl;

    public RESTAction(URL target, URL callbackUrl) {
        this.target = target;
        this.callbackUrl = callbackUrl != null ? callbackUrl : target;
    }

    public static RESTActionBuilder post(URL target) {
        return new PostRESTAction.PostRESTActionBuilder(target);
    }

    public static RESTActionBuilder delete(URL target) {
        return new DeleteRESTAction.DeleteRESTActionBuilder(target);
    }

    @Override
    public ActionResult invoke(LRAData lraData) {
        log.infof("executing action - %s", this);

        Client client = ClientBuilder.newClient();
        URI build = null;
        try {
            build = UriBuilder
                    .fromUri(target.toURI())
                    .build();
        } catch (URISyntaxException ex) {
            return ActionResult.failure(ex);
        }

        log.info("action request url - " + build);
        WebTarget target = client.target(build);

        Response response = doRequest(target, lraData);

        ActionResult result = response.getStatus() == Response.Status.OK.getStatusCode() ?
                ActionResult.success() : ActionResult.failure();

        response.close();

        return result;
    }

    protected abstract Response doRequest(WebTarget target, LRAData lraData);

    public URL getCallbackUrl() {
        return callbackUrl;
    }

    public abstract static class RESTActionBuilder {

        protected URL target;
        protected URL callbackUrl;

        protected RESTActionBuilder(URL target) {
            this.target = target;
        }

        public RESTActionBuilder callbackUrl(URL callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public abstract RESTAction build();

    }
}
