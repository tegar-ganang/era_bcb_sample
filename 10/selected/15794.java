package com.clanwts.bots.gamelist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.clanwts.bncs.bot.SimpleBattleNetChatBot;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExServer;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExServer.GameInfo;
import edu.cmu.ece.agora.futures.FutureListener;

public class WC3Gnome extends SimpleBattleNetChatBot {

    private static final int SQL_FAIL_DELAY_SEC = 20;

    private static final int BNET_FAIL_DELAY_SEC = 20;

    private static final int BNET_SUCCESS_DELAY_SEC = 5;

    private final XLogger log;

    private final ScheduledExecutorService ses;

    private final String sql_host;

    private final int sql_port;

    private final String sql_user;

    private final String sql_pass;

    private final String sql_schema;

    private final String realm;

    private Connection connection;

    private PreparedStatement game_count_stmt;

    private PreparedStatement map_lookup_stmt;

    private PreparedStatement map_insert_stmt;

    private PreparedStatement last_insert_id_stmt;

    private PreparedStatement game_insert_stmt;

    public WC3Gnome(BattleNetChatClientFactory fact, String host, int port, String user, String pass, boolean pvpgn, String channel, String sql_host, int sql_port, String sql_user, String sql_pass, String sql_schema, String realm) throws SQLException {
        super(fact, host, port, user, pass, pvpgn, channel);
        this.log = XLoggerFactory.getXLogger(WC3Gnome.class.getCanonicalName() + " (" + user + "@" + host + ":" + port + ")");
        this.ses = Executors.newSingleThreadScheduledExecutor();
        this.sql_host = sql_host;
        this.sql_port = sql_port;
        this.sql_user = sql_user;
        this.sql_pass = sql_pass;
        this.sql_schema = sql_schema;
        this.realm = realm.toLowerCase();
        ses.execute(new DatabaseConnectTask());
    }

    private class DatabaseConnectTask implements Runnable {

        @Override
        public void run() {
            String sql_conn_string = String.format("jdbc:mysql://%s:%d/%s?useUnicode=yes&characterEncoding=UTF-8", sql_host, sql_port, sql_schema);
            log.debug(String.format("Using SQL connection string %s with user %s and a password of length %d.", sql_conn_string, sql_user, sql_pass.length()));
            try {
                connection = DriverManager.getConnection(sql_conn_string, sql_user, sql_pass);
                connection.setAutoCommit(false);
                game_count_stmt = connection.prepareStatement("SELECT COUNT(*) FROM games WHERE name=?");
                map_lookup_stmt = connection.prepareStatement("SELECT id, sha1 FROM maps WHERE file=?");
                map_insert_stmt = connection.prepareStatement("INSERT INTO maps (file, sha1) VALUES(?, ?)");
                last_insert_id_stmt = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                game_insert_stmt = connection.prepareStatement("INSERT INTO games (name, map_id, host, realm, ip, port) VALUES(?, ?, ?, ?, ?, ?)");
            } catch (SQLException e) {
                log.warn(String.format("SQL connection failed.  Waiting %d seconds until next attempt.", SQL_FAIL_DELAY_SEC), e);
                ses.schedule(this, SQL_FAIL_DELAY_SEC, TimeUnit.SECONDS);
                return;
            }
            log.trace(String.format("SQL connection succeeded.  Executing game list update task."));
            ses.schedule(new GameListUpdateTask(), BNET_SUCCESS_DELAY_SEC, TimeUnit.SECONDS);
        }
    }

    private class GameListUpdateTask implements Runnable {

        @Override
        public void run() {
            getGameList().addListener(new FutureListener<GetAdvListExServer>() {

                @Override
                public void onCancellation(Throwable cause) {
                    log.warn(String.format("Game list update failed.  Waiting %d seconds until next attempt.", BNET_FAIL_DELAY_SEC));
                    ses.schedule(GameListUpdateTask.this, BNET_FAIL_DELAY_SEC, TimeUnit.SECONDS);
                }

                @Override
                public void onCompletion(GetAdvListExServer result) {
                    log.debug(String.format("Game list update successful.  Retrieved %d games.", result.gameList.size()));
                    try {
                        for (GameInfo gi : result.gameList) processGameInfo(gi);
                    } catch (SQLException e) {
                        log.warn(String.format("Unexpected SQLException while processing game info.  " + "Shutting down SQL connection and attempting to re-establish in %d seconds.", SQL_FAIL_DELAY_SEC), e);
                        try {
                            connection.close();
                        } catch (SQLException e1) {
                            log.warn("Unexpected SQLException while closing database.", e1);
                        }
                        ses.schedule(new DatabaseConnectTask(), SQL_FAIL_DELAY_SEC, TimeUnit.SECONDS);
                        return;
                    }
                    ses.schedule(GameListUpdateTask.this, BNET_SUCCESS_DELAY_SEC, TimeUnit.SECONDS);
                }
            });
        }
    }

    private void processGameInfo(GameInfo gi) throws SQLException {
        String map_file_name = FilenameUtils.getName(gi.ssi.mapPath).toLowerCase();
        String map_sha1 = (gi.ssi.mapSHA1 == null) ? "(null)" : String.valueOf(Hex.encodeHex(gi.ssi.mapSHA1));
        log.trace(String.format("Found game %s : %s : %s\n", gi.gameName, map_file_name, map_sha1));
        if (gi.ssi.mapSHA1 == null) {
            log.debug(String.format("SHA1 not present in game info.  Aborting database update."));
            return;
        }
        game_count_stmt.setString(1, gi.gameName);
        ResultSet gamelist_rs = game_count_stmt.executeQuery();
        if (!gamelist_rs.next()) {
            log.error("Unable to get count of matching game names.  Aborting database update.");
            gamelist_rs.close();
            connection.rollback();
            return;
        }
        int count = gamelist_rs.getInt(1);
        gamelist_rs.close();
        if (count != 0) {
            connection.rollback();
            return;
        }
        map_lookup_stmt.setString(1, map_file_name);
        ResultSet maps_rs = map_lookup_stmt.executeQuery();
        int map_id;
        if (maps_rs.next()) {
            String stored_sha1 = maps_rs.getString("sha1");
            if (!map_sha1.equalsIgnoreCase(stored_sha1)) {
                log.debug(String.format("Map SHA1 mismatch - got %s, expected %s.  Aborting database update.", map_sha1, stored_sha1));
                maps_rs.close();
                connection.rollback();
                return;
            }
            map_id = maps_rs.getInt("id");
        } else {
            map_insert_stmt.setString(1, map_file_name);
            map_insert_stmt.setString(2, map_sha1);
            map_insert_stmt.executeUpdate();
            ResultSet last_id_rs = last_insert_id_stmt.executeQuery();
            if (!last_id_rs.next()) {
                log.error("Unable to retrieve last insert ID after map insertion.  Aborting database update.");
                last_id_rs.close();
                maps_rs.close();
                connection.rollback();
                return;
            }
            map_id = last_id_rs.getInt(1);
            last_id_rs.close();
        }
        maps_rs.close();
        game_insert_stmt.setString(1, gi.gameName);
        game_insert_stmt.setInt(2, map_id);
        game_insert_stmt.setString(3, gi.ssi.hostName);
        game_insert_stmt.setString(4, realm);
        game_insert_stmt.setString(5, gi.hostAddress.getAddress().getHostAddress());
        game_insert_stmt.setInt(6, gi.hostAddress.getPort());
        game_insert_stmt.executeUpdate();
        connection.commit();
        log.trace("Database update succeeded.");
    }
}
