package com.jguild.devportal.sourcetemplate;

import com.jguild.devportal.infrastructure.versioncontrol.subversion.SVNRepository;
import com.jguild.devportal.infrastructure.versioncontrol.subversion.SVNRepositoryModuleCfg;
import com.jguild.devportal.project.Module;
import com.jguild.devportal.project.Project;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import javax.persistence.*;
import java.io.*;
import java.net.URI;
import java.util.Properties;
import java.util.Map;

/**
 * Source Template Generator
 */
@Entity
@NamedQueries(value = { @NamedQuery(name = "SourceTemplate.findAll", query = "select t from SourceTemplate t"), @NamedQuery(name = "SourceTemplate.findById", query = "select t from SourceTemplate t where t.id = :id"), @NamedQuery(name = "SourceTemplate.findByName", query = "select t from SourceTemplate t where t.name = :name"), @NamedQuery(name = "SourceTemplate.deleteById", query = "delete from SourceTemplate where id = :id") })
public class SourceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(unique = true)
    private String name;

    private Type type;

    private String location;

    public SourceTemplate() {
    }

    public SourceTemplate(final String name, final Type type, final String location) {
        this.name = name;
        this.type = type;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public enum Type {

        URI, SVN
    }

    public void generateSources(@NotNull final Module module, @NotNull final File dest) throws SourceGenerationException {
        try {
            final File tmp = File.createTempFile("dvp", "st");
            tmp.delete();
            switch(type) {
                case URI:
                    retrieveTemplateByURL(tmp);
                    break;
                case SVN:
                    retrieveTemplateBySVN(tmp);
                    break;
                default:
                    throw new SourceGenerationException("Unsupposed source template type: " + type);
            }
            doGenerate(module, dest, tmp);
            FileUtils.deleteDirectory(tmp);
        } catch (Exception e) {
            if (e instanceof SourceGenerationException) {
                throw ((SourceGenerationException) e);
            } else {
                throw new SourceGenerationException(e);
            }
        }
    }

    private void retrieveTemplateBySVN(final File templateDir) throws SVNException {
        final SVNClientManager clientManager = SVNClientManager.newInstance();
        clientManager.getUpdateClient().doExport(SVNURL.parseURIDecoded(location), templateDir, SVNRevision.HEAD, SVNRevision.HEAD, null, false, true);
    }

    private void retrieveTemplateByURL(final File templateDir) throws IOException {
        final URI uri = URI.create(location);
        if (!uri.getScheme().equalsIgnoreCase("file")) {
        } else {
            final File f = new File(uri);
            if (f.isFile()) {
            } else {
                FileUtils.copyDirectory(f, templateDir, true);
            }
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void doGenerate(final Module module, final File dest, final File explodedTemplate) throws IOException, TemplateException {
        assert explodedTemplate != null;
        assert dest.isDirectory();
        dest.mkdirs();
        final Configuration cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(explodedTemplate);
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg.setSharedVariable("module", module);
        final Properties idx = loadSTIndex(cfg);
        for (final Map.Entry<Object, Object> entry : idx.entrySet()) {
            final String key = entry.getKey().toString();
            processTemplate(cfg, key, entry.getValue().toString(), explodedTemplate, dest);
        }
    }

    private Properties loadSTIndex(final Configuration cfg) throws IOException, TemplateException {
        final Template fileListTemplate = cfg.getTemplate("stindex.ftl");
        final StringWriter buf = new StringWriter();
        fileListTemplate.process(null, buf);
        final Properties p = new Properties();
        final InputStream reader = IOUtils.toInputStream(buf.toString());
        p.load(reader);
        reader.close();
        buf.close();
        return p;
    }

    private void processTemplate(final Configuration cfg, final String name, final String value, final File explodedTemplate, final File dest) throws IOException, TemplateException {
        final File destFile = new File(dest.getAbsolutePath() + File.separator + value.replace("/", File.separator));
        if (name.endsWith(".ftl")) {
            destFile.getParentFile().mkdirs();
            final Template template = cfg.getTemplate(name);
            final FileWriter writer = new FileWriter(destFile);
            template.process(null, writer);
            writer.close();
        } else {
            final File templateFile = new File(explodedTemplate.getAbsolutePath() + File.separator + name.replace("/", File.separator));
            if (templateFile.exists()) {
                destFile.getParentFile().mkdirs();
                FileUtils.copyFile(templateFile, destFile);
            } else if (name.equals(value)) {
                destFile.mkdirs();
            } else {
                throw new IOException("Error in template '" + name + "' doesn't exist in the template, and the destination '" + name + "' isn't the same as the name, which would have indicated it was a directory");
            }
        }
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SourceTemplate)) {
            return false;
        }
        final SourceTemplate that = (SourceTemplate) o;
        return !(id != null ? !id.equals(that.id) : that.id != null) && !(location != null ? !location.equals(that.location) : that.location != null) && !(name != null ? !name.equals(that.name) : that.name != null) && type == that.type;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        return result;
    }

    public static void main(final String[] args) throws SourceGenerationException {
        final Project project = new Project("Test Project", "TST");
        final Module module = project.createModule("Test Project", "TST");
        final SVNRepositoryModuleCfg moduleCfg = new SVNRepositoryModuleCfg(new SVNRepository("testrepo"), module);
        module.addInfrastructureConfig(moduleCfg);
        final SourceTemplate template = new SourceTemplate("Example", SourceTemplate.Type.URI, new File("src\\sourcecodetemplates").toURI().toString());
        template.generateSources(module, new File("_test"));
    }
}
