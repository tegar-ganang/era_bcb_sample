package com.huishi.security.camera.huishi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.apache.http.HttpResponse;
import org.osgi.framework.Bundle;
import com.tenline.pinecone.platform.monitor.ProtocolHelper;
import com.tenline.pinecone.platform.monitor.httpcomponents.AbstractProtocolResponser;

/**
 * @author Bill
 *
 */
public class HuishiProtocolResponser extends AbstractProtocolResponser {

    /**
	 * @param bundle
	 */
    public HuishiProtocolResponser(Bundle bundle) {
        super(bundle);
    }

    @Override
    public void completed(HttpResponse message) {
        super.completed(message);
        if (message.getEntity().getContentType().getValue().equals("image/jpeg")) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(ImageIO.read(message.getEntity().getContent()), "jpeg", output);
                TreeMap<String, byte[]> map = new TreeMap<String, byte[]>();
                map.put(bundle.getHeaders().get("Video-Stream").toString(), output.toByteArray());
                publisher.addToReadQueue(ProtocolHelper.unmarshel(map));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
