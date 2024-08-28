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
package com.ericsson.oss.mediation.camel.components.eftp.exceptions;

/**
 * 
 * <p>
 * An exception which allows capturing of error response codes and error
 * messages from remote systems or remote access protocols such as FTP, SSH etc.
 * </p>
 * 
 * @author etonayr
 * 
 */
public class GenericEftpException extends Exception {

    private static final long serialVersionUID = 803164849596097693L;
    private int errorCode = -1; // default errorCode value
    private final String errorDescription;

    /**
     * Creates a <code>GenericEftpException</code> with the specified error code
     * and error description.
     * 
     * @param errorCode
     * @param errorDescription
     */
    public GenericEftpException(final int errorCode,
            final String errorDescription) {
        super(errorDescription);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * Creates a <code>GenericEftpException</code> with the specified error
     * code, error description and the underlying cause.
     * 
     * @param errorCode
     * @param errorDescription
     * @param error
     */
    public GenericEftpException(final int errorCode,
            final String errorDescription, final Throwable error) {
        super(error);
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    /**
     * Creates a <code>GenericEftpException</code> with an error description and
     * the underlying cause.
     * 
     * @param errorDescription
     * @param error
     */
    public GenericEftpException(final String errorDescription,
            final Throwable error) {
        super(error);
        this.errorDescription = errorDescription;
    }

    /**
     * Returns the error code of a <code>GenericEftpException</code>. If no
     * error code was created in the construction of the exception, the default
     * value of -1 is used.
     * 
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @return the errorDescription
     */
    public String getErrorDescription() {
        return errorDescription;
    }

}
