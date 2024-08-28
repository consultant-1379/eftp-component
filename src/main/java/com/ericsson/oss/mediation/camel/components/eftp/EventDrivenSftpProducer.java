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

import static com.ericsson.oss.mediation.camel.components.eftp.EftpConstants.JSchErrorMessages.*;

import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;
import com.ericsson.oss.mediation.camel.components.eftp.pool.*;
import com.ericsson.oss.mediation.camel.components.eftp.utils.EftpUtilities;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

/**
 * The EventDrivenSftp producer.
 */
public class EventDrivenSftpProducer extends DefaultProducer {

    private static final transient Logger LOG = LoggerFactory
            .getLogger(EventDrivenSftpProducer.class);

    private final EventDrivenFtpEndpoint endpoint;

    public EventDrivenSftpProducer(final EventDrivenFtpEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        LOG.debug("EventDrivenSftpProducer constructor for endpoint {}",
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
        final String srcFile = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_SOURCE_FILE);
        final String srcDir = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_SOURCE_DIRECTORY);
        final String destFile = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_DESTINATION_FILE);
        final String destDir = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_DESTINATION_DIRECTORY);

        LOG.debug(
                "process will be called for sourceDir=[{}] and sourceFile=[{}], destDir=[{}] and destFile=[{}]",
                new Object[] { srcDir, srcFile, destDir, destFile });

        try {
            final ChannelSftp channel = setupConnection(exchange);
            final String fileToGet = EftpUtilities.normalizeSourceFilePath(
                    srcDir, srcFile);
            exchange.getIn().setBody(getFile(fileToGet, channel));
            exchange.getIn().setHeader(
                    Exchange.FILE_NAME,
                    EftpUtilities
                            .createFilePathWithSeparator(destDir, destFile));

        } catch (GenericEftpException gex) {
            LOG.error("Exception caught during SFTP transfer {}", gex);
            throw gex;
        } catch (Exception ex) {
            LOG.error("Exception caught during SFTP transfer {}", ex);
            throw new GenericEftpException(
                    "Exception caught during SFTP transfer", ex);
        }
    }

    private InputStream getFile(final String fileToGet,
            final ChannelSftp channel) throws GenericEftpException {
        try {
            return channel.get(fileToGet);
        } catch (final SftpException sftpException) {
            LOG.error(
                    "Exception caught while trying to getFile=[{}], stack trace is:{}",
                    fileToGet, sftpException);

            final String errorMsg = "An SftpException "
                    + sftpException.getLocalizedMessage()
                    + " occured trying to download the file {" + fileToGet
                    + "} to an output stream";

            throw new GenericEftpException(
                    EftpUtilities.extractSftpErrorCodeException(sftpException),
                    errorMsg, sftpException);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.impl.DefaultProducer#isSingleton()
     */
    @Override
    public boolean isSingleton() {
        return false;
    }

    private ChannelSftp setupConnection(final Exchange exchange)
            throws GenericEftpException {
        final String ipAddress = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_IP_ADDRESS);

        final Integer port = Integer.parseInt(exchange.getIn()
                .getHeader(EftpConstants.EFTP_TARGET_PORT).toString());

        final String username = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_USERNAME);

        final String password = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_TARGET_PASSWORD);

        final String secure = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_SECURE_FTP);

        final ConnectionConfig key = new ConnectionConfig(ipAddress, port,
                username, password, secure);

        try {
            LOG.debug("About to borrow channel with key=[{}]", key);
            final ChannelSftp channel = obtainPoolReference().borrowObject(key);
            exchange.getIn().setHeader(EftpConstants.EFTP_SESSION,
                    channel.getSession());
            exchange.getIn().setHeader(EftpConstants.EFTP_CONNECTION_KEY, key);
            exchange.getIn().setHeader(EftpConstants.EFTP_CHANNEL, channel);
            exchange.getIn().removeHeader(EftpConstants.EFTP_TARGET_USERNAME);
            exchange.getIn().removeHeader(EftpConstants.EFTP_TARGET_PASSWORD);
            return channel;
        } catch (Exception e) {
            resolveJschErrorCodes(e);
            LOG.error(
                    "Exception caught while calling setupConnection method on connectionPool, stacktrace: {}",
                    e);
            return null;
        }
    }

    private SftpConnectionPool obtainPoolReference() {
        return (SftpConnectionPool) this.getEndpoint().getCamelContext()
                .getRegistry().lookup(Constants.SFTP_POOL);
    }

    /**
     * @param jschException
     * @throws GenericEftpException
     */
    private void resolveJschErrorCodes(final Exception jschException)
            throws GenericEftpException {
        String error_message = jschException.getMessage();

        // this error code will be returned only in case of non JSchException
        int errorCode = UNKNOWN_EXCEPTION.getErrorCode();

        if (error_message.contains(CONNECTION_TIMED_OUT.getErrorMsg())) {
            errorCode = CONNECTION_TIMED_OUT.getErrorCode();
            error_message = CONNECTION_TIMED_OUT.getReplyMsg();

        } else if (error_message.contains(INVALID_ADD.getErrorMsg())) {
            errorCode = INVALID_ADD.getErrorCode();
            error_message = INVALID_ADD.getReplyMsg();

        } else if (error_message.contains(UNKNOWN_HOST_EXCEPTION.getErrorMsg())) {
            errorCode = UNKNOWN_HOST_EXCEPTION.getErrorCode();
            error_message = UNKNOWN_HOST_EXCEPTION.getReplyMsg();

        } else if (error_message.contains(USER_NAME_NOT_NULL.getErrorMsg())) {
            errorCode = USER_NAME_NOT_NULL.getErrorCode();
            error_message = USER_NAME_NOT_NULL.getReplyMsg();

        } else if (error_message.contains(SESSION_DOWN.getErrorMsg())) {
            errorCode = SESSION_DOWN.getErrorCode();
            error_message = SESSION_DOWN.getReplyMsg();

        } else if (error_message.contains(CONNECTION_REFUSED.getErrorMsg())) {
            errorCode = CONNECTION_REFUSED.getErrorCode();
            error_message = CONNECTION_REFUSED.getReplyMsg();

        } else if (error_message.contains(READ_TIMED_OUT.getErrorMsg())) {
            errorCode = READ_TIMED_OUT.getErrorCode();
            error_message = READ_TIMED_OUT.getReplyMsg();

        } else if (error_message.contains(AUTH_FAIL.getErrorMsg())) {
            errorCode = AUTH_FAIL.getErrorCode();
            error_message = AUTH_FAIL.getReplyMsg();
        }

        throw new GenericEftpException(errorCode,
                "Connection could not be established with Network Element due to "
                        + error_message, jschException);
    }

}