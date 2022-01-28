package org.apache.maven.shared.release.phase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.junit.Assert.assertEquals;

import org.apache.maven.shared.release.PlexusJUnit4TestCase;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.junit.Test;

/**
 * Test the the end release phase. Nothing to see here really, but we want to make sure it is configured.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class EndReleasePhaseTest
    extends PlexusJUnit4TestCase
{
    private ReleasePhase phase;

    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "end-release" );
    }

    @Test
    public void testExecute()
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = phase.execute( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        assertEquals( ReleaseResult.SUCCESS, result.getResultCode() );
    }

    @Test
    public void testSimulate()
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = phase.simulate( new ReleaseDescriptor(), new DefaultReleaseEnvironment(), null );

        assertEquals( ReleaseResult.SUCCESS, result.getResultCode() );
    }
}
