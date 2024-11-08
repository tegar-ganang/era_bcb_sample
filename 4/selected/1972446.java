package cz.krtinec.telka.provider;

import gnu.java.nio.charset.Windows1250;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;
import cz.krtinec.telka.CannotLoadProgrammeException;
import cz.krtinec.telka.IProgrammeProvider;
import cz.krtinec.telka.dto.Channel;
import cz.krtinec.telka.dto.Programme;

/**
 * Default implementation.
 * @author krtek
 *
 */
public class ProgrammeProvider implements IProgrammeProvider {

    private static final String CACHE_FILENAME = "program.cache";

    private static final String CACHE_TIMESTAMP = "program.timestamp";

    private static final String CLASS_NAME = ProgrammeProvider.class.getSimpleName();

    private ChannelCacheHolder holder = null;

    private Context context = null;

    private static final String URL = "http://xmltv.arcao.com/xml.php?gids[]=ct1.ceskatelevize.cz&" + "gids[]=ct2.ceskatelevize.cz&" + "gids[]=nova.nova.cz&" + "gids[]=prima.iprima.cz&" + "gids[]=cool.iprima.cz&" + "gids[]=ct24.ct24.cz&" + "gids[]=ct4sport.ct24.cz&" + "gids[]=ocko.idnes.cz&" + "gids[]=sport.nova.cz&" + "gids[]=cinema.nova.cz&" + "gids[]=mtv.nova.cz";

    public ProgrammeProvider(Context context) {
        this.context = context;
    }

    public void disable(Channel channel) {
        throw new NoSuchMethodError("Not yet implemented!");
    }

    public void enable(Channel channel) {
        throw new NoSuchMethodError("Not yet implemented!");
    }

    public Collection<Channel> getAllChannels(int reloadInterval) {
        if (this.holder == null) {
            this.holder = loadChannels(reloadInterval);
            for (Channel ch : holder.channels.keySet()) {
                for (Programme p : holder.channels.get(ch)) {
                    p.getState();
                }
            }
        }
        return holder.channels.keySet();
    }

    public Collection<Channel> getEnabledChannels(int reloadInterval) {
        return getAllChannels(reloadInterval);
    }

    public List<Programme> getProgrammes(Channel channel) {
        return holder.channels.get(channel);
    }

    public Integer nowPlaying(Channel channel) {
        List<Programme> programmes = holder.channels.get(channel);
        int i = 0;
        for (Programme p : programmes) {
            if (p.getState().isRunning()) {
                return i;
            }
            i++;
        }
        return i;
    }

    private ChannelCacheHolder loadChannels(int reloadInterval) {
        Log.i(CLASS_NAME, "ReloadInterval: " + reloadInterval);
        try {
            ObjectInputStream ois = new ObjectInputStream(context.openFileInput(CACHE_TIMESTAMP));
            long timestamp = ois.readLong();
            ois.close();
            long interval = System.currentTimeMillis() - timestamp;
            if (interval < reloadInterval) {
                Log.i(CLASS_NAME, "Last reloaded before " + interval + " [ms], going to reuse.");
                ois = new ObjectInputStream(context.openFileInput(CACHE_FILENAME));
                ChannelCacheHolder holder = (ChannelCacheHolder) ois.readObject();
                ois.close();
                Log.i(CLASS_NAME, "Programme loaded from cache.");
                return holder;
            } else {
                Log.i(CLASS_NAME, "Last reloaded before " + interval + " [ms], going to load again.");
            }
        } catch (IOException e) {
            Log.i(CLASS_NAME, "No stored programme found - going to load from net...");
        } catch (ClassNotFoundException e) {
            Log.i(CLASS_NAME, "ClassNotFoundException - going to load from net...");
        }
        try {
            ChannelCacheHolder holder = loadChannelsFromNetAndStore();
            return holder;
        } catch (Exception e) {
            Log.e(CLASS_NAME, "Exception: ", e);
            throw new CannotLoadProgrammeException("Cannot load TV programme", e);
        }
    }

    private ChannelCacheHolder loadChannelsFromNetAndStore() throws IOException, MalformedURLException, SAXException, FileNotFoundException {
        Map<Channel, List<Programme>> channels = loadChannelsFromNet();
        ChannelCacheHolder holder = new ChannelCacheHolder(channels, System.currentTimeMillis());
        long timestamp = System.currentTimeMillis();
        ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(CACHE_FILENAME, Context.MODE_PRIVATE));
        oos.writeObject(holder);
        oos.close();
        oos = new ObjectOutputStream(context.openFileOutput(CACHE_TIMESTAMP, Context.MODE_PRIVATE));
        oos.writeLong(timestamp);
        oos.close();
        Log.i(CLASS_NAME, "Programme stored to cache.");
        return holder;
    }

    private Map<Channel, List<Programme>> loadChannelsFromNet() throws IOException, MalformedURLException, SAXException {
        Log.i(CLASS_NAME, "Going to parse programmes");
        HttpURLConnection uc = (HttpURLConnection) new URL(URL).openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        InputStream is = uc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, new Windows1250()), 8096 * 4);
        ProgrammeHandler handler = new ProgrammeHandler();
        long start = System.currentTimeMillis();
        Xml.parse(br, handler);
        long stop = System.currentTimeMillis();
        Log.i(CLASS_NAME, "Parsed in " + (stop - start) + " millis");
        Map<Channel, List<Programme>> channels = handler.getChannels();
        return channels;
    }

    public void reload() {
        try {
            this.holder = loadChannelsFromNetAndStore();
        } catch (Exception e) {
            Log.e(CLASS_NAME, "Reload failed");
        }
    }
}

class ChannelCacheHolder implements Serializable {

    ChannelCacheHolder(Map<Channel, List<Programme>> channels, long timestamp) {
        this.channels = channels;
        this.timestamp = timestamp;
    }

    Map<Channel, List<Programme>> channels;

    long timestamp;
}
