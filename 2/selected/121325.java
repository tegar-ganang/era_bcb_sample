package a_servlet;

import jatcore.spacetime.GnssConstellation;
import jatcore.spacetime.GnssSv;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

/**
 *
 * @author quinput
 */
public class GpsSvServlet extends HttpServlet {

    AbsoluteDate ad = null;

    Logger logger = Logger.getLogger(GpsSvServlet.class.getName());

    String root = null;

    String root_resources = null;

    InputStream alm_stream = null;

    GnssConstellation gc = new GnssConstellation();

    private int counter = 1;

    int prn = 1;

    int get_called = 0;

    String ref_time_pos = null;

    String m_tag = "message";

    ServletContext context = null;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (root == null) {
            FileInputStream fis = null;
            try {
                root = getClass().getClassLoader().getResource("orekit-data").getPath();
                System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
                jatcore.orekit.ConfigOrekit.Setup_orekit_data();
                context = this.getServletContext();
                String path = context.getRealPath("/data");
                File yumafile = new File(path, "downloaded.alm");
                if (yumafile.exists() && yumafile.length() > 5000) {
                    fis = new FileInputStream(yumafile);
                    gc.add_yuma(fis);
                } else {
                    root_resources = getClass().getClassLoader().getResource("jatcore-data").getPath();
                    alm_stream = GpsSvServlet.class.getResourceAsStream("/jatcore-data/current.alm");
                    gc.add_yuma(alm_stream);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GpsSvServlet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
            }
        }
    }

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String res = "";
        String ref = "";
        String load_alm_response = "";
        String latest_gps_epoch = "";
        String task = request.getParameter("task");
        String message = "";
        String sv_xml_str = null;
        if (task != null) {
            if (task.equals("updatesv")) {
                String sv_request = request.getParameter("sv");
                String str_lat = request.getParameter("lat");
                String str_longi = request.getParameter("longi");
                String str_hoe = request.getParameter("hoe");
                get_called++;
                if (get_called == 1) {
                }
                if (sv_request != null) {
                    int sv_nr = Integer.decode(sv_request).intValue();
                    double lat_nr = Double.parseDouble(str_lat);
                    double longi_nr = Double.parseDouble(str_longi);
                    double hoe_nr = Double.parseDouble(str_hoe);
                    GnssSv gs = gc.getId(sv_nr);
                    if (gs == null) {
                        sv_xml_str = "<sv>" + "<svid>" + sv_nr + "</svid>" + "<data>" + sv_nr + " not found or not healty</data>" + "<color>rgb(240,240,240)</color>" + "</sv>";
                    } else {
                        try {
                            AbsoluteDate ad = new AbsoluteDate(new Date(), TimeScalesFactory.getUTC());
                            ref_time_pos = "Used Pos =  Lat:" + lat_nr + " Long:" + longi_nr + " Time in UTC:" + ad;
                            PVCoordinates pv_ecef = gs.getPV(ad);
                            System.out.println(pv_ecef);
                            GeodeticPoint satg = gs.getGeodeticPos(pv_ecef, ad);
                            OneAxisEllipsoid oae = new OneAxisEllipsoid(6378137.0, 1.0 / 298.257223563, FramesFactory.getITRF2005());
                            GeodeticPoint muc = new GeodeticPoint(Math.toRadians(lat_nr), Math.toRadians(longi_nr), hoe_nr);
                            Vector3D pos_u = oae.transform(muc);
                            Vector3D vel_u = new Vector3D(0, 0, 0);
                            double[] rrr = GnssSv.Range_RangeRate(pv_ecef, new PVCoordinates(pos_u, vel_u));
                            double[] cae = GnssSv.calcAzEl(pv_ecef.getPosition(), pos_u);
                            String s_prn = String.format("%2d", sv_nr);
                            String pos_delta_d = String.format("%13.7e", rrr[0]);
                            String range_rate_str = String.format("%9.3f", rrr[1]);
                            String elevation_str = String.format("%5.1f", Math.toDegrees(cae[1]));
                            String declination_str = String.format("%5.1f", Math.toDegrees(cae[0]));
                            String sat_lat_str = String.format("%5.1f", Math.toDegrees(satg.getLatitude()));
                            String sat_longi_str = String.format("%6.1f", Math.toDegrees(satg.getLongitude()));
                            String sv_data_str = s_prn + "  " + pos_delta_d + "  " + range_rate_str + "  " + elevation_str + " " + declination_str + "  " + sat_lat_str + "  " + sat_longi_str;
                            String sv_color_str = null;
                            double ele = Math.toDegrees(cae[1]);
                            if (ele > 10.0) {
                                double green = 255 - ele;
                                sv_color_str = "rgb(0," + (int) green + ",0)";
                            } else {
                                sv_color_str = "rgb(255,255,255)";
                            }
                            sv_xml_str = "<sv>" + "<svid>" + sv_nr + "</svid>" + "<data>" + sv_data_str + "</data>" + "<color>" + sv_color_str + "</color>" + "</sv>";
                        } catch (OrekitException ex) {
                            Logger.getLogger(GpsSvServlet.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    latest_gps_epoch = "<latest_epoch>" + gc.getRefEpoch() + "</latest_epoch>";
                }
            }
            if (task.equals("reset")) {
                counter = 1;
            }
            if (task.equals("load_alma")) {
                context = this.getServletContext();
                String path = context.getRealPath("/data");
                File downloadedfile = new File(path, "downloaded.alm");
                String alm_yuma_url = "http://www.navcen.uscg.gov/?pageName=currentAlmanac&format=yuma";
                URL url = new URL(alm_yuma_url);
                try {
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.connect();
                    InputStream input = url.openStream();
                    PrintWriter out = new PrintWriter(new FileWriter(downloadedfile));
                    Reader reader = new InputStreamReader(input);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String strLine = "";
                    int count = 0;
                    while (count < 1000) {
                        strLine = bufferedReader.readLine();
                        if (strLine != null && strLine != "") {
                            out.println(strLine);
                        } else {
                        }
                        count++;
                    }
                    out.close();
                } catch (Exception e) {
                    load_alm_response = "<alert>Error in try to get data from  " + url + " </alert>";
                }
                load_alm_response = "<alert>Get Data from  " + url + " </alert>";
                FileInputStream fis = new FileInputStream(downloadedfile);
                gc.add_yuma(fis);
            }
            if (task.equals("reset_alma")) {
                root_resources = getClass().getClassLoader().getResource("jatcore-data").getPath();
                alm_stream = GpsSvServlet.class.getResourceAsStream("/jatcore-data/current.alm");
                gc.add_yuma(alm_stream);
            }
        }
        ref = "<ref>" + ref_time_pos + "</ref>";
        PrintWriter out = response.getWriter();
        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        out.println("<response>");
        out.println(ref);
        out.println(load_alm_response);
        out.println(latest_gps_epoch);
        if (sv_xml_str != null) {
            out.println(sv_xml_str);
        }
        out.println("</response>");
        out.close();
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
}
