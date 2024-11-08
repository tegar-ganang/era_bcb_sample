package test;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.infordata.ifw2m.jpa.RemoveImmediately;
import net.infordata.ifw2m.jpa.SaveOnPost;
import net.infordata.ifw2m.mdl.InvalidFieldSetException;
import net.infordata.ifw2m.mdl.flds.BooleanField;
import net.infordata.ifw2m.mdl.flds.EnumField;
import net.infordata.ifw2m.mdl.flds.FieldDefinition;
import net.infordata.ifw2m.mdl.flds.FieldSetComparator;
import net.infordata.ifw2m.mdl.flds.IField;
import net.infordata.ifw2m.mdl.flds.NumberField;
import net.infordata.ifw2m.mdl.flds.PojoFieldMetaData;
import net.infordata.ifw2m.mdl.flds.PojoFieldSet;
import net.infordata.ifw2m.mdl.flds.StringField;
import net.infordata.ifw2m.mdl.flds.TextField;
import net.infordata.ifw2m.mdl.tbl.ATableFieldSet;
import net.infordata.ifw2m.mdl.tbl.ITable;
import net.infordata.ifw2m.mdl.tbl.Table;
import net.infordata.ifw2m.util.ThreeStateEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generated code for the test suite <b>THbrTable</b> located at
 * <i>/ifw2mh.test/src/test/THbrTable.testsuite</i>.
 */
public class TJpaTable extends TestCase {

    private static Logger LOGGER = LoggerFactory.getLogger(TJpaTable.class);

    /**
   * Constructor for THbrTable.
   * @param name
   */
    public TJpaTable(String name) {
        super(name);
    }

    /**
   * Returns the JUnit test suite that implements the <b>TJpaTable</b> definition.
   */
    public static Test suite() {
        TestSuite tJpaTable = new TestSuite("TJpaTable");
        tJpaTable.addTest(new TJpaTable("loadData"));
        tJpaTable.addTest(new TJpaTable("saveData"));
        tJpaTable.addTest(new TJpaTable("postHandler"));
        return tJpaTable;
    }

    private static final Object[][] SDATA = new Object[][] { { "vp", "Valentino", "Proietti", "1", "1234", true, ThreeStateEnum.NULL }, { "mc", "Massimiliano", "Colozzi", "2", "2345", true, ThreeStateEnum.NULL }, { "sm", "Sonia", "Mansillo", "4", "4567", false, ThreeStateEnum.NULL }, { "al", "Andrea", "Locarini", "5", "5678", true, ThreeStateEnum.NULL }, { "cm", "Claudio", "Manunta", "3", "3456", true, ThreeStateEnum.NULL }, { "gn", "Gianluca", "Natalini", "6", "6789", true, ThreeStateEnum.NULL }, { "lg", "Loredana", "Gavazzi", "7", "7891", false, ThreeStateEnum.NULL }, { "mp", "Mauro", "Pimpini", "8", "8901", true, ThreeStateEnum.NULL }, { "ci", "Corrado", "Iacono", "9", "9012", true, ThreeStateEnum.NULL }, { "pp", "A. Pasquale", "Primo", "0", "0123", true, ThreeStateEnum.NULL } };

    /**
   * @see junit.framework.TestCase#setUp()
   */
    @Override
    protected void setUp() throws Exception {
        EntityManager session = Util.getCurrentEntityManager(true);
        EntityTransaction trans = session.getTransaction();
        trans.begin();
        try {
            Person person = null;
            try {
                person = (Person) session.createQuery("select p from Person p where p.uid=:uid").setHint("org.hibernate.readOnly", true).setParameter("uid", "vp").getSingleResult();
            } catch (NoResultException ex) {
            }
            if (person != null) {
                LOGGER.info("Skip data setup");
                return;
            }
            for (int i = 0; i < SDATA.length; i++) {
                person = new Person();
                person.setUid((String) SDATA[i][0]);
                person.setFirstName((String) SDATA[i][1]);
                person.setLastName((String) SDATA[i][2]);
                person.setAge(Integer.valueOf((String) SDATA[i][3]));
                person.setMale((Boolean) SDATA[i][5]);
                person.setContacted((ThreeStateEnum) SDATA[i][6]);
                session.persist(person);
            }
            trans.commit();
        } finally {
            if (trans.isActive()) trans.rollback();
        }
    }

    /**
   * @see junit.framework.TestCase#tearDown()
   */
    @Override
    protected void tearDown() throws Exception {
        Util.getCurrentEntityManager(false).close();
    }

    private static PojoFieldSet<Person> PFSP = new PojoFieldSet<Person>(Person.class, new FieldDefinition[] { new FieldDefinition(new PojoFieldMetaData("id"), new NumberField(NumberFormat.getIntegerInstance())), new FieldDefinition(new PojoFieldMetaData("uid"), new StringField()), new FieldDefinition(new PojoFieldMetaData("firstName"), new TextField()), new FieldDefinition(new PojoFieldMetaData("lastName"), new TextField()), new FieldDefinition(new PojoFieldMetaData("age"), new NumberField(NumberFormat.getIntegerInstance(), Integer.class)), new FieldDefinition(new PojoFieldMetaData("male"), new BooleanField()), new FieldDefinition(new PojoFieldMetaData("contacted"), new EnumField<ThreeStateEnum>(ThreeStateEnum.class)) });

    /**
   * loadData
   * @throws Exception
   */
    public void loadData() throws Exception {
        Table table = new Table(PFSP);
        EntityManager session = Util.getCurrentEntityManager(true);
        EntityTransaction trans = session.getTransaction();
        trans.begin();
        try {
            List<?> results = session.createQuery("select p from Person p ").setHint("org.hibernate.readOnly", true).getResultList();
            for (Iterator<?> it = results.iterator(); it.hasNext(); ) {
                Person person = (Person) it.next();
                PojoFieldSet<Person> fs = PFSP.clone();
                fs.setPojo(person);
                table.load(fs, false);
            }
        } finally {
            if (trans.isActive()) trans.rollback();
        }
        table.loaded();
        assertEquals("vp", table.getCurrent().get("uid").getValue());
        table.setSortComparator(new FieldSetComparator(new String[] { "uid" }));
        assertEquals("vp", table.getCurrent().get("uid").getValue());
        table.setCurrent(0);
        assertEquals("al", table.getCurrent().get("uid").getValue());
    }

    /**
   * saveData
   * @throws Exception
   */
    @SuppressWarnings("unchecked")
    public void saveData() throws Exception {
        Table table = new Table(PFSP);
        table.setSortComparator(new FieldSetComparator(new String[] { "uid" }));
        EntityManager session = Util.getCurrentEntityManager(true);
        EntityTransaction trans = session.getTransaction();
        trans.begin();
        try {
            List<?> results = session.createQuery("select p from Person p ").setHint("org.hibernate.readOnly", true).getResultList();
            for (Iterator<?> it = results.iterator(); it.hasNext(); ) {
                Person person = (Person) it.next();
                PojoFieldSet<Person> fs = PFSP.clone();
                fs.setPojo(person);
                table.load(fs, false);
            }
        } finally {
            if (trans.isActive()) trans.rollback();
        }
        table.loaded();
        assertEquals("al", table.getCurrent().get("uid").getValue());
        table.edit();
        {
            table.getCurrent().get("uid").setValue("");
            try {
                table.post(false);
                assertTrue(false);
            } catch (InvalidFieldSetException ex) {
            }
            table.cancel();
            assertEquals("al", table.getCurrent().get("uid").getValue());
        }
        IField<String> lastName = table.edit().<String>get("lastName");
        final String newValue = "Loka".equalsIgnoreCase(lastName.getValue()) ? "Locarini" : "Loka";
        lastName.setValue(newValue);
        table.post(false);
        assertFalse(table.isNew(table.getCurrent()));
        assertTrue(table.isChanged(table.getCurrent()));
        {
            PojoFieldSet<Person> fs = table.append(PFSP);
            fs.get("uid").setText("fdn");
            fs.get("firstName").setText("Fabio");
            fs.get("lastName").setText("Di Natale");
            fs.get("age").setText("35");
            fs.get("male").setText("true");
            fs.get("contacted").setText("TRUE");
            table.post(false);
        }
        trans = session.getTransaction();
        trans.begin();
        try {
            table.beginSaving();
            ITable<ATableFieldSet> changed = table.getChangedFieldSets();
            for (int i = 0; i < changed.getSize(); i++) {
                PojoFieldSet<Person> fs = (PojoFieldSet<Person>) changed.get(i);
                assertEquals("al", fs.getPojo().getUid());
                fs.setPojo(session.merge(fs.getPojo()));
            }
            ITable<ATableFieldSet> created = table.getNewFieldSets();
            for (int i = 0; i < created.getSize(); i++) {
                PojoFieldSet<Person> fs = (PojoFieldSet<Person>) created.get(i);
                assertEquals("fdn", fs.getPojo().getUid());
                assertNull(fs.getPojo().getId());
                assertNull(fs.get("id").getValue());
                session.createQuery("delete Person p where p.uid = :uid").setParameter("uid", "fdn").executeUpdate();
                session.persist(fs.getPojo());
                fs.setPojo(fs.getPojo());
                assertNotNull(fs.getPojo().getId());
                assertNotNull(fs.get("id").getValue());
            }
            trans.commit();
            assertNull(table.saved());
        } finally {
            if (trans.isActive()) trans.rollback();
            table.cancel();
        }
    }

    /**
   * postHandler
   * @throws Exception
   */
    public void postHandler() throws Exception {
        final SaveOnPost tph = new SaveOnPost();
        Table table = new Table(PFSP);
        table.setSortComparator(new FieldSetComparator(new String[] { "uid" }));
        EntityManager session = Util.getCurrentEntityManager(true);
        EntityTransaction trans = session.getTransaction();
        trans.begin();
        try {
            session.createQuery("delete Person p where p.uid = :uid").setParameter("uid", "fdn").executeUpdate();
            List<?> results = session.createQuery("select p from Person p ").setHint("org.hibernate.readOnly", true).getResultList();
            for (Iterator<?> it = results.iterator(); it.hasNext(); ) {
                Person person = (Person) it.next();
                PojoFieldSet<Person> fs = PFSP.clone();
                fs.setPojo(person);
                table.load(fs, false);
            }
            trans.commit();
        } finally {
            if (trans.isActive()) trans.rollback();
        }
        table.loaded();
        assertEquals("al", table.getCurrent().get("uid").getValue());
        table.edit();
        {
            table.getCurrent().get("uid").setValue(null);
            try {
                table.post(false, tph);
                assertTrue(false);
            } catch (InvalidFieldSetException ex) {
            }
            table.cancel();
            assertEquals("al", table.getCurrent().get("uid").getValue());
        }
        IField<String> lastName = table.edit().<String>get("lastName");
        final String newValue = "Loka".equalsIgnoreCase(lastName.getValue()) ? "Locarini" : "Loka";
        lastName.setValue(newValue);
        table.post(false, tph);
        assertFalse(table.isNew(table.getCurrent()));
        assertFalse(table.isChanged(table.getCurrent()));
        trans = session.getTransaction();
        {
            PojoFieldSet<Person> fs = table.append(PFSP);
            fs.get("uid").setText("fdn");
            fs.get("firstName").setText("Fabio");
            fs.get("lastName").setText("Di Natale");
            fs.get("age").setText("35");
            fs.get("male").setValue(true);
            fs.get("contacted").setValue(ThreeStateEnum.FALSE);
            assertNull(fs.getPojo());
            assertNull(fs.get("id").getValue());
            table.post(false, tph);
            assertNotNull(fs.getPojo().getId());
            assertNotNull(fs.get("id").getValue());
            LOGGER.info("NEWID: " + fs.get("id").getValue());
        }
        {
            assertEquals("fdn", table.getCurrent().get("uid").getValue());
            table.remove(new RemoveImmediately());
            LOGGER.info("NEWCURRENT: " + table.getCurrent().get("uid").getValue());
            assertEquals("gn", table.getCurrent().get("uid").getValue());
        }
    }
}
