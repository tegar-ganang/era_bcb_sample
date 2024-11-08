package jshm.dataupdaters;

import static jshm.hibernate.HibernateUtil.openSession;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.netbeans.spi.wizard.ResultProgressHandle;
import jshm.*;
import jshm.rb.*;
import jshm.sh.scraper.RbSongScraper;
import jshm.xml.RbSongDataFetcher;
import jshm.xml.RbSongInfoFetcher;

public class RbSongUpdater {

    static final Logger LOG = Logger.getLogger(RbSongUpdater.class.getName());

    public static void updateViaXml(final GameTitle game) throws Exception {
        updateViaXml(null, game);
    }

    public static void updateViaXml(final ResultProgressHandle progress, final GameTitle game) throws Exception {
        if (!(game instanceof RbGameTitle)) throw new IllegalArgumentException("game must be an RbGameTitle");
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            if (null != progress) progress.setBusy("Downloading song data for " + game);
            RbSongDataFetcher fetcher = new RbSongDataFetcher();
            fetcher.fetch((RbGameTitle) game);
            LOG.finer("xml updated at " + fetcher.updated);
            Map<RbGame, Tiers> tiers = fetcher.tierMap;
            LOG.finer("xml had " + tiers.size() + " tiers for " + game);
            if (null != progress) progress.setBusy("Updating tiers");
            for (RbGame key : tiers.keySet()) {
                Tiers.setTiers(key, tiers.get(key));
                key.mapTiers(tiers.get(key));
            }
            Tiers.write();
            List<RbSong> songs = fetcher.songs;
            LOG.finer("xml had " + songs.size() + " songs for " + game);
            int i = 0, total = songs.size();
            for (RbSong song : songs) {
                if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                RbSong result = (RbSong) session.createQuery("FROM RbSong WHERE scoreHeroId=:shid AND gameTitle=:gameTtl").setInteger("shid", song.getScoreHeroId()).setString("gameTtl", song.getGameTitle().title).uniqueResult();
                if (null == result) {
                    LOG.info("Inserting song: " + song);
                    session.save(song);
                } else {
                    LOG.finest("found song: " + result);
                    if (result.update(song, true)) {
                        LOG.info("Updating song to: " + result);
                        session.update(result);
                    } else {
                        LOG.finest("No changes to song: " + result);
                    }
                }
                if (i % 64 == 0) {
                    session.flush();
                    session.clear();
                }
                i++;
            }
            List<SongOrder> orders = fetcher.orders;
            LOG.finer("xml had " + orders.size() + " song orderings for " + game);
            LOG.info("Deleting old song orders");
            int deletedOrderCount = session.createQuery("delete SongOrder where gameTitle=:gameTitle").setString("gameTitle", game.toString()).executeUpdate();
            LOG.finer("deleted " + deletedOrderCount + " old song orders");
            i = 0;
            total = orders.size();
            for (SongOrder order : orders) {
                order.setSong(RbSong.getByScoreHeroId(session, order.getSong().getScoreHeroId()));
                LOG.info("Inserting song order: " + order);
                session.save(order);
                if (i % 64 == 0) {
                    session.flush();
                    session.clear();
                    if (null != progress) progress.setProgress("Processing song order lists...", i, total);
                }
                i++;
            }
            tx.commit();
        } catch (HibernateException e) {
            if (null != tx && tx.isActive()) tx.rollback();
            LOG.throwing("RbSongUpdater", "updateViaXml", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
    }

    public static void updateViaScraping(final GameTitle game) throws Exception {
        updateViaScraping(null, game);
    }

    public static void updateViaScraping(final ResultProgressHandle progress, final GameTitle game) throws Exception {
        if (!(game instanceof RbGameTitle)) throw new IllegalArgumentException("game must be an RbGameTitle");
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            for (Game g : Game.getByTitle(game)) {
                if (null != progress) progress.setBusy("Downloading song list for " + g);
                List<RbSong> songs = RbSongScraper.scrape((RbGame) g);
                LOG.finer("scraped " + songs.size() + " songs for " + g);
                assert null != RbSongScraper.lastScrapedTiers;
                LOG.finer("remapping " + RbSongScraper.lastScrapedTiers.size() + " tiers for " + g);
                Tiers tiers = new Tiers(RbSongScraper.lastScrapedTiers);
                Tiers.setTiers(g, tiers);
                ((RbGame) g).mapTiers(tiers);
                int i = 0, total = songs.size();
                for (RbSong song : songs) {
                    if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                    RbSong result = (RbSong) session.createQuery("FROM RbSong WHERE scoreHeroId=:shid AND gameTitle=:gameTtl").setInteger("shid", song.getScoreHeroId()).setString("gameTtl", song.getGameTitle().title).uniqueResult();
                    if (null == result) {
                        LOG.info("Inserting song: " + song);
                        session.save(song);
                    } else {
                        LOG.finest("found song: " + result);
                        if (result.update(song, false)) {
                            LOG.info("Updating song to: " + result);
                            session.update(result);
                        } else {
                            LOG.finest("No changes to song: " + result);
                        }
                    }
                    if (i % 64 == 0) {
                        session.flush();
                        session.clear();
                    }
                    i++;
                }
            }
            Tiers.write();
            LOG.info("Deleting old song orders");
            int deletedOrderCount = session.createQuery("delete SongOrder where gameTitle=:gameTitle").setString("gameTitle", game.toString()).executeUpdate();
            LOG.finer("deleted " + deletedOrderCount + " old song orders");
            for (Game g : Game.getByTitle(game)) {
                if (null != progress) progress.setBusy("Downloading song order lists for " + g);
                List<SongOrder> orders = RbSongScraper.scrapeOrders(progress, (RbGame) g);
                LOG.finer("scraped " + orders.size() + " song orderings for " + g);
                int i = 0, total = orders.size();
                for (SongOrder order : orders) {
                    LOG.info("Inserting song order: " + order);
                    session.save(order);
                    if (i % 64 == 0) {
                        session.flush();
                        session.clear();
                        if (null != progress) progress.setProgress("Processing song order lists...", i, total);
                    }
                    i++;
                }
            }
            tx.commit();
        } catch (HibernateException e) {
            if (null != tx && tx.isActive()) tx.rollback();
            LOG.throwing("RbSongUpdater", "updateViaScraping", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
    }

    public static void updateSongInfo() throws Exception {
        updateSongInfo(null);
    }

    @SuppressWarnings("unchecked")
    public static void updateSongInfo(final ResultProgressHandle progress) throws Exception {
        if (null != progress) progress.setBusy("Downloading song meta data...");
        RbSongInfoFetcher fetcher = new RbSongInfoFetcher();
        fetcher.fetch();
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            int i = 0, total = fetcher.songMap.size();
            for (String key : fetcher.songMap.keySet()) {
                if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                String upperTtl = key.toUpperCase();
                String ttlReplacement = upperTtl.replace("AND", "%").replace(" '", "%").replace('&', '%').replace('/', '%').replace('\'', '%').replace('!', '%').replace('?', '%').replace('è', '%').replace('Ü', '%');
                List<RbSong> result = (List<RbSong>) session.createQuery("FROM RbSong WHERE UPPER(title) LIKE :ttl AND gameTitle != :tbrbGt").setString("ttl", ttlReplacement).setString("tbrbGt", RbGameTitle.TBRB.title).list();
                if (!upperTtl.equals(ttlReplacement)) {
                    LOG.fine("Looking for {" + ttlReplacement + "}, given {" + upperTtl + "}, found " + result.size());
                }
                for (RbSong s : result) {
                    if (s.update(fetcher.songMap.get(key))) {
                        LOG.info("Updating song to: " + s);
                        session.update(s);
                    } else {
                        LOG.finer("No changes to song: " + s);
                    }
                }
                if (i % 64 == 0) {
                    session.flush();
                    session.clear();
                }
                i++;
            }
            tx.commit();
        } catch (HibernateException e) {
            if (null != tx && tx.isActive()) tx.rollback();
            LOG.throwing("RbSongUpdater", "updateSongInfo", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
        for (GameTitle g : GameTitle.getBySeries(GameSeries.ROCKBAND)) {
            g.initDynamicTiers();
        }
    }
}
