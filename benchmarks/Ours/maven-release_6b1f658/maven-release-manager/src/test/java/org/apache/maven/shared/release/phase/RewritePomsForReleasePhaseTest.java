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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.junit.Test;

/**
 * Test the SCM modification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhaseTest
    extends AbstractEditModeRewritingReleasePhaseTestCase
{
    private static final String NEXT_VERSION = "1.0";

    private static final String ALTERNATIVE_NEXT_VERSION = "2.0";

    public RewritePomsForReleasePhaseTest( String modelETL )
    {
        super( modelETL );
    }

    @Override
    protected String getRoleHint()
    {
        return "rewrite-poms-for-release";
    }

    protected List<MavenProject> prepareReactorProjects( String path, boolean copyFiles )
        throws Exception
    {
        return createReactorProjects( "rewrite-for-release/", path );
    }

    protected String readTestProjectFile( String fileName )
        throws IOException
    {
        return ReleaseUtil.readXmlFile( getTestFile( "target/test-classes/projects/rewrite-for-release/" + fileName ) );
    }

    @Test
    public void testSimulateRewrite()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom/pom.xml" );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        String actual = readTestProjectFile( "basic-pom/pom.xml" );
        assertEquals( "Check the original POM untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom/pom.xml.tag" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    @Test
    public void testRewriteWithDashedComments()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-dashes-in-comment" );
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        String expected = readTestProjectFile( "basic-pom-with-dashes-in-comment/pom.xml" );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        String actual = readTestProjectFile( "basic-pom-with-dashes-in-comment/pom.xml" );
        assertEquals( "Check the original POM is untouched", expected, actual );

        expected = readTestProjectFile( "basic-pom-with-dashes-in-comment/expected-pom.xml" );
        actual = readTestProjectFile( "basic-pom-with-dashes-in-comment/pom.xml.tag" );
        assertEquals( "Check the transformed POM", expected, actual );
    }

    @Test
    public void testClean()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    @Test
    public void testCleanNotExists()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjectsFromBasicPom();
        ReleaseDescriptor config = createDescriptorFromBasicPom( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );

        File testFile = getTestFile( "target/test-classes/projects/rewrite-for-release/basic-pom/pom.xml.tag" );
        testFile.delete();
        assertFalse( testFile.exists() );

        phase.clean( reactorProjects );

        assertFalse( testFile.exists() );
    }

    // MRELEASE-116
    @Test
    public void testScmOverridden()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-overridden-scm" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );
        config.mapReleaseVersion( "groupId:subsubproject", NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    protected void mapAlternateNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, ALTERNATIVE_NEXT_VERSION );
    }

    protected void mapNextVersion( ReleaseDescriptor config, String projectId )
    {
        config.mapReleaseVersion( projectId, NEXT_VERSION );
    }

    protected ReleaseDescriptor createConfigurationForPomWithParentAlternateNextVersion( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        return config;
    }

    protected ReleaseDescriptor createConfigurationForWithParentNextVersion( List<MavenProject> reactorProjects )
        throws Exception
    {
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", NEXT_VERSION );
        return config;
    }

    protected void unmapNextVersion( ReleaseDescriptor config, String projectId )
    {
        // nothing to do
    }

    @Test
    public void testRewriteBasicPomWithCvs()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-cvs" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithScmExpression()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-scm-expression" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithTagBase()
        throws Exception
    {

        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-tag-base" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.setScmTagBase( "file://localhost/tmp/scm-repo/releases" );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithTagBaseAndVaryingScmUrls()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-tag-base-and-varying-scm-urls" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.setScmTagBase( "file://localhost/tmp/scm-repo/allprojects/releases" );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithCvsFromTag()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-cvs-from-tag" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteBasicPomWithEmptyScm()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-with-empty-scm" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteInterpolatedVersions()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "interpolated-versions" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewriteInterpolatedVersionsDifferentVersion()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "interpolated-versions" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );

        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject3", NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        for ( Iterator<MavenProject> i = reactorProjects.iterator(); i.hasNext(); )
        {
            MavenProject project = i.next();

            // skip subproject1 - we don't need to worry about its version mapping change, it has no deps of any kind
            if ( !"groupId".equals( project.getGroupId() ) || !"subproject1".equals( project.getArtifactId() ) )
            {
                comparePomFiles( project, "-different-version", true );
            }
        }
    }

    @Test
    public void testRewriteBasicPomWithInheritedScm()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "basic-pom-inherited-scm" );
        ReleaseDescriptor config = createConfigurationForWithParentNextVersion( reactorProjects );
        config.mapReleaseVersion( "groupId:subsubproject", NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithParentAndProperties()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-parent-and-properties" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    // MRELEASE-311
    @Test
    public void testRewritePomWithDependencyPropertyCoordinate()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-property-dependency-coordinate" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1-3.4", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    // MRELEASE-305
    @Test
    public void testRewritePomWithScmOfParentEndingWithASlash()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-scm-of-parent-ending-with-a-slash" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithDeepSubprojects()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "multimodule-with-deep-subprojects" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapReleaseVersion( "groupId:artifactId", NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomForFlatMultiModule()
        throws Exception
    {
        List<MavenProject> reactorProjects =
            createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "/root-project" );
        ReleaseDescriptor config = createConfigurationForPomWithParentAlternateNextVersion( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    // MRELEASE-383
    @Test
    public void testRewritePomWithCDATASectionOnWindows()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "cdata-section" );
        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        mapNextVersion( config, "groupId:artifactId" );

        RewritePomsForReleasePhase phase = (RewritePomsForReleasePhase) this.phase;
        phase.setLs( "\r\n" );
        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        // compare POMS without line ending normalization
        assertTrue( comparePomFiles( reactorProjects, false ) );
    }

    protected ReleaseDescriptor createDescriptorFromProjects( List<MavenProject> reactorProjects )
    {
        ReleaseDescriptor descriptor = super.createDescriptorFromProjects( reactorProjects );
        descriptor.setScmReleaseLabel( "release-label" );
        return descriptor;
    }

    @Test
    public void testRewritePomWithExternallyReleasedParent()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "pom-with-externally-released-parent" );

        ReleaseDescriptor config = createDescriptorFromProjects( reactorProjects );
        config.mapResolvedSnapshotDependencies( "external:parent-artifactId", "1", "2-SNAPSHOT" );
        config.mapReleaseVersion( "groupId:subproject1", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    // MRELEASE-454
    @Test
    public void testRewritePomWithImportedDependencyManagementInReactor()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "imported-dependency-management-in-reactor" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

    @Test
    public void testRewritePomWithDifferentVersionsAcrossModules()
        throws Exception
    {
        List<MavenProject> reactorProjects = createReactorProjects( "modules-with-different-versions" );
        ReleaseDescriptor config = createMappedConfiguration( reactorProjects );
        config.mapReleaseVersion( "groupId:subproject2", ALTERNATIVE_NEXT_VERSION );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        assertTrue( comparePomFiles( reactorProjects ) );
    }

}
