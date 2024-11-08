package com.beem.project.beem.smack.avatar;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * An AvatarRetriever which retrieve the avatar over HTTP.
 */
public class HttpAvatarRetriever implements AvatarRetriever {

    private URL mUrl;

    private String mUrlString;

    /**
	 * Create a HttpAvatarRetriever.
	 * 
	 * @param url
	 *            the url of the avatar to download.
	 */
    public HttpAvatarRetriever(final URL url) {
        mUrl = url;
    }

    /**
	 * Create a HttpAvatarRetriever.
	 * 
	 * @param url
	 *            the url of the avatar to download.
	 */
    public HttpAvatarRetriever(final String url) {
        mUrlString = url;
    }

    @Override
    public byte[] getAvatar() throws IOException {
        if (mUrl == null) mUrl = new URL(mUrlString);
        InputStream in = mUrl.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            byte[] data = new byte[1024];
            int nbread;
            while ((nbread = in.read(data)) != -1) {
                os.write(data, 0, nbread);
            }
        } finally {
            in.close();
            os.close();
        }
        return os.toByteArray();
    }
}
