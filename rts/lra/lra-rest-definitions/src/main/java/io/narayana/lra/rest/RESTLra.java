package io.narayana.lra.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.xstefank.lra.definition.LRADefinition;

@JsonSerialize(as = RESTLraImpl.class)
@JsonDeserialize(as = RESTLraImpl.class)
public interface RESTLra extends LRADefinition {

    /**
     * Provides the callback URL to be invoked on the LRA
     * completion / compensation
     *
     * The target URL is expected to provide the complete
     * and compensate endpoint according to {@link ParticipantCallback}
     *
     */
    String getCallbackURL();

}
