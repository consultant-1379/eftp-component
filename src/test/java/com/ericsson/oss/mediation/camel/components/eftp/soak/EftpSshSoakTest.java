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
package com.ericsson.oss.mediation.camel.components.eftp.soak;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.JndiRegistry;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.camel.components.eftp.EftpConstants;
import com.ericsson.oss.mediation.camel.components.eftp.pool.*;
import com.ericsson.oss.mediation.camel.components.eftp.utils.BaseCamelIntegrationTest;
import com.ericsson.oss.mediation.camel.components.eftp.utils.FileTransferDataSet;
import com.jcraft.jsch.ChannelSftp;

public class EftpSshSoakTest extends BaseCamelIntegrationTest {

    private static final transient Logger LOG = LoggerFactory
            .getLogger(EftpSshSoakTest.class);

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        final JndiRegistry registry = super.createRegistry();
        registry.bind("fileTransferDataSet", new FileTransferDataSet());

        final SftpConnectionPool sftpPool = new SftpConnectionPool(1, 0, 20,
                15000, 5000, 2000, 20);
        registry.bind(Constants.SFTP_POOL, sftpPool);
        return registry;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Before
    public void createFiles() {

        for (int i = 0; i < 10; i++) {
            final String fileName = "dataFile" + i + ".txt";
            final File f = new File("src/test/resources/" + fileName);
            final String text = "Test file conent " + i;
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(f);
            } catch (final FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                pw.write(text);
                pw.flush();
                pw.close();
            }
        }
    }

    @After
    public void removeFiles() {
        for (int i = 0; i < 10; i++) {
            final String fileName = "dataFile" + i + ".txt";
            final File f = new File("src/test/resources/" + fileName);
            f.delete();
        }
    }

    @Test
    public void testEftpConnectionPooling() throws Exception {
        context.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("dataset:fileTransferDataSet?produceDelay=3&size=12")
                        .routeId("test").to("eftp://blah").to("mock:result")
                        .process(new Processor() {
                            @Override
                            public void process(final Exchange exchange)
                                    throws Exception {
                                final ConnectionConfig key = (ConnectionConfig) exchange
                                        .getIn()
                                        .getHeader(
                                                EftpConstants.EFTP_CONNECTION_KEY);

                                
                                final ChannelSftp channel = (ChannelSftp) exchange
                                        .getIn().getHeader(
                                                EftpConstants.EFTP_CHANNEL);
                                SftpConnectionPool sftpPool = (SftpConnectionPool) exchange
                                        .getContext().getRegistry()
                                        .lookup(Constants.SFTP_POOL);
                                sftpPool.returnObject(key, channel);
                            }
                        });

            }
        });

        context.start();
        Thread.sleep(7000);

        final MockEndpoint ep = getMockEndpoint("mock:result");
        final List<Exchange> exchanges = ep.getReceivedExchanges();
        for (final Exchange exchange : exchanges) {
            final Map<String, Object> headers = exchange.getIn().getHeaders();
            final Set<Entry<String, Object>> set = headers.entrySet();
            for (final Entry<String, Object> entry : set) {
                log.debug("Header: " + entry.getKey() + "  : "
                        + entry.getValue());
            }
            log.debug("---------------");
        }

        context.stop();
    }
}
