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
package com.ericsson.oss.mediation.camel.components.eftp.utils;

import org.apache.camel.Exchange;
import org.apache.camel.component.dataset.SimpleDataSet;

import com.ericsson.oss.mediation.camel.components.eftp.EftpConstants;

public class FileTransferDataSet extends SimpleDataSet {

    private final String[] users = { "userA", "userB", "userC" };

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.camel.component.dataset.DataSetSupport#populateMessage(org
     * .apache.camel.Exchange, long)
     */
    @Override
    public void populateMessage(Exchange exchange, long messageIndex)
            throws Exception {

        int user = 0 + (int) (Math.random() * ((2 - 0) + 1));

        exchange.getIn().setHeader(EftpConstants.EFTP_SOURCE_FILE,
                "dataFile" + messageIndex + ".txt");
        exchange.getIn().setHeader(EftpConstants.EFTP_SOURCE_DIRECTORY,
                "src/test/resources");
        exchange.getIn().setHeader(EftpConstants.EFTP_DESTINATION_FILE,
                "newfile" + messageIndex + ".txt");
        exchange.getIn().setHeader(EftpConstants.EFTP_DESTINATION_DIRECTORY,
                "src/test/resources");
        exchange.getIn().setHeader(EftpConstants.EFTP_SECURE_FTP, "true");

        exchange.getIn().setHeader(EftpConstants.EFTP_TARGET_IP_ADDRESS,
                "localhost");
        exchange.getIn().setHeader(EftpConstants.EFTP_TARGET_PORT, "" + 2222);
        exchange.getIn().setHeader(EftpConstants.EFTP_TARGET_USERNAME,
                users[user]);
        exchange.getIn().setHeader(EftpConstants.EFTP_TARGET_PASSWORD, "any");
    }
}
