package net.sf.buildbox.releasator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import net.sf.buildbox.releasator.m2.MavenBuilderFactory;
import net.sf.buildbox.releasator.spi.CodeBuilder;
import net.sf.buildbox.strictlogging.AbstractStrictLogger;
import net.sf.buildbox.strictlogging.api.*;
import net.sf.buildbox.strictlogging.javalogapi.JavaStrictLoggerFactory;
import net.sf.buildbox.util.FileUtils;
import net.sf.buildbox.util.StringUtils;

public final class Releasator {

    private static final Catalog cat = StrictCatalogFactory.getCatalog(Catalog.class);

    private static final StrictLogger logger;

    static {
        final JavaStrictLoggerFactory loggerFactory = new JavaStrictLoggerFactory();
        loggerFactory.setSeverity(Severity.DEBUG);
        logger = loggerFactory.getInstance("rls");
    }

    private static ReleasatorConfig config = new ReleasatorConfig();

    static void prepare(File tmp, boolean dry, String svnUrl, VersionNumber version, String author, String codename) throws IOException, InterruptedException {
        if (version.getLevel() == VersionLevel.SNAPSHOT) {
            throw new IllegalArgumentException("Invalid release version number: " + version);
        }
        final File wc = new File(tmp, "wc");
        final File changesXml = new File(wc, "changes.xml");
        if (!changesXml.exists()) {
            throw new FileNotFoundException("changes.xml");
        }
    }

    static void upload(File tmp, String scmUrl, Set<DistroType> what) throws IOException, InterruptedException {
        final File codeDir = new File(tmp, "code");
        final RevisionControl vcs = RevisionControl.Factory.get(scmUrl);
        AbstractStrictLogger.injectLogger(vcs, logger.getSubLogger("vcs"));
        logger.log(cat.checkout(scmUrl));
        vcs.checkout(scmUrl, codeDir);
        final File configFile = new File(StringUtils.expandSysProps("${user.home}/src/buildbox/tools/releasator/data/releasator-config.xml"));
        FileUtils.copyFile(configFile, new File(tmp, configFile.getName()), true);
        final CodeBuilder bs = new MavenBuilderFactory().get(codeDir, tmp);
        final Map<DistroType, Dray> drays = new LinkedHashMap<DistroType, Dray>();
        for (DistroType distroType : what) {
            final Dray dray = config.getUploadDray(bs.getGroupId(), bs.getArtifactId(), bs.getVersion(), distroType);
            AbstractStrictLogger.injectLogger(dray, logger.getSubLogger("dray." + distroType));
            drays.put(distroType, dray);
        }
        AbstractStrictLogger.injectLogger(bs, logger.getSubLogger("bs"));
        logger.log(cat.building(bs.getGroupId(), bs.getArtifactId(), bs.getVersion()));
        for (DistroType distroType : what) {
            bs.requestDistro(distroType);
            logger.log(cat.reqDistro(distroType));
        }
        final BuildResult buildResult = bs.build();
        for (DistroType distroType : what) {
            final File d = buildResult.getDistro(distroType);
            logger.log(cat.uploading(distroType));
            final Dray dray = drays.get(distroType);
            dray.uploadDirectory(d, ".");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final File tmp = new File("/tmp/Releasator");
        FileUtils.deepDelete(tmp);
        FileUtils.mkdirs(tmp);
        upload(tmp, args[0], new LinkedHashSet<DistroType>(Arrays.asList(DistroType.M2PACKAGE, DistroType.M2SITE)));
    }

    private static interface Catalog extends StrictCatalog {

        @StrictCatalogEntry(severity = Severity.INFO, format = "Parsing GWT module: %s")
        LogMessage parsing(String gwtModuleIUd);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Checking out from %s")
        LogMessage checkout(String svnUrl);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Building module %s:%s:%s")
        LogMessage building(String groupId, String artifactId, String versionId);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Requesting distribution type: %s")
        LogMessage reqDistro(DistroType distroType);

        @StrictCatalogEntry(severity = Severity.INFO, format = "Uploading distribution: %s")
        LogMessage uploading(DistroType distroType);
    }
}
