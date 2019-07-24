package io.narayana.lra.client.internal.proxy.nonjaxrs.jandex;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.jboss.jandex.DotName;

public class DotNames {

    public static final DotName LRA = DotName.createSimple(org.eclipse.microprofile.lra.annotation.ws.rs.LRA.class.getName());

    public static final DotName COMPENSATE = DotName.createSimple(Compensate.class.getName());

    public static final DotName GET = DotName.createSimple(javax.ws.rs.GET.class.getName());
    public static final DotName POST = DotName.createSimple(javax.ws.rs.POST.class.getName());
    public static final DotName PUT = DotName.createSimple(javax.ws.rs.PUT.class.getName());
    public static final DotName DELETE = DotName.createSimple(javax.ws.rs.DELETE.class.getName());
    public static final DotName HEAD = DotName.createSimple(javax.ws.rs.HEAD.class.getName());
    public static final DotName OPTIONS = DotName.createSimple(javax.ws.rs.OPTIONS.class.getName());
    
}
