package org.opennms.netmgt.vacuumd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.DbConnectionFactory;
import org.opennms.netmgt.config.VacuumdConfigFactory;
import org.opennms.netmgt.config.vacuumd.Automation;
import org.opennms.netmgt.mock.MockUtil;

/**
 * @author david
 *
 */
public class AutoProcessor implements Runnable {

    private Automation m_automation;

    public void run() {
        if (m_automation == null) return;
        try {
            runAutomation(m_automation);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param trigRowCount
     * @param trigOp
     * @param resultRows
     */
    public boolean triggerRowCheck(int trigRowCount, String trigOp, int resultRows) {
        boolean runAction = false;
        if ("<".equals(trigOp)) {
            if (trigRowCount < resultRows) runAction = true;
        } else if ("<=".equals(trigOp)) {
            if (trigRowCount <= resultRows) runAction = true;
        } else if ("=".equals(trigOp)) {
            if (trigRowCount == resultRows) runAction = true;
        } else if (">=".equals(trigOp)) {
            if (trigRowCount >= resultRows) runAction = true;
        } else if (">".equals(trigOp)) {
            if (trigRowCount > resultRows) runAction = true;
        }
        return runAction;
    }

    public boolean runAutomation(Automation auto) throws SQLException {
        boolean actionSuccessful = false;
        String actionSQL = VacuumdConfigFactory.getInstance().getAction(auto.getActionName()).getStatement().getContent();
        Collection actionColumns = getTokenizedColumns(actionSQL);
        DbConnectionFactory dcf = DatabaseConnectionFactory.getInstance();
        Connection conn = null;
        Statement triggerStatement = null;
        ResultSet triggerResultSet = null;
        int resultRows = 0;
        try {
            conn = dcf.getConnection();
            triggerStatement = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            MockUtil.println("trigger statement: " + VacuumdConfigFactory.getInstance().getTrigger(auto.getTriggerName()).getStatement().getContent());
            triggerResultSet = triggerStatement.executeQuery(VacuumdConfigFactory.getInstance().getTrigger(auto.getTriggerName()).getStatement().getContent());
            resultRows = countRows(triggerResultSet);
            if (resultRows < 1) return actionSuccessful;
            int triggerRowCount = VacuumdConfigFactory.getInstance().getTrigger(auto.getTriggerName()).getRowCount();
            String triggerOperator = VacuumdConfigFactory.getInstance().getTrigger(auto.getTriggerName()).getOperator();
            if (!triggerRowCheck(triggerRowCount, triggerOperator, resultRows)) return actionSuccessful;
            if (!resultSetHasRequiredActionColumns(triggerResultSet, actionColumns)) return actionSuccessful;
            PreparedStatement actionStatement = null;
            try {
                triggerResultSet.beforeFirst();
                conn.setAutoCommit(false);
                while (triggerResultSet.next()) {
                    actionStatement = convertActionToPreparedStatement(triggerResultSet, actionSQL, conn);
                    actionStatement.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                MockUtil.println("Cleaning up action statement.");
                actionStatement.close();
            }
            actionSuccessful = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            MockUtil.println("Cleaning up resultset.");
            triggerResultSet.close();
            MockUtil.println("Cleaning up trigger statement.");
            triggerStatement.close();
            MockUtil.println("Cleaning up database connection.");
            conn.close();
        }
        return actionSuccessful;
    }

    public boolean resultSetHasRequiredActionColumns(ResultSet rs, String actionSQL) {
        Collection actionColumns = getTokenizedColumns(actionSQL);
        return resultSetHasRequiredActionColumns(rs, actionColumns);
    }

    public boolean resultSetHasRequiredActionColumns(ResultSet rs, Collection actionColumns) {
        boolean verified = false;
        Iterator it = actionColumns.iterator();
        while (it.hasNext()) {
            try {
                if (rs.findColumn((String) it.next()) > 0) {
                    verified = true;
                }
            } catch (SQLException e) {
                MockUtil.println(e.getMessage());
                verified = false;
            }
        }
        return verified;
    }

    public Collection getTokenizedColumns(String targetString) {
        String expression = "\\$\\{(\\w+)\\}";
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(targetString);
        MockUtil.println(targetString);
        Collection tokens = new ArrayList();
        int count = 0;
        while (matcher.find()) {
            count++;
            MockUtil.println("Token " + count + ": " + matcher.group(1));
            tokens.add(matcher.group(1));
        }
        return tokens;
    }

    public int getTokenCount(String targetString) {
        String expression = "(\\$\\{\\w+\\})";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(targetString);
        int count = 0;
        while (matcher.find()) {
            count++;
            MockUtil.println("Token " + count + ": " + matcher.group());
        }
        return count;
    }

    public int countRows(ResultSet rs) throws SQLException {
        int rows = 0;
        while (rs.next()) rows++;
        rs.beforeFirst();
        return rows;
    }

    public PreparedStatement convertActionToPreparedStatement(ResultSet rs, String actionSQL, Connection conn) throws SQLException {
        String actionJDBC = actionSQL.replaceAll("\\$\\{\\w+\\}", "?");
        MockUtil.println("This: " + actionSQL + "\nTurned into this: " + actionJDBC);
        PreparedStatement stmt = conn.prepareStatement(actionJDBC);
        ArrayList actionColumns = (ArrayList) getTokenizedColumns(actionSQL);
        Iterator it = actionColumns.iterator();
        String actionColumnName = null;
        int colType;
        int i = 0;
        while (it.hasNext()) {
            actionColumnName = (String) it.next();
            stmt.setObject(++i, rs.getObject(actionColumnName));
        }
        return stmt;
    }

    public boolean containsTokens(String targetString) {
        return getTokenCount(targetString) > 0;
    }

    /**
     * @return Returns the automation.
     */
    public Automation getAutomation() {
        return m_automation;
    }

    /**
     * @param automation The automation to set.
     */
    public void setAutomation(Automation automation) {
        m_automation = automation;
    }
}
