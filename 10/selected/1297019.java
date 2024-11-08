package jshm.dataupdaters;

import static jshm.hibernate.HibernateUtil.openSession;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jshm.Difficulty;
import jshm.Game;
import jshm.GameTitle;
import jshm.Instrument;
import jshm.SongOrder;
import jshm.Tiers;
import jshm.wt.*;
import jshm.xml.GhSongInfoFetcher;
import jshm.xml.WtSongDataFetcher;
import jshm.sh.scraper.WtSongScraper;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.netbeans.spi.wizard.ResultProgressHandle;

public class WtSongUpdater {

    static final Logger LOG = Logger.getLogger(WtSongUpdater.class.getName());

    public static void updateViaXml(final GameTitle game) throws Exception {
        updateViaXml(null, game);
    }

    public static void updateViaXml(final ResultProgressHandle progress, final GameTitle game) throws Exception {
        if (!(game instanceof WtGameTitle)) throw new IllegalArgumentException("game must be a WtGameTitle");
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            if (null != progress) progress.setBusy("Downloading song data for " + game);
            WtSongDataFetcher fetcher = new WtSongDataFetcher();
            fetcher.fetch((WtGameTitle) game);
            LOG.finer("xml updated at " + fetcher.updated);
            Map<WtGame, Tiers> tiers = fetcher.tierMap;
            LOG.finer("xml had " + tiers.size() + " tiers for " + game);
            if (null != progress) progress.setBusy("Updating tiers");
            for (WtGame key : tiers.keySet()) {
                Tiers.setTiers(key, tiers.get(key));
                key.mapTiers(tiers.get(key));
            }
            Tiers.write();
            List<WtSong> songs = fetcher.songs;
            LOG.finer("xml had " + songs.size() + " songs for " + game);
            int i = 0, total = songs.size();
            for (WtSong song : songs) {
                if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                WtSong result = (WtSong) session.createQuery("FROM WtSong WHERE scoreHeroId=:shid AND gameTitle=:gameTtl").setInteger("shid", song.getScoreHeroId()).setString("gameTtl", song.getGameTitle().title).uniqueResult();
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
                order.setSong(WtSong.getByScoreHeroId(session, (WtGameTitle) game, order.getSong().getScoreHeroId()));
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
            LOG.throwing("WtSongUpdater", "updateViaXml", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
    }

    public static void updateViaScraping(final GameTitle game) throws Exception {
        updateViaScraping(null, game);
    }

    public static void updateViaScraping(final ResultProgressHandle progress, final GameTitle game) throws Exception {
        if (!(game instanceof WtGameTitle)) throw new IllegalArgumentException("game must be a WtGameTitle");
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            for (Game g : Game.getByTitle(game)) {
                if (null != progress) progress.setBusy("Downloading song list for " + g);
                List<WtSong> songs = WtSongScraper.scrape((WtGame) g);
                LOG.finer("scraped " + songs.size() + " songs for " + g);
                assert null != WtSongScraper.lastScrapedTiers;
                LOG.finer("remapping " + WtSongScraper.lastScrapedTiers.size() + " tiers");
                Tiers tiers = new Tiers(WtSongScraper.lastScrapedTiers);
                Tiers.setTiers(g, tiers);
                ((WtGame) g).mapTiers(tiers);
                int i = 0, total = songs.size();
                for (WtSong song : songs) {
                    if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                    WtSong result = (WtSong) session.createQuery("FROM WtSong WHERE scoreHeroId=:shid AND gameTitle=:gameTtl").setInteger("shid", song.getScoreHeroId()).setString("gameTtl", song.getGameTitle().title).uniqueResult();
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
                    i++;
                }
                if (((WtGameTitle) g.title).supportsExpertPlus) {
                    if (null != progress) progress.setBusy("Checking Expert+ song status for " + g);
                    songs = WtSongScraper.scrape((WtGame) g, Instrument.Group.DRUMS, Difficulty.EXPERT_PLUS);
                    LOG.finer("scraped " + songs.size() + " songs for expert+ check for " + g);
                    i = 0;
                    total = songs.size();
                    for (WtSong song : songs) {
                        if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                        WtSong result = (WtSong) session.createQuery("FROM WtSong WHERE scoreHeroId=:shid AND gameTitle=:gameTtl").setInteger("shid", song.getScoreHeroId()).setString("gameTtl", song.getGameTitle().title).uniqueResult();
                        if (null == result) {
                            LOG.warning("Didn't find song in DB during Expert+ check: " + song);
                        } else {
                            LOG.finest("found song: " + result);
                            result.setExpertPlusSupported(true);
                            LOG.info("Updating song to supprot Expert+: " + result);
                            session.update(result);
                        }
                        if (i % 64 == 0) {
                            session.flush();
                            session.clear();
                        }
                        i++;
                    }
                }
            }
            Tiers.write();
            LOG.info("Deleting old song orders");
            int deletedOrderCount = session.createQuery("delete SongOrder where gameTitle=:gameTitle").setString("gameTitle", game.toString()).executeUpdate();
            LOG.finer("deleted " + deletedOrderCount + " old song orders");
            for (Game g : Game.getByTitle(game)) {
                if (null != progress) progress.setBusy("Downloading song order lists for " + g);
                List<SongOrder> orders = WtSongScraper.scrapeOrders(progress, (WtGame) g);
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
            LOG.throwing("WtSongUpdater", "updateViaScraping", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
    }

    public static void updateSongInfo(WtGameTitle ttl) throws Exception {
        updateSongInfo(null, ttl);
    }

    @SuppressWarnings("unchecked")
    public static void updateSongInfo(final ResultProgressHandle progress, WtGameTitle ttl) throws Exception {
        if (null != progress) progress.setBusy("Downloading song meta data...");
        GhSongInfoFetcher fetcher = new GhSongInfoFetcher();
        fetcher.fetch(ttl);
        Session session = null;
        Transaction tx = null;
        try {
            session = openSession();
            tx = session.beginTransaction();
            int i = 0, total = fetcher.songMap.size();
            for (String key : fetcher.songMap.keySet()) {
                if (null != progress) progress.setProgress(String.format("Processing song %s of %s", i + 1, total), i, total);
                String upperTtl = key.toUpperCase();
                List<WtSong> result = (List<WtSong>) session.createQuery("FROM WtSong WHERE gameTitle=:gameTtl AND UPPER(title) LIKE :ttl").setString("gameTtl", ttl.title).setString("ttl", upperTtl).list();
                LOG.finer("result.size() == " + result.size());
                for (WtSong s : result) {
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
            LOG.throwing("WtSongUpdater", "updateSongInfo", e);
            throw e;
        } finally {
            if (null != session && session.isOpen()) session.close();
        }
        ttl.initDynamicTiers();
    }
}
