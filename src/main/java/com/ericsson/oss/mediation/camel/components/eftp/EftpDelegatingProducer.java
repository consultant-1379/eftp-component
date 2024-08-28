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

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.camel.components.eftp.exceptions.GenericEftpException;

/**
 * The Delegating producer checks the incoming exchange header for a value of
 * secureFtp and delegates to the relevant producer based on this value
 */
public class EftpDelegatingProducer extends DefaultProducer {

    private static final transient Logger LOG = LoggerFactory
            .getLogger(EventDrivenFtpProducer.class);
    private final EventDrivenFtpEndpoint endpoint;

    /**
     * Producer constructor
     * 
     * @param endpoint
     */
    public EftpDelegatingProducer(final EventDrivenFtpEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        LOG.debug("EftpDelegatingProducer constructor called...");
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
        LOG.debug("Processing exchange...Looking for secure/unsecure header");
        final String secure = (String) exchange.getIn().getHeader(
                EftpConstants.EFTP_SECURE_FTP);

        /*
         * No information about node security in headers, throw exception
         */
        if (secure == null) {
            LOG.error("The secureFtp header was not specified on the incoming message of the exchange."
                    + " Set it to false for FTP connections or true for SFTP connections");
            throw new GenericEftpException(
                    0,
                    "The secureFtp header was not specified on the incoming message of the exchange."
                            + " Set it to false for FTP connections or true for SFTP connections");
        }
        final boolean secureFlag = Boolean.valueOf(secure);
        if (secureFlag) {
            LOG.debug("Secure flag is set on exchange, delegating to EventDrivenSftpProducer");

            new EventDrivenSftpProducer(endpoint).process(exchange);
        } else {
            LOG.debug("Secure flag is not set on exchange, delegating to EventDrivenFtpProducer");
            new EventDrivenFtpProducer(endpoint).process(exchange);
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

}
