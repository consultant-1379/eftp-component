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

import java.io.File;

import com.jcraft.jsch.SftpException;

/**
 * A class for utilities related to the EFTP Camel Component Endpoint
 * 
 * @author etonayr
 * 
 */
public final class EftpUtilities {

    private EftpUtilities() {

    }

    /**
     * This method will create full path for sourceDir and sourceFile, checking
     * that either sourceDir ends with file separator, or sourceFile begins with
     * file separator.
     * 
     * <p>
     * sourceDirectory = /var/tmp (no separator)<br>
     * sourceFileName = file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * <p>
     * sourceDirectory = /var/tmp/<b>/</b> (separator present on sourceDirectory
     * but not on file)<br>
     * sourceFileName = file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * <p>
     * sourceDirectory = /var/tmp<b>/</b> (separator present on sourceFile but
     * not on sourceDir)<br>
     * sourceFileName = /file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * <p>
     * sourceDirectory = /var/tmp/<b>/</b> (separator present on sourceDirectory
     * and on sourceFile)<br>
     * sourceFileName = /file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * @param sourceDirectory
     * @param sourceFileName
     * @return path name
     */
    public static String normalizeSourceFilePath(final String srcDir,
            final String srcFile) {
        if (srcDir.endsWith(File.separator)
                && srcFile.startsWith(File.separator)) {
            return srcDir.substring(0, srcDir.length() - 1) + srcFile;
        } else if (srcDir.endsWith(File.separator)
                && !srcFile.startsWith(File.separator)) {
            return srcDir + srcFile;
        } else if (!srcDir.endsWith(File.separator)
                && srcFile.startsWith(File.separator)) {
            return srcDir + srcFile;
        } else {
            return srcDir + File.separator + srcFile;
        }

    }

    /**
     * This methods creates a partial file path by joining the specified source
     * directory and source file name parameters. The method checks if the
     * system file separator is present on the source directory parameter and if
     * it is not it will add it e.g.
     * 
     * <p>
     * sourceDirectory = /var/tmp (no separator)<br>
     * sourceFileName = file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * <p>
     * sourceDirectory = /var/tmp<b>/</b> (separator present)<br>
     * sourceFileName = file.txt<br>
     * returns /var/tmp/file.txt
     * </p>
     * 
     * @param sourceDirectory
     * @param sourceFileName
     * @return path name
     */
    public static String createFilePathWithSeparator(
            final String sourceDirectory, final String sourceFileName) {
        final String systemSeparator = System.getProperty("file.separator");
        final boolean endsWithSeparator = sourceDirectory
                .endsWith(systemSeparator);

        final StringBuilder builder = new StringBuilder();
        builder.append(sourceDirectory);

        if (!endsWithSeparator) {
            builder.append(systemSeparator);
        }
        builder.append(sourceFileName);
        if (sourceDirectory.startsWith(systemSeparator)) {
            builder.deleteCharAt(0);
        }
        return builder.toString();
    }

    /**
     * <p>
     * This method extracts the error code from a JschException. The error code
     * is the first item in the String when calling toString on a JschException
     * and is followed by a colon (:)
     * </p>
     * <p>
     * Example<br>
     * <b>0: Some error message description....</b>
     * </p>
     * 
     * @param exception
     *            - SftpException
     * @return the integer error code
     */
    public static int extractSftpErrorCodeException(
            final SftpException exception) {
        final String errorMessage = exception.toString();
        final String errorCodeString = errorMessage.substring(0,
                errorMessage.indexOf(":")).trim();
        return Integer.parseInt(errorCodeString);
    }
}
