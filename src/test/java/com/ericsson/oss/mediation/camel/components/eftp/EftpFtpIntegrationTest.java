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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.*;

/**
 * This test will not be run by Maven unless the profile run-integration-test is
 * active
 * 
 * @author etonayr
 * 
 */
public class EftpFtpIntegrationTest extends CamelTestSupport {

    private static final String USERNAME = "test";
    private static final int PORT = 58533;
    private static FtpServer server;

    @BeforeClass
    public static void startFtpServer() throws Exception {
        final FtpServerFactory serverFactory = new FtpServerFactory();
        final ListenerFactory factory = new ListenerFactory();
        factory.setPort(PORT);
        serverFactory.addListener("default", factory.createListener());
        server = serverFactory.createServer();
        final PropertiesUserManagerFactory userFactory = new PropertiesUserManagerFactory();

        /**
         * not used atm, using defaults
         */
        final File userFile = new File("src/test/resources/users.properties");
        final File userHome = new File("src/test/resources");
        userHome.setWritable(true);
        userHome.mkdirs();
        userFactory.setFile(userFile);
        final UserManager um = userFactory.createUserManager();
        final BaseUser user = new BaseUser();
        user.setName(USERNAME);
        user.setPassword(USERNAME);
        user.setHomeDirectory(userHome.getAbsolutePath());

        final List<Authority> auths = new ArrayList<Authority>();
        final Authority auth = new WritePermission();
        auths.add(auth);
        user.setAuthorities(auths);

        um.save(user);

        serverFactory.setUserManager(um);
        server.start();

    }

    @AfterClass
    public static void shutdownFtpServer() {
        if (server != null) {
            server.stop();
        }

    }

    @Test
    public void testEventDrivenFtpComponent() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        Endpoint ep = context.getEndpoint("direct:start");
        Producer producer = ep.createProducer();
        Exchange ex = getExchange(ep, "testfile.txt");
        producer.process(ex);

        mock.expectedMinimumMessageCount(1);
        for (Exchange result : mock.getExchanges()) {
            assertTrue(result.getIn().getBody() instanceof InputStream);
        }

        assertMockEndpointsSatisfied();
    }

    private Exchange getExchange(Endpoint ep, String fileToGet)
            throws Exception {
        Exchange ex = ep.createExchange();
        ex.getIn().setHeader(EftpConstants.EFTP_SOURCE_FILE, fileToGet);
        ex.getIn().setHeader(EftpConstants.EFTP_SOURCE_DIRECTORY, "/");
        ex.getIn()
                .setHeader(EftpConstants.EFTP_DESTINATION_FILE, "newfile.txt");
        ex.getIn().setHeader(EftpConstants.EFTP_DESTINATION_DIRECTORY, "/");
        ex.getIn().setHeader(EftpConstants.EFTP_SECURE_FTP, "false");

        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_IP_ADDRESS, "localhost");
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_PORT, "" + PORT);
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_USERNAME, USERNAME);
        ex.getIn().setHeader(EftpConstants.EFTP_TARGET_PASSWORD, USERNAME);

        return ex;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start").to("eftp://ericsson").to("mock:result");
            }
        };
    }

}
