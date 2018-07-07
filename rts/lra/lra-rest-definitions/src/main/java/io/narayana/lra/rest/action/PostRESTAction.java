package io.narayana.lra.rest.action;

import io.narayana.lra.rest.RESTAction;
import org.xstefank.lra.model.LRAData;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URL;

public class PostRESTAction extends RESTAction {

    private PostRESTAction(URL target, URL callbackUrl) {
        super(target, callbackUrl);
    }

    @Override
    protected Response doRequest(WebTarget target, LRAData lraData) {
        return target.request()
                .header(RESTAction.LRA_HTTP_HEADER, lraData.getLraId())
                .post(Entity.json(lraData.getData()));
    }

    public static final class PostRESTActionBuilder extends RESTActionBuilder {

        public PostRESTActionBuilder(URL target) {
            super(target);
        }

        @Override
        public RESTAction build() {
            return new PostRESTAction(target, callbackUrl);
        }
    }
}
