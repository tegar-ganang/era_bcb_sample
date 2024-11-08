package ddbadmin.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import ddbadmin.DDBAdminView;
import ddbserver.common.ResultType;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Vector;

/**
 *
 * @author Roar
 */
public class AdminUtil {

    private SocketConnector socketConnector;

    private DDBAdminView mainWindow;

    private Vector<String> header = new Vector<String>();

    private Vector values = new Vector();

    private String host;

    private int port;

    public void newSession() {
        try {
            socketConnector = new SocketConnector(new Socket(InetAddress.getByName(host), port));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
    }

    private boolean checkname(String name) {
        return true;
    }

    private boolean checktype(String type) {
        if (!type.equalsIgnoreCase("int") && !type.equalsIgnoreCase("integer") && !type.equalsIgnoreCase("char") && !type.equalsIgnoreCase("varchar") && !type.equalsIgnoreCase("float") && !type.equalsIgnoreCase("double")) {
            return false;
        }
        return true;
    }

    public void ExecuteCreateDB(String sql) {
        String dbname = sql;
        socketConnector.send("admin");
        socketConnector.send("createdb");
        socketConnector.send(dbname);
    }

    public void ExecuteCreate(String sql) {
        String sqls[] = sql.split("\\s", 2);
        if (sqls[0].equalsIgnoreCase("table")) {
            sqls = sqls[1].split("\\(", 2);
            String tableName = sqls[0].trim();
            sqls = sqls[1].split(",");
            System.out.println("Debug\\(,\\): \r\n");
            for (String item : sqls) {
                System.out.println(item);
            }
            int cols = sqls.length;
            String colname[] = new String[cols];
            String coltype[] = new String[cols];
            for (int i = 0; i < cols; i++) {
                String nametype[] = sqls[i].trim().split("\\(|\\)|\\s");
                if (nametype.length > 3 || nametype.length < 2 || (nametype.length == 3 && !nametype[2].matches("[1-9][0-9]*"))) {
                    mainWindow.setText("Sql Syntax error!");
                    return;
                }
                if (!checktype(nametype[1])) {
                    mainWindow.setText("Wrong type in: " + sql + "\r\n");
                }
                colname[i] = nametype[0].trim();
                coltype[i] = nametype[1].trim();
                if (nametype.length == 3) {
                    coltype[i] += "(" + nametype[2] + ")";
                }
            }
            socketConnector.send("admin");
            socketConnector.send("create");
            socketConnector.send("table");
            socketConnector.send(tableName);
            socketConnector.send("" + cols);
            for (int i = 0; i < cols; i++) {
                socketConnector.send(colname[i]);
                socketConnector.send(coltype[i]);
            }
        } else if (sqls[0].equalsIgnoreCase("database")) {
            mainWindow.setText("not implament yet\r\n");
        } else {
            mainWindow.setText("error at:" + sql + "\r\n");
        }
    }

    public void ExecuteDrop(String sql, String arg) {
        if (arg.equalsIgnoreCase("dropdb")) {
            socketConnector.send("admin");
            socketConnector.send(arg);
            socketConnector.send(sql);
        } else {
            String[] table_names = sql.split("\\s");
            socketConnector.send("admin");
            socketConnector.send(arg);
            socketConnector.send(Integer.toString(table_names.length));
            for (String item : table_names) {
                socketConnector.send(item);
            }
        }
    }

    public void ExecuteInsert(String sql) {
        String[] part = sql.split("\\s", 2);
        if (!part[0].equalsIgnoreCase("into")) {
            mainWindow.setText("syntax error: lacking of \"into\"");
            return;
        }
        int p1 = part[1].indexOf("(");
        int p2 = part[1].lastIndexOf("(");
        int q1 = part[1].indexOf(")");
        int q2 = part[1].lastIndexOf(")");
        if (p1 == -1 || p2 == -1) {
            mainWindow.setText("syntax error: lacking of \"(\"");
            return;
        } else if (q1 == -1 || q2 == -1) {
            mainWindow.setText("syntax error: lacking of \")\"");
            return;
        }
        String tablename = part[1].substring(0, p1).trim();
        String[] attrs = part[1].substring(p1 + 1, q1).trim().split(",");
        String[] values = part[1].substring(p2 + 1, q2).trim().split(",");
        if (attrs.length != values.length) {
            mainWindow.setText("syntax error: the number of values is wrong");
            return;
        }
        socketConnector.send("admin");
        socketConnector.send("insert");
        socketConnector.send(tablename);
        socketConnector.send(Integer.toString(attrs.length));
        for (int i = 0; i < attrs.length; i++) {
            socketConnector.send(attrs[i].trim() + " " + values[i].trim());
        }
    }

    public void execDelete(String sql) {
        sql = sql.toLowerCase();
        if (sql.indexOf("from") == -1) {
            mainWindow.setText("Syntax error: missing \"from\"");
            return;
        }
        String regex = "(from)|(where)";
        String[] parts = sql.split(regex);
        if (parts.length != 3) {
            mainWindow.setText("Syntax error: should be delete from...where...");
            return;
        }
        String[] tables = parts[1].split(",");
        String[] attrs = parts[2].split("and");
        socketConnector.send("admin");
        socketConnector.send("delete");
        socketConnector.send(Integer.toString(tables.length));
        for (int i = 0; i < tables.length; i++) {
            socketConnector.send(tables[i].trim());
        }
        socketConnector.send(Integer.toString(attrs.length));
        for (int i = 0; i < attrs.length; i++) {
            socketConnector.send(attrs[i].trim());
        }
    }

    public void execSelect(String sql) {
        sql = sql.toLowerCase();
        String regex = "(from)|(where)";
        String[] tmp = sql.trim().split(regex);
        String[] attrs = tmp[0].split(",");
        String[] tables = tmp[1].split(",");
        String[] predicates = tmp[2].split("and");
        ArrayList<String> joins = new ArrayList<String>();
        ArrayList<String> selects = new ArrayList<String>();
        for (int i = 0; i < predicates.length; i++) {
            String s = predicates[i];
            if (s.indexOf("=") != -1) {
                String[] lr = s.split("=");
                if (lr[0].indexOf(".") != -1 && lr[1].indexOf(".") != -1) {
                    joins.add(s);
                    continue;
                }
            }
            selects.add(s);
        }
        socketConnector.send("admin");
        socketConnector.send("select");
        socketConnector.send(Integer.toString(attrs.length));
        for (int i = 0; i < attrs.length; i++) {
            socketConnector.send(attrs[i]);
        }
        socketConnector.send(Integer.toString(tables.length));
        for (int i = 0; i < tables.length; i++) {
            socketConnector.send(tables[i]);
        }
        socketConnector.send(Integer.toString(joins.size()));
        for (String join : joins) {
            socketConnector.send(join);
        }
        socketConnector.send(Integer.toString(selects.size()));
        for (String select : selects) {
            socketConnector.send(select);
        }
        String receivePort = socketConnector.read();
        mainWindow.setText("reciving on port " + receivePort + ", please wait...");
        ResultType rt = (ResultType) this.reciveObject(this.host, Integer.parseInt(receivePort));
        header = rt.getHeaderVector();
        values = rt.getValuesVector();
        Vector pageValue = new Vector();
        mainWindow.setText("Total record number: " + values.size());
        if (values.size() <= 10) {
            for (int i = 0; i < values.size(); i++) {
                pageValue.addElement(values.get(i));
            }
        } else {
            for (int i = 0; i < 10; i++) {
                pageValue.addElement(values.get(i));
            }
        }
        mainWindow.displayQueryResutl(header, pageValue);
    }

    public void ImportData(String fileName) {
        socketConnector.send("admin");
        socketConnector.send("importData");
        socketConnector.send("1");
        socketConnector.send("import " + fileName.substring(0, fileName.length()));
    }

    public void ExecuteScript(String fileName) {
        ArrayList<String> scripts = new ArrayList<String>();
        String line = new String();
        if (fileName != null) {
            scripts = ParserScript(fileName);
        } else {
            return;
        }
        socketConnector.send("admin");
        socketConnector.send("ExecuteScript");
        socketConnector.send("" + scripts.size());
        for (int i = 0; i < scripts.size(); i++) {
            line = scripts.get(i);
            socketConnector.send(line);
        }
    }

    public ArrayList<String> ParserScript(String fileName) {
        File file = new File(fileName);
        BufferedReader br;
        ArrayList<String> scriptLines = new ArrayList<String>();
        String line = new String();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            if (br.ready()) {
                if ((line = br.readLine()) != null) {
                    do {
                        scriptLines.add(line);
                    } while ((line = br.readLine()) != null);
                }
            }
            return scriptLines;
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return null;
    }

    public void Execute(String sql) {
        newSession();
        if (sql.indexOf(';') == -1) {
            mainWindow.setText("Missing \';\':" + sql + "\r\n");
            return;
        }
        if (sql.split(";").length != 1) {
            mainWindow.setText("Missing \';\':" + sql + "\r\n");
            return;
        }
        sql = sql.split(";", 2)[0];
        String sqls[] = sql.split("\\s", 2);
        if (sqls[0].equalsIgnoreCase("createdb")) {
            ExecuteCreateDB(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("create")) {
            ExecuteCreate(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("drop")) {
            String[] tmp = sqls[1].split("\\s", 2);
            if (tmp[0].equalsIgnoreCase("database")) {
                ExecuteDrop(tmp[1], "dropdb");
            } else if (tmp[0].equalsIgnoreCase("table")) {
                ExecuteDrop(tmp[1], "droptable");
            } else {
            }
        } else if (sqls[0].equalsIgnoreCase("insert")) {
            ExecuteInsert(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("delete")) {
            execDelete(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("select")) {
            execSelect(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("importData")) {
            ImportData(sqls[1]);
        } else if (sqls[0].equalsIgnoreCase("ExecuteScript")) {
            ExecuteScript(sqls[1]);
        } else {
            mainWindow.setText("error at:" + sql + "\r\n");
        }
    }

    public AdminUtil(String ip, int port, DDBAdminView mainWindow) {
        try {
            this.host = ip;
            this.port = port;
            this.socketConnector = new SocketConnector(new Socket(InetAddress.getByName(ip), port));
            this.mainWindow = mainWindow;
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
            mainWindow.setText("cannot connect");
        }
    }

    public int connect() {
        socketConnector.send("admin");
        socketConnector.send("echo");
        String str = socketConnector.read();
        mainWindow.setText(str + " connected");
        return 0;
    }

    public int turnPage(int pageNumber) {
        Vector pageValue = new Vector();
        int currentPage = 0;
        int totalPageCount = 1;
        if (values.size() % 10 == 0) {
            totalPageCount = values.size() / 10;
        } else if (values.size() % 10 != 0) {
            totalPageCount = values.size() / 10 + 1;
        }
        if (pageNumber == 0 || pageNumber >= totalPageCount) {
            for (int i = 10 * (totalPageCount - 1); i < values.size(); i++) {
                pageValue.addElement(values.get(i));
            }
            currentPage = totalPageCount;
        } else if (pageNumber < totalPageCount) {
            for (int i = 10 * (pageNumber - 1); i < 10 * pageNumber; i++) {
                pageValue.addElement(values.get(i));
            }
            currentPage = pageNumber;
        }
        mainWindow.displayQueryResutl(header, pageValue);
        return currentPage;
    }

    public Object reciveObject(String ip, int objectPort) {
        Object retval = null;
        try {
            Socket socket = new Socket(ip, objectPort);
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
            retval = objectInput.readObject();
            try {
                objectOutput.close();
                objectInput.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void reciveFile(String ip, int filePort, String fileName) {
        byte[] buffer = new byte[2048];
        try {
            Socket socket = new Socket(ip, filePort);
            DataInputStream dataInput = new DataInputStream(socket.getInputStream());
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            DataOutputStream fio = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            while (true) {
                int read = 0;
                if (dataInput != null) {
                    read = dataInput.read(buffer);
                }
                if (read == -1) {
                    break;
                }
                fio.write(buffer, 0, read);
            }
            fio.close();
            dataInput.close();
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }
}
