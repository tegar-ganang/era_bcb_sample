package net.jetrix.monitor.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import net.jetrix.agent.QueryAgent;
import net.jetrix.agent.QueryInfo;
import net.jetrix.agent.PlayerInfo;
import net.jetrix.monitor.NetworkUtils;
import net.jetrix.monitor.ServerInfo;
import net.jetrix.monitor.ServerStats;
import net.jetrix.monitor.PlayerStats;
import net.jetrix.monitor.dao.ServerInfoDao;
import net.jetrix.monitor.dao.ServerStatsDao;
import net.jetrix.monitor.dao.PlayerStatsDao;

/**
 * Job polling the tetrinet servers.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 822 $, $Date: 2010-02-22 10:16:11 -0500 (Mon, 22 Feb 2010) $
 */
public class ServerSurveyJob extends TransactionalQuartzJob {

    protected void executeTransactional(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ApplicationContext context = (ApplicationContext) jobExecutionContext.getMergedJobDataMap().get("applicationContext");
        ServerInfoDao serverInfoDao = (ServerInfoDao) context.getBean("serverInfoDao");
        List<ServerInfo> servers = serverInfoDao.getServers();
        log.info("Checking servers... ");
        long t0 = System.currentTimeMillis();
        List<Callable<ServerInfo>> workers = new ArrayList<Callable<ServerInfo>>();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (ServerInfo server : servers) {
            workers.add(new Worker(server));
        }
        try {
            List<Future<ServerInfo>> results = executor.invokeAll(workers, 45, TimeUnit.SECONDS);
            executor.shutdown();
            for (Future<ServerInfo> result : results) {
                try {
                    handleResult(context, result.get());
                } catch (ExecutionException e) {
                    log.log(Level.WARNING, "Error when checking the server", e);
                } catch (CancellationException e) {
                    log.log(Level.WARNING, "Server survey task cancelled", e);
                }
            }
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
        log.info(servers.size() + " servers checked in " + (System.currentTimeMillis() - t0) / 1000 + " seconds");
    }

    private void handleResult(ApplicationContext context, ServerInfo server) {
        ServerInfoDao serverInfoDao = (ServerInfoDao) context.getBean("serverInfoDao");
        ServerStatsDao serverStatsDao = (ServerStatsDao) context.getBean("serverStatsDao");
        PlayerStatsDao playerStatsDao = (PlayerStatsDao) context.getBean("playerStatsDao");
        if (!server.isOnline()) {
            server.getPlayers().clear();
        }
        serverInfoDao.save(server);
        ServerStats stats = server.getStats();
        stats.setServerId(server.getId());
        stats.setDate(server.getLastChecked());
        if (stats.getPlayerCount() > 0) {
            serverStatsDao.save(stats);
        }
        for (PlayerInfo player : server.getPlayers()) {
            PlayerStats playerStats = playerStatsDao.getStats(player.getNick());
            if (playerStats == null) {
                playerStats = new PlayerStats();
                playerStats.setName(player.getNick());
                playerStats.setFirstSeen(server.getLastOnline());
            }
            playerStats.setTeam(player.getTeam());
            playerStats.setLastSeen(server.getLastOnline());
            playerStats.setLastServer(server);
            if (player.isPlaying()) {
                playerStats.setLastPlayed(server.getLastOnline());
            }
            playerStatsDao.save(playerStats);
        }
    }

    private class Worker implements Callable<ServerInfo> {

        private ServerInfo server;

        public Worker(ServerInfo server) {
            this.server = server;
        }

        public ServerInfo call() throws Exception {
            QueryAgent agent = new QueryAgent();
            server.setLastChecked(new Date());
            server.setStats(new ServerStats());
            try {
                agent.connect(server.getHostname());
                QueryInfo info = agent.getInfo();
                server.getStats().update(info);
                server.setVersion(info.getVersion());
                server.setLastOnline(server.getLastChecked());
                if (server.getChannels() == null) {
                    server.setChannels(info.getChannels());
                } else {
                    server.getChannels().clear();
                    server.getChannels().addAll(info.getChannels());
                }
                server.setPlayers(info.getPlayers());
                if (server.getStats().getActivePlayerCount() > server.getMaxActivePlayerCount()) {
                    server.setMaxActivePlayerCount(server.getStats().getActivePlayerCount());
                    server.setMaxActivePlayerDate(server.getLastChecked());
                }
                if (server.getStats().getPlayerCount() > server.getMaxPlayerCount()) {
                    server.setMaxPlayerCount(server.getStats().getPlayerCount());
                    server.setMaxPlayerDate(server.getLastChecked());
                }
                if (server.getStats().getActivePlayerCount() > 0) {
                    server.setLastActive(server.getLastChecked());
                }
                if (server.getStats().getPlayerCount() > 0) {
                    server.setLastPopulated(server.getLastPopulated());
                }
                server.setSpectate(NetworkUtils.isPortOpen(server.getHostname(), 31458));
            } catch (Exception e) {
                log.fine("Unable to check the server " + server.getHostname() + " : " + e.getMessage());
            } finally {
                agent.disconnect();
            }
            return server;
        }
    }
}
