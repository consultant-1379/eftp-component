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

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.*;
import org.apache.sshd.server.auth.UserAuthPassword;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.filesystem.NativeFileSystemFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.*;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;

/**
 * This test will not be run by Maven unless the profile run-integration-test is
 * active
 * 
 * @author etonayr
 * 
 */
public class EftpJSchIntegrationTest extends CamelTestSupport {

    private static SshServer sshd;
    private static final String USERNAME = "test";
    private static final String PASSWD = "secret";
    private static final int PORT = 58558;

    @BeforeClass
    public static void startSSHService() {

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(PORT);
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(final String username,
                    final String password, final ServerSession session) {
                if (username.equals(USERNAME) && password.equals(PASSWD)) {
                    return true;
                }
                return false;
            }
        });
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setFileSystemFactory(new NativeFileSystemFactory());
        sshd.setCommandFactory(new ScpCommandFactory());

        final List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>(
                1);
        userAuthFactories.add(new UserAuthPassword.Factory());
        sshd.setUserAuthFactories(userAuthFactories);

        final List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();

        namedFactoryList.add(new SftpSubsystem.Factory());
        sshd.setSubsystemFactories(namedFactoryList);

        try {
            sshd.start();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEventDrivenSftpComponentErrors_File_Not_Available()
            throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:error");
        final Endpoint ep = context.getEndpoint("direct:start");
        final Producer producer = ep.createProducer();
        final Exchange ex = getExchange(ep, "notfoundfile.txt");
        producer.process(ex);
        mock.expectedMinimumMessageCount(1);

        Exchange result = mock.getReceivedExchanges().get(0);
        GenericEftpException exception = (GenericEftpException) result
                .getProperty(Exchange.EXCEPTION_CAUGHT);

        log.debug("Stack trace: {}", exception);
        assertEquals(2, exception.getErrorCode());

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEventDrivenSftpComponentErrors_SFTP_Server_NA()
            throws Exception {

        shutdownSSHService();

        final MockEndpoint mock = getMockEndpoint("mock:error");
        final Endpoint ep = context.getEndpoint("direct:start");
        final Producer producer = ep.createProducer();
        final Exchange ex = getExchange(ep, "file.txt");
        producer.process(ex);

        mock.expectedMinimumMessageCount(1);
        final GenericEftpException exception = (GenericEftpException) ex
                .getProperty(Exchange.EXCEPTION_CAUGHT);

        //		assertEquals(CONNECTION_REFUSED.getReplyMsg(), exception.getErrorDescription()); 

        log.debug(exception.getCause().toString());

        assertMockEndpointsSatisfied();

        startSSHService();
    }

    @Test
    public void testEventDrivenSftpComponentErrors_SFTP_Server_Wrong_User()
            throws Exception {

        final MockEndpoint mock = getMockEndpoint("mock:error");
        final Endpoint ep = context.getEndpoint("direct:start");
        final Producer producer = ep.createProducer();
        final Exchange ex = getExchange(ep, "file.txt");

        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_USERNAME, "wrongUser");
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_PASSWORD, "secret");

        producer.process(ex);

        mock.expectedMinimumMessageCount(1);
        final GenericEftpException exception = (GenericEftpException) ex
                .getProperty(Exchange.EXCEPTION_CAUGHT);

        //		assertEquals(AUTH_FAIL.getReplyMsg(), exception.getErrorDescription()); 

        log.debug(exception.getCause().toString());

        assertMockEndpointsSatisfied();
    }

    @AfterClass
    public static void shutdownSSHService() {
        try {
            if (sshd != null) {
                sshd.stop(true);
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Exchange getExchange(final Endpoint ep, final String fileToGet)
            throws Exception {
        final Exchange ex = ep.createExchange();
        ex.getIn().setHeader(EftpConstants.EFTP_SOURCE_FILE, fileToGet);
        ex.getIn().setHeader(EftpConstants.EFTP_SOURCE_DIRECTORY,
                "src/test/resources");
        ex.getIn()
                .setHeader(EftpConstants.EFTP_DESTINATION_FILE, "newfile.txt");
        ex.getIn().setHeader(EftpConstants.EFTP_DESTINATION_DIRECTORY, "/");
        ex.getIn().setHeader(EftpConstants.EFTP_SECURE_FTP, "true");

        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_IP_ADDRESS, "localhost");
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_PORT, "" + PORT);
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_USERNAME, USERNAME);
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_PASSWORD, PASSWD);

        return ex;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").errorHandler(
                        deadLetterChannel("mock:error")).to("eftp://Ericsson1");
            }
        };
    }
}