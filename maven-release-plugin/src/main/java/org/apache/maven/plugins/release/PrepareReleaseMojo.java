package org.apache.maven.plugins.release;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.config.ReleaseUtils;

import java.util.Arrays;

/**
 * Prepare for a release in SCM.
 * For more info see <a href="/plugins/maven-release-plugin/examples/prepare-release.html">this example</a>.
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @aggregator
 * @goal prepare
 * @todo [!] check how this works with version ranges
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{

    /**
     * Resume a previous release attempt from the point where it was stopped.
     *
     * @parameter expression="${resume}" default-value="true"
     */
    private boolean resume;

    /**
     * @deprecated Please use release:prepare-with-pom instead.
     *
     * @parameter default-value="false" expression="${generateReleasePoms}"
     */
    private boolean generateReleasePoms;

    /**
     * Whether to use "edit" mode on the SCM, to lock the file for editing during SCM operations.
     *
     * @parameter expression="${useEditMode}" default-value="false"
     */
    private boolean useEditMode;

    /**
     * Whether to update dependencies version to the next development version.
     *
     * @parameter expression="${updateDependencies}" default-value="true"
     * @since 2.0-beta-5
     */
    private boolean updateDependencies;

    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     * @parameter expression="${autoVersionSubmodules}" default-value="false"
     * @since 2.0-beta-5
     */
    private boolean autoVersionSubmodules;

    /**
     * Dry run: don't checkin or tag anything in the scm repository, or modify the checkout. Running
     * <code>mvn -DdryRun=true release:prepare</code> is useful in order to check that modifications to poms and scm
     * operations (only listed on the console) are working as expected. Modified POMs are written alongside the
     * originals without modifying them.
     *
     * @parameter expression="${dryRun}" default-value="false"
     */
    private boolean dryRun;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     *
     * @parameter expression="${addSchema}" default-value="true"
     */
    private boolean addSchema;

    /**
     * Goals to run as part of the preparation step, after transformation but before committing. Space delimited.
     *
     * @parameter expression="${preparationGoals}" default-value="clean verify"
     */
    private String preparationGoals;

    /**
     * Commits to do are atomic or by project.
     *
     * @parameter expression="${commitByProject}" default-value="false"
     * @since 2.0-beta-5
     */
    private boolean commitByProject;

    /**
     * Whether to allow timestamped SNAPSHOT dependencies. Default is to fail when finding any SNAPSHOT.
     *
     * @parameter expression="${ignoreSnapshots}" default-value="false"
     * @since 2.0-beta-7
     */
    private boolean allowTimestampedSnapshots;

    /**
     * Whether to allow usage of a SNAPSHOT version of the Release Plugin. This in an internal property used to support
     * testing of the plugin itself in batch mode.
     *
     * @parameter expression="${allowReleasePluginSnapshot}" default-value="false"
     * @readonly
     * @since 2.0-beta-9
     */
    private boolean allowReleasePluginSnapshot;

     /**
     * Additional files that will skipped when checking for
     * modifications on the working copy.
     *
     * Is ignored, when checkModificationExcludes is set.
     *
     *
     * @parameter
     * @since 2.1
     */
    private String[] checkModificationExcludes;

    /**
     * Command-line version of checkModificationExcludes
     *
     *
     * @parameter expression="${checkModificationExcludeList}"
     * @since 2.1
     */
    private String checkModificationExcludeList;

    /**
     * Default version to use when preparing a release or a branch.
     *
     * @parameter expression="${releaseVersion}"
     * @since 2.0-beta-8
     */
    private String releaseVersion;

    /**
     * Default version to use for new local working copy.
     *
     * @parameter expression="${developmentVersion}"
     * @since 2.0-beta-8
     */
    private String developmentVersion;

    /**
     * currently only implemented with svn scm. Enable a workaround to prevent issue
     * due to svn client > 1.5.0 (http://jira.codehaus.org/browse/SCM-406)
     *
     *
     * @parameter expression="${remoteTagging}" default-value="true"
     * @since 2.0-beta-9
     */
    private boolean remoteTagging;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.0
     */
    protected MavenSession session;

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( generateReleasePoms )
        {
            throw new MojoFailureException(
                "Generating release POMs is no longer supported in release:prepare. Please run release:prepare-with-pom instead." );
        }

        prepareRelease( generateReleasePoms );
    }

    protected void prepareRelease( boolean generateReleasePoms )
        throws MojoExecutionException, MojoFailureException
    {
        // this is here so the subclass can call it without getting the extra generateReleasePoms check in execute() above
        super.execute();

        ReleaseDescriptor config = createReleaseDescriptor();
        config.setAddSchema( addSchema );
        config.setGenerateReleasePoms( generateReleasePoms );
        config.setScmUseEditMode( useEditMode );
        config.setPreparationGoals( preparationGoals );
        config.setCommitByProject( commitByProject );
        config.setUpdateDependencies( updateDependencies );
        config.setAutoVersionSubmodules( autoVersionSubmodules );
        config.setAllowTimestampedSnapshots( allowTimestampedSnapshots );
        config.setSnapshotReleasePluginAllowed( allowReleasePluginSnapshot );
        config.setDefaultReleaseVersion( releaseVersion );
        config.setDefaultDevelopmentVersion( developmentVersion );
        config.setRemoteTagging( remoteTagging );

        if (checkModificationExcludeList != null) {
           checkModificationExcludes = checkModificationExcludeList
                   .replaceAll("\\s", "")
                   .split(",");
        }

        if (checkModificationExcludes != null) {
            config.setCheckModificationExcludes( Arrays.asList( checkModificationExcludes ) );
        }

        // Create a config containing values from the session properties (ie command line properties with cli).
        ReleaseDescriptor sysPropertiesConfig
                = ReleaseUtils.copyPropertiesToReleaseDescriptor( session.getExecutionProperties() );
        mergeCommandLineConfig( config, sysPropertiesConfig );

        try
        {
            releaseManager.prepare( config, getReleaseEnvironment(), reactorProjects, resume, dryRun );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }

    /**
     * This method takes some of the release configuration picked up from the command line system properties and copies
     * it into the release config object.
     *
     * @param config The release configuration to merge the system properties into, must not be <code>null</code>.
     * @param sysPropertiesConfig The configuration from the system properties to merge in, must not be
     *            <code>null</code>.
     */
    private void mergeCommandLineConfig( ReleaseDescriptor config, ReleaseDescriptor sysPropertiesConfig )
    {
        // If the user specifies versions, these should override the existing versions
        if ( sysPropertiesConfig.getReleaseVersions() != null )
        {
            config.getReleaseVersions().putAll( sysPropertiesConfig.getReleaseVersions() );
        }
        if ( sysPropertiesConfig.getDevelopmentVersions() != null )
        {
            config.getDevelopmentVersions().putAll( sysPropertiesConfig.getDevelopmentVersions() );
        }
    }

}
