package edu.conexion;

import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Vector;

/**
 * Clase que realiza el acceso a la base de datos.
 * Driver, sUrl, sUsr, sPwd est�n fijos, pero deben leerse de un archivo encriptado
 */
public final class DataAccess {

    private static String sUrl = null;

    private static String sUsr = null;

    private static String sPwd = null;

    private java.sql.Connection oConexion;

    public DataAccess() throws Exception {
        sUrl = "jdbc:mysql://localhost/patecatl";
        sUsr = "root";
        sPwd = "1123581321";
    }

    /**
     * Realiza la conexi�n a la base de datos.
     */
    public boolean openConection() throws Exception {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            oConexion = DriverManager.getConnection(sUrl, sUsr, sPwd);
            System.out.println("conectado");
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Realiza la desconexi�n a la base de datos.
     */
    public boolean closeConection() throws Exception {
        oConexion.close();
        System.out.println("desconectado");
        return true;
    }

    /**
     * C�digo que se ejecuta cuando este objeto es colectado.
     */
    public void finalize() throws Exception {
        oConexion.close();
        oConexion = null;
    }

    /**
     * Realiza una consulta a la base de datos y retorna un vector de resultados.
     */
    public synchronized Vector<Object> executeQuerys(String psQuery) throws Exception {
        Statement stmt = null;
        ResultSet rset = null;
        Vector<Object> vrset = null;
        ResultSetMetaData rsmd = null;
        int nNumCols = 0;
        try {
            stmt = oConexion.createStatement();
            rset = stmt.executeQuery(psQuery);
            rsmd = rset.getMetaData();
            nNumCols = rsmd.getColumnCount();
            vrset = convierteAVector(rset, rsmd, nNumCols);
        } finally {
            if (rset != null) {
                rset.close();
                stmt.close();
            }
            rset = null;
            stmt = null;
        }
        return vrset;
    }

    /**
     * Realiza una petici�n de modificaci�n de datos, retornando
     * un int con el n�mero de registros afectados.
     */
    public synchronized int executeCommand(String psStatement) throws Exception {
        int ret = 0;
        Vector<String> vTransaction = new Vector<String>();
        vTransaction.addElement(psStatement);
        ret = executeCommand(vTransaction);
        return ret;
    }

    /**
     * Realiza una serie de peticiones de modificaci�n de datos, retornando
     * un int con el n�mero de registros afectados.
     * Estas peticiones son ejecutadas todas en una transacci�n.
     */
    public synchronized int executeCommand(Vector<String> pvStatement) throws Exception {
        int ret = 0, i = 0;
        Statement stmt = null;
        String temp = "";
        try {
            oConexion.setAutoCommit(false);
            stmt = oConexion.createStatement();
            for (i = 0; i < pvStatement.size(); i++) {
                temp = (String) pvStatement.elementAt(i);
                ret += stmt.executeUpdate(temp);
            }
            oConexion.commit();
        } catch (SQLException e) {
            oConexion.rollback();
            throw e;
        } finally {
            stmt.close();
            stmt = null;
        }
        return ret;
    }

    /**
     * Recorre un result set y entrega el vector resultante.
     */
    private synchronized Vector<Object> convierteAVector(ResultSet rset, ResultSetMetaData rsmd, int nNumCols) throws Exception {
        Vector<Object> vrset = new Vector<Object>();
        Vector<Object> vrsettmp = null;
        int i = 0;
        while (rset.next()) {
            vrsettmp = new Vector<Object>();
            for (i = 1; i <= nNumCols; i++) {
                switch(rsmd.getColumnType(i)) {
                    case Types.CHAR:
                    case Types.VARCHAR:
                        String varchar = "" + doubleQuote(rset.getString(i));
                        vrsettmp.addElement(varchar);
                        break;
                    case Types.INTEGER:
                        vrsettmp.addElement(new Double(rset.getLong(i)));
                        break;
                    case Types.SMALLINT:
                        vrsettmp.addElement(new Double(rset.getInt(i)));
                        break;
                    case Types.NUMERIC:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                        vrsettmp.addElement(new Double(rset.getDouble(i)));
                        break;
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        vrsettmp.addElement((rset.getTimestamp(i) == null ? null : new Date(rset.getTimestamp(i).getTime())));
                        break;
                    default:
                        String str = "" + rset.getString(i);
                        vrsettmp.addElement(str);
                }
            }
            vrset.addElement(vrsettmp);
        }
        return vrset;
    }

    /**
     * Imprime en forma adecuada este objeto.
     * @return String los datos del objeto.
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("Class = DataAccess \n");
        s.append("    static sUrl  = " + sUrl + "\n");
        s.append("    static sUsr = " + sUsr + "\n");
        s.append("    static sPwd  = " + sPwd + "\n");
        s.append("    oConexion = " + oConexion + "\n");
        return s.toString();
    }

    /**
     * Si la cadena contiene comillas en la base de datos, convierte a c�digo.
     * @return String cadena sin las comillas internas.
     */
    private String doubleQuote(String psCadena) {
        if (psCadena == null) {
            psCadena = "";
        }
        String CadenaEntrada = "";
        if (psCadena.equals("")) {
            return psCadena;
        } else if (psCadena.equals("\"")) {
            return "&quot;";
        } else {
            int indice = -2;
            CadenaEntrada = psCadena;
            while ((indice = CadenaEntrada.indexOf("\"", indice + 2)) != -1) CadenaEntrada = CadenaEntrada.substring(0, CadenaEntrada.indexOf("\"", indice)) + "&quot;" + CadenaEntrada.substring(CadenaEntrada.indexOf("\"", indice) + 1);
        }
        return CadenaEntrada;
    }
}
