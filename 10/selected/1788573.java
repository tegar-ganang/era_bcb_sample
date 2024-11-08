package org.vikamine.rcp.plugin.dtp.actions;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.eclipse.datatools.connectivity.IConnectionProfile;
import org.eclipse.datatools.connectivity.IManagedConnection;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.vikamine.app.DMManager;
import org.vikamine.kernel.data.Attribute;
import org.vikamine.kernel.data.DataRecordSet;
import org.vikamine.rcp.plugin.dtp.Resources;

public class ExportDataAction extends DTPAction {

    public ExportDataAction(IConnectionProfile profile) {
        super(profile);
    }

    @Override
    public boolean isEnabled() {
        if (DMManager.getInstance().getOntology() == null) return false; else return true;
    }

    @Override
    public void run() {
        Shell currentShell = Display.getCurrent().getActiveShell();
        if (DMManager.getInstance().getOntology() == null) return;
        DataRecordSet data = DMManager.getInstance().getOntology().getDataView().dataset();
        InputDialog input = new InputDialog(currentShell, Resources.I18N.getString("vikamine.dtp.title"), Resources.I18N.getString("vikamine.dtp.export.tablename"), data.getRelationName(), null);
        input.open();
        String tablename = input.getValue();
        if (tablename == null) return;
        super.getProfile().connect();
        IManagedConnection mc = super.getProfile().getManagedConnection("java.sql.Connection");
        java.sql.Connection sql = (java.sql.Connection) mc.getConnection().getRawConnection();
        try {
            sql.setAutoCommit(false);
            DatabaseMetaData dbmd = sql.getMetaData();
            ResultSet tables = dbmd.getTables(null, null, tablename, new String[] { "TABLE" });
            if (tables.next()) {
                if (!MessageDialog.openConfirm(currentShell, Resources.I18N.getString("vikamine.dtp.title"), Resources.I18N.getString("vikamine.dtp.export.overwriteTable"))) return;
                Statement statement = sql.createStatement();
                statement.executeUpdate("DROP TABLE " + tablename);
                statement.close();
            }
            String createTableQuery = null;
            for (int i = 0; i < data.getNumAttributes(); i++) {
                if (DMManager.getInstance().getOntology().isIDAttribute(data.getAttribute(i))) continue;
                if (createTableQuery == null) createTableQuery = ""; else createTableQuery += ",";
                createTableQuery += getColumnDefinition(data.getAttribute(i));
            }
            Statement statement = sql.createStatement();
            statement.executeUpdate("CREATE TABLE " + tablename + "(" + createTableQuery + ")");
            statement.close();
            exportRecordSet(data, sql, tablename);
            sql.commit();
            sql.setAutoCommit(true);
            MessageDialog.openInformation(currentShell, Resources.I18N.getString("vikamine.dtp.title"), Resources.I18N.getString("vikamine.dtp.export.successful"));
        } catch (SQLException e) {
            try {
                sql.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            MessageDialog.openError(currentShell, Resources.I18N.getString("vikamine.dtp.title"), Resources.I18N.getString("vikamine.dtp.export.failed"));
            e.printStackTrace();
        }
    }

    private void exportRecordSet(DataRecordSet data, Connection sql, String tablename) throws SQLException {
        for (int row = 0; row < data.getNumInstances(); row++) {
            String exportSqlQueryHead = null;
            String exportSqlQueryBody = null;
            for (int column = 0; column < data.getNumAttributes(); column++) {
                if (DMManager.getInstance().getOntology().isIDAttribute(data.getAttribute(column))) {
                    continue;
                }
                if (exportSqlQueryHead == null) exportSqlQueryHead = ""; else exportSqlQueryHead += ",";
                if (exportSqlQueryBody == null) exportSqlQueryBody = ""; else exportSqlQueryBody += ",";
                exportSqlQueryHead += data.getAttribute(column).getId();
                if (data.getAttribute(column).isNumeric()) exportSqlQueryBody += data.get(row).getValue(column); else exportSqlQueryBody += "'" + data.get(row).getStringValue(column) + "'";
            }
            Statement stm = sql.createStatement();
            String query = "INSERT INTO " + tablename + "(" + exportSqlQueryHead + ") VALUES (" + exportSqlQueryBody + ")";
            stm.executeUpdate(query);
            stm.close();
        }
    }

    private String getColumnDefinition(Attribute attribute) {
        if (DMManager.getInstance().getOntology().isIDAttribute(attribute)) {
            return "";
        }
        String def = attribute.getId() + " ";
        if (attribute.isNumeric()) {
            def += "DOUBLE";
        } else {
            def += "VARCHAR(255)";
        }
        return def;
    }
}
