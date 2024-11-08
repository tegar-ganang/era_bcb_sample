package org.nexopenframework.ide.eclipse.hibernate3.launch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.ext.actions.InstallProjectAction;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.Assert;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerCallback;
import org.nexopenframework.ide.eclipse.commons.xml.ContentHandlerTemplate;
import org.nexopenframework.ide.eclipse.commons.xml.XMLUtils;
import org.nexopenframework.ide.eclipse.hibernate3.HibernateActivator;
import org.nexopenframework.ide.eclipse.hibernate3.model.Dependency;
import org.nexopenframework.ide.eclipse.hibernate3.util.DriverClassSupport;
import org.nexopenframework.ide.eclipse.ui.NexOpenUIActivator;
import org.nexopenframework.ide.eclipse.ui.util.NexOpenProjectUtils;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>
 * Launches the maven2 goal in order to generate the classes (EJB 3.0 entities,
 * Business Facades, Controllers and Views). Follows methodology RAD.
 * </p>
 * 
 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class NexOpenHibernateLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

    /**driver class handler*/
    private static final DriverClassSupport support = new DriverClassSupport();

    /**
	 * <p>
	 * Basically creates the configuration necessary in a business Maven2
	 * <code>pom.xml</code> and runs the Maven 2 command <b><code>mvn clean install -Preverse-engineering</code></b>
	 * sentence for given profile <code>reverse-engineering</code>
	 * </p>
	 * 
	 * @see InstallProjectAction#scheduleJob(IProject, IProgressMonitor)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration,
	 *      java.lang.String, org.eclipse.debug.core.ILaunch,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
    @SuppressWarnings("unchecked")
    public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, final IProgressMonitor monitor) throws CoreException {
        {
            Assert.notNull(configuration);
            Assert.notNull(monitor);
        }
        final String projectName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.NEXOPEN_PROJECT_NAME, "");
        final IProject prj = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProject(projectName).getProject();
        if (NexOpenProjectUtils.isNexOpenProject(prj)) {
            final IFile pom = prj.getFile("pom.xml");
            if (!pom.exists()) {
                throw new IllegalStateException("Not a NexOpen project. Not Maven2 root pom.xml available");
            }
            ContentHandlerTemplate.handle(pom, new ContentHandlerCallback() {

                public void processHandle(final Document doc) {
                    handleRootProfile(doc);
                }
            });
            final IFile bpom = prj.getFile("business/pom.xml");
            if (!bpom.exists()) {
                throw new IllegalStateException("Not a NexOpen project. Not Maven2 business pom.xml available");
            }
            ContentHandlerTemplate.handle(bpom, new ContentHandlerCallback() {

                public void processHandle(final Document doc) {
                    try {
                        handleBusinessProfile(doc, configuration, prj);
                    } catch (final CoreException e) {
                        if (Logger.getLog().isInfoEnabled()) {
                            Logger.getLog().info("CoreException", e);
                        }
                        throw new RuntimeException(e);
                    }
                }
            });
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                final Properties props = new Properties();
                final String dialectName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.HIBERNATE_DIALECT, "MySQL5InnoDB");
                props.setProperty("hibernate.dialect", support.getDialectClass(dialectName));
                props.setProperty("hibernate.connection.driver_class", configuration.getAttribute(INexOpenLaunchConfigurationConstants.JDBC_DRIVER, "com.mysql.jdbc.Driver"));
                props.setProperty("hibernate.connection.url", configuration.getAttribute(INexOpenLaunchConfigurationConstants.JDBC_URL, "jdbc:mysql://<host><:port>/<database>"));
                props.setProperty("hibernate.connection.username", configuration.getAttribute(INexOpenLaunchConfigurationConstants.JDBC_USERNAME, "sa"));
                props.setProperty("hibernate.connection.password", configuration.getAttribute(INexOpenLaunchConfigurationConstants.JDBC_PASSWORD, ""));
                props.store(output, "hibernate properties for code generation using NexOpen Tools 1.0.0");
                final IFile props_file = prj.getFile("business/src/test/resources/hibernate.properties");
                if (!props_file.exists()) {
                    props_file.create(new ByteArrayInputStream(output.toByteArray()), true, monitor);
                } else {
                    props_file.setContents(new ByteArrayInputStream(output.toByteArray()), true, false, monitor);
                }
            } catch (final IOException e) {
                Logger.getLog().error("I/O exception ", e);
                throw new RuntimeException(e);
            } finally {
                try {
                    output.flush();
                    output.close();
                } catch (IOException e) {
                }
            }
            if (NexOpenProjectUtils.is04xProject(prj)) {
                final IFile appContext = prj.getFile("web/src/main/webapp/WEB-INF/applicationContext.xml");
                if (!appContext.exists()) {
                    throw new IllegalStateException("It no exists applicationContext.xml under web/src/main/webapp/WEB-INF, not a NexOpen project");
                }
                ContentHandlerTemplate.handle(appContext, new ContentHandlerCallback() {

                    public void processHandle(final Document doc) {
                        final Element root = doc.getDocumentElement();
                        final List<Element> beans = XMLUtils.getChildElementsByTagName(root, "bean");
                        for (final Element bean : beans) {
                            final String id = bean.getAttribute("id");
                            if ("valueListAdapterResolver".equals(id)) {
                                try {
                                    final String pkgName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.NEXOPEN_PACKAGE, "");
                                    final String className = new StringBuilder(pkgName).append(".vlh.support.AnnotationValueListAdapterResolver").toString();
                                    bean.setAttribute("class", className);
                                    break;
                                } catch (final CoreException e) {
                                    if (Logger.getLog().isInfoEnabled()) {
                                        Logger.getLog().info("CoreException", e);
                                    }
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
            }
            {
                final String dialectName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.HIBERNATE_DIALECT, "MySQL5InnoDB");
                if (support.isReverseEngineeringFileNeeded(dialectName)) {
                    try {
                        final IFile revengFile = prj.getFile("business/src/test/resources/" + support.getReversEngineeringFile(dialectName));
                        if (!revengFile.exists()) {
                            final Bundle bundle = HibernateActivator.getDefault().getBundle();
                            final Path src = new Path("resources/" + support.getReversEngineeringFile(dialectName));
                            final InputStream in = FileLocator.openStream(bundle, src, false);
                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            IOUtils.copy(in, baos);
                            String str = baos.toString();
                            str = str.replace("${schema}", configuration.getAttribute(INexOpenLaunchConfigurationConstants.JDBC_USERNAME, "sa"));
                            revengFile.create(new ByteArrayInputStream(str.getBytes()), true, null);
                        }
                    } catch (final IOException e) {
                        if (Logger.getLog().isInfoEnabled()) {
                            Logger.getLog().info("CoreException", e);
                        }
                        throw new RuntimeException(e);
                    }
                }
            }
            final IResource resource = (IResource) prj.getAdapter(IResource.class);
            final QualifiedName qn = new QualifiedName("org.nexopenframework.ide.eclipse.ui", "default.profile");
            final String profile = resource.getPersistentProperty(qn);
            resource.setPersistentProperty(qn, "reverse-engineering");
            try {
                final InstallProjectAction action = new InstallProjectAction();
                action.scheduleJob(prj, monitor);
                prj.refreshLocal(2, monitor);
            } finally {
                prj.setPersistentProperty(qn, profile);
            }
        } else {
            Logger.getLog().info("Not a NexOpen project :: " + prj);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleBusinessProfile(final Document doc, final ILaunchConfiguration configuration, final IProject prj) throws CoreException {
        final Element root = doc.getDocumentElement();
        Element profiles = XMLUtils.getChildElementByTagName(root, "profiles");
        if (profiles == null) {
            profiles = doc.createElement("profiles");
            root.appendChild(profiles);
        }
        final List<Element> elem_profiles = XMLUtils.getChildElementsByTagName(profiles, "profile");
        boolean exists = false;
        for (final Element elem : elem_profiles) {
            final Element id = XMLUtils.getChildElementByTagName(elem, "id");
            final String strId = id.getTextContent();
            if ("reverse-engineering".equals(strId)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            Logger.getLog().debug("create Maven2 profile reverse-engineering in business pom.xml");
            final Element profile = doc.createElement("profile");
            final Element id = doc.createElement("id");
            id.setTextContent("reverse-engineering");
            final Element build = doc.createElement("build");
            final Element plugins = doc.createElement("plugins");
            final Element plugin = doc.createElement("plugin");
            final Element groupId = doc.createElement("groupId");
            groupId.setTextContent("org.codehaus.mojo");
            final Element artifactId = doc.createElement("artifactId");
            artifactId.setTextContent("hibernatereveng-maven-plugin");
            final Element version = doc.createElement("version");
            final String reveng_version = NexOpenUIActivator.getDefault().getReverseEngVersion();
            version.setTextContent(reveng_version);
            plugin.appendChild(groupId);
            plugin.appendChild(artifactId);
            plugin.appendChild(version);
            createExecutions(doc, plugin, configuration, prj);
            createDependencies(doc, plugin, configuration);
            plugins.appendChild(plugin);
            build.appendChild(plugins);
            profile.appendChild(id);
            profile.appendChild(build);
            profiles.appendChild(profile);
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleRootProfile(final Document doc) {
        final Element root = doc.getDocumentElement();
        final Element profiles = XMLUtils.getChildElementByTagName(root, "profiles");
        final List<Element> elem_profiles = XMLUtils.getChildElementsByTagName(profiles, "profile");
        boolean exists = false;
        for (final Element elem : elem_profiles) {
            final Element id = XMLUtils.getChildElementByTagName(elem, "id");
            final String strId = id.getTextContent();
            if ("reverse-engineering".equals(strId)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            Logger.getLog().debug("create Maven2 profile reverse-engineering in root pom.xml");
            final Element profile = doc.createElement("profile");
            final Element id = doc.createElement("id");
            id.setTextContent("reverse-engineering");
            final Element props = doc.createElement("properties");
            final Element jclScope = doc.createElement("jclScope");
            jclScope.setTextContent("provided");
            final Element jtaScope = doc.createElement("jtaScope");
            jtaScope.setTextContent("provided");
            final Element jstlScope = doc.createElement("jstlScope");
            jstlScope.setTextContent("provided");
            props.appendChild(jclScope);
            props.appendChild(jtaScope);
            props.appendChild(jstlScope);
            profile.appendChild(id);
            profile.appendChild(props);
            profiles.appendChild(profile);
        }
    }

    /**
	 * @param doc
	 * @param plugin
	 * @param configuration
	 * @param prj
	 * @throws CoreException
	 */
    private void createExecutions(final Document doc, final Element plugin, final ILaunchConfiguration configuration, final IProject prj) throws CoreException {
        final Element executions = doc.createElement("executions");
        final Element execution = doc.createElement("execution");
        final Element phase = doc.createElement("phase");
        phase.setTextContent("generate-sources");
        final Element config = doc.createElement("configuration");
        final Element detectManyToMany = doc.createElement("detectManyToMany");
        detectManyToMany.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.DETECT_MANY_TO_MANY, true));
        final Element detectOptimisticLock = doc.createElement("detectOptimisticLock");
        detectOptimisticLock.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.DETECT_OPTMISTIC_LOCK, true));
        final Element concatUnderscoredTables = doc.createElement("concatUnderscoredTables");
        concatUnderscoredTables.setTextContent("true");
        final Element cache = doc.createElement("cache");
        cache.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.CACHE, false));
        final Element cacheConcurrencyStrategy = doc.createElement("cacheConcurrencyStrategy");
        cacheConcurrencyStrategy.setTextContent("READ_WRITE");
        final Element packageName = doc.createElement("packageName");
        packageName.setTextContent(configuration.getAttribute(INexOpenLaunchConfigurationConstants.NEXOPEN_PACKAGE, ""));
        final Element properties = doc.createElement("properties");
        properties.setTextContent("${basedir}/src/test/resources/hibernate.properties");
        Element reverseEngineeringFile = null;
        {
            final String dialectName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.HIBERNATE_DIALECT, "MySQL5InnoDB");
            if (support.isReverseEngineeringFileNeeded(dialectName)) {
                reverseEngineeringFile = doc.createElement("reverseEngineeringFile");
                reverseEngineeringFile.setTextContent("${basedir}/src/test/resources/" + support.getReversEngineeringFile(dialectName));
            }
        }
        final Element outputDirectory = doc.createElement("outputDirectory");
        outputDirectory.setTextContent("${basedir}/src/main/java");
        final Element testOutputDirectory = doc.createElement("testOutputDirectory");
        testOutputDirectory.setTextContent("${basedir}/src/test/java");
        final Element generateFacades = doc.createElement("generateFacades");
        generateFacades.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.GENERATE_FACADES, true));
        final Element jpaEnabled = doc.createElement("jpaEnabled");
        jpaEnabled.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.JPA_ENABLED, false));
        final Element validate = doc.createElement("validate");
        validate.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.VALIDATE, true));
        final Element optimize = doc.createElement("optimize");
        optimize.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.OPTMIZE, true));
        final Element superClassName = doc.createElement("superClassName");
        superClassName.setTextContent(configuration.getAttribute(INexOpenLaunchConfigurationConstants.BASE_CLASS_NAME, "org.nexopenframework.persistence.entity.AbstractEntityImpl"));
        final Element crud = doc.createElement("crud");
        crud.setTextContent("" + configuration.getAttribute(INexOpenLaunchConfigurationConstants.MVC_PROVIDER, true));
        final Element mvcProvider = doc.createElement("mvcProvider");
        mvcProvider.setTextContent(configuration.getAttribute(INexOpenLaunchConfigurationConstants.MVC_PROVIDER_IMPLENTOR, ""));
        final Element webOuputDirectory = doc.createElement("webOuputDirectory");
        webOuputDirectory.setTextContent("${basedir}/../web/src/main/java");
        final Element webResourcesOuputDirectory = doc.createElement("webResourcesOuputDirectory");
        webResourcesOuputDirectory.setTextContent("${basedir}/../web/src/main/resources");
        final Element supports04x = doc.createElement("supports04x");
        supports04x.setTextContent(NexOpenProjectUtils.is04xProject(prj) ? "true" : "false");
        final Element jspOuputDirectory = doc.createElement("jspOuputDirectory");
        jspOuputDirectory.setTextContent("${basedir}/../web/src/main/webapp/WEB-INF/jsp");
        config.appendChild(detectManyToMany);
        config.appendChild(detectOptimisticLock);
        config.appendChild(concatUnderscoredTables);
        config.appendChild(cache);
        config.appendChild(cacheConcurrencyStrategy);
        config.appendChild(packageName);
        config.appendChild(properties);
        if (reverseEngineeringFile != null) {
            config.appendChild(reverseEngineeringFile);
        }
        config.appendChild(outputDirectory);
        config.appendChild(testOutputDirectory);
        config.appendChild(generateFacades);
        config.appendChild(jpaEnabled);
        config.appendChild(validate);
        config.appendChild(optimize);
        config.appendChild(superClassName);
        config.appendChild(crud);
        config.appendChild(mvcProvider);
        config.appendChild(webOuputDirectory);
        config.appendChild(webResourcesOuputDirectory);
        config.appendChild(supports04x);
        config.appendChild(jspOuputDirectory);
        final Element goals = doc.createElement("goals");
        final Element goal = doc.createElement("goal");
        goal.setTextContent("generate");
        goals.appendChild(goal);
        execution.appendChild(phase);
        execution.appendChild(config);
        execution.appendChild(goals);
        executions.appendChild(execution);
        plugin.appendChild(executions);
    }

    /**
	 * @param doc
	 * @param plugin
	 * @param configuration
	 * @throws CoreException
	 */
    private void createDependencies(final Document doc, final Element plugin, final ILaunchConfiguration configuration) throws CoreException {
        final String dialectName = configuration.getAttribute(INexOpenLaunchConfigurationConstants.HIBERNATE_DIALECT, "MySQL5InnoDB");
        final Dependency dep = support.getDependency(dialectName);
        final Element dependencies = doc.createElement("dependencies");
        final Element dependency = doc.createElement("dependency");
        final Element groupId = doc.createElement("groupId");
        groupId.setTextContent(dep.getGroupId());
        final Element artifactId = doc.createElement("artifactId");
        artifactId.setTextContent(dep.getArtifactId());
        final Element version = doc.createElement("version");
        version.setTextContent(dep.getVersion());
        final Element scope = doc.createElement("scope");
        scope.setTextContent("provided");
        dependency.appendChild(groupId);
        dependency.appendChild(artifactId);
        dependency.appendChild(version);
        dependency.appendChild(scope);
        dependencies.appendChild(dependency);
        plugin.appendChild(dependencies);
    }
}
