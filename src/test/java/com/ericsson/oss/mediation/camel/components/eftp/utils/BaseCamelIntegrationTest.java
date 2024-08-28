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

import java.util.ArrayList;
import java.util.List;

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
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class BaseCamelIntegrationTest extends CamelTestSupport {

    protected static SshServer sshd;
    protected static int sshPort = 2222;

    @BeforeClass
    public static void startSshServer() {

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(sshPort);
        sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(final String username,
                    final String password, final ServerSession session) {
                return true;
            }
        });
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setFileSystemFactory(new NativeFileSystemFactory());
        sshd.setCommandFactory(new ScpCommandFactory());

        List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>(
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

    @AfterClass
    public static void shutdownSshServer() {
        try {
            if (sshd != null) {
                sshd.stop(true);
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

}
