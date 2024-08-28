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

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class EftpUtilitiesTest {

    /**
     * Test method for
     * {@link com.ericsson.oss.mediation.camel.components.eftp.utils.EftpUtilities#createFilePathWithSeparator(java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testCreateFilePathWithSeparator_SeparatorMissing() {
        final String srcFile = "test.txt";
        final String srcDir = "tmp";

        assertEquals("tmp" + File.separator + srcFile,
                EftpUtilities.createFilePathWithSeparator(srcDir, srcFile));
    }

    @Test
    public void testCreateFilePathWithSeparator_SeparatorPresent() {
        final String srcFile = "test.txt";
        final String srcDir = "tmp" + File.separator;
        assertEquals("tmp" + File.separator + srcFile,
                EftpUtilities.createFilePathWithSeparator(srcDir, srcFile));
    }

    @Test
    public void testNormalizeSourceFilePath_NoSeparatorsPresent() {
        final String srcFile = "A20130316.2300-2315:1.xml.gz";
        final String srcDir = "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data";
        final String fileToGet = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        assertEquals(srcDir + File.separator + srcFile, fileToGet);
    }

    @Test
    public void testNormalizeSourceFilePath_BothSeparatorsPresent() {
        final String srcFile = "/A20130316.2300-2315:1.xml.gz";
        final String srcDir = "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data/";
        final String fileToGet = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        assertEquals(
                "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data/A20130316.2300-2315:1.xml.gz",
                fileToGet);
    }

    @Test
    public void testNormalizeSourceFilePath_DirSeparatorPresentFileSeparatorNotPresent() {
        final String srcFile = "A20130316.2300-2315:1.xml.gz";
        final String srcDir = "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data/";
        final String fileToGet = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        assertEquals(
                "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data/A20130316.2300-2315:1.xml.gz",
                fileToGet);
    }

    @Test
    public void testNormalizeSourceFilePath_DirSeparatorNotPresentFileSeparatorPresent() {
        final String srcFile = "/A20130316.2300-2315:1.xml.gz";
        final String srcDir = "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data";
        final String fileToGet = EftpUtilities.normalizeSourceFilePath(srcDir,
                srcFile);
        assertEquals(
                "/netsim/netsim_dbdir/simdir/netsim/netsimdir/LTED125-V2x160-ST-FDD-LTE04/LTE04ERBS00108/fs/c/pm_data/A20130316.2300-2315:1.xml.gz",
                fileToGet);
    }
}
