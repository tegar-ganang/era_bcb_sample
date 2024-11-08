package org.wfp.vam.intermap.kernel.map;

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import org.jdom.*;
import org.wfp.vam.intermap.http.ConcurrentHTTPTransactionHandler;
import org.wfp.vam.intermap.http.cache.*;
import org.wfp.vam.intermap.kernel.*;
import org.wfp.vam.intermap.kernel.map.mapServices.*;
import org.wfp.vam.intermap.kernel.map.mapServices.arcims.*;
import org.wfp.vam.intermap.kernel.map.images.ImageMerger;

public class MapMerger {

    private static HttpGetFileCache cache;

    private static int dpi = 96;

    private BoundingBox bBox = new BoundingBox();

    private Hashtable htServices = new Hashtable();

    private Hashtable htTransparency = new Hashtable();

    private Vector vTransparency = new Vector();

    private Vector vRank = new Vector();

    private int nextId = 1;

    private Vector vImageUrls = new Vector();

    private int activeServiceId;

    private String imageName = null;

    private String imagePath = null;

    private float degScale;

    private float distScale;

    private Hashtable htErrors = new Hashtable();

    private Hashtable htExpanded = new Hashtable();

    private Hashtable htShow = new Hashtable();

    private boolean reaspectWms = true;

    public static void setCache(HttpGetFileCache cache) {
        MapMerger.cache = cache;
    }

    public static void setDpi(int dpi) {
        MapMerger.dpi = dpi;
    }

    public void reaspectWms(boolean reaspect) {
        this.reaspectWms = reaspect;
    }

    ;

    /** Sets the Map BoundingBox */
    public void setBoundingBox(BoundingBox bb) {
        bBox = bb;
    }

    /** Returns the Map BoudingBox */
    public BoundingBox getBoundingBox() {
        return bBox;
    }

    /** Sets the active layer inside a given servce */
    public void setActiveLayer(int service, int layer) throws Exception {
        if (!htServices.containsKey(new Integer(service))) throw new Exception("");
        activeServiceId = service;
        getService(service).setActiveLayer(layer);
    }

    /** Returns the active service id */
    public int getActiveServiceId() {
        return activeServiceId;
    }

    /** Returns the active Layer id */
    public int getActiveLayerId() {
        return getService(getActiveServiceId()).getActiveLayer();
    }

    /** Adds a service on the top */
    public void addService(MapService service) {
        if (htServices.size() == 0) activeServiceId = nextId;
        Integer id = new Integer(nextId++);
        htServices.put(id, service);
        htTransparency.put(id, new Float(1.0));
        htExpanded.put(id, new Boolean(true));
        htShow.put(id, new Boolean(true));
        vRank.add(0, id);
    }

    /** Removes the service with the given id */
    public void delService(int id) throws Exception {
        Integer t = new Integer(id);
        htServices.remove(t);
        vRank.remove(t);
        htTransparency.remove(t);
        htExpanded.remove(t);
        if (activeServiceId == id) if (vRank.size() > 0) {
            activeServiceId = ((Integer) vRank.get(0)).intValue();
            getService(activeServiceId).setActiveLayer(1);
        } else {
            activeServiceId = -1;
        }
    }

    /** Moves a service up */
    public void moveServiceUp(int id) {
        Integer t = new Integer(id);
        int pos = vRank.indexOf(t);
        if (pos > 0) {
            vRank.remove(t);
            vRank.add(pos - 1, t);
        }
    }

    /** Moves a service down */
    public void moveServiceDown(int id) {
        Integer t = new Integer(id);
        int pos = vRank.indexOf(t);
        if (pos < vRank.size() - 1) {
            vRank.remove(t);
            vRank.add(pos + 1, t);
        }
    }

    public void setServicesOrder(int[] order) {
        vRank.removeAllElements();
        for (int i = 0; i < order.length; i++) vRank.add(new Integer(order[i]));
    }

    public Element toElementSimple() {
        Element elServices = new Element("services");
        for (int i = 0; i < vRank.size(); i++) {
            MapService s = (MapService) htServices.get(vRank.get(i));
            elServices.addContent(new Element("layer").setAttribute("id", vRank.get(i) + "").setAttribute("title", "" + s.getTitle()).setAttribute("type", "" + s.getType()));
        }
        return elServices;
    }

    /** Converts this object to a JDOM Element */
    public Element toElement() {
        Element elServices = new Element("services");
        for (int i = 0; i < vRank.size(); i++) {
            MapService s = (MapService) htServices.get(vRank.get(i));
            elServices.addContent(s.toElement().setAttribute("id", vRank.get(i) + ""));
        }
        DecimalFormat df = new DecimalFormat("0.0000000000");
        elServices.addContent(new Element("activeLayer").setAttribute("service", "" + activeServiceId).setAttribute("layer", "" + getActiveLayerId())).addContent(new Element("degScale").setText(df.format(degScale))).addContent(new Element("distScale").setText(df.format(distScale) + "")).addContent(bBox.toElement()).addContent(getTransparency()).addContent(getExpanded()).addContent(getErrors());
        return elServices;
    }

    /** Returns an Element containing the transparency value for each service */
    public Element getTransparency() {
        Element elTransparency = new Element("transparency");
        for (Enumeration e = htServices.keys(); e.hasMoreElements(); ) {
            Integer serviceId = (Integer) e.nextElement();
            int f = (int) (((Float) htTransparency.get(serviceId)).floatValue() * 100);
            elTransparency.addContent(new Element("service").setAttribute("id", serviceId.toString()).setAttribute("transparency", f + "").setAttribute("name", ((MapService) htServices.get(serviceId)).getName()));
        }
        return elTransparency;
    }

    /** Returns the transparency of a given layer */
    public float getLayerTransparency(int id) {
        Integer serviceId = new Integer(id);
        return (int) (((Float) htTransparency.get(serviceId)).floatValue() * 100);
    }

    /** Sets the transparency value for a given service */
    public void setTransparency(int id, float transparency) throws Exception {
        if (htServices.get(new Integer(id)) == null) throw new Exception("Illegal service id");
        htTransparency.put(new Integer(id), new Float(transparency));
    }

    /** Expands a service in the layer frame (used for ArcIMS services only) */
    public void expandService(int id) {
        htExpanded.put(new Integer(id), new Boolean(true));
    }

    /** Collapses a service in the layer frame (used for ArcIMS services only) */
    public void collapseService(int id) {
        htExpanded.put(new Integer(id), new Boolean(false));
    }

    /** Expands a service in the layer frame (used for ArcIMS services only) */
    public void showService(int id) {
        htShow.put(new Integer(id), new Boolean(true));
    }

    /** Collapses a service in the layer frame (used for ArcIMS services only) */
    public void hideService(int id) {
        htShow.put(new Integer(id), new Boolean(false));
    }

    public boolean toggleVisibility(int id) {
        Boolean bVisibility = (Boolean) htShow.get(new Integer(id));
        boolean newVisibility = !bVisibility.booleanValue();
        htShow.put(new Integer(id), new Boolean(newVisibility));
        return newVisibility;
    }

    /** Get error messages from the remote servers */
    public Element getErrors() {
        Element errors = new Element("errors");
        for (Enumeration e = htErrors.keys(); e.hasMoreElements(); ) {
            Integer id = (Integer) e.nextElement();
            errors.addContent(new Element("layer").setAttribute("id", id.toString()).setAttribute("message", (String) htErrors.get(id)));
        }
        return errors;
    }

    /** Returns number of map services contained */
    public int size() {
        return htServices.size();
    }

    /** Returns an Enumeration containing all the services */
    public Enumeration getServices() {
        return htServices.elements();
    }

    /** Returns the MapService element with the given id*/
    public MapService getService(int id) {
        return (MapService) htServices.get(new Integer(id));
    }

    /** Returns an element containing informations about the expanded or collapsed
	 *  services
	 */
    private Element getExpanded() {
        Element expanded = new Element("expandedServices");
        for (Enumeration e = htExpanded.keys(); e.hasMoreElements(); ) {
            Integer serviceId = (Integer) e.nextElement();
            boolean ex = ((Boolean) htExpanded.get(serviceId)).booleanValue();
            expanded.addContent(new Element("service").setAttribute("id", serviceId.toString()).setAttribute("expanded", ex ? "true" : "false"));
        }
        return expanded;
    }

    private void buildWmsRequests(int width, int height) {
        if (reaspectWms) bBox = reaspect(bBox, width, height);
        MapService prevService = (MapService) htServices.get(vRank.get(0));
        if (vRank.size() == 1) {
            try {
                vImageUrls.add(prevService.getImageUrl(bBox, width, height));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        MapService service;
        Vector imageNames = new Vector();
        String serverURL = null;
        int i = 0;
        boolean flag = false;
        while (!flag) {
            prevService = (MapService) htServices.get(vRank.get(i));
            if (htShow.get(vRank.get(i)).equals(new Boolean(true))) {
                imageNames.add(prevService.getName());
                flag = true;
            }
            i++;
        }
        while (i < vRank.size()) {
            System.out.println(i + " - imageNames = " + imageNames);
            service = (MapService) htServices.get(vRank.get(i));
            serverURL = service.getServerURL();
            if (htShow.get(vRank.get(i)).equals(new Boolean(true))) {
                if (serverURL.equals(prevService.getServerURL())) {
                    vImageUrls.add(null);
                } else {
                    vImageUrls.add(prevService.getGroupImageUrl(bBox, width, height, imageNames));
                    imageNames.clear();
                }
            } else vImageUrls.add(null);
            imageNames.add(service.getName());
            if ((i == vRank.size() - 1) && (prevService.getServerURL().equals(serverURL))) vImageUrls.add(service.getGroupImageUrl(bBox, width, height, imageNames));
            prevService = service;
            i++;
        }
        System.out.println("- imageNames = " + imageNames);
        System.out.println("- vImageUrls = " + vImageUrls);
        i++;
    }

    private void sendGetImageRequests(int width, int height) {
        vImageUrls.clear();
        {
            if (reaspectWms) bBox = reaspect(bBox, width, height);
            for (int i = 0; i < vRank.size(); i++) {
                MapService ms = (MapService) htServices.get(vRank.get(i));
                if (htShow.get(vRank.get(i)).equals(new Boolean(true))) {
                    try {
                        vImageUrls.add(ms.getImageUrl(bBox, width, height));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else vImageUrls.add(null);
            }
        }
        degScale = Math.abs(getBoundingBox().getEast() - getBoundingBox().getWest()) / width;
        distScale = (long) (423307109.727 * degScale / 96.0 * dpi);
    }

    public String merge(int width, int height) throws Exception {
        htErrors.clear();
        sendGetImageRequests(width, height);
        Vector files = new Vector();
        ConcurrentHTTPTransactionHandler c = new ConcurrentHTTPTransactionHandler();
        c.setCache(cache);
        c.checkIfModified(false);
        for (int i = 0; i < vImageUrls.size(); i++) {
            if ((String) vImageUrls.get(i) != null) {
                c.register((String) vImageUrls.get(i));
            } else {
            }
        }
        c.doTransactions();
        vTransparency = new Vector();
        for (int i = 0; i < vImageUrls.size(); i++) {
            if (vImageUrls.get(i) != null) {
                String path = c.getResponseFilePath((String) vImageUrls.get(i));
                if (path != null) {
                    String contentType = c.getHeaderValue((String) vImageUrls.get(i), "content-type");
                    if (contentType.startsWith("image")) {
                        files.add(path);
                        vTransparency.add(htTransparency.get(vRank.get(i)));
                    }
                }
            }
        }
        if (files.size() > 1) {
            File output = TempFiles.getFile();
            String path = output.getPath();
            ImageMerger.mergeAndSave(files, vTransparency, path, ImageMerger.GIF);
            imageName = output.getName();
            imagePath = output.getPath();
            return (imageName);
        } else if (files.size() == 1) {
            File f = new File((String) files.get(0));
            File out = TempFiles.getFile();
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(out));
            byte buf[] = new byte[1024];
            for (int nRead; (nRead = is.read(buf, 0, 1024)) > 0; os.write(buf, 0, nRead)) ;
            os.flush();
            os.close();
            is.close();
            imageName = out.getName();
            return imageName;
        } else return "";
    }

    public String getImageName() {
        return imageName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getDegScale() {
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(degScale);
    }

    public String getDistScale() {
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(distScale);
    }

    private BoundingBox reaspect(BoundingBox bb, int x, int y) {
        float north = bb.getNorth();
        float south = bb.getSouth();
        float east = bb.getEast();
        float west = bb.getWest();
        float a = Math.abs(east - west);
        float b = Math.abs(north - south);
        if ((x / a) > (y / b)) {
            float d = b * x / y - a;
            west -= d / 2;
            east += d / 2;
        }
        if ((y / b) > (x / a)) {
            float d = a * y / x - b;
            south -= d / 2;
            north += d / 2;
        }
        if ((Math.abs(east - west)) > 360) {
            east = 180;
            west = -180;
            float d = (float) y / x * 180;
            north = d;
            south = -d;
        }
        if ((Math.abs(north - south)) > 360) {
            north = 180;
            south = -180;
            float d = (float) x / y * 180;
            east = d;
            west = -d;
        }
        return new BoundingBox(north, south, east, west);
    }

    private class GetImageUrlThread extends Thread {

        private MapService service;

        private BoundingBox bb;

        private int width, height;

        private String url;

        private boolean serviceError = false;

        private Element error;

        public void run() {
            sendRequest();
        }

        private void sendRequest() {
            try {
                url = service.getImageUrl(bb, width, height);
            } catch (ServiceException e) {
                serviceError = true;
            } catch (Exception e) {
                serviceError = true;
                url = null;
            }
        }

        public MapService getService() {
            return service;
        }

        public void setParameters(MapService service, BoundingBox bb, int width, int height) {
            this.service = service;
            this.bb = bb;
            this.width = width;
            this.height = height;
        }

        public String getUrl() throws ServiceException {
            if (serviceError) {
                throw new ServiceException();
            }
            return url;
        }

        public Element getResponse() {
            return service.getLastResponse();
        }
    }

    private class HttpThread extends Thread {

        private static final int BUF_LEN = 1024;

        private String stUrl;

        private String path;

        private HttpClient c;

        public void run() {
            connect();
        }

        public void setParameters(String url) {
            stUrl = url;
        }

        public String getPath() {
            return path;
        }

        public HttpClient getHttpClient() {
            return c;
        }

        private void connect() {
            BufferedInputStream is = null;
            BufferedOutputStream os = null;
            try {
                c = new HttpClient(stUrl);
                File tf = TempFiles.getFile();
                c.getFile(tf);
                path = tf.getPath();
            } catch (Exception e) {
                path = null;
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                }
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
