/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import io.narayana.lra.arquillian.resource.DelayedAfterLRAResource;
import io.narayana.lra.arquillian.spi.NarayanaLRARecovery;
import io.narayana.lra.client.NarayanaLRAClient;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class AfterLRAStatusIT {

    @ArquillianResource
    private URI baseURI;

    @Inject
    private NarayanaLRAClient narayanaLRAClient;

    @Inject
    private LRAMetricService lraMetricService;

    private NarayanaLRARecovery narayanaLRARecovery = new NarayanaLRARecovery();

    private Client client;

    @Deployment
    public static WebArchive deploy() {
        return Deployer.deploy(AfterLRAStatusIT.class.getSimpleName());
    }

    @Before
    public void before() {
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Verifies that the LRA state model is respected during the after LRA phase.
     */
    @Test
    public void afterLRAPhaseModelCheckTest() {
        URI lra = URI.create(getString(DelayedAfterLRAResource.LRA_PATH));

        // lra is closed when the method is finished, afterLRA method should be invoked returning 500 status
        narayanaLRARecovery.waitForEndPhaseReplay(lra);
        assertTrue(lraMetricService.getMetric(LRAMetricType.AfterLRA, lra) >= 1);

        URI lastEndedLRAId = URI.create(getString(DelayedAfterLRAResource.LAST_LRA_ID_PATH));
        LRAStatus lastEndedLRAStatus = LRAStatus.valueOf(getString(DelayedAfterLRAResource.LAST_STATUS_PATH));

        assertEquals("Invalid ended LRA id included in the After LRA call", lra, lastEndedLRAId);
        assertEquals("Invalid status included in the After LRA call", LRAStatus.Closed, lastEndedLRAStatus);

        LRAStatus statusFromCoordinator = narayanaLRAClient.getStatus(lra, false);
        assertEquals("Unexpected status returned from the coordinator", LRAStatus.Closed, statusFromCoordinator);

        Response response = client.target(baseURI)
            .path(DelayedAfterLRAResource.ROOT_PATH)
            .path(DelayedAfterLRAResource.CLEAR_AFTER_LRA_PATH)
            .request()
            .put(null);

        assertEquals(200, response.getStatus());

        narayanaLRARecovery.waitForEndPhaseReplay(lra);
        statusFromCoordinator = narayanaLRAClient.getStatus(lra, false);
        assertTrue("Unexpected status returned from the coordinator", statusFromCoordinator == null ||
            statusFromCoordinator == LRAStatus.Closed);
    }

    private String getString(String path) {
        Response response = null;

        try {
            response = client.target(baseURI)
                .path(DelayedAfterLRAResource.ROOT_PATH)
                .path(path)
                .request()
                .get();

            assertEquals(200, response.getStatus());
            assertTrue(response.hasEntity());

            return response.readEntity(String.class);
        } catch (Exception e) {
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
