package net.sf.ahtutils.controller.factory.java.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.ahtutils.exception.processing.UtilsConfigurationException;
import net.sf.ahtutils.xml.access.Access;
import net.sf.ahtutils.xml.access.Category;
import net.sf.exlp.util.io.FileIO;
import net.sf.exlp.util.io.StringIO;
import net.sf.exlp.util.xml.JaxbUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@SuppressWarnings("rawtypes")
public class AbstractJavaSecurityFileFactory {

    static final Logger logger = LoggerFactory.getLogger(AbstractJavaSecurityFileFactory.class);

    protected File fTmpDir;

    protected String classPrefix;

    protected Map freemarkerNodeModel;

    protected Configuration freemarkerConfiguration;

    public AbstractJavaSecurityFileFactory(File fTmpDir, String classPrefix) {
        this.fTmpDir = fTmpDir;
        this.classPrefix = classPrefix;
        freemarkerNodeModel = new HashMap();
        freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/");
    }

    protected String createFileName(String code) {
        return createClassName(code) + ".java";
    }

    public void processViews(String fXml) throws FileNotFoundException, UtilsConfigurationException {
        Access access = JaxbUtil.loadJAXB(fXml, Access.class);
        processCategories(access.getCategory());
    }

    protected void processCategories(List<Category> lCategory) throws UtilsConfigurationException {
    }

    protected String createClassName(String code) {
        StringBuffer sb = new StringBuffer();
        sb.append(classPrefix);
        sb.append(code.subSequence(0, 1).toString().toUpperCase());
        sb.append(code.substring(1, code.length()));
        return sb.toString();
    }

    protected void createFile(File f, String template) throws IOException, TemplateException {
        File fTmp = new File(fTmpDir, f.getName());
        logger.debug("Using tmp-Dir: " + fTmpDir);
        logger.debug("f.getName()=" + f.getName());
        logger.debug("ftmp=" + fTmp.getAbsolutePath());
        Template ftl = freemarkerConfiguration.getTemplate(template, "UTF-8");
        ftl.setEncoding("UTF-8");
        StringWriter sw = new StringWriter();
        ftl.process(freemarkerNodeModel, sw);
        sw.flush();
        StringIO.writeTxt(fTmp, sw.toString());
        boolean doCopy = false;
        if (!f.exists()) {
            logger.debug(f.getAbsolutePath() + " does not exist, so COPY");
            doCopy = true;
        } else {
            String hashExisting = FileIO.getHash(f);
            String hashNew = FileIO.getHash(fTmp);
            logger.debug("hashExisting " + hashExisting);
            logger.debug("hashNew      " + hashNew);
            if (!hashExisting.equals(hashNew)) {
                doCopy = true;
            }
            logger.debug("Hash evaluated: COPY:" + doCopy);
        }
        if (doCopy) {
            FileUtils.copyFile(fTmp, f);
        } else {
            logger.debug("Dont copy");
        }
    }
}
