package com.j256.ormlite.android;

import java.sql.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.BaseConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Android version of the connection source. Takes a standard Android {@link SQLiteOpenHelper}. For best results, use
 * {@link OrmLiteSqliteOpenHelper}. You can also construct with a {@link SQLiteDatabase}.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidConnectionSource extends BaseConnectionSource implements ConnectionSource {

    private static final Logger logger = LoggerFactory.getLogger(AndroidConnectionSource.class);

    private final SQLiteOpenHelper helper;

    private final SQLiteDatabase sqliteDatabase;

    private AndroidDatabaseConnection connection = null;

    private volatile boolean isOpen = true;

    private final DatabaseType databaseType = new SqliteAndroidDatabaseType();

    public AndroidConnectionSource(SQLiteOpenHelper helper) {
        this.helper = helper;
        this.sqliteDatabase = null;
    }

    public AndroidConnectionSource(SQLiteDatabase sqliteDatabase) {
        this.helper = null;
        this.sqliteDatabase = sqliteDatabase;
    }

    public DatabaseConnection getReadOnlyConnection() throws SQLException {
        return getReadWriteConnection();
    }

    public DatabaseConnection getReadWriteConnection() throws SQLException {
        DatabaseConnection conn = getSavedConnection();
        if (conn != null) {
            return conn;
        }
        if (connection == null) {
            SQLiteDatabase db;
            if (sqliteDatabase == null) {
                try {
                    db = helper.getWritableDatabase();
                } catch (android.database.SQLException e) {
                    throw SqlExceptionUtil.create("Getting a writable database from helper " + helper + " failed", e);
                }
            } else {
                db = sqliteDatabase;
            }
            connection = new AndroidDatabaseConnection(db, true);
            logger.trace("created connection {} for db {}, helper {}", connection, db, helper);
        } else {
            logger.trace("{}: returning read-write connection {}, helper {}", this, connection, helper);
        }
        return connection;
    }

    public void releaseConnection(DatabaseConnection connection) {
    }

    public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
        return saveSpecial(connection);
    }

    public void clearSpecialConnection(DatabaseConnection connection) {
        clearSpecial(connection, logger);
    }

    public void close() {
        isOpen = false;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
    }
}
