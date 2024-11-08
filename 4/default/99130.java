import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class ResourceMidlet extends javax.microedition.midlet.MIDlet {

    public void doit(String resourceName, boolean doPrint) {
        InputStream i;
        i = this.getClass().getResourceAsStream(resourceName);
        if (i == null) {
            System.out.println("Could not open " + resourceName);
        } else if (doPrint) {
            byte buf[] = new byte[100];
            int readsize;
            do {
                try {
                    readsize = i.read(buf, 0, buf.length);
                    if (readsize != -1) {
                        System.out.write(buf, 0, readsize);
                    }
                } catch (IOException e) {
                    System.out.println("Got IOException");
                    e.printStackTrace();
                    break;
                }
            } while (readsize != -1);
        } else {
            System.out.println("Successfully opened " + resourceName);
        }
    }

    public void startApp() {
        InputStream i;
        System.out.println("ResourceMidlet:");
        doit("embeddedResource", true);
        System.out.println("NOW FOR SOMETHING NOT POSSIBLE");
        try {
            doit("//etc/motd", true);
        } catch (Throwable t) {
            System.out.print("CAUGHT ");
            t.printStackTrace();
        }
        System.out.println("AND NOW FOR SOMETHING NOT ALLOWED");
        try {
            doit("ResourceMidlet.class", false);
        } catch (Throwable t) {
            System.out.print("CAUGHT ");
            t.printStackTrace();
        }
        System.out.println("ResourceMidlet exiting");
        notifyDestroyed();
    }

    public static void main(String ignore[]) {
        new ResourceMidlet().startApp();
    }

    public void destroyApp(boolean ignore) {
    }
}
