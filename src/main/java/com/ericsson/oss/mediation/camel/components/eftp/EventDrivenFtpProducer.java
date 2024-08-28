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

import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;
import com.ericsson.oss.mediation.camel.components.eftp.pool.*;
import com.ericsson.oss.mediation.camel.components.eftp.pool.exception.GenericPoolException;
import com.ericsson.oss.mediation.camel.components.eftp.utils.EftpUtilities;

/**
 * The EventDrivenFtp producer.
 */
public class EventDrivenFtpProducer extends DefaultProducer {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.impl.DefaultProducer#isSingleton()
     */
    @Override
    public boolean isSingleton() {
        return false;
    }

    private static final transient Logger LOG = LoggerFactory
            .getLogger(EventDrivenFtpProducer.class);

    private final EventDrivenFtpEndpoint endpoint;

    /**
     * Producer constructor
     * 
     * @param endpoint
     */
    public EventDrivenFtpProducer(final EventDrivenFtpEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;

        LOG.debug("EventDrivenFtpProducer constructor for endpoint {}",
                this.endpoint.getEndpointUri());
    }

    /**
     * The process method is the entry point from Camel into this processor
     * 
     * @param exchange
     *            {@link Exchange}
     * @return
     * @throws GenericEftpException
     */
    @Override
    public void process(final Exchange exchange) throws GenericEftpException {
        FTPClient ftpClient = null;
        try {

            final String destDirectory = (String) exchange.getIn().getHeader(
                    EftpConstants.EFTP_DESTINATION_DIRECTORY);
            final String destFile = (String) exchange.getIn().getHeader(
                    EftpConstants.EFTP_DESTINATION_FILE);
            final String srcFile = (String) exchange.getIn().getHeader(
                    EftpConstants.EFTP_SOURCE_FILE);
            final String srcDir = (String) exchange.getIn().getHeader(
                    EftpConstants.EFTP_SOURCE_DIRECTORY);

            ftpClient = setupConnection(exchange);

            final Message message = exchange.getIn();
            message.setBody(getFile(srcDir, srcFile, ftpClient));

            LOG.debug("process will be called for destination: {}",
                    destDirectory + destFile);

            message.setHeader(Exchange.FILE_NAME, EftpUtilities
                    .createFilePathWithSeparator(destDirectory, destFile));

            exchange.setIn(message);
        } catch (final GenericEftpException e) {
            LOG.error("Error detected during connection, stack trace: {}", e);
            LOG.error("Error code: [{}] and description: [{}]",
                    e.getErrorCode(), e.getErrorDescription());
            throw e;
        } catch (final Exception e) {
            LOG.error("Exception thrown: [{}]", e);
            throw new GenericEftpException(ftpClient.getReplyCode(),
                    ftpClient.getReplyString(), e);
        }
    }

    private FTPClient setupConnection(final Exchange exchange)
            throws GenericEftpException {
        final String ipAddress = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_IP_ADDRESS);

        final Integer port = Integer.valueOf(exchange.getIn()
                .getHeader(EftpConstants.EFTP_TARGET_PORT).toString());

        final String username = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_USERNAME);

        final String password = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_PASSWORD);

        final String secure = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_SECURE_FTP);

        final ConnectionConfig key = new ConnectionConfig(ipAddress, port,
                username, password, secure);
        exchange.getIn().setHeader(EftpConstants.EFTP_CONNECTION_KEY, key);
        exchange.getIn().removeHeader(EftpConstants.EFTP_TARGET_USERNAME);
        exchange.getIn().removeHeader(EftpConstants.EFTP_TARGET_PASSWORD);

        try {
            LOG.debug("About to borrow connection with key=[{}]", key);
            final FTPClient ftpClient = obtainPoolReference().borrowObject(key);
            exchange.getIn().setHeader(EftpConstants.EFTP_CLIENT, ftpClient);
            return ftpClient;
        } catch (final GenericPoolException gpe) {

            LOG.error(
                    "GenericPoolException caught while calling borrowObject method on connectionPool, stacktrace: {}",
                    gpe);
            throw new GenericEftpException(gpe.getCode(), gpe.getReason(), gpe);
        } catch (final Exception e) {
            LOG.error(
                    "Exception caught while calling borrowObject method on connectionPool, stacktrace: {}",
                    e);
            throw new GenericEftpException(-1, e.getMessage(), e);
        }
    }

    private FtpConnectionPool obtainPoolReference() {
        return (FtpConnectionPool) this.getEndpoint().getCamelContext()
                .getRegistry().lookup(Constants.FTP_POOL);
    }

    /**
     * Helper method to get the file from open FTP session
     * 
     * @param request
     *            Request containing information about the file
     * @return
     * @throws GenericEftpException
     */
    private InputStream getFile(final String srcDir, final String srcFile,
            final FTPClient ftpClient) throws GenericEftpException {
        try {
            final String filePath = EftpUtilities.normalizeSourceFilePath(
                    srcDir, srcFile);
            if (srcFile.endsWith("gz")) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            }
            LOG.debug("getFile will be called for source: {}", filePath);

            //Added to as fix for TT478- etonayr 29 November 2012
            //We can't use an InputStream via the retrieveFileStream(..) method
            //of the FtpClient API as doing so will also require us to call completePendingCommand()
            //completePendingCommand() has a weird bug where it sometimes hangs on large files
            //"large" being unknown, but anything more than a few megabytes.
            //This is a known issue and has been discussed on Apache commons Net mailing list as far back as 2004
            //but it dosen't seem to have been fixed or acknowledged as a "real" bug.
            //Instead we now use a retrieveFile and buffer the file in memory before converting to an InputStream.            
            final InputStream stream = ftpClient.retrieveFileStream(filePath);

            // If we dont get a positive response we throw and exception
            // Need to check the reply code and explicitly throw an exception as
            // a negative ftp reply code will not throw an exception in Java
            if (!isPositiveResponse(ftpClient.getReplyCode())) {
                throw new GenericEftpException(ftpClient.getReplyCode(),
                        ftpClient.getReplyString(), null);
            }
            return stream;
        } catch (final GenericEftpException e) {
            throw e;
        } catch (final Exception e) {
            // the unforseen, we'll include the reply code, to see what was
            // happening when we got here.
            throw new GenericEftpException(ftpClient.getReplyCode(),
                    ftpClient.getReplyString(), e);
        }
    }

    /**
     * Helper for checking for positive responses from the FTP server.
     * 
     * @param replyCode
     *            reply code received from the FTP server
     * @return true if the reply code indicates the last action performed was
     *         accepted.
     */
    private boolean isPositiveResponse(final int replyCode) {
        return FTPReply.isPositiveCompletion(replyCode)
                || FTPReply.isPositiveIntermediate(replyCode)
                || FTPReply.isPositivePreliminary(replyCode);
    }

}
