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

package io.narayana.lra.arquillian;

import io.narayana.lra.arquillian.resource.NestedParticipant;
import io.narayana.lra.arquillian.resource.TestBase;
import io.narayana.lra.arquillian.spi.NarayanaLRARecovery;
import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.logging.LRALogger;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@RunWith(Arquillian.class)
public class NestedLRAIT {

    @Inject
    private LRAMetricService lraMetricService;

    @ArquillianResource
    private URL baseURL;

    @Deployment
    public static WebArchive deploy() {
        return TestBase.deploy(NestedLRAIT.class.getSimpleName() + ".war");
    }

    @Before
    public void before() {
        lraMetricService.clear();
    }

    @Test
    public void testFinishLRA() throws URISyntaxException {
        NarayanaLRAClient lraClient = new NarayanaLRAClient(); // the narayana client API for using LRAs
        URI topLevelLRA = lraClient.startLRA("topLevelLRA");

        // start and close nested LRA with participant enlistment
        Client client = ClientBuilder.newClient();

        Response response = client.target(UriBuilder.fromUri(baseURL.toExternalForm())
            .path(NestedParticipant.NESTED_PARTICIPANT_PATH)
            .path(NestedParticipant.ACTION_PATH).build())
            .request()
            .header(LRA.LRA_HTTP_PARENT_CONTEXT_HEADER, topLevelLRA)
            .get();

        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.hasEntity());

        URI nestedLRA = URI.create(response.readEntity(String.class));

        Assert.assertEquals(2,
            lraMetricService.getMetric(LRAMetricType.Nested, topLevelLRA, NestedParticipant.class.getName()));
        Assert.assertEquals(1,
            lraMetricService.getMetric(LRAMetricType.Completed, nestedLRA, NestedParticipant.class.getName()));
        Assert.assertEquals(0,
            lraMetricService.getMetric(LRAMetricType.Forget, nestedLRA, NestedParticipant.class.getName()));

        // close top level LRA
        LRALogger.logger.error("Closing parent");
        lraClient.closeLRA(topLevelLRA);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LRALogger.logger.error("trigger recovery");
        new NarayanaLRARecovery().waitForEndPhaseReplay(topLevelLRA);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(4,
            lraMetricService.getMetric(LRAMetricType.Nested, topLevelLRA, NestedParticipant.class.getName()));
        Assert.assertEquals(2,
            lraMetricService.getMetric(LRAMetricType.Forget, nestedLRA, NestedParticipant.class.getName()));


    }
}
