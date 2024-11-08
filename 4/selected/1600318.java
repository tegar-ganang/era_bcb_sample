package org.maverickdbms.database.pgsql;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import org.maverickdbms.basic.ConstantString;
import org.maverickdbms.basic.MaverickException;
import org.maverickdbms.basic.Factory;
import org.maverickdbms.basic.InputChannel;
import org.maverickdbms.basic.PrintChannel;
import org.maverickdbms.basic.Program;
import org.maverickdbms.basic.Session;
import org.maverickdbms.basic.MaverickString;
import org.maverickdbms.util.UtilResolver;

public class Resolver implements org.maverickdbms.basic.Resolver, Program {

    static final String PROP_CATALOG = "org.maverickdbms.database.pgsql.catalog";

    static final String PROP_MASTER_DICT = "org.maverickdbms.database.pgsql.master_dict";

    static final String PROP_PASSWORD = "org.maverickdbms.database.pgsql.password";

    static final String PROP_SCHEMA = "org.maverickdbms.database.pgsql.schema";

    static final String PROP_URL = "org.maverickdbms.database.pgsql.url";

    static final String PROP_USER = "org.maverickdbms.database.pgsql.username";

    static final String DEFAULT_CATALOG = "";

    static final String DEFAULT_PASSWORD = "";

    static final String DEFAULT_SCHEMA = "";

    static final String DEFAULT_URL = "";

    static final String DEFAULT_USER = "";

    static final String DEFAULT_MASTER_DICT = "org.maverickdbms.database.pgsql.IMasterDictionaryFile";

    private static final String DATA_FILE = "DATA";

    private static final String DICT_FILE = "DICT";

    private static final String DICT_DICT_FILE = "DICT.DICT";

    static final String DB_DRIVER = "org.postgresql.Driver";

    static final String USER_PROMPT = "User Name: ";

    static final String PASS_PROMPT = "Password: ";

    private Session session;

    private Factory factory;

    private Driver drv;

    private Connection conn;

    private String catalog;

    private String schema;

    private MasterDictionaryFile master;

    private Hashtable files = new Hashtable();

    private Hashtable dicts = new Hashtable();

    private Hashtable lists = new Hashtable();

    private org.maverickdbms.basic.Resolver next;

    public void close() {
        conn = null;
    }

    public void createFile(ConstantString type, ConstantString name, int flags, ConstantString[] args) throws MaverickException {
        int modulo = (args.length > 0) ? args[0].intValue() : 1;
        String[] fields = new String[modulo + 1];
        fields[0] = "ID";
        for (int i = 1; i < modulo + 1; i++) {
            fields[i] = "A" + i;
        }
        if (type.length() == 0 || type.equals(DATA_FILE)) {
            StringBuffer sb = new StringBuffer("CREATE TABLE ");
            sb.append(name);
            sb.append(" ( ");
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                    sb.append(fields[i]);
                    sb.append(' ');
                    sb.append("TEXT");
                } else {
                    sb.append(fields[i]);
                    sb.append(' ');
                    sb.append("VARCHAR(255) PRIMARY KEY");
                }
            }
            sb.append(" );");
            try {
                Statement s = conn.createStatement();
                int success = s.executeUpdate(sb.toString());
            } catch (SQLException e) {
                throw new MaverickException(0, e);
            }
        }
        String t = type.toString();
        if (t.length() == 0 || t.equals(DICT_FILE)) {
            StringBuffer sb = new StringBuffer("CREATE TABLE ");
            sb.append("D_");
            sb.append(name);
            sb.append(" ( ");
            int magicnumber = 8;
            for (int i = 0; i < magicnumber; i++) {
                if (i > 0) {
                    sb.append(", ");
                    sb.append("A" + i);
                    sb.append(' ');
                    sb.append("TEXT");
                } else {
                    sb.append("ID");
                    sb.append(' ');
                    sb.append("VARCHAR(255) PRIMARY KEY");
                }
            }
            sb.append(" );");
            try {
                Statement s = conn.createStatement();
                int success = s.executeUpdate(sb.toString());
            } catch (SQLException e) {
                throw new MaverickException(0, e);
            }
            sb = new StringBuffer("INSERT INTO D_");
            sb.append(name);
            sb.append(" (");
            for (int i = 0; i < magicnumber; i++) {
                if (i > 0) {
                    sb.append(", A" + i);
                } else {
                    sb.append("ID");
                }
            }
            sb.append(") VALUES");
            sb.append(" ('");
            sb.append(master.getIdString());
            sb.append("', 'D', '");
            sb.append(0);
            sb.append("','','");
            sb.append(name.toString());
            sb.append("','10L','S','')");
            sb.append(";");
            try {
                Statement s = conn.createStatement();
                int success = s.executeUpdate(sb.toString());
            } catch (SQLException e) {
                throw new MaverickException(0, e);
            }
        }
    }

    public void deleteList(ConstantString name, MaverickString status) throws MaverickException {
        if (next != null) {
            next.deleteList(name, status);
        } else {
            throw new MaverickException(0, "Sorry deleteList not implemented.");
        }
    }

    public void dropFile(ConstantString type, ConstantString name) throws MaverickException {
        if (type.length() == 0 || type.equals(DATA_FILE)) {
            StringBuffer sb = new StringBuffer("DROP TABLE ");
            sb.append(name);
            sb.append(";");
            try {
                Statement s = conn.createStatement();
                int success = s.executeUpdate(sb.toString());
            } catch (SQLException e) {
                throw new MaverickException(0, e);
            }
        }
        String t = type.toString();
        if (t.length() == 0 || t.equals(DICT_FILE)) {
            StringBuffer dict = new StringBuffer("DROP TABLE ");
            dict.append("D_");
            dict.append(name);
            dict.append(";");
            try {
                Statement s = conn.createStatement();
                int success = s.executeUpdate(dict.toString());
            } catch (SQLException e) {
                throw new MaverickException(0, e);
            }
        }
    }

    public boolean isClosed() {
        return conn == null;
    }

    public ConstantString run(Session session, MaverickString[] args) throws MaverickException {
        this.session = session;
        factory = session.getFactory();
        catalog = session.getProperty(PROP_CATALOG, DEFAULT_CATALOG);
        schema = session.getProperty(PROP_SCHEMA, DEFAULT_SCHEMA);
        String masterdict = session.getProperty(PROP_MASTER_DICT, DEFAULT_MASTER_DICT);
        try {
            Class mdclass = Class.forName(masterdict);
            Class[] parameters = { Factory.class };
            Constructor constructor = mdclass.getConstructor(parameters);
            Object[] parameters2 = { factory };
            master = (MasterDictionaryFile) constructor.newInstance(parameters2);
        } catch (ClassNotFoundException cnfe2) {
            throw new MaverickException(0, cnfe2);
        } catch (IllegalAccessException iae2) {
            throw new MaverickException(0, iae2);
        } catch (InstantiationException ie) {
            throw new MaverickException(0, ie);
        } catch (InvocationTargetException ite) {
            throw new MaverickException(0, ite);
        } catch (NoSuchMethodException nsme) {
            throw new MaverickException(0, nsme);
        }
        InputChannel input = session.getInputChannel();
        PrintChannel channel = session.getChannel(Session.SCREEN_CHANNEL);
        MaverickString status = session.getStatus();
        try {
            Driver drv = (Driver) Class.forName(DB_DRIVER).newInstance();
            String user = session.getProperty(PROP_USER, DEFAULT_USER);
            session.PROMPT(ConstantString.EMPTY);
            if (user == null || user.length() == 0) {
                channel.PRINT(factory.getConstant(USER_PROMPT), false, status);
                MaverickString temp = factory.getString();
                input.INPUT(temp, ConstantString.ZERO, true, false, status);
                user = temp.toString();
            }
            String pass = session.getProperty(PROP_PASSWORD, DEFAULT_PASSWORD);
            if (pass == null || pass.length() == 0) {
                channel.PRINT(factory.getConstant(PASS_PROMPT), false, status);
                MaverickString temp = factory.getString();
                input.INPUT(temp, ConstantString.ZERO, true, false, status);
                pass = temp.toString();
            }
            String url = session.getProperty(PROP_URL, DEFAULT_URL);
            conn = DriverManager.getConnection(url, user, pass);
            UtilResolver resolver = new UtilResolver();
            resolver.run(session, new MaverickString[0]);
            factory.pushResolver(this);
            return null;
        } catch (ClassNotFoundException cnfe) {
            throw new MaverickException(0, cnfe);
        } catch (IllegalAccessException iae) {
            throw new MaverickException(0, iae);
        } catch (InstantiationException ie) {
            throw new MaverickException(0, ie);
        } catch (SQLException sqle) {
            throw new MaverickException(0, sqle);
        }
    }

    public void CLEARSELECT() throws MaverickException {
        throw new MaverickException(0, "Sorry CLEARSELECT is not implemented yet");
    }

    public org.maverickdbms.basic.Resolver getNextResolver() {
        return next;
    }

    public ConstantString resolveFile(Program program, MaverickString var, ConstantString type, ConstantString name, int flags, MaverickString status) throws MaverickException {
        if (type.equals(DICT_DICT_FILE)) {
            var.setFile(master);
            return ConstantString.RETURN_SUCCESS;
        }
        String filename = name.toString();
        try {
            if (type.equals(DICT_FILE)) {
                File f = (File) dicts.get(filename);
                if (f == null || f.isClosed()) {
                    f = new DictionaryFile(session, master, factory, conn, catalog, schema, "D_" + filename);
                    f.addProgram(program);
                    if (f.isClosed()) {
                        return ConstantString.RETURN_ELSE;
                    }
                    dicts.put(filename, f);
                } else {
                    f.addProgram(program);
                }
                f.addProgram(program);
                var.setFile(f);
            } else {
                File f = (File) files.get(filename);
                if (f == null || f.isClosed()) {
                    f = new File(factory, conn, catalog, schema, filename);
                    f.addProgram(program);
                    if (f.isClosed()) {
                        return ConstantString.RETURN_ELSE;
                    }
                    files.put(filename, f);
                } else {
                    f.addProgram(program);
                }
                var.setFile(f);
            }
            return ConstantString.RETURN_SUCCESS;
        } catch (SQLException sqle) {
            throw new MaverickException(0, sqle);
        }
    }

    public void RELEASE() throws MaverickException {
        throw new MaverickException(0, "Sorry RELEASE is not implemented yet");
    }

    public Program resolveCommand(ConstantString command) throws MaverickException {
        return (next != null) ? next.resolveCommand(command) : null;
    }

    public ConstantString resolveList(MaverickString var, MaverickString count, ConstantString record, MaverickString status) throws MaverickException {
        return (next != null) ? next.resolveList(var, count, record, status) : ConstantString.RETURN_ELSE;
    }

    public Program resolveProgram(ConstantString name) throws MaverickException {
        return next.resolveProgram(name);
    }

    public void setNextResolver(org.maverickdbms.basic.Resolver next) {
        this.next = next;
    }

    public void writeList(ConstantString var, ConstantString record, MaverickString status) throws MaverickException {
        if (next != null) {
            next.writeList(var, record, status);
        } else {
            throw new MaverickException(0, "Sorry not implemented.");
        }
    }
}
