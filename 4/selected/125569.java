package org.etf.dbx.formatter.html;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.etf.dbx.Messages;
import org.etf.dbx.formatter.AbstractFormatter;
import org.etf.dbx.formatter.DbxFormatterException;
import org.etf.dbx.jaxb.FieldType;
import org.etf.dbx.jaxb.GroupType;
import org.etf.dbx.jaxb.Schema;
import org.etf.dbx.jaxb.TableType;
import org.etf.dbx.jaxb.FieldType.FieldSpec;
import org.etf.dbx.utils.Helper;

/**
 * 
 * @author Illya_Yalovyy
 */
public class HtmlFormatter extends AbstractFormatter {

    private static final Logger logger = Logger.getLogger(HtmlFormatter.class);

    private static final String DEF_TEMPLATE_DIR = "html";

    public static final String FORMATTER_PROCCESS_NAME = "html";

    public static final String PAGES_SUB_DIR = "pages";

    public static final String MAIN_CSS_FILE_NAME = "main.css";

    private String htmlTemplatesDir;

    public HtmlFormatter() {
        this(DEF_TEMPLATE_DIR);
    }

    public HtmlFormatter(String templateDir) {
        this.htmlTemplatesDir = templateDir;
    }

    @Override
    public void process(Schema schema, String dirName) throws DbxFormatterException {
        logger.info(MessageFormat.format(Messages.FORMATTER_PROCESS_BEGIN, FORMATTER_PROCCESS_NAME, schema.getName()));
        super.process(schema, dirName);
        logger.info(MessageFormat.format(Messages.FORMATTER_PROCESS_END, FORMATTER_PROCCESS_NAME, schema.getName()));
    }

    @Override
    protected void processImpl(Schema schema, String dirName) throws DbxFormatterException {
        buildIndex(schema, dirName);
        final String pagesDir = Helper.addSubDir(dirName, PAGES_SUB_DIR);
        buildSchema(schema, pagesDir);
        buildGroups(schema, pagesDir);
        buildTypes(schema, pagesDir);
        copyResources(pagesDir);
    }

    private void copyResources(String pagesDir) throws DbxFormatterException {
        final String sourceFile = MessageFormat.format("{0}/{1}/{2}", TEMPLATE_BASE_DIR, htmlTemplatesDir, MAIN_CSS_FILE_NAME);
        try {
            FileUtils.copyFileToDirectory(new File(sourceFile), new File(pagesDir));
        } catch (IOException e) {
            throw new DbxFormatterException(e);
        }
    }

    /**
	 * Build index.html
	 */
    private void buildIndex(Schema schema, String dirName) throws DbxFormatterException {
        String template = loadTemplate(htmlTemplatesDir, "index");
        if (StringUtils.isBlank(template)) return;
        StringTemplate st = new StringTemplate(template);
        st.setAttribute("pagesDir", PAGES_SUB_DIR);
        st.setAttribute("forwardPage", schema.getName() + ".schema.html");
        String html = st.toString();
        try {
            FileUtils.writeStringToFile(new File(dirName, "index.html"), html);
        } catch (IOException ex) {
            throw new DbxFormatterException(ex);
        }
    }

    private void buildSchema(Schema schema, String dirName) throws DbxFormatterException {
        String template = loadTemplate(htmlTemplatesDir, "schema");
        if (StringUtils.isBlank(template)) return;
        StringTemplate st = new StringTemplate(template);
        st.setAttribute("Schema", schema);
        st.setAttribute("Groups", schema.getGroups().getGroup());
        String html = st.toString();
        try {
            FileUtils.writeStringToFile(new File(dirName, schema.getName() + ".schema" + ".html"), html);
        } catch (IOException ex) {
            throw new DbxFormatterException(ex);
        }
    }

    private void buildGroups(Schema schema, String dirName) throws DbxFormatterException {
        String template = loadTemplate(htmlTemplatesDir, "group");
        if (StringUtils.isBlank(template)) return;
        StringTemplate st = new StringTemplate(template);
        for (GroupType gt : schema.getGroups().getGroup()) {
            List<TableType> tables = new ArrayList<TableType>();
            for (TableType tt : schema.getTables().getTable()) {
                if (tt.getGroups().contains(gt)) {
                    tables.add(tt);
                }
            }
            st.reset();
            st.setAttribute("Schema", schema);
            st.setAttribute("Tables", tables);
            st.setAttribute("Group", gt);
            String html = st.toString();
            try {
                FileUtils.writeStringToFile(new File(dirName, schema.getName() + "." + gt.getName() + ".group" + ".html"), html);
            } catch (IOException ex) {
                throw new DbxFormatterException(ex);
            }
        }
    }

    private void buildTypes(Schema schema, String dirName) throws DbxFormatterException {
        String template = loadTemplate(htmlTemplatesDir, "type");
        if (StringUtils.isBlank(template)) return;
        StringTemplate st = new StringTemplate(template);
        st.setAttribute("Schema", schema);
        final List<TypeDef> typesList = new LinkedList<TypeDef>();
        for (FieldType fieldType : schema.getTypes().getType()) {
            final TypeDef typeDef = new TypeDef();
            typeDef.name = fieldType.getName();
            typeDef.defaultSql = getSqlType(fieldType, null);
            if (fieldType.getFieldSpec() != null) {
                for (FieldSpec fieldSpec : fieldType.getFieldSpec()) {
                    final FieldSpecDef specDef = new FieldSpecDef();
                    specDef.dbName = fieldSpec.getDbName().toString();
                    specDef.sql = getSqlType(fieldType, fieldSpec.getDbName());
                    typeDef.fieldSpec.add(specDef);
                }
            }
            typesList.add(typeDef);
        }
        st.setAttribute("Types", typesList);
        String html = st.toString();
        try {
            FileUtils.writeStringToFile(new File(dirName, schema.getName() + ".type" + ".html"), html);
        } catch (IOException ex) {
            throw new DbxFormatterException(ex);
        }
    }

    public static class TypeDef {

        public String name;

        public String defaultSql;

        public List<FieldSpecDef> fieldSpec = new LinkedList<FieldSpecDef>();
    }

    public static class FieldSpecDef {

        public String dbName;

        public String sql;
    }
}
