package openr66.database.data;

import java.sql.Types;
import openr66.database.DbConstant;
import openr66.database.DbPreparedStatement;
import openr66.database.exception.OpenR66DatabaseException;
import openr66.database.exception.OpenR66DatabaseNoDataException;
import openr66.database.exception.OpenR66DatabaseSqlError;
import openr66.protocol.config.Configuration;

/**
 * @author Frederic Bregier
 *
 */
public class DbR66Configuration extends AbstractDbData {

    public static enum Columns {

        READGLOBALLIMIT, WRITEGLOBALLIMIT, READSESSIONLIMIT, WRITESESSIONLIMIT, DELAYLIMIT, UPDATEDINFO, HOSTID
    }

    public static int[] dbTypes = { Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.INTEGER, Types.VARCHAR };

    public static String table = " CONFIGURATION ";

    private String hostid;

    private long readgloballimit;

    private long writegloballimit;

    private long readsessionlimit;

    private long writesessionlimit;

    private long delayllimit;

    private int updatedInfo;

    private boolean isSaved = false;

    private final DbValue primaryKey = new DbValue(hostid, Columns.HOSTID.name());

    private final DbValue[] otherFields = { new DbValue(readgloballimit, Columns.READGLOBALLIMIT.name()), new DbValue(writegloballimit, Columns.WRITEGLOBALLIMIT.name()), new DbValue(readsessionlimit, Columns.READSESSIONLIMIT.name()), new DbValue(writesessionlimit, Columns.WRITESESSIONLIMIT.name()), new DbValue(delayllimit, Columns.DELAYLIMIT.name()), new DbValue(updatedInfo, Columns.UPDATEDINFO.name()) };

    private final DbValue[] allFields = { otherFields[0], otherFields[1], otherFields[2], otherFields[3], otherFields[4], otherFields[5], primaryKey };

    private static final String selectAllFields = Columns.READGLOBALLIMIT.name() + "," + Columns.WRITEGLOBALLIMIT.name() + "," + Columns.READSESSIONLIMIT.name() + "," + Columns.WRITESESSIONLIMIT.name() + "," + Columns.DELAYLIMIT.name() + "," + Columns.UPDATEDINFO.name() + "," + Columns.HOSTID.name();

    private static final String updateAllFields = Columns.READGLOBALLIMIT.name() + "=?," + Columns.WRITEGLOBALLIMIT.name() + "=?," + Columns.READSESSIONLIMIT.name() + "=?," + Columns.WRITESESSIONLIMIT.name() + "=?," + Columns.DELAYLIMIT.name() + "=?," + Columns.UPDATEDINFO.name() + "=?";

    private static final String insertAllValues = " (?,?,?,?,?,?,?) ";

    @Override
    protected void setToArray() {
        allFields[Columns.HOSTID.ordinal()].setValue(this.hostid);
        allFields[Columns.READGLOBALLIMIT.ordinal()].setValue(this.readgloballimit);
        allFields[Columns.WRITEGLOBALLIMIT.ordinal()].setValue(this.writegloballimit);
        allFields[Columns.READSESSIONLIMIT.ordinal()].setValue(this.readsessionlimit);
        allFields[Columns.WRITESESSIONLIMIT.ordinal()].setValue(this.writesessionlimit);
        allFields[Columns.DELAYLIMIT.ordinal()].setValue(this.delayllimit);
        allFields[Columns.UPDATEDINFO.ordinal()].setValue(this.updatedInfo);
    }

    @Override
    protected void setFromArray() throws OpenR66DatabaseSqlError {
        this.hostid = (String) allFields[Columns.HOSTID.ordinal()].getValue();
        this.readgloballimit = (Long) allFields[Columns.READGLOBALLIMIT.ordinal()].getValue();
        this.writegloballimit = (Long) allFields[Columns.WRITEGLOBALLIMIT.ordinal()].getValue();
        this.readsessionlimit = (Long) allFields[Columns.READSESSIONLIMIT.ordinal()].getValue();
        this.writesessionlimit = (Long) allFields[Columns.WRITESESSIONLIMIT.ordinal()].getValue();
        this.delayllimit = (Long) allFields[Columns.DELAYLIMIT.ordinal()].getValue();
        this.updatedInfo = (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue();
    }

    /**
     *
     * @param hostid
     * @param rg Read Global Limit
     * @param wg Write Global Limit
     * @param rs Read Session Limit
     * @param ws Write Session Limit
     * @param del Delay Limit
     * @param updatedInfo
     */
    public DbR66Configuration(String hostid, long rg, long wg, long rs, long ws, long del, int updatedInfo) {
        this.hostid = hostid;
        this.readgloballimit = rg;
        this.writegloballimit = wg;
        this.readsessionlimit = rs;
        this.writesessionlimit = ws;
        this.delayllimit = del;
        this.updatedInfo = updatedInfo;
        this.setToArray();
        this.isSaved = false;
    }

    /**
     * @param hostid
     * @throws OpenR66DatabaseException
     */
    public DbR66Configuration(String hostid) throws OpenR66DatabaseException {
        this.hostid = hostid;
        select();
    }

    @Override
    public void delete() throws OpenR66DatabaseException {
        DbPreparedStatement preparedStatement = new DbPreparedStatement(DbConstant.admin.session);
        try {
            preparedStatement.createPrepareStatement("DELETE FROM " + table + " WHERE " + Columns.HOSTID.name() + " = ?");
            primaryKey.setValue(hostid);
            this.setValue(preparedStatement, primaryKey);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new OpenR66DatabaseNoDataException("No row found");
            }
            this.isSaved = false;
        } finally {
            preparedStatement.realClose();
        }
    }

    @Override
    public void insert() throws OpenR66DatabaseException {
        if (this.isSaved) {
            return;
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(DbConstant.admin.session);
        try {
            preparedStatement.createPrepareStatement("INSERT INTO " + table + " (" + selectAllFields + ") VALUES " + insertAllValues);
            this.setValues(preparedStatement, allFields);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new OpenR66DatabaseNoDataException("No row found");
            }
            this.isSaved = true;
        } finally {
            preparedStatement.realClose();
        }
    }

    @Override
    public void select() throws OpenR66DatabaseException {
        DbPreparedStatement preparedStatement = new DbPreparedStatement(DbConstant.admin.session);
        try {
            preparedStatement.createPrepareStatement("SELECT " + selectAllFields + " FROM " + table + " WHERE " + Columns.HOSTID.name() + " = ?");
            primaryKey.setValue(hostid);
            this.setValue(preparedStatement, primaryKey);
            preparedStatement.executeQuery();
            if (preparedStatement.getNext()) {
                this.getValues(preparedStatement, allFields);
                this.setFromArray();
                this.isSaved = true;
            } else {
                throw new OpenR66DatabaseNoDataException("No row found");
            }
        } finally {
            preparedStatement.realClose();
        }
    }

    @Override
    public void update() throws OpenR66DatabaseException {
        if (this.isSaved) {
            return;
        }
        DbPreparedStatement preparedStatement = new DbPreparedStatement(DbConstant.admin.session);
        try {
            preparedStatement.createPrepareStatement("UPDATE " + table + " SET " + updateAllFields + " WHERE " + Columns.HOSTID.name() + " = ?");
            this.setValues(preparedStatement, allFields);
            int count = preparedStatement.executeUpdate();
            if (count <= 0) {
                throw new OpenR66DatabaseNoDataException("No row found");
            }
            this.isSaved = true;
        } finally {
            preparedStatement.realClose();
        }
    }

    @Override
    public void changeUpdatedInfo(int status) {
        if (this.updatedInfo != status) {
            this.updatedInfo = status;
            allFields[Columns.UPDATEDINFO.ordinal()].setValue(this.updatedInfo);
            this.isSaved = false;
        }
    }

    public void updateConfiguration() {
        Configuration.configuration.changeNetworkLimit(writegloballimit, readgloballimit, writesessionlimit, readsessionlimit);
    }
}
