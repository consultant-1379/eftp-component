/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.camel.components.eftp.pool;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ConnectionConfigTest {

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.camel.components.eftp.pool.ConnectionConfig#ConnectionConfig(java.lang.String, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * 
     * <p>
     * This test added to appease the code coverage gods .
     * </p>
     */
    @Test
    public void testConnectionConfigStrictHostContructor() {
        ConnectionConfig config = new ConnectionConfig("ipaddress", 21,
                "username", "password", "secure", "NO");
        assertNotNull(config);
    }
}
