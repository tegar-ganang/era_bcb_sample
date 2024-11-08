package uk.ac.rdg.resc.ncwms.controller;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;

/**
 *
 * @author ads
 */
public class Blog extends HttpServlet {

    String requestPostURL = "http://blogs.blogmydata.org/api/rest/addpost/uid/b13f3e037c6082ed20346a96520c496f";

    String requestDataURL = "http://blogs.blogmydata.org/api/rest/adddata/uid/b13f3e037c6082ed20346a96520c496f";

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String img = null;
            String zipdata = null;
            String dataIdxml = null;
            String objtype = "png";
            Map<String, String> params = new HashMap<String, String>();
            if (request.getParameter("viztype").equalsIgnoreCase("animation")) {
                objtype = "avi";
                img = getMovieImage(request.getParameter("movieurl"));
                params.put("timeEnd", request.getParameter("timeend"));
            } else {
                img = getImageData(request.getParameter("imageurl"));
            }
            params.put("datasetvar", request.getParameter("datasetid"));
            params.put("title", request.getParameter("title"));
            params.put("varname", request.getParameter("variable"));
            params.put("varunit", request.getParameter("units"));
            params.put("timeTxt", request.getParameter("timetxt"));
            params.put("postTime", WmsUtils.dateTimeToISO8601(new DateTime()));
            params.put("elev", request.getParameter("elevation"));
            params.put("elevunits", request.getParameter("elevationUnits"));
            params.put("elevationPositive", request.getParameter("elevationPositive"));
            params.put("crs", request.getParameter("crs"));
            params.put("username", parseUsername(request.getParameter("bopenid")));
            params.put("blogText", request.getParameter("btext"));
            params.put("geom", request.getParameter("geom"));
            params.put("url", request.getParameter("url"));
            params.put("viztype", request.getParameter("viztype"));
            params.put("lon", request.getParameter("lon"));
            params.put("lat", request.getParameter("lat"));
            params.put("calSystem", request.getParameter("calen"));
            params.put("varstdname", request.getParameter("varstdname"));
            String geotxt = getGeoTxt(request.getParameter("geom"));
            dataIdxml = getXMLDataRequest(request.getParameter("title"), img, request.getParameter("url"), objtype);
            String respXml = getXmlResponse(dataIdxml, requestDataURL);
            String dataid = getDataId(respXml);
            String xml = getXMLPostRequest(params, dataid, geotxt, request.getParameter("viztype"));
            String respPostXml = getXmlResponse(xml, requestPostURL);
            String result = getMessage(respPostXml);
            response.getWriter().write(result);
            response.setContentType("text/xml");
            response.setHeader("Cache-Control", "no-cache");
            response.getWriter().write(result);
        } finally {
        }
    }

    public String getXmlResponse(String xml, String addr) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod(addr);
            method.addParameter("request", xml);
            int statusCode = client.executeMethod(method);
            InputStream rstream = null;
            rstream = method.getResponseBodyAsStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(rstream));
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String getDataId(String xml) {
        try {
            StringReader reader = new StringReader(xml);
            InputSource inputSource = new InputSource(reader);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            Document doc = factory.newDocumentBuilder().parse(inputSource);
            org.w3c.dom.Element element = doc.getDocumentElement();
            NodeList nl = doc.getElementsByTagName("result");
            Element el = (Element) nl.item(0);
            String datasetid = getTextValue(el, "data_id");
            return datasetid;
        } catch (Exception e) {
            return null;
        }
    }

    public String getMessage(String xml) {
        try {
            StringReader reader = new StringReader(xml);
            InputSource inputSource = new InputSource(reader);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            Document doc = factory.newDocumentBuilder().parse(inputSource);
            NodeList nl = doc.getElementsByTagName("result");
            Element el = (Element) nl.item(0);
            String msg = getTextValue(el, "post_info");
            return msg.replaceAll("xml", "html");
        } catch (Exception e) {
            return null;
        }
    }

    private static String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }
        return textVal;
    }

    private String getGeoTxt(String geom) {
        String[] bbox = geom.split(",");
        String bformat = "POLYGON((" + bbox[0] + " " + bbox[1] + "," + bbox[2] + " " + bbox[1] + "," + bbox[2] + " " + bbox[3] + "," + bbox[0] + " " + bbox[3] + "," + bbox[0] + " " + bbox[1] + "))";
        return bformat;
    }

    protected String getImageData(String url) {
        try {
            BufferedImage img = ImageIO.read(new URL(url));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            baos.flush();
            byte[] encodedImage = Base64.encodeBase64(baos.toByteArray());
            String data = new String(encodedImage, "utf-8");
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    protected String getMovieImage(String url) {
        try {
            URL movie = new URL(url);
            InputStream is = movie.openStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            byte[] buffer = new byte[1000];
            int read = -1;
            ByteArrayOutputStream byteout = new ByteArrayOutputStream();
            while ((read = dis.read(buffer)) >= 0) {
                byteout.write(buffer, 0, read);
            }
            byteout.flush();
            byte[] encodedImage = Base64.encodeBase64(byteout.toByteArray());
            String data = new String(encodedImage, "utf-8");
            is = null;
            dis = null;
            byteout = null;
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    protected String parseUsername(String username) {
        username = username.replaceAll("([a-zA-Z]+://)", "");
        username = username.replaceAll("([a-zA-Z]+://)", "");
        username = username.replaceAll("/$", "");
        username = username.replaceAll("/?/i", "-");
        username = username.replaceAll("/&", "-");
        username = username.replaceAll("//", "-");
        username = username.replaceAll("/s", "");
        return username;
    }

    protected String getXMLDataRequest(String btitle, String data, String url, String ext) {
        String filename = null;
        if (ext.equalsIgnoreCase("avi")) {
            filename = "movie.avi";
        } else {
            filename = "screenshot.png";
        }
        String xmlRequest = new String();
        System.out.println(url.replace("&", "&amp;"));
        xmlRequest = xmlRequest.concat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlRequest = xmlRequest.concat("<dataset>");
        xmlRequest = xmlRequest.concat("<title>" + btitle + "</title>");
        xmlRequest = xmlRequest.concat("<data><dataitem type=\"inline\" ext=\"" + ext + "\" main=\"1\" filename=\"" + filename + "\">" + data + "</dataitem>");
        xmlRequest = xmlRequest.concat("<dataitem type=\"url\" ext=\"url\" filename=\"" + "\">" + url.replace("&", "&amp;") + "</dataitem></data>");
        xmlRequest = xmlRequest.concat("</dataset>");
        return xmlRequest;
    }

    protected String getXMLPostRequest(Map params, String dataid, String geotxt, String viztype) {
        String xmlRequest = new String();
        String[] dt = ((String) params.get("datasetvar")).split("/");
        xmlRequest = xmlRequest.concat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlRequest = xmlRequest.concat("<post>");
        xmlRequest = xmlRequest.concat("<title>" + params.get("title") + "</title>");
        xmlRequest = xmlRequest.concat("<section>ahm2007</section>");
        xmlRequest = xmlRequest.concat("<author>");
        xmlRequest = xmlRequest.concat("<username>" + params.get("username") + "</username>");
        xmlRequest = xmlRequest.concat("</author>");
        xmlRequest = xmlRequest.concat("<content>[data=size:525x363]" + dataid + "[/data]<br/> " + "\n" + params.get("blogText") + "</content>");
        xmlRequest = xmlRequest.concat("<datestamp>" + params.get("postTime") + "</datestamp>");
        xmlRequest = xmlRequest.concat("<blog_sname>godivasandpit</blog_sname>");
        xmlRequest = xmlRequest.concat("<metadata><vizType>" + params.get("viztype") + "</vizType>");
        if (viztype.equalsIgnoreCase("staticMapWithPoint")) {
            String pt = "POINT((" + (String) params.get("lon") + " " + (String) params.get("lat") + "))";
            xmlRequest = xmlRequest.concat("<regionOfinterest>" + pt + "</regionOfinterest>");
        }
        xmlRequest = xmlRequest.concat("<dataset>" + dt[0] + "</dataset>");
        xmlRequest = xmlRequest.concat("<variableid>" + dt[1] + "</variableid>");
        xmlRequest = xmlRequest.concat("<conventions>" + "CF-1.4" + "</conventions>");
        xmlRequest = xmlRequest.concat("<variableStandardName>" + params.get("varstdname") + "</variableStandardName>");
        xmlRequest = xmlRequest.concat("<variableUnits>" + params.get("varunit") + "</variableUnits>");
        xmlRequest = xmlRequest.concat("<bbox>" + geotxt + "</bbox>");
        xmlRequest = xmlRequest.concat("<crs>" + params.get("crs") + "</crs>");
        xmlRequest = xmlRequest.concat("<time>" + params.get("timeTxt") + "</time>");
        if (viztype.equalsIgnoreCase("animation")) {
            xmlRequest = xmlRequest.concat("<timeEnd>" + params.get("timeEnd") + "</timeEnd>");
        }
        xmlRequest = xmlRequest.concat("<calendarSystem>" + params.get("calSystem") + "</calendarSystem>");
        xmlRequest = xmlRequest.concat("<elevation>" + params.get("elev") + "</elevation>");
        xmlRequest = xmlRequest.concat("<elevationUnits>" + params.get("elevunits") + "</elevationUnits>");
        xmlRequest = xmlRequest.concat("<elevationPositive>" + params.get("elevationPositive") + "</elevationPositive></metadata>");
        xmlRequest = xmlRequest.concat("<attached_data><data type=\"local\">" + dataid + "</data></attached_data>");
        xmlRequest = xmlRequest.concat("</post>");
        return xmlRequest;
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
        doPost(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
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
