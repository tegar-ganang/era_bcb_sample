package eu.irreality.age.filemanagement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;
import eu.irreality.age.InputOutputClient;
import eu.irreality.age.World;
import eu.irreality.age.i18n.UIMessages;

public class WorldLoader {

    public static World loadWorldFromPath(String moduledir, Vector gameLog, InputOutputClient io, Object mundoSemaphore) {
        World theWorld = null;
        File inputAsFile = new File(moduledir);
        if (inputAsFile.isFile()) {
            System.out.println("Attempting world location: " + inputAsFile);
            try {
                theWorld = new World(moduledir, io, false);
                System.out.println("World generated.\n");
                if (mundoSemaphore != null) {
                    synchronized (mundoSemaphore) {
                        mundoSemaphore.notifyAll();
                    }
                }
                gameLog.addElement(inputAsFile.getAbsolutePath());
            } catch (java.io.IOException ioe) {
                io.write(UIMessages.getInstance().getMessage("load.world.cannot.read.world") + " " + inputAsFile + "\n");
                ioe.printStackTrace();
                return null;
            }
        } else {
            try {
                System.out.println("Attempting world location: " + moduledir + "/world.xml");
                theWorld = new World(moduledir + "/world.xml", io, false);
                System.out.println("World generated.\n");
                if (mundoSemaphore != null) {
                    synchronized (mundoSemaphore) {
                        mundoSemaphore.notifyAll();
                    }
                }
                gameLog.addElement(moduledir + "/world.xml");
            } catch (java.io.IOException e) {
                io.write(UIMessages.getInstance().getMessage("load.world.cannot.read.world.ondir") + " " + moduledir + "\n");
                System.out.println(e);
            }
        }
        if (theWorld == null) {
            io.write(UIMessages.getInstance().getMessage("load.world.invalid.dir", "$dir", moduledir));
            return null;
        }
        return theWorld;
    }

    /**
	 * If the given pathname or URL points to a compressed file that can contain a world (jar, zip, agz); then this method returns
	 * the pathname that points inside the file to recover world.xml.
	 * If the given pathname does not point to a compressed file, then this method returns the same string that was passed as a parameter.
	 * @param pathnameOrUrl
	 * @return
	 * @throws IOException 
	 */
    public static String goIntoFileIfCompressed(String pathnameOrUrl) {
        if (!pathnameOrUrl.endsWith(".jar") && !pathnameOrUrl.endsWith(".zip") && !pathnameOrUrl.endsWith(".agz")) return pathnameOrUrl;
        if (pathnameOrUrl.startsWith("zip:") || pathnameOrUrl.startsWith("jar:")) return pathnameOrUrl; else {
            URL url = URLUtils.stringToURL(pathnameOrUrl);
            try {
                url = new URL("jar", "", url + "!/world.xml");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return url.toString();
        }
    }

    public static World loadWorld(String pathnameOrUrl, Vector gameLog, InputOutputClient io, Object mundoSemaphore) {
        World theWorld = null;
        try {
            pathnameOrUrl = goIntoFileIfCompressed(pathnameOrUrl);
        } catch (SecurityException se) {
            ;
        }
        try {
            URL url = null;
            try {
                url = new URL(pathnameOrUrl);
            } catch (MalformedURLException mue) {
                url = WorldLoader.class.getClassLoader().getResource(pathnameOrUrl);
                if (url == null) throw mue;
            }
            if (url.toString().endsWith("/")) url = new URL(url.toString() + "world.xml");
            theWorld = new World(url, io, false);
            System.out.println("World generated.\n");
            if (mundoSemaphore != null) {
                synchronized (mundoSemaphore) {
                    mundoSemaphore.notifyAll();
                }
            }
            gameLog.addElement(theWorld.getResource("world.xml").toString());
            return theWorld;
        } catch (MalformedURLException e1) {
            return loadWorldFromPath(pathnameOrUrl, gameLog, io, mundoSemaphore);
        } catch (IOException ioe) {
            io.write(UIMessages.getInstance().getMessage("load.world.cannot.read.word") + " " + pathnameOrUrl + "\n");
            ioe.printStackTrace();
            return null;
        }
    }
}
