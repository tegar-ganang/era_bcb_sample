package org.mariella.persistence.test.junit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import oracle.jdbc.driver.OracleDriver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mariella.persistence.loader.ClusterLoader;
import org.mariella.persistence.loader.ClusterLoaderConditionProvider;
import org.mariella.persistence.loader.LoaderContext;
import org.mariella.persistence.mapping.SchemaMapping;
import org.mariella.persistence.persistor.ClusterDescription;
import org.mariella.persistence.persistor.DatabaseAccess;
import org.mariella.persistence.persistor.Persistor;
import org.mariella.persistence.runtime.Modifiable;
import org.mariella.persistence.runtime.ModificationTracker;
import org.mariella.persistence.runtime.RIListener;
import org.mariella.persistence.schema.ClassDescription;
import org.mariella.persistence.test.model.Adresse;
import org.mariella.persistence.test.model.LieferAdresse;
import org.mariella.persistence.test.model.Person;
import org.mariella.persistence.test.model.TestPersistence;

public class ModificationTest {

    private ModificationTracker modificationTracker;

    private RIListener riListener;

    private Connection connection;

    private DatabaseAccess idGenerator = new DatabaseAccess() {

        public long generateId() {
            try {
                PreparedStatement ps = getConnection().prepareStatement("select idsequence.nextval from dual");
                try {
                    ResultSet rs = ps.executeQuery();
                    try {
                        rs.next();
                        return rs.getLong(1);
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Connection getConnection() {
            return connection;
        }
    };

    private SchemaMapping getSchemaMapping() {
        return TestPersistence.Singleton.getSchemaMapping();
    }

    private ClassDescription getClassDescription(Class<?> cls) {
        return TestPersistence.Singleton.getSchemaMapping().getSchemaDescription().getClassDescription(cls.getName());
    }

    @Before
    public void setUp() throws Exception {
        modificationTracker = new ModificationTracker();
        riListener = new RIListener(TestPersistence.Singleton.getSchemaMapping().getSchemaDescription());
        modificationTracker.addListener(riListener);
        DriverManager.registerDriver(new OracleDriver());
        connection = DriverManager.getConnection("jdbc:oracle:thin:@vievmsdrsld1.eu.boehringer.com:1521:htssd", "aim", "aim");
        connection.setAutoCommit(false);
    }

    @Test
    public void test() {
        Person markus = new Person();
        modificationTracker.addNewParticipant(markus);
        Person martin = new Person();
        modificationTracker.addNewParticipant(martin);
        Adresse adresse = new LieferAdresse();
        modificationTracker.addNewParticipant(adresse);
        markus.setName("markus");
        martin.setName("martin");
        adresse.setStrasse("dr. boehringer gasse");
        markus.getAdressen().add(adresse);
        adresse.setPerson(martin);
    }

    @Test
    public void testPersistor() throws Exception {
        PreparedStatement ps;
        ps = connection.prepareStatement("delete from privatadresse");
        ps.executeUpdate();
        ps.close();
        ps = connection.prepareStatement("delete from adresse");
        ps.executeUpdate();
        ps.close();
        ps = connection.prepareStatement("delete from person");
        ps.executeUpdate();
        ps.close();
        Persistor p;
        Adresse csd = new LieferAdresse();
        csd.setStrasse("Amalienstrasse 68");
        modificationTracker.addNewParticipant(csd);
        Person markus = new Person();
        markus.setName("markus");
        modificationTracker.addNewParticipant(markus);
        markus.getPrivatAdressen().add(csd);
        Person martin = new Person();
        martin.setName("martin");
        modificationTracker.addNewParticipant(martin);
        p = new Persistor(getSchemaMapping(), idGenerator, modificationTracker);
        p.persist();
        Adresse bia = new LieferAdresse();
        modificationTracker.addNewParticipant(bia);
        bia.setStrasse("dr. boehringer gasse");
        markus.getAdressen().add(bia);
        bia.setPerson(martin);
        markus.setContactPerson(martin);
        p = new Persistor(getSchemaMapping(), idGenerator, modificationTracker);
        try {
            p.persist();
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoader() throws Exception {
        ClusterDescription cd = new ClusterDescription();
        cd.setRootDescription(getClassDescription(Person.class));
        cd.setPathExpressions(new String[] { "root", "root.contactPerson", "root.contactPerson.adressen" });
        ClusterLoader clusterLoader = new ClusterLoader(getSchemaMapping(), cd);
        LoaderContext loaderContext = new LoaderContext(modificationTracker);
        List<Modifiable> result = (List<Modifiable>) clusterLoader.load(connection, loaderContext, ClusterLoaderConditionProvider.Default);
        Assert.assertEquals(result.size(), 2);
        Person aim = (Person) result.get(0);
        Person ms = (Person) result.get(1);
        Assert.assertEquals(aim.getName(), "markus");
        Assert.assertEquals(aim.getAdressen().size(), 0);
        Assert.assertNull(aim.getContactPersonFor());
        Assert.assertEquals(aim.getContactPerson(), ms);
        Assert.assertEquals(ms.getContactPersonFor(), aim);
        Assert.assertEquals(ms.getName(), "martin");
        Assert.assertEquals(ms.getAdressen().size(), 1);
        Assert.assertEquals(ms.getAdressen().get(0).getStrasse(), "dr. boehringer gasse");
        ms.getAdressen().remove(0);
        Persistor p = new Persistor(getSchemaMapping(), idGenerator, modificationTracker);
        try {
            p.persist();
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoader2() throws Exception {
        ClusterDescription cd = new ClusterDescription();
        cd.setRootDescription(getClassDescription(Person.class));
        cd.setPathExpressions(new String[] { "root", "root.privatAdressen" });
        ClusterLoader clusterLoader = new ClusterLoader(getSchemaMapping(), cd);
        LoaderContext loaderContext = new LoaderContext(modificationTracker);
        List<Modifiable> result = (List<Modifiable>) clusterLoader.load(connection, loaderContext, ClusterLoaderConditionProvider.Default);
        Assert.assertEquals(result.size(), 2);
        Person aim = (Person) result.get(0);
        aim.getPrivatAdressen().remove(0);
        aim.setName("markus");
        Persistor p = new Persistor(getSchemaMapping(), idGenerator, modificationTracker);
        try {
            p.persist();
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    @After
    public void tearDown() throws Exception {
        modificationTracker.removeListener(riListener);
        modificationTracker.dispose();
        connection.close();
    }
}
