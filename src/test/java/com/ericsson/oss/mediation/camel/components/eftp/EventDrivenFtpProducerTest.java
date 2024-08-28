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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.InputStream;

import org.apache.camel.*;
import org.apache.camel.spi.Registry;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;
import com.ericsson.oss.mediation.camel.components.eftp.pool.*;

@RunWith(MockitoJUnitRunner.class)
public class EventDrivenFtpProducerTest {

    private static final int COMMAND_OK = 200;

    private static final int CANT_OPEN_DATA_CONNECTION = 425;

    private static final String CANT_OPEN_DATA_CONNECTION_STRING = "Cannot open data connection";

    private static final int FILE_NOTAVAILABLE = 450;

    private static final int GENERIC_ERROR = -1;

    private EventDrivenFtpProducer eftpProducer;

    @Mock
    private EventDrivenFtpEndpoint endpoint;

    @Mock
    private EndpointConfiguration epConfiguration;

    @Mock
    private FTPClient mockedClient;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private CamelContext camelCTX;

    @Mock
    private Registry registry;

    @Mock
    private FtpConnectionPool connectionPool;

    private static final String hostName = "anyHost";

    private static final int port = 21;

    private static final String userName = "user";

    private static final String password = "password";

    private static final String srcDir = "srcDir";
    private static final String srcFile = "srcFile";
    private static final String destDir = "destDir";
    private static final String destFile = "destFile";

    @Before
    public void setUp() throws Exception {
        setUpConfiguration();
        setUpExchange();
        eftpProducer = new TestEftpProducer(endpoint);
    }

    private void setUpConfiguration() throws Exception {
        when(endpoint.getCamelContext()).thenReturn(camelCTX);
        when(camelCTX.getRegistry()).thenReturn(registry);
        when(registry.lookup(Constants.FTP_POOL)).thenReturn(connectionPool);
        when(
                endpoint.getCamelContext().getRegistry()
                        .lookup(Constants.FTP_POOL)).thenReturn(connectionPool);
        when(epConfiguration.getParameter("host")).thenReturn(hostName);
        when(epConfiguration.getParameter("port")).thenReturn(port);
        when(endpoint.getEndpointConfiguration()).thenReturn(epConfiguration);
        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenReturn(mockedClient);
    }

    private void setUpExchange() {
        when(message.getHeader(EftpConstants.EFTP_DESTINATION_DIRECTORY))
                .thenReturn(destDir);
        when(message.getHeader(EftpConstants.EFTP_DESTINATION_FILE))
                .thenReturn(destFile);
        when(message.getHeader(EftpConstants.EFTP_SOURCE_DIRECTORY))
                .thenReturn(srcDir);
        when(message.getHeader(EftpConstants.EFTP_SOURCE_FILE)).thenReturn(
                srcFile);

        when(message.getHeader(EftpConstants.EFTP_TARGET_IP_ADDRESS))
                .thenReturn(hostName);
        when(message.getHeader(EftpConstants.EFTP_TARGET_PASSWORD)).thenReturn(
                password);
        when(message.getHeader(EftpConstants.EFTP_TARGET_USERNAME)).thenReturn(
                userName);
        when(message.getHeader(EftpConstants.EFTP_TARGET_PORT))
                .thenReturn("21");
        when(exchange.getIn()).thenReturn(message);
    }

    @Test
    public void connect_FailedConnection_ThrowsEftpExceptionWithCorrectErrorCode()
            throws Exception {
        when(mockedClient.getReplyCode()).thenReturn(CANT_OPEN_DATA_CONNECTION);
        when(mockedClient.getReplyString()).thenReturn(
                CANT_OPEN_DATA_CONNECTION_STRING);
        when(mockedClient.isConnected()).thenReturn(false);
        try {
            eftpProducer.process(exchange);
            fail("Expected EFTP Exception");
        } catch (final GenericEftpException e) {
            assertEquals(CANT_OPEN_DATA_CONNECTION, e.getErrorCode());
            assertEquals(CANT_OPEN_DATA_CONNECTION_STRING,
                    e.getErrorDescription());
        }
        verify(mockedClient, never()).login(userName, password);
    }

    @Test
    public void connect_FailedConnection_DoesNotAttemptLogin() throws Exception {
        when(mockedClient.getReplyCode()).thenReturn(CANT_OPEN_DATA_CONNECTION);
        try {
            eftpProducer.process(exchange);
            fail("Expected EFTP Exception");
        } catch (final GenericEftpException e) {
        }
        verify(mockedClient, never()).login(userName, password);
    }

    @Test
    public void connect_AnyJob_UsesCorrectHostAndPort() throws Exception {
        setUpSuccessfulConnection();
        eftpProducer.process(exchange);
        verify(mockedClient).retrieveFileStream(any(String.class));
    }

    @Test
    public void process_SuccessfulJob_SetsCountingInputStreamAsBody()
            throws Exception {
        setUpSuccessfulConnection();
        eftpProducer.process(exchange);
        verify(message).setBody(any(InputStream.class));
        verify(exchange).setIn(message);
    }

    @Test
    public void process_SuccessfulJob_SetsDestinationFileNameInHeader()
            throws Exception {
        setUpSuccessfulConnection();
        eftpProducer.process(exchange);
        verify(message).setHeader(Exchange.FILE_NAME,
                destDir + File.separator + destFile);
        verify(exchange).setIn(message);
    }

    @Test
    public void process_ExceptionDuringFileTransfer_ThrowsEftpExceptionWithCorrectErrorCode()
            throws Exception {
        setUpSuccessfulConnection();
        when(mockedClient.getReplyCode()).thenReturn(COMMAND_OK,
                FILE_NOTAVAILABLE);

        GenericEftpException ex = new GenericEftpException(450,
                "Mock File not avaialable exception");

        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenThrow(ex);

        try {
            eftpProducer.process(exchange);
            fail("Expected EFTP Exception");
        } catch (final GenericEftpException e) {
            assertEquals(GENERIC_ERROR, e.getErrorCode());
        }
    }

    private void setUpSuccessfulConnection() throws Exception {
        when(mockedClient.getReplyCode()).thenReturn(COMMAND_OK);
        when(mockedClient.login(userName, password)).thenReturn(true);
        when(mockedClient.isConnected()).thenReturn(true);
    }

    /**
     * Extending class so we can inject a mocked FTP Client ...
     */
    private class TestEftpProducer extends EventDrivenFtpProducer {

        public TestEftpProducer(final EventDrivenFtpEndpoint endpoint) {
            super(endpoint);
        }

    }

}
