package net.sf.buildbox.releasator.m2;

import net.sf.buildbox.releasator.spi.CodeBuilder;
import net.sf.buildbox.releasator.spi.CodeBuilderFactory;
import net.sf.buildbox.releasator.BuildResult;
import net.sf.buildbox.releasator.DistroType;
import net.sf.buildbox.releasator.LoggingCommandLineExec;
import net.sf.buildbox.util.CommandLineExec;
import net.sf.buildbox.util.FileUtils;
import net.sf.buildbox.strictlogging.api.StrictLogger;
import java.io.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.net.URL;
import javax.xml.transform.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.InputSource;

public final class MavenBuilderFactory implements CodeBuilderFactory {

    public boolean matches(File codedir) {
        final File pom = new File(codedir, "pom.xml");
        return pom.exists();
    }

    public CodeBuilder get(File basedir, File workdir) throws IOException {
        final File pom = new File(basedir, "pom.xml");
        if (!pom.exists()) throw new IllegalArgumentException("No build system found for " + basedir);
        return new Maven2CodeBuilder(pom, workdir);
    }

    public static Map<File, Model> moduleList(File pom) throws IOException, XmlPullParserException {
        final Map<File, Model> result = new LinkedHashMap<File, Model>();
        moduleList(result, pom);
        return result;
    }

    private static void moduleList(Map<File, Model> result, File pom) throws IOException, XmlPullParserException {
        final Model model = new MavenXpp3Reader().read(new FileReader(pom));
        result.put(pom, model);
        @SuppressWarnings("unchecked") final List<String> modules = model.getModules();
        for (String module : modules) {
            moduleList(result, new File(pom.getParentFile(), module + "/pom.xml"));
        }
    }

    /**
     *
     */
    public static class Maven2CodeBuilder implements CodeBuilder {

        protected StrictLogger logger;

        private static final TransformerFactory factory = TransformerFactory.newInstance();

        private final File workdir;

        private final File pom;

        private final File m2repo;

        private File settingsCache;

        private final Model mavenModel;

        private final File config;

        private final Map<DistroType, File> requestedDistros = new LinkedHashMap<DistroType, File>();

        Maven2CodeBuilder(File pom, File workdir) throws IOException {
            this.workdir = workdir;
            this.pom = pom;
            this.m2repo = new File(workdir, "repository");
            this.config = new File(workdir, "releasator-config.xml");
            try {
                mavenModel = new MavenXpp3Reader().read(new FileReader(pom));
            } catch (XmlPullParserException e) {
                throw new IOException(e.getClass().getName() + ": " + e.getMessage());
            }
        }

        public File getSettingsFile() throws IOException {
            if (settingsCache == null) {
                this.settingsCache = new File(workdir, "settings.xml");
                {
                    final URL res = Maven2CodeBuilder.class.getResource("releasator-config2settings.xsl");
                    final InputStream stream = res.openStream();
                    final Source xsltSource = new SAXSource(new InputSource(stream));
                    try {
                        final Transformer transformer = factory.newTransformer(xsltSource);
                        transformer.setOutputProperty("indent", "yes");
                        transformer.setErrorListener(new ErrorListener() {

                            public void warning(TransformerException exception) throws TransformerException {
                                System.out.println("XSLT: " + exception.getMessage());
                            }

                            public void error(TransformerException exception) throws TransformerException {
                                throw exception;
                            }

                            public void fatalError(TransformerException exception) throws TransformerException {
                                throw exception;
                            }
                        });
                        final InputStream is = new FileInputStream(config);
                        transformer.transform(new StreamSource(is), new StreamResult(settingsCache));
                    } catch (TransformerConfigurationException e) {
                        throw new IllegalStateException(e);
                    } catch (TransformerException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            if (!settingsCache.exists()) {
                throw new FileNotFoundException("missing generated settings file: " + settingsCache);
            }
            return settingsCache;
        }

        public String getGroupId() {
            return mavenModel.getGroupId();
        }

        public String getArtifactId() {
            return mavenModel.getArtifactId();
        }

        public String getVersion() {
            return mavenModel.getVersion();
        }

        public void requestDistro(DistroType distroType) {
            final File f = new File(workdir, "repo." + distroType.name());
            if (f.exists()) {
                throw new IllegalStateException(String.format("Distribution %s was already built at %s", distroType, f));
            }
            requestedDistros.put(distroType, f);
        }

        public BuildResult build() throws IOException, InterruptedException {
            final String executable = "mvn";
            final String[] args = { "--batch-mode", "--file", pom.getAbsolutePath(), "--settings", getSettingsFile().getAbsolutePath(), "-Dmaven.repo.local=" + m2repo.getAbsolutePath(), "net.sf.buildbox.maven:buildbox-bbx-plugin:AttachChanges", "-ff" };
            final CommandLineExec exec = new LoggingCommandLineExec(logger, executable, args) {

                final Pattern annoyingDownloadMessage = Pattern.compile("^Downloading: .*$");

                final Pattern annoyingDownloadProgress = Pattern.compile("^\\d+/\\d+[KMb]?\\s*$");

                final Pattern annoyingDownloadResult = Pattern.compile("^\\d+[KMb] downloaded$");

                @Override
                protected void stdout(String line) {
                    if (annoyingDownloadMessage.matcher(line).matches()) return;
                    if (annoyingDownloadProgress.matcher(line).matches()) return;
                    if (annoyingDownloadResult.matcher(line).matches()) return;
                    super.stdout(line);
                }
            };
            if (requestedDistros.containsKey(DistroType.M2PACKAGE)) {
                exec.addArguments("-DaltDeploymentRepository=local::default::file:" + requestedDistros.get(DistroType.M2PACKAGE).getAbsolutePath(), "deploy");
            }
            if (requestedDistros.containsKey(DistroType.M2SITE)) {
                exec.addArguments("site");
            }
            exec.setFailOnError(true);
            exec.call();
            return new BuildResult() {

                public File getDistro(DistroType distroType) throws IOException {
                    final File targetRepo = requestedDistros.get(distroType);
                    switch(distroType) {
                        case M2SITE:
                            try {
                                final Map<File, Model> modules = moduleList(pom);
                                m2site(modules, targetRepo);
                                return targetRepo;
                            } catch (XmlPullParserException e) {
                                throw new IOException(e.getClass().getName() + ": " + e.getMessage());
                            }
                    }
                    if (targetRepo == null) {
                        throw new UnsupportedOperationException("distribution type: " + distroType);
                    }
                    return targetRepo;
                }
            };
        }

        private static void m2site(Map<File, Model> modules, File targetRepo) throws IOException {
            for (Map.Entry<File, Model> entry : modules.entrySet()) {
                final Model model = entry.getValue();
                final Reporting reporting = model.getReporting();
                File siteDir = null;
                if (reporting != null) {
                    final String od = reporting.getOutputDirectory();
                    if (od != null) {
                        siteDir = new File(od);
                    }
                }
                if (siteDir == null) {
                    siteDir = new File(entry.getKey().getParentFile(), "target/site");
                }
                System.out.println("od = " + siteDir);
                final String suffix = String.format("%s/%s/%s/site", model.getGroupId(), model.getArtifactId(), model.getVersion());
                final File targetDir = new File(targetRepo, suffix);
                targetDir.getParentFile().mkdirs();
                System.out.println(siteDir + " -> " + targetDir);
                FileUtils.deepCopy(siteDir, targetDir);
            }
        }

        private static void m2package(File buildRepo, Map<File, Model> modules, File targetRepo) throws IOException {
            for (Model model : modules.values()) {
                final String suffix = String.format("%s/%s/%s", model.getGroupId().replace('.', File.separatorChar), model.getArtifactId(), model.getVersion());
                final File buildDir = new File(buildRepo, suffix);
                final File targetDir = new File(targetRepo, suffix);
                final File[] files = buildDir.listFiles(new FileFilter() {

                    public boolean accept(File file) {
                        final String name = file.getName();
                        if (file.isDirectory()) return false;
                        if (name.startsWith("maven-metadata") && name.endsWith(".xml")) return false;
                        return true;
                    }
                });
                for (File file : files) {
                    final File targetFile = new File(targetDir, file.getName());
                    System.out.println(String.format("cp %s %s", file, targetFile));
                    FileUtils.copyFile(file, targetFile, true);
                }
            }
        }
    }
}
