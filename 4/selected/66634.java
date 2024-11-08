package com.rbnb.sapi;

import com.rbnb.api.*;

public class PlugIn extends Client {

    public PlugIn() {
        super(1, null, 0);
    }

    public long BytesTransferred() {
        if (plugin != null) return plugin.bytesTransferred(); else return 0L;
    }

    void doOpen(Server server, String clientName, String userName, String password) throws Exception {
        plugin = server.createPlugIn(clientName);
        if (userName != null) {
            plugin.setUsername(new Username(userName, password));
        }
        plugin.start();
    }

    public final PlugInChannelMap Fetch(long blockTimeout, PlugInChannelMap picm) throws SAPIException {
        if (picm == null) picm = new PlugInChannelMap(); else picm.Clear();
        try {
            Object result = plugin.fetch(blockTimeout < 0 ? com.rbnb.api.Sink.FOREVER : blockTimeout);
            plugin.fillRequestOptions(picm.GetRequestOptions());
            picm.processPlugInRequest(result);
            return picm;
        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Queries the server to see if any requests have been made of this
	  *  PlugIn.  The channel names are placed in a newly created
	  *  <code>PlugInChannelMap</code> object.
	  * <p>Calls <code>Fetch(long,null)</code>.
	  * <p>
	  * @see #Fetch(long, PlugInChannelMap)
	  */
    public final PlugInChannelMap Fetch(long blockTimeout) throws SAPIException {
        return Fetch(blockTimeout, null);
    }

    public int Flush(PlugInChannelMap ch, boolean doStream) throws SAPIException {
        if (doStream) throw new IllegalArgumentException("Streaming not supported for PlugIns.");
        int toFlush = ch.getChannelsPut();
        try {
            Rmap response = ch.produceOutput(doStream);
            plugin.addChild(response);
            ch.clearData();
            ch.incrementNext();
        } catch (Exception e) {
            throw new SAPIException(e);
        }
        return toFlush;
    }

    public int Flush(PlugInChannelMap ch) throws SAPIException {
        return Flush(ch, false);
    }

    public void Register(ChannelMap cm) throws SAPIException {
        super.doRegister(cm);
    }

    public void SetRingBuffer(int cache, String mode, int archive) {
        throw new RuntimeException("PlugIns do not support archiving.");
    }

    com.rbnb.api.Client getClient() {
        return plugin;
    }

    final void clearData() {
        plugin = null;
    }

    private com.rbnb.api.PlugIn plugin;
}
