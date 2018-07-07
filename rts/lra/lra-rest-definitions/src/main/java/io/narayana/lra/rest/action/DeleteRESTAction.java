package io.narayana.lra.rest.action;

import io.narayana.lra.rest.RESTAction;
import org.xstefank.lra.model.LRAData;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URL;

public class DeleteRESTAction extends RESTAction {

    private DeleteRESTAction(URL target, URL callbackUrl) {
        super(target, callbackUrl);
    }

    @Override
    protected Response doRequest(WebTarget target, LRAData lraData) {
        return target.request()
                .header(RESTAction.LRA_HTTP_HEADER, lraData.getLraId())
                .delete();
    }

    public static final class DeleteRESTActionBuilder extends RESTActionBuilder {

        public DeleteRESTActionBuilder(URL target) {
            super(target);
        }

        @Override
        public RESTAction build() {
            return new DeleteRESTAction(target, callbackUrl);
        }
    }
}
