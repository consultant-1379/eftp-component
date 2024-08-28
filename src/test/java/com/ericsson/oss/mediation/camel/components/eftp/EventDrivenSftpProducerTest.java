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

import static com.ericsson.oss.mediation.camel.components.eftp.EftpConstants.JSchErrorMessages.AUTH_FAIL;
import static com.ericsson.oss.mediation.camel.components.eftp.EftpConstants.JSchErrorMessages.INVALID_ADD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.InputStream;

import org.apache.camel.*;
import org.apache.camel.spi.Registry;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;
import com.ericsson.oss.mediation.camel.components.eftp.pool.*;
import com.ericsson.oss.mediation.camel.components.eftp.utils.EftpUtilities;
import com.jcraft.jsch.*;

@RunWith(MockitoJUnitRunner.class)
public class EventDrivenSftpProducerTest {

    private EventDrivenSftpProducer esftpProducer;

    @Mock
    private EventDrivenFtpEndpoint endpoint;

    @Mock
    private EndpointConfiguration epConfiguration;

    @Mock
    private Exchange exchange;

    @Mock
    private Message message;

    @Mock
    private Session session;

    @Mock
    private CamelContext camelCTX;

    @Mock
    private Registry registry;

    @Mock
    private ChannelSftp channel;

    @Mock
    private SftpConnectionPool connectionPool;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final String hostName = "anyHost";

    private static final int port = 22;

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
        esftpProducer = new TestEsftpProducer(endpoint);

    }

    private void setUpConfiguration() throws Exception {
        when(endpoint.getCamelContext()).thenReturn(camelCTX);
        when(camelCTX.getRegistry()).thenReturn(registry);
        when(registry.lookup(Constants.SFTP_POOL)).thenReturn(connectionPool);
        when(
                endpoint.getCamelContext().getRegistry()
                        .lookup(Constants.SFTP_POOL))
                .thenReturn(connectionPool);
        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenReturn(channel);
        when(epConfiguration.getParameter("host")).thenReturn(hostName);
        when(epConfiguration.getParameter("port")).thenReturn(port);
        when(endpoint.getEndpointConfiguration()).thenReturn(epConfiguration);
        when(session.openChannel(Mockito.anyString())).thenReturn(channel);
    };

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
    public void connect_AnyJob_UsesCorrectHostAndPort() throws Exception {
        esftpProducer.process(exchange);
        verify(channel, times(1)).get(any(String.class));
    }

    @Test
    public void process_SuccessfulJob_SetsCountingInputStreamAsBody()
            throws Exception {
        esftpProducer.process(exchange);
        verify(message).setBody(any(InputStream.class));
    }

    @Test
    public void process_SuccessfulJob_SetsDestinationFileNameInHeader()
            throws Exception {
        esftpProducer.process(exchange);
        verify(message).setHeader(Exchange.FILE_NAME,
                destDir + File.separator + destFile);
    }

    @Test
    public void process_FailedJob_SetCorrectSFTPError_FileNotAvailable()
            throws Exception {
        final String file = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        final String errormessage = "An SftpException " + "No such file"
                + " occured trying to download the file { " + file
                + "} to a output stream";

        when(channel.get(file)).thenThrow(new SftpException(2, errormessage));

        exception.expect(GenericEftpException.class);
        exception.expectMessage(errormessage);

        esftpProducer.process(exchange);
    }

    @Test
    public void process_FailedJob_SetCorrectSFTPError_ReadPermissionDenied()
            throws Exception {
        final String file = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        final String errormessage = "An SftpException " + "Permission denied"
                + " occured trying to download the file { " + file
                + "} to a output stream";
        when(channel.get(file)).thenThrow(new SftpException(3, errormessage));

        exception.expect(GenericEftpException.class);
        exception.expectMessage(errormessage);

        esftpProducer.process(exchange);
    }

    @Test
    public void process_FailedJob_SetCorrectJSchErrors_Invalid_Add()
            throws Exception {
        final String errormessage = "Connection could not be established with Network Element due to "
                + INVALID_ADD.getReplyMsg();
        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenThrow(
                        new JSchException(
                                "Address is invalid on local machine, or port is not valid on remote machine"));

        try {
            esftpProducer.process(exchange);
            fail("should have thrown GenericEftpException");
        } catch (GenericEftpException e) {
            assertEquals(INVALID_ADD.getErrorCode(), e.getErrorCode());
            assertEquals(errormessage, e.getErrorDescription());
        }
    }

    @Test
    public void process_FailedJob_SetCorrectJSchErrors_Auth_Fail()
            throws Exception {
        final String errormessage = "Connection could not be established with Network Element due to "
                + AUTH_FAIL.getReplyMsg();
        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenThrow(new JSchException("Auth fail"));

        try {
            esftpProducer.process(exchange);
            fail("should have thrown GenericEftpException");
        } catch (GenericEftpException e) {
            assertEquals(AUTH_FAIL.getErrorCode(), e.getErrorCode());
            assertEquals(errormessage, e.getErrorDescription());
        }
    }

    @Test
    public void process_FailedJob_SetErrorCodeToUnknown() throws Exception {
        final String errormessage = "Connection could not be established with Network Element due to Unknown error code";
        when(connectionPool.borrowObject(any(ConnectionConfig.class)))
                .thenThrow(new Exception("Unknown error code"));

        try {
            esftpProducer.process(exchange);
            fail("should have thrown GenericEftpException");
        } catch (GenericEftpException e) {
            assertEquals(8, e.getErrorCode());
            assertEquals(errormessage, e.getErrorDescription());
        }
    }

    /**
     * Extending class so we can inject a mocked FTP Client ...
     */
    private class TestEsftpProducer extends EventDrivenSftpProducer {

        public TestEsftpProducer(final EventDrivenFtpEndpoint endpoint) {
            super(endpoint);
        }

    }
}
