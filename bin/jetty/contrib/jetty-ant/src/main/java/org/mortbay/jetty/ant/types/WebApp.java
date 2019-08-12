// ========================================================================
// Copyright 2006-2007 Sabre Holdings.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * Ant's WebApp object definition.
 *
 * @author Jakub Pawlowicz
 */
public class WebApp
{

    /** List of web application libraries. */
    private List libraries = new ArrayList();

    /** List of web application class directories. */
    private List classes = new ArrayList();

    /** Reference to Ant project variable. */
    private Project project;

    /** Application context path. */
    private String contextPath;

    /** Application name. */
    private String name;

    /** Application war file (either exploded or not). */
    private File warFile;

    /** Location of application web.xml file. */
    private File webXmlFile;

    /** Location of jetty-env.xml file. */
    private File jettyEnvXml;

    /** List of extra scan targets for this web application. */
    private FileSet scanTargets;

    /** List of optional context handlers. */
    private ContextHandlers contextHandlers;

    private File webDefaultXmlFile;

    /**
     * Length of interval in which application will be scanned for changes (0
     * means scanner wouldn't be created).
     */
    private int scanIntervalSeconds = 0;

    public WebApp(Project project)
    {
        this.project = project;
    }

    public File getWebDefaultXmlFile()
    {
        return this.webDefaultXmlFile;
    }

    public void setWebDefaultXmlFile(File webDefaultXmlfile)
    {
        this.webDefaultXmlFile = webDefaultXmlfile;
    }

    public void addLib(FileSet lib)
    {
        libraries.add(lib);
    }

    public List getLibraries()
    {
        return libraries;
    }

    public void addClasses(FileSet classes)
    {
        this.classes.add(classes);
    }

    public List getClasses()
    {
        return classes;
    }

    public File getWarFile()
    {
        return warFile;
    }

    public void setWarFile(File warFile)
    {
        this.warFile = warFile;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return a list of classpath files (libraries and class directories).
     */
    public List getClassPathFiles()
    {
        List classPathFiles = new ArrayList();
        Iterator classesIterator = classes.iterator();
        while (classesIterator.hasNext())
        {
            FileSet clazz = (FileSet) classesIterator.next();
            classPathFiles.add(clazz.getDirectoryScanner(project).getBasedir());
        }

        Iterator iterator = libraries.iterator();
        while (iterator.hasNext())
        {
            FileSet library = (FileSet) iterator.next();
            String[] includedFiles = library.getDirectoryScanner(project).getIncludedFiles();
            File baseDir = library.getDirectoryScanner(project).getBasedir();

            for (int i = 0; i < includedFiles.length; i++)
            {
                classPathFiles.add(new File(baseDir, includedFiles[i]));
            }
        }


        return classPathFiles;
    }

    /**
     * @return a <code>FileMatchingConfiguration</code> object describing the
     *         configuration of all libraries added to this particular web app
     *         (both classes and libraries).
     */
    public FileMatchingConfiguration getLibrariesConfiguration()
    {
        FileMatchingConfiguration config = new FileMatchingConfiguration();

        Iterator classesIterator = classes.iterator();
        while (classesIterator.hasNext())
        {
            FileSet clazz = (FileSet) classesIterator.next();
            config.addDirectoryScanner(clazz.getDirectoryScanner(project));
        }

        Iterator librariesIterator = libraries.iterator();
        while (librariesIterator.hasNext())
        {
            FileSet library = (FileSet) librariesIterator.next();
            config.addDirectoryScanner(library.getDirectoryScanner(project));
        }

        return config;
    }

    public FileMatchingConfiguration getScanTargetsConfiguration()
    {
        FileMatchingConfiguration configuration = new FileMatchingConfiguration();

        if (scanTargets != null)
        {
            configuration.addDirectoryScanner(scanTargets.getDirectoryScanner(project));
        }

        return configuration;
    }

    /**
     * @return location of web.xml file (either inside WAR or on the external
     *         location).
     */
    public File getWebXmlFile()
    {
        if (webXmlFile == null)
        {
            File webInf = new File(warFile, "WEB-INF");
            return new File(webInf, "web.xml");
        }

        return webXmlFile;
    }

    public void setWebXmlFile(File webXmlFile)
    {
        this.webXmlFile = webXmlFile;
    }

    public void addScanTargets(FileSet scanTargets)
    {
        if (this.scanTargets != null)
        {
            throw new BuildException("Only one <scanTargets> tag is allowed!");
        }

        this.scanTargets = scanTargets;
    }

    public void addContextHandlers(ContextHandlers contextHandlers)
    {
        if (this.contextHandlers != null)
        {
            throw new BuildException("Only one <contextHandlers> tag is allowed!");
        }

        this.contextHandlers = contextHandlers;
    }

    public int getScanIntervalSeconds()
    {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds)
    {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public File getJettyEnvXml()
    {
        return jettyEnvXml;
    }

    public void setJettyEnvXml(File jettyEnvXml)
    {
        this.jettyEnvXml = jettyEnvXml;
    }

    public List getContextHandlers()
    {
        return (contextHandlers != null ? contextHandlers.getContextHandlers() : new ArrayList());
    }
}
