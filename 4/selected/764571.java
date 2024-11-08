package org.wct.plaf.www.basic;

import org.wct.*;
import org.apache.ecs.*;
import org.apache.ecs.html.*;
import java.io.*;
import javax.servlet.http.*;
import Acme.JPM.Encoders.*;
import java.net.URL;
import java.net.URLConnection;

/**
   This  class implements the Image ui for the basic WWW look and feel.
**/
public class BasicWWWImageUI extends BasicWWWComponentUI {

    /**
       Singletron instance
    **/
    private static BasicWWWImageUI ui = new BasicWWWImageUI();

    public static BasicWWWImageUI createUI(Object o) {
        return ui;
    }

    /**
       Creates a new BasicWWWImageUI
    **/
    public BasicWWWImageUI() {
    }

    /**
     * Returns the representation fo this component as a GenericElement (see Apache's ECS documentation)
* It is assumed that this representation is intended to be inserted in a h\
tml document
    */
    public GenericElement render(Component c) {
        Image img = (Image) c;
        if (!img.hasListeners()) {
            IMG im = new IMG(getComponentURL(c));
            return im;
        }
        String id = getFormID(c);
        Input im = new Input(Input.image, id, "");
        im.setSrc(getComponentURL(c));
        im.setOnClick("setTrigger('" + id + "'); return true;");
        return im;
    }

    /**
     * Request the component to service an aditional resource request.
     * Aditional resource requests are other resources that the browser might need
* (such as images) besides the component code embeded on the document.
*/
    public void handleResourceRequest(Component c, HttpServletRequest req, HttpServletResponse resp) {
        Image img = (Image) c;
        Object source = img.getSource();
        try {
            if (source instanceof java.net.URL) {
                URLConnection co = ((URL) source).openConnection();
                if (img.getContentType() == null) resp.setContentType(co.getContentType()); else resp.setContentType(img.getContentType());
                byte[] buf = new byte[4096];
                int count;
                InputStream in = co.getInputStream();
                OutputStream out = resp.getOutputStream();
                while ((count = in.read(buf, 0, 4096)) != -1) out.write(buf, 0, count);
                in.close();
            } else if (source instanceof java.awt.Image) {
                resp.setContentType("image/gif");
                GifEncoder enc = new GifEncoder(((java.awt.Image) source), resp.getOutputStream());
                enc.encode();
            }
        } catch (Exception e) {
            try {
                resp.setContentType("text/plain");
                PrintWriter pr = new PrintWriter(new OutputStreamWriter(resp.getOutputStream()));
                e.printStackTrace(pr);
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Process an event on the specified component.
     * @param comp the event's target component.
     * @param var the name of the form variable
     * @param val the value of the variable
     */
    public void processEvent(Component c, String var, String[] val) {
        int x = Integer.valueOf(val[0].substring(0, val[0].indexOf(','))).intValue();
        int y = Integer.valueOf(val[0].substring(val[0].indexOf(',') + 1, val[0].length())).intValue();
        ((Image) c).clicked(x, y);
    }
}
