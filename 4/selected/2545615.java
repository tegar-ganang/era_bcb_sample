package org.vardb.util.setup;

import java.util.List;
import java.util.Map;
import org.springframework.context.support.GenericApplicationContext;
import org.vardb.util.CBeanHelper;
import org.vardb.util.CException;
import org.vardb.util.CFileHelper;
import org.vardb.util.CSpringHelper;
import org.vardb.util.dao.CDatabaseHelper;
import org.vardb.util.tags.ITagService;
import org.vardb.util.users.IUserService;
import org.vardb.util.web.CMessageWriter;

public abstract class CAbstractSetup implements ISetup {

    public static final String DEFAULT_SETUP_PROPERTIES_PATH = "classpath:setup.properties";

    protected ISetupParams params;

    protected CMessageWriter writer = new CMessageWriter();

    protected CLoadTableParams loadparams = new CLoadTableParams();

    protected GenericApplicationContext applicationContext = null;

    public CAbstractSetup(ISetupParams params) {
        GenericApplicationContext context = new GenericApplicationContext();
        CSpringHelper.registerPropertyPlaceholderConfigurer(context, DEFAULT_SETUP_PROPERTIES_PATH, CFileHelper.getFilenameAsUrl(params.getSetupfile()));
        Map<String, Object> map = params.getPropertyMap();
        CSpringHelper.registerBean(context, "params", params.getClass(), map);
        context.refresh();
        this.params = (ISetupParams) context.getBean("params");
        updateProperties(this.params, params);
        System.out.println("setup params=" + this.params.toString());
        this.params.validate();
    }

    private void updateProperties(ISetupParams target, ISetupParams src) {
        target.setSetupfile(src.getSetupfile());
        target.setAction(src.getAction());
    }

    protected GenericApplicationContext getApplicationContext() {
        if (this.applicationContext == null) this.applicationContext = loadApplicationContext();
        return this.applicationContext;
    }

    private GenericApplicationContext loadApplicationContext() {
        if (!CDatabaseHelper.databaseExists(params.getDb())) throw new CException("can't create application context because database not created yet: " + params.getDb().getName());
        GenericApplicationContext context = new GenericApplicationContext();
        CSpringHelper.registerDataSource(context, "dataSource", params.getDb());
        CSpringHelper.loadXmlBeanDefinitions(context, "spring-core.xml", "spring-services.xml");
        context.refresh();
        return context;
    }

    protected ITagService getTagService() {
        return (ITagService) getApplicationContext().getBean("tagService");
    }

    protected IUserService getUserService() {
        return (IUserService) getApplicationContext().getBean("userService");
    }

    public void dropdb() {
        if (!CFileHelper.confirm("Delete database " + params.getDb().getName() + "? (press y to confirm drop or any other key to skip)")) {
            System.out.println("Skipping dropdb");
            return;
        }
        CDatabaseHelper.dropDatabase(params.getDb());
    }

    public void createdb() {
        dropDbIfExists();
        CDatabaseHelper.createDatabase(params.getDb());
    }

    private void dropDbIfExists() {
        if (!CDatabaseHelper.databaseExists(params.getDb())) return;
        if (params.getDropdbifexists()) dropdb(); else writer.message("database already exists and getDropdbifexists() is false - quitting");
    }

    public void sql() {
        String sql = CDatabaseHelper.concatenateScripts(params.getSqlDir());
        CFileHelper.writeFile(params.getTempDir() + "sql", sql);
        CDatabaseHelper.executeSql(params.getDb(), sql);
    }

    public void tagtypes() {
        ITagService tagService = getTagService();
        for (String filename : CFileHelper.listFilesRecursively(params.getXmlDir() + "tagtypes/", ".txt")) {
            tagService.createTagTypeFromFile(filename);
        }
        for (String filename : CFileHelper.listFilesRecursively(params.getXmlDir() + "attributetypes/", ".txt")) {
            tagService.createTagTypeFromAttributeFile(filename);
        }
        tagService.getDao().updateAttributeCounts();
    }

    public abstract void loadusers();

    public abstract void xml();

    public void vacuum() {
        CDatabaseHelper.vacuum(params.getDb());
    }
}
