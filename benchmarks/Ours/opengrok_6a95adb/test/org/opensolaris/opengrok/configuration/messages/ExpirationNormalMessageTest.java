/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

 /*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 */
package org.opensolaris.opengrok.configuration.messages;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.TreeSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

public class ExpirationNormalMessageTest {

    RuntimeEnvironment env;

    private Message[] makeArray(Message... messages) {
        return messages;
    }

    @Before
    public void setUp() {
        env = RuntimeEnvironment.getInstance();
        env.removeAllMessages();
    }

    @After
    public void tearDown() {
        env.removeAllMessages();
    }

    @Test
    public void testExpirationSingle() {
        runSingle();
    }

    @Test
    public void testExpirationMultiple() {
        runMultiple();
    }

    /**
     * This doesn't make sense since we're testing the behaviour of the timer
     * thread.
     */
    @Test
    public void testExpirationConcurrent() throws Exception {
        for (int i = 0; i < 10; i++) {
            runConcurrentModification();
        }
    }

    @Test
    public void testExpirationConcurrentTimer() throws Exception {
        env.startExpirationTimer();
        for (int i = 0; i < 10; i++) {
            runConcurrentModification();
        }
        env.stopExpirationTimer();
    }

    protected void runSingle() {
        Assert.assertEquals(0, env.getMessagesInTheSystem());
        NormalMessage m1 = new NormalMessage();
        m1.addTag("main")
                .setExpiration(new Date(System.currentTimeMillis() + 2000000));
        m1.setText("text");
        env.addMessage(m1);
        Assert.assertEquals(1, env.getMessagesInTheSystem());

        for (int i = 0; i < 50; i++) {
            Assert.assertEquals(1, env.getMessagesInTheSystem());
            Assert.assertNotNull(env.getMessages());
            Assert.assertEquals(new TreeSet<Message>(Arrays.asList(makeArray(m1))), env.getMessages());
        }
        m1.setExpiration(new Date(System.currentTimeMillis() - 2000000));
        Assert.assertEquals(0, env.getMessagesInTheSystem());
    }

    protected void runMultiple() {
        Assert.assertEquals(0, env.getMessagesInTheSystem());
        NormalMessage m1 = new NormalMessage();
        m1.addTag("main")
                .setExpiration(new Date(System.currentTimeMillis() + 2000000));
        m1.setText("text");
        env.addMessage(m1);

        NormalMessage m2 = new NormalMessage();
        m2.addTag("main")
                .setExpiration(new Date(System.currentTimeMillis() + 2000000));
        m2.setText("other text");
        env.addMessage(m2);

        Assert.assertEquals(2, env.getMessagesInTheSystem());
        Assert.assertNotNull(env.getMessages());
        Assert.assertEquals(new TreeSet<Message>(Arrays.asList(makeArray(m1, m2))), env.getMessages());

        for (int i = 0; i < 30; i++) {
            Assert.assertEquals(2, env.getMessagesInTheSystem());
            Assert.assertNotNull(env.getMessages());
            Assert.assertEquals(new TreeSet<Message>(Arrays.asList(makeArray(m1, m2))), env.getMessages());
        }
        // expire first
        m1.setExpiration(new Date(System.currentTimeMillis() - 2000000));
        for (int i = 0; i < 30; i++) {
            Assert.assertEquals(1, env.getMessagesInTheSystem());
            Assert.assertNotNull(env.getMessages());
            Assert.assertEquals(new TreeSet<Message>(Arrays.asList(makeArray(m2))), env.getMessages());
        }
        // expire second
        m2.setExpiration(new Date(System.currentTimeMillis() - 2000000));
        Assert.assertEquals(0, env.getMessagesInTheSystem());
    }

    protected void runConcurrentModification() throws Exception {
        long current = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            NormalMessage m = new NormalMessage();
            m.addTag("main");
            m.setText("text");
            m.setExpiration(new Date(current + 2000000));
            m.setCreated(new Date(current - 2000 - i));
            m.apply(env);
        }

        Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread th, Throwable ex) {
                if (ex instanceof ConcurrentModificationException) {
                    Assert.fail("The messages shouldn't throw an concurrent modification exception");
                } else {
                    Assert.fail("The messages shouldn't throw any other exception, too");
                }
            }
        };

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                invokeExpireMessages();
            }
        });
        t.setUncaughtExceptionHandler(h);

        Assert.assertEquals(500, env.getMessagesInTheSystem());
        Assert.assertEquals(500, env.getMessages("main").size());

        // expire all
        for (Message m : env.getMessages("main")) {
            m.setExpiration(new Date(current - 2000000));
        }

        for (int i = 0; i < 500; i++) {
            if (i == 100) {
                t.start();
            }
            try {
                for (Message m : env.getMessages("main")) {
                    m.setText("Hello message");
                    Assert.assertNotNull(m.getText());
                }
            } catch (ConcurrentModificationException ex) {
                Assert.fail("The messages shouldn't throw an concurrent modification exception");
            } catch (Throwable ex) {
                Assert.fail("The messages shouldn't throw any other exception, too");
            }
        }
        try {
            t.join();
        } catch (InterruptedException ex) {
        }
        Assert.assertEquals(0, env.getMessagesInTheSystem());
        Assert.assertEquals(0, env.getMessages("main").size());
    }

    private void invokeExpireMessages() {
        try {
            Method method = RuntimeEnvironment.class.getDeclaredMethod("expireMessages");
            method.setAccessible(true);
            method.invoke(env);
        } catch (Exception ex) {
            Assert.fail("invokeRemoveAll should not throw an exception");
        }
    }
}
