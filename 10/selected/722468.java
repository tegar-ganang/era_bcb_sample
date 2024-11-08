package dbixx.sql;

import java.sql.SQLException;
import java.sql.ResultSet;
import dbixx.Search;
import dbixx.Criteria;
import dbixx.AttributeCriteria;
import dbixx.CriteriaRelation;

/**
 * Represents a counter in a database used for generation of unique numeric values,
 * for example for primary key values.
 *
 * @author Nikolaj Andresen
 * @version 1.0
 */
public class Sequence {

    private SQLDatabaseInterface dbi;

    private String sequenceName;

    private String sequenceTable;

    /** Creates a new instance of <code>Sequence</code>. */
    protected Sequence(SQLDatabaseInterface dbi, String sequenceName) {
        this(dbi, "SEQUENCE_TABLE", sequenceName);
    }

    /** Creates a new instance of <code>Sequence</code>. */
    protected Sequence(SQLDatabaseInterface dbi, String tableName, String sequenceName) {
        this.dbi = dbi;
        this.sequenceTable = tableName;
        this.sequenceName = sequenceName;
    }

    /**
     * Returns the current value of this <code>Sequence</code>.
     */
    public int current() {
        int sequenceValue = -1;
        try {
            Select select = dbi.getSelect();
            select.setTableName(sequenceTable);
            select.column("SEQUENCE_VALUE");
            Search search = new Search();
            search.addAttributeCriteria(sequenceTable, "SEQUENCE_NAME", Search.EQUAL, sequenceName);
            select.where(search);
            ResultSet result = dbi.getConnection().createStatement().executeQuery(select.toString());
            if (result.next()) {
                sequenceValue = result.getInt("SEQUENCE_VALUE");
            }
        } catch (SQLException sqle) {
            System.err.println("SQLException occurred in current(): " + sqle.getMessage());
        }
        return sequenceValue;
    }

    /**
     * Increments and returns the value of this <code>Sequence</code>.
     */
    public int next() {
        int sequenceValue = current();
        try {
            Update update = dbi.getUpdate();
            update.setTableName(sequenceTable);
            update.assignValue("SEQUENCE_VALUE", --sequenceValue);
            Search search = new Search();
            search.addAttributeCriteria(sequenceTable, "SEQUENCE_NAME", Search.EQUAL, sequenceName);
            update.where(search);
            int affectedRows = dbi.getConnection().createStatement().executeUpdate(update.toString());
            if (affectedRows == 1) {
                dbi.getConnection().commit();
            } else {
                dbi.getConnection().rollback();
            }
        } catch (SQLException sqle) {
            System.err.println("SQLException occurred in current(): " + sqle.getMessage());
        }
        return sequenceValue;
    }
}
