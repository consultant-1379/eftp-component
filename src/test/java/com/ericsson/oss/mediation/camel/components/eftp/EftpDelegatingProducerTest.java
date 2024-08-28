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
package com.ericsson.oss.mediation.camel.components.eftp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.apache.camel.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;

@RunWith(MockitoJUnitRunner.class)
public class EftpDelegatingProducerTest {

    @Mock
    private EventDrivenFtpEndpoint endpoint;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private EndpointConfiguration config;

    @Before
    public void setup() {
        when(endpoint.createExchange()).thenReturn(exchange);
        when(endpoint.getEndpointConfiguration()).thenReturn(config);
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(EftpConstants.EFTP_SECURE_FTP)).thenReturn(
                "true");
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.camel.components.eftp.EftpDelegatingProducer#isSingleton()}
     * .
     */
    @Test
    public void testIsSingleton() {
        EftpDelegatingProducer producer = new EftpDelegatingProducer(endpoint);
        assertFalse(producer.isSingleton());
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.camel.components.eftp.EftpDelegatingProducer#EftpDelegatingProducer(com.ericsson.oss.mediation.camel.components.eftp.EventDrivenFtpEndpoint)}
     * .
     */
    @Test
    public void testEftpDelegatingProducer() {
        EftpDelegatingProducer producer = new EftpDelegatingProducer(endpoint);
        assertNotNull(producer);
    }

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.camel.components.eftp.EftpDelegatingProducer#process(org.apache.camel.Exchange)}
     * .
     */
    @Test(expected = GenericEftpException.class)
    public void testNoSecurityFlagSetException() throws Exception {
        when(message.getHeader(EftpConstants.EFTP_SECURE_FTP)).thenReturn(null);
        when(config.getParameter(EftpConstants.EFTP_SECURE_FTP)).thenReturn(
                null);

        EftpDelegatingProducer producer = new EftpDelegatingProducer(endpoint);
        producer.process(exchange);
    }

}
