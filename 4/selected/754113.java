package org.kwantu.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AnnotationConfiguration;
import org.kwantu.m2.KwantuFaultException;
import org.kwantu.m2.KwantuItemNotFoundException;
import org.kwantu.m2.model.KwantuBusinessObjectModel;
import org.kwantu.m2.model.KwantuModel;
import org.kwantu.m2.model.KwantuClass;
import org.kwantu.m2.model.KwantuModelResolver;
import org.kwantu.m2.model.KwantuRelationship;
import org.kwantu.m2.model.ui.Composite;
import org.kwantu.m2.model.ui.KwantuComponent;
import org.kwantu.m2.model.ui.KwantuPage;
import org.kwantu.m2.model.ui.KwantuPanel;
import org.kwantu.m2.model.ui.MethodArg;
import org.kwantu.m2generator.M2Generator;
import org.kwantu.m2generator.M2GeneratorBuildFailedException;
import org.kwantu.persistence.PersistentObject;

/** Provides the context for browsing Kwantu Models.
 */
public class M2ApplicationContext extends ApplicationContext {

    private static final Log LOG = LogFactory.getLog(M2ApplicationContext.class);

    private WicketApplicationController controller;

    private HashMap<String, KwantuComponent> kwantuUiComponents;

    private ModelBrowser modelBrowser;

    Properties dbProperties;

    public M2ApplicationContext(WicketApplicationController controller, Properties dbProperties) {
        super();
        this.controller = controller;
        this.dbProperties = dbProperties;
        initializeHibernateSessionFactory();
        controller.addApplicationContext(WicketApplicationController.KEY_M2BROWSER, this);
        modelBrowser = new ModelBrowser(getDbSession(), controller);
        loadComponentMap();
    }

    @Override
    void configureHibernate(AnnotationConfiguration config) {
        LOG.info("Configuring hibernate for the m2 database");
        config.setProperties(dbProperties);
        HibernateUtil.configureHibernateForM2(config);
    }

    @Override
    public Set<KwantuPanel> getUiPanels() {
        return modelBrowser.getUiPanels();
    }

    @Override
    public void save(PersistentObject... objects) {
        Session session = getDbSession();
        session.getTransaction().begin();
        for (PersistentObject o : objects) {
            LOG.info("Saving " + o);
            session.saveOrUpdate(o);
        }
        session.flush();
        session.getTransaction().commit();
    }

    /**
     * Call the kwantuMethod with the given arguments.
     *
     * Some arguments are to be retrieved using the xpaths provided in the MethodArg
     * others are provided directly in the argVals hashmap (probably from a button dialog.)
     *
     * Currently we don't support multiple argument lists
     * (ie you can't have setItem(int i) and setItem(String s) only one setItem is accepted.)
     *
     * In order to fix this we will also need the parameter types to designate the specific method.
     * (admittedly you could determine them from the classes of the arguments but only if they're
     * not null).
     *
     * @param context  the instance
     * @param methodName   the method name
     * @param methodArgs any arguments.
     */
    @Override
    public void invoke(Object contextIn, String methodNameIn, List<MethodArg> methodArgs, HashMap<String, Object> argVals) {
        Object context = contextIn;
        String methodName = methodNameIn;
        LOG.info("Invoking " + methodName + " on " + context + ", num args " + methodArgs.size());
        if (methodName.startsWith("/")) {
            int methodSlash = methodName.lastIndexOf("/");
            context = getFromContext(methodName.substring(0, methodSlash), null);
            methodName = methodName.substring(methodSlash + 1);
        }
        List args = super.resolveArgs(contextIn, methodNameIn, methodArgs, argVals);
        invoke(context, methodName, args.toArray(), true);
    }

    public void serializeAll(String filename) {
        serializeAllToDbUnit(filename);
    }

    public void serializeAllToDbUnit(String filename) {
        LOG.info("Serializing application data in dbunit format to " + filename);
        try {
            Connection jdbcConnection = getDbSession().connection();
            IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);
            connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
            IDataSet fullDataSet = connection.createDataSet();
            FlatXmlDataSet.write(fullDataSet, new FileOutputStream(filename));
        } catch (IOException e) {
            throw new KwantuFaultException("Unable to serialise application data to file " + filename, e);
        } catch (DatabaseUnitException ex) {
            throw new KwantuFaultException("Exception when initializing database", ex);
        } catch (SQLException ex) {
            throw new KwantuFaultException("Exception when initializing database", ex);
        }
    }

    /**
     * Use JXPath to access attributes (and more, traverse references) by reflection.
     * @param xpath The name of the attribute, (or the path to the attribute).
     * @param parentContext  The object possessing the attribute (or the first element of the path).
     * @return
     */
    public Object getFromContext(String xpath, Object parentContext) {
        return controller.getFromContext(xpath, parentContext);
    }

    public void setInContext(String xpath, Object contextObject, Object value) {
        controller.setInContext(xpath, contextObject, value);
    }

    @Override
    public void delete(PersistentObject o) {
        LOG.info("About to delete  " + o + ".");
        getDbSession().getTransaction().begin();
        getDbSession().delete(o);
        getDbSession().getTransaction().commit();
    }

    @Override
    public void initializeRootRenderingContext(HashMap<String, Object> map) {
        map.put("ModelBrowser", modelBrowser);
    }

    @Override
    public String getApplicationKey() {
        return WicketApplicationController.KEY_M2BROWSER;
    }

    KwantuModel getKwantuModel(String name) {
        Query query = getDbSession().createQuery("from KwantuModel where name = :name");
        query.setString("name", name);
        return (KwantuModel) query.uniqueResult();
    }

    KwantuModel createKwantuModel(String name) {
        KwantuModel model = new KwantuModel(name, new KwantuBusinessObjectModel());
        return model;
    }

    KwantuModel createOrEmptyKwantuModel(String modelName) {
        KwantuModel model = getKwantuModel(modelName);
        if (model != null) {
            emptyKwantuModel(model, getDbSession());
            return model;
        }
        return new KwantuModel(modelName);
    }

    public void saveKwantuModel(KwantuModel model, boolean overwrite) {
        Session session = getDbSession();
        session.beginTransaction();
        saveKwantuModelUncommitted(model, overwrite, session);
        session.getTransaction().commit();
    }

    public void saveKwantuModelDependencies(KwantuModel model, List<KwantuModel> dependencies) {
        Session session = getDbSession();
        session.beginTransaction();
        model.addKwantuModelDependencies(dependencies.toArray(new KwantuModel[] {}));
        session.save(model);
        for (KwantuModel dep : dependencies) {
            session.save(dep);
        }
        session.getTransaction().commit();
    }

    private void emptyKwantuModel(KwantuModel model, Session session) {
        KwantuBusinessObjectModel bom = model.getKwantuBusinessObjectModel();
        if (bom != null) {
            bom.setOwningKwantuModel(null);
            model.setKwantuBusinessObjectModel(null);
            session.delete(bom);
        }
        Set<KwantuPanel> pages = model.getUiPanels();
        for (KwantuPage p : pages.toArray(new KwantuPage[0])) {
            model.deleteUiPanel(p);
        }
    }

    private void saveKwantuModelUncommitted(KwantuModel model, boolean overwrite, Session session) {
        try {
            Query query = session.createQuery("from KwantuModel m where m.name = :s");
            query.setString("s", model.getName());
            KwantuModel existingModel = (KwantuModel) query.uniqueResult();
            if (existingModel != null) {
                if (!overwrite) {
                    throw new KwantuFaultException("model exists already and overwrite is 'false'.");
                }
                emptyKwantuModel(existingModel, session);
                KwantuBusinessObjectModel bom = model.getKwantuBusinessObjectModel();
                bom.setOwningKwantuModel(existingModel);
                existingModel.setKwantuBusinessObjectModel(bom);
                for (KwantuPanel p : model.getUiPanels()) {
                    p.setOwningKwantuModel(existingModel);
                    existingModel.getUiPanels().add(p);
                }
                model = existingModel;
            }
            for (KwantuClass c : model.getKwantuBusinessObjectModel().getKwantuClasses()) {
                for (KwantuRelationship rel : c.getDeclaredKwantuRelationships()) {
                    session.save(rel);
                }
                session.save(c);
            }
            session.save(model);
        } catch (HibernateException ex) {
            session.getTransaction().rollback();
            throw new KwantuFaultException("Unable to complete database transaction", ex);
        }
    }

    void saveAndBuildKwantuModel(KwantuModel model) {
        Session session = getDbSession();
        session.beginTransaction();
        saveKwantuModelUncommitted(model, true, session);
        try {
            M2Generator generator = new M2Generator();
            generator.setKwantuModel(model);
            generator.setDefaultMavenSettingsFile();
            generator.clean();
            generator.buildModel();
        } catch (M2GeneratorBuildFailedException ex) {
            getDbSession().getTransaction().rollback();
            controller.getFeedbacker().error("Build failed: " + ex.getMessage());
            throw new KwantuFaultException("build failed", ex);
        }
        session.getTransaction().commit();
    }

    public KwantuModelResolver getKwantuModelResolver() {
        return new KwantuModelResolver() {

            @Override
            public KwantuModel resolve(String name) throws KwantuItemNotFoundException {
                return getKwantuModel(name);
            }
        };
    }
}
