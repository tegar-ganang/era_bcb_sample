package com.JMySQL;

import com.mysql.jdbc.ResultSetMetaData;
import com.mysql.jdbc.ResultSetImpl;
import com.mysql.jdbc.Statement;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.exceptions.MySQLDataException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;
import com.mysql.jdbc.exceptions.MySQLTransactionRollbackException;
import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import java.util.Scanner;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * El objetivo de esta API es  FACILITAR el uso de MySQL con JAVA
 * permitiendo una interacción más simple y transparente con el manejo de transacciones,
 * en lo que respecta consultas SQL, SELECT, INSERT, DELETE, UPDATE,
 * ALTER, CREATE, DROP, etc... (LDM,DDL) soporta 4 Niveles de Aislamiento
 * así como  el manejo de Auto-Cometido, ejecuta en caso de fallos Rollback,
 * de manera dinámica  activa o desactiva el modo Solo-Lectura.

La clase tiene la posibilidad de aumentar sus funciones pero para ello el usuario
 * debe tener un mayor conocimiento y ser especifico en propiedades exclusivas de MySQL,
 * haciendo uso de un un objecto Properties.
Ejemplo:
        (Estas estan pre-cargadas por Default en este paquete)
        propiedades.put("autoReconnect", "true");
        propiedades.put("roundRobinLoadBalance", "true");
        propiedades.put("user", usr);
        propiedades.put("password",  pw);

Debido a que la API es exclusiva para el funcionamiento con MySQL
 * es completamente necesario contar con el conector Oficial de
 * MySQL JConnector versión 5.1.7, que ha sido agregado en este paquete
 * y fue descargado de la pagina oficial de MySQL.
 *
 * El conector de MySQL mysql-connector-java-5.1.7-bin.jar esta bajo la licencia GNU así como este paquete.
 * @author Raúl Eduardo González Argote
 * @version  0.8 GNU
 * @since  7 julio de 2009
 * @see Conectar
 * @see java.sql.ResultSet
 * @see java.sql.Connection
 * @see java.util.Properties
 *
 **/
public class JMySQL extends Conectar implements InstruccionesSQL {

    private static final String email = "raulegleza@gmail.com";

    private int filasDelete = 0;

    private int filasInsert = 0;

    private int filasUpdate = 0;

    private String execConsulta;

    private Scanner tipoConsulta;

    private ResultSetMetaData rsmd;

    private ResultSetImpl rs;

    private boolean realizado = false;

    /**
     * 
     */
    public JMySQL() {
    }

    /**
     * Crea una conexion a un Servidor de Base de Datos de MySQL
     * @param login El usurio del Servidor de Base de Datos
     * @param pass La contraseña del usuario del Servidor de la Base de Datos
     * @param ruta La URL del Servidor de Base de Datos
     */
    public JMySQL(String login, String pass, String ruta) {
        conectarMySQL(login, pass, ruta);
    }

    /**
     * Crea una conexion a un Servidor de Base de Datos de MySQL
     * @param login El usurio del Servidor de Base de Datos
     * @param pass La contraseña del usuario del Servidor de la Base de Datos
     * @param ruta La URL del Servidor de Base de Datos
     */
    public JMySQL(String login, char[] pass, String ruta) {
        conectarMySQL(login, pass, ruta);
    }

    /**
     * Crea una conexion a un Servidor de Base de Datos de MySQL
     * @param prop Agrega Propiedades de manera personalizada para mayor funcionalidad
     * @param ruta La URL donde se encuentra el servidor de Base de Datos
     */
    public JMySQL(java.util.Properties prop, String ruta) {
        conectarMySQL(prop, ruta);
    }

    /**
     * Datos Extras sobre tablas a las cuales se les ejecuto alguna insutrcción.
     * @return Los MetaDatos sobre la tabla a la cual se le ejecuto la ultima consulta.
     */
    public ResultSetMetaData getRsMetaDatos() {
        return rsmd;
    }

    @Override
    public ResultSetImpl select(String consulta, boolean autocommit, int transactionIsolation, Connection cx) throws SQLException {
        if (!consulta.contains(";")) {
            this.tipoConsulta = new Scanner(consulta);
            if (this.tipoConsulta.hasNext()) {
                execConsulta = this.tipoConsulta.next();
                if (execConsulta.equalsIgnoreCase("select")) {
                    Connection conexion = cx;
                    Statement stm = null;
                    try {
                        conexion.setAutoCommit(autocommit);
                        if (transactionIsolation == 1 || transactionIsolation == 2 || transactionIsolation == 4 || transactionIsolation == 8) {
                            conexion.setTransactionIsolation(transactionIsolation);
                        } else {
                            throw new IllegalArgumentException("Valor invalido sobre TransactionIsolation,\n TRANSACTION_NONE no es soportado por MySQL");
                        }
                        stm = (Statement) conexion.createStatement(ResultSetImpl.TYPE_SCROLL_SENSITIVE, ResultSetImpl.CONCUR_UPDATABLE);
                        conexion.setReadOnly(true);
                        if (stm.execute(consulta.trim(), Statement.RETURN_GENERATED_KEYS)) {
                            if (autocommit == false) {
                                conexion.commit();
                            }
                            rs = (ResultSetImpl) stm.getResultSet();
                            rsmd = (ResultSetMetaData) rs.getMetaData();
                            return rs;
                        } else {
                            return null;
                        }
                    } catch (MySQLNonTransientConnectionException e) {
                        e.printStackTrace();
                        return null;
                    } catch (MySQLDataException e) {
                        System.out.println("Datos incorrectos");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return null;
                    } catch (MySQLSyntaxErrorException e) {
                        System.out.println("Error en la sintaxis de la Consulta en MySQL");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return null;
                    } catch (SQLException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    throw new IllegalArgumentException("No es una instruccion Select");
                }
            } else {
                try {
                    throw new JMySQLException("Error Grave , notifique al departamento de Soporte Tecnico \n" + email);
                } catch (JMySQLException ex) {
                    Logger.getLogger(JMySQL.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
            }
        } else {
            throw new IllegalArgumentException("No estan permitidas las MultiConsultas en este metodo");
        }
    }

    @Override
    public boolean update(String consulta, boolean autocommit, int transactionIsolation, Connection cx) throws SQLException {
        filasUpdate = 0;
        if (!consulta.contains(";")) {
            this.tipoConsulta = new Scanner(consulta);
            if (this.tipoConsulta.hasNext()) {
                execConsulta = this.tipoConsulta.next();
                if (execConsulta.equalsIgnoreCase("update")) {
                    Connection conexion = cx;
                    Statement st = null;
                    try {
                        conexion.setAutoCommit(autocommit);
                        if (transactionIsolation == 1 || transactionIsolation == 2 || transactionIsolation == 4 || transactionIsolation == 8) {
                            conexion.setTransactionIsolation(transactionIsolation);
                        } else {
                            throw new IllegalArgumentException("Valor invalido sobre TransactionIsolation,\n TRANSACTION_NONE no es soportado por MySQL");
                        }
                        st = (Statement) conexion.createStatement(ResultSetImpl.TYPE_SCROLL_SENSITIVE, ResultSetImpl.CONCUR_UPDATABLE);
                        conexion.setReadOnly(false);
                        filasUpdate = st.executeUpdate(consulta.trim(), Statement.RETURN_GENERATED_KEYS);
                        if (filasUpdate > -1) {
                            if (autocommit == false) {
                                conexion.commit();
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } catch (MySQLIntegrityConstraintViolationException e) {
                        System.out.println("Posible duplicacion de DATOS");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLNonTransientConnectionException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLDataException e) {
                        System.out.println("Datos incorrectos");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (MySQLSyntaxErrorException e) {
                        System.out.println("Error en la sintaxis de la Consulta en MySQL");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (SQLException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } finally {
                        try {
                            if (st != null) {
                                if (!st.isClosed()) {
                                    st.close();
                                }
                            }
                            if (!conexion.isClosed()) {
                                conexion.close();
                            }
                        } catch (NullPointerException ne) {
                            ne.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    throw new IllegalArgumentException("No es una instruccion Update");
                }
            } else {
                try {
                    throw new JMySQLException("Error Grave , notifique al departamento de Soporte Tecnico \n" + email);
                } catch (JMySQLException ex) {
                    Logger.getLogger(JMySQL.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            }
        } else {
            throw new IllegalArgumentException("No estan permitidas las MultiConsultas en este metodo");
        }
    }

    @Override
    public boolean insert(String consulta, boolean autocommit, int transactionIsolation, Connection cx) throws SQLException {
        filasInsert = 0;
        if (!consulta.contains(";")) {
            this.tipoConsulta = new Scanner(consulta);
            if (this.tipoConsulta.hasNext()) {
                execConsulta = this.tipoConsulta.next();
                if (execConsulta.equalsIgnoreCase("insert")) {
                    Connection conexion = cx;
                    Statement st = null;
                    try {
                        conexion.setAutoCommit(autocommit);
                        if (transactionIsolation == 1 || transactionIsolation == 2 || transactionIsolation == 4 || transactionIsolation == 8) {
                            conexion.setTransactionIsolation(transactionIsolation);
                        } else {
                            throw new IllegalArgumentException("Valor invalido sobre TransactionIsolation,\n TRANSACTION_NONE no es soportado por MySQL");
                        }
                        st = (Statement) conexion.createStatement(ResultSetImpl.TYPE_SCROLL_SENSITIVE, ResultSetImpl.CONCUR_UPDATABLE);
                        conexion.setReadOnly(false);
                        filasInsert = st.executeUpdate(consulta.trim(), Statement.RETURN_GENERATED_KEYS);
                        if (filasInsert > -1) {
                            if (autocommit == false) {
                                conexion.commit();
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } catch (MySQLIntegrityConstraintViolationException e) {
                        System.out.println("Posible duplicacion de DATOS");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLNonTransientConnectionException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLDataException e) {
                        System.out.println("Datos incorrectos");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (MySQLSyntaxErrorException e) {
                        System.out.println("Error en la sintaxis de la Consulta en MySQL");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (SQLException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } finally {
                        try {
                            if (st != null) {
                                if (!st.isClosed()) {
                                    st.close();
                                }
                            }
                            if (!conexion.isClosed()) {
                                conexion.close();
                            }
                        } catch (NullPointerException ne) {
                            ne.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    throw new IllegalArgumentException("No es una instruccion Insert");
                }
            } else {
                try {
                    throw new JMySQLException("Error Grave , notifique al departamento de Soporte Tecnico \n" + email);
                } catch (JMySQLException ex) {
                    Logger.getLogger(JMySQL.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            }
        } else {
            throw new IllegalArgumentException("No estan permitidas las MultiConsultas en este metodo");
        }
    }

    @Override
    public boolean delete(String consulta, boolean autocommit, int transactionIsolation, Connection cx) throws SQLException {
        filasDelete = 0;
        if (!consulta.contains(";")) {
            this.tipoConsulta = new Scanner(consulta);
            if (this.tipoConsulta.hasNext()) {
                execConsulta = this.tipoConsulta.next();
                if (execConsulta.equalsIgnoreCase("delete")) {
                    Connection conexion = cx;
                    Statement st = null;
                    try {
                        conexion.setAutoCommit(autocommit);
                        if (transactionIsolation == 1 || transactionIsolation == 2 || transactionIsolation == 4 || transactionIsolation == 8) {
                            conexion.setTransactionIsolation(transactionIsolation);
                        } else {
                            throw new IllegalArgumentException("Valor invalido sobre TransactionIsolation,\n TRANSACTION_NONE no es soportado por MySQL");
                        }
                        st = (Statement) conexion.createStatement(ResultSetImpl.TYPE_SCROLL_SENSITIVE, ResultSetImpl.CONCUR_UPDATABLE);
                        conexion.setReadOnly(false);
                        filasDelete = st.executeUpdate(consulta.trim(), Statement.RETURN_GENERATED_KEYS);
                        if (filasDelete > -1) {
                            if (autocommit == false) {
                                conexion.commit();
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } catch (MySQLIntegrityConstraintViolationException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLNonTransientConnectionException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } catch (MySQLDataException e) {
                        System.out.println("Datos incorrectos");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (MySQLSyntaxErrorException e) {
                        System.out.println("Error en la sintaxis de la Consulta en MySQL");
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        return false;
                    } catch (SQLException e) {
                        if (autocommit == false) {
                            try {
                                conexion.rollback();
                                System.out.println("Se ejecuto un Rollback");
                            } catch (MySQLTransactionRollbackException sqlE) {
                                System.out.println("No se ejecuto un Rollback");
                                sqlE.printStackTrace();
                            } catch (SQLException se) {
                                se.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        return false;
                    } finally {
                        try {
                            if (st != null) {
                                if (!st.isClosed()) {
                                    st.close();
                                }
                            }
                            if (!conexion.isClosed()) {
                                conexion.close();
                            }
                        } catch (NullPointerException ne) {
                            ne.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    throw new IllegalArgumentException("No es una instruccion Delete");
                }
            } else {
                try {
                    throw new JMySQLException("Error Grave , notifique al departamento de Soporte Tecnico \n" + email);
                } catch (JMySQLException ex) {
                    Logger.getLogger(JMySQL.class.getName()).log(Level.SEVERE, null, ex);
                    return false;
                }
            }
        } else {
            throw new IllegalArgumentException("No estan permitidas las MultiConsultas en este metodo");
        }
    }

    @Override
    public boolean otraConsultaSql(String consulta, boolean autocommit, boolean ro, int transactionIsolation, Connection cx) throws SQLException {
        if (!consulta.contains(";")) {
            Connection conexion = cx;
            Statement st = null;
            execConsulta = consulta;
            try {
                conexion.setAutoCommit(autocommit);
                if (transactionIsolation == 1 || transactionIsolation == 2 || transactionIsolation == 4 || transactionIsolation == 8) {
                    conexion.setTransactionIsolation(transactionIsolation);
                } else {
                    throw new IllegalArgumentException("Valor invalido sobre TransactionIsolation,\n TRANSACTION_NONE no es soportado por MySQL");
                }
                st = (Statement) conexion.createStatement(ResultSetImpl.TYPE_SCROLL_SENSITIVE, ResultSetImpl.CONCUR_UPDATABLE);
                conexion.setReadOnly(ro);
                if (!st.execute(consulta.trim(), Statement.RETURN_GENERATED_KEYS)) {
                    if (autocommit == false) {
                        conexion.commit();
                    }
                    return true;
                } else {
                    return false;
                }
            } catch (MySQLIntegrityConstraintViolationException e) {
                System.out.println("Posible duplicacion de DATOS");
                if (autocommit == false) {
                    try {
                        conexion.rollback();
                        System.out.println("Se ejecuto un Rollback");
                    } catch (MySQLTransactionRollbackException sqlE) {
                        System.out.println("No se ejecuto un Rollback");
                        sqlE.printStackTrace();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
                e.printStackTrace();
                return false;
            } catch (MySQLNonTransientConnectionException e) {
                if (autocommit == false) {
                    try {
                        conexion.rollback();
                        System.out.println("Se ejecuto un Rollback");
                    } catch (MySQLTransactionRollbackException sqlE) {
                        System.out.println("No se ejecuto un Rollback");
                        sqlE.printStackTrace();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
                e.printStackTrace();
                return false;
            } catch (MySQLDataException e) {
                System.out.println("Datos incorrectos");
                if (autocommit == false) {
                    try {
                        conexion.rollback();
                        System.out.println("Se ejecuto un Rollback");
                    } catch (MySQLTransactionRollbackException sqlE) {
                        System.out.println("No se ejecuto un Rollback");
                        sqlE.printStackTrace();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
                return false;
            } catch (MySQLSyntaxErrorException e) {
                System.out.println("Error en la sintaxis de la Consulta en MySQL");
                if (autocommit == false) {
                    try {
                        conexion.rollback();
                        System.out.println("Se ejecuto un Rollback");
                    } catch (MySQLTransactionRollbackException sqlE) {
                        System.out.println("No se ejecuto un Rollback");
                        sqlE.printStackTrace();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
                return false;
            } catch (SQLException e) {
                if (autocommit == false) {
                    try {
                        conexion.rollback();
                        System.out.println("Se ejecuto un Rollback");
                    } catch (MySQLTransactionRollbackException sqlE) {
                        System.out.println("No se ejecuto un Rollback");
                        sqlE.printStackTrace();
                    } catch (SQLException se) {
                        se.printStackTrace();
                    }
                }
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (st != null) {
                        if (!st.isClosed()) {
                            st.close();
                        }
                    }
                    if (!conexion.isClosed()) {
                        conexion.close();
                    }
                } catch (NullPointerException ne) {
                    ne.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new IllegalArgumentException("No estan permitidas las MultiConsultas en este metodo");
        }
    }

    /**
     * Ejecuta las consultas SQL con el nivel de Aislamiento TRANSACTION_READ_COMMITTED,
     * autocommit activado y dinamicamente evualua si debe usar o no el modo de solo-lecutar,
     * permite ejecutar varios comandos de SQL al mismo tiempo
     * @param consulta recibe instrucciones SQL (LDM y LDD)
     * @return Regresa un Object, por lo que debe realizar un Casting(Boolean o ResultSet o MultiConsultas)
     * si ejecuta varias instrucciones al mismo tiempo debera crear un objeto de MultiConsultas.
     * @throws SQLSyntaxErrorException
     */
    public Object jsql(String consulta) throws SQLException {
        if (!consulta.contains(";")) {
            Scanner sc = new Scanner(consulta);
            if (sc.hasNext()) {
                execConsulta = sc.next();
            }
            Object var = null;
            if (execConsulta.equalsIgnoreCase(sqlInstrucciones.delete.name())) {
                var = delete(consulta.trim(), true, Connection.TRANSACTION_READ_COMMITTED, conexion());
                filasDelete = getFilasDelete();
                if (var.equals(true) || filasDelete >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.update.name())) {
                var = update(consulta.trim(), true, Connection.TRANSACTION_READ_COMMITTED, conexion());
                filasUpdate = getFilasUpdate();
                if (var.equals(true) || filasUpdate >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.select.name())) {
                var = select(consulta.trim(), true, Connection.TRANSACTION_READ_COMMITTED, conexion());
                if ((ResultSetImpl) var != null) {
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.insert.name())) {
                var = insert(consulta.trim(), true, Connection.TRANSACTION_READ_COMMITTED, conexion());
                filasInsert = getFilasInsert();
                if (var.equals(true) || filasInsert >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.create.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.alter.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.grant.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.drop.name())) {
                if (otraConsultaSql(consulta.trim(), true, false, Connection.TRANSACTION_READ_COMMITTED, conexion()) == true) {
                    System.out.println("Ejecutando " + execConsulta);
                    realizado = true;
                    return true;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else {
                throw new MySQLSyntaxErrorException("Valor Invalido,\n La instrucción " + execConsulta + " no esta soportada ,\n posible error de Sintaxis");
            }
        } else {
            Scanner sc = new Scanner(consulta).useDelimiter(";");
            MultiConsultas mc = new MultiConsultas();
            while (sc.hasNext()) {
                String t = sc.next().trim();
                if (!t.equals(null) || !t.equals("") || t != null) mc.add(jsql(t));
            }
            return mc;
        }
    }

    /**
     * Ejecuta las consultas SQL con el nivel de Aislamiento que se intruduzca, permite manejar el
     * autocommit y dinamicamente evualua si debe usar o no el modo de solo-lecutar,
     * permite ejecutar varios comandos de SQL al mismo tiempo.
     * @param consulta recibe instrucciones SQL (LDM y LDD)
     * @param nivelAislamiento los unicos niveles soportados son:
     *  TRANSACTION_READ_COMMITTED	2
     *  TRANSACTION_READ_UNCOMMITTED	1
     *  TRANSACTION_REPEATABLE_READ	4
     *  TRANSACTION_SERIALIZABLE	8
     * @return Regresa un Object, por lo que debe realizar un Casting(Boolean o ResultSet o MultiConsultas) para poder obtener el valor correcto.
     * @throws SQLSyntaxErrorException
     */
    public Object jsql(String consulta, int nivelAislamiento) throws SQLException {
        if (!consulta.contains(";")) {
            Scanner sc = new Scanner(consulta);
            if (sc.hasNext()) {
                execConsulta = sc.next();
            }
            Object var = null;
            if (execConsulta.equalsIgnoreCase(sqlInstrucciones.delete.name())) {
                var = delete(consulta.trim(), false, nivelAislamiento, conexion());
                filasDelete = getFilasDelete();
                if (var.equals(true) || filasDelete >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.update.name())) {
                var = update(consulta.trim(), false, nivelAislamiento, conexion());
                filasUpdate = getFilasUpdate();
                if (var.equals(true) || filasUpdate >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.select.name())) {
                var = select(consulta.trim(), false, nivelAislamiento, conexion());
                if ((ResultSetImpl) var != null) {
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.insert.name())) {
                var = insert(consulta.trim(), false, nivelAislamiento, conexion());
                filasInsert = getFilasInsert();
                if (var.equals(true) || filasInsert >= 0) {
                    realizado = (Boolean) var;
                    return var;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else if (execConsulta.equalsIgnoreCase(sqlInstrucciones.create.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.alter.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.grant.name()) || execConsulta.equalsIgnoreCase(sqlInstrucciones.drop.name())) {
                if (otraConsultaSql(consulta.trim(), false, false, Connection.TRANSACTION_READ_COMMITTED, conexion()) == true) {
                    System.out.println("Ejecutando " + execConsulta);
                    realizado = true;
                    return true;
                } else {
                    throw new Error("Error al ejecutar la instruccion de tipo " + execConsulta);
                }
            } else {
                throw new MySQLSyntaxErrorException("Valor Invalido,\n La instrucción " + execConsulta + " no esta soportada ,\n posible error de Sintaxis");
            }
        } else {
            Scanner sc = new Scanner(consulta).useDelimiter(";");
            MultiConsultas mc = new MultiConsultas();
            while (sc.hasNext()) {
                String t = sc.next().trim();
                if (!t.equals(null) || !t.equals("") || t != null) mc.add(jsql(t));
            }
            return mc;
        }
    }

    /**
     *
     * @return Retorna el nombre del Autor del Software
     */
    public String autor() {
        System.out.println("Autor: " + autor);
        return "Autor: " + autor;
    }

    /**
     * Regresa el resultado del la ultima instrucción que se haya ejecutado con el metodo jsql.
     * @return true si fue cometida satisfactoriamente o false si hubo algun fallo.
     */
    public boolean realizado() {
        return this.realizado;
    }

    /**
     * Regresa el nombre de la ultima Consulta realizada por el metodo jsql.
     * @return el nombre de la ultima consulta, ejemplo: SELECT,INSERT,CREATE ... etc..
     */
    public String consulta() {
        return this.execConsulta;
    }

    /**
     * El numero de filas afectadas.
     * @return el numero de filas afectadas por la ultima instrucción Insert ejecutada.
     */
    public int getFilasInsert() {
        return this.filasInsert;
    }

    /**
     * El numero de filas afectadas.
     * @return el numero de filas afectadas por la ultima instrucción Delete ejecutada.
     */
    public int getFilasDelete() {
        return this.filasDelete;
    }

    /**
     * El numero de filas afectadas.
     * @return el numero de filas afectadas por la ultima instrucción Update ejecutada.
     */
    public int getFilasUpdate() {
        return this.filasUpdate;
    }

    /**
     *
     * @return 
     */
    public ResultSetImpl getResultSet() {
        return rs;
    }
}
