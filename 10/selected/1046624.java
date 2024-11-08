package org.fao.waicent.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.Icon;
import org.apache.xpath.XPathAPI;
import org.fao.waicent.db.QueryUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XPatternOutline implements Cloneable, XMLable {

    XPatternPaint pattern;

    XOutline outline;

    XPoint point = null;

    public synchronized Object clone() throws CloneNotSupportedException {
        XPatternOutline x = (XPatternOutline) super.clone();
        if (pattern != null) {
            x.pattern = (XPatternPaint) pattern.clone();
        }
        if (outline != null) {
            x.outline = (XOutline) outline.clone();
        }
        if (point != null) {
            x.point = (XPoint) point.clone();
        }
        return x;
    }

    /**     *  alisaf: constructor.     *  Only calls load(Document,Element)     */
    public XPatternOutline(Document doc, Element ele) throws IOException {
        load(doc, ele);
    }

    public void load(Document doc, Element ele) throws IOException {
        XMLUtil.checkType(doc, ele, this);
        for (int i = 0; i < ele.getElementsByTagName("Pattern").getLength(); i++) {
            pattern = new XPatternPaint(doc, (Element) ele.getElementsByTagName("Pattern").item(i));
        }
        if (pattern == null) {
            pattern = new XPatternPaint();
        }
        for (int i = 0; i < ele.getElementsByTagName("Outline").getLength(); i++) {
            outline = new XOutline(doc, (Element) ele.getElementsByTagName("Outline").item(i));
        }
        if (outline == null) {
            outline = new XOutline();
        }
        for (int i = 0; i < ele.getElementsByTagName("Point").getLength(); i++) {
            point = new XPoint(doc, (Element) ele.getElementsByTagName("Point").item(i));
        }
    }

    public void save(Document doc, Element ele) throws IOException {
        XMLUtil.setType(doc, ele, this);
        Element pattern_ele = doc.createElement("Pattern");
        ele.appendChild(pattern_ele);
        pattern.save(doc, pattern_ele);
        Element outline_ele = doc.createElement("Outline");
        ele.appendChild(outline_ele);
        outline.save(doc, outline_ele);
        if (this.hasPoint()) {
            Element point_ele = doc.createElement("Point");
            ele.appendChild(point_ele);
            point.save(doc, point_ele);
        }
    }

    public boolean hasPoint() {
        if (point == null) return false;
        if (point.getType() == XPoint.POINT_NONE) return false;
        return true;
    }

    public String toString() {
        String output = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><PatternOutline type=\"org.fao.waicent.util.XPatternOutline\">";
        if (pattern != null) output += pattern.toString();
        if (outline != null) output += outline.toString();
        if (point != null) output += point.toString();
        output += "</PatternOutline>";
        return output;
    }

    public void save(DataOutputStream out) throws IOException {
        pattern.save(out);
        outline.save(out);
        if (point == null) point = new XPoint();
        point.save(out);
    }

    public XPatternOutline(ResultSet r, Connection con) throws SQLException {
        loadDB(r, con);
    }

    public XPatternOutline(Connection con, long id) throws SQLException {
        loadDB(con, id);
    }

    protected void loadDB(Connection con, long id) throws SQLException {
        Statement statement = null;
        ResultSet result_set = null;
        StringBuffer SQL = null;
        try {
            statement = con.createStatement();
            SQL = new StringBuffer("select ID,PATTERN_FILL_COLOR,PATTERN_FILL_STYLE,PATTERN_FILL_DENSITY,PATTERN_BACKGROUND_COLOR,OUTLINE_COLOR,OUTLINE_LINE_STYLE,OUTLINE_LINE_WIDTH from ");
            SQL.append("PATTERN_OUTLINE where ID=");
            SQL.append(id);
            result_set = statement.executeQuery(new String(SQL));
            result_set.next();
            loadDB(result_set, con);
            SQL = null;
        } catch (SQLException e) {
            System.err.println(e + " SQL:=" + SQL);
            throw e;
        } finally {
            if (result_set != null) {
                try {
                    result_set.close();
                } catch (SQLException e) {
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    protected void loadDB(ResultSet r, Connection con) throws SQLException {
        pattern = new XPatternPaint(XColor.loadColor(r.getInt("PATTERN_BACKGROUND_COLOR")), XColor.loadColor(r.getInt("PATTERN_FILL_COLOR")), (byte) r.getInt("PATTERN_FILL_STYLE"), (byte) r.getInt("PATTERN_FILL_DENSITY"));
        outline = new XOutline(XColor.loadColor(r.getInt("OUTLINE_COLOR")), (byte) r.getInt("OUTLINE_LINE_STYLE"), (byte) r.getInt("OUTLINE_LINE_WIDTH"));
    }

    public static void deleteDB(Connection con, long id) throws SQLException {
        Statement statement = null;
        StringBuffer SQL = null;
        try {
            statement = con.createStatement();
            SQL = new StringBuffer("delete from ");
            SQL.append("PATTERN_OUTLINE where ID = ");
            SQL.append(id);
            statement.executeUpdate(new String(SQL));
            SQL = null;
        } catch (SQLException e) {
            System.err.println(e + " SQL:=" + SQL);
            throw e;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public long saveDB(Connection con) throws SQLException {
        return saveDB(con, -1, false);
    }

    public long saveDB(Connection con, long id) throws SQLException {
        return saveDB(con, id, false);
    }

    public long saveDB(Connection con, long id, boolean commit) throws SQLException {
        StringBuffer SQL = null;
        Statement statement = null;
        ResultSet result_set = null;
        try {
            statement = con.createStatement();
            if (id < 0) {
                id = QueryUtils.sequenceGetNextID(con, "PATTERN_OUTLINE");
            } else {
                deleteDB(con, id);
            }
            SQL = new StringBuffer("insert into ");
            SQL.append("PATTERN_OUTLINE values (");
            SQL.append(id);
            SQL.append(",");
            SQL.append(XColor.toInt(pattern.getPatternColor()));
            SQL.append(",");
            SQL.append(pattern.getPatternStyle());
            SQL.append(",");
            SQL.append(pattern.getPatternDensity());
            SQL.append(",");
            SQL.append(XColor.toInt(pattern.getBackgroundColor()));
            SQL.append(",");
            SQL.append(XColor.toInt(outline.getColor()));
            SQL.append(",");
            SQL.append(outline.getStyle());
            SQL.append(",");
            SQL.append(outline.getWidth());
            SQL.append(")");
            statement.executeUpdate(new String(SQL));
            SQL = null;
            if (commit) {
                con.commit();
            }
        } catch (SQLException e) {
            System.err.println(getClass().getName() + ":" + e + " SQL:=" + SQL);
            if (commit) {
                con.rollback();
            }
            throw e;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }
        return id;
    }

    /**     *  alisaf: additional constructor to build the Definition from DOM tree     */
    public XPatternOutline(Element element) throws Exception {
        Element pattern_element = (Element) XPathAPI.selectSingleNode(element, "XPatternPaint");
        pattern = new XPatternPaint(pattern_element);
        Element outline_element = (Element) XPathAPI.selectSingleNode(element, "XOutline");
        outline = new XOutline(outline_element);
        point = null;
        Element point_element = (Element) XPathAPI.selectSingleNode(element, "XPoint");
        if (point_element != null) point = new XPoint(point_element);
    }

    /**     *  alisaf: added to support storing Input Stream into DOM     */
    public XPatternOutline(DataInputStream in, Document doc, Element element) throws IOException {
        pattern = new XPatternPaint(in, doc, element);
        outline = new XOutline(in, doc, element);
        point = null;
    }

    /**	 *  pwg: added to support storing Input Stream into DOM including point     */
    public XPatternOutline(DataInputStream in, Document doc, Element element, int version) throws IOException {
        pattern = new XPatternPaint(in, doc, element);
        outline = new XOutline(in, doc, element);
        point = new XPoint(in, doc, element, version);
        if (point.getType() == XPoint.POINT_NONE) point = null;
    }

    /**     *  alisaf: constructor to read datastream and build this object.     */
    public XPatternOutline(DataInputStream in) throws IOException {
        pattern = new XPatternPaint(in);
        outline = new XOutline(in);
        point = null;
    }

    /**	 *  pwg: constructor to read datastream and build this object including point.	 */
    public XPatternOutline(DataInputStream in, int version) throws IOException {
        pattern = new XPatternPaint(in);
        outline = new XOutline(in);
        point = new XPoint(in, version);
        if (point.getType() == XPoint.POINT_NONE) point = null;
    }

    public XPatternOutline() {
        this((XPatternPaint) null, null);
    }

    public XPatternOutline(XPatternPaint pattern) {
        this(pattern, null);
    }

    public XPatternOutline(XPatternPaint pattern, XOutline outline) {
        this.pattern = pattern;
        this.outline = outline;
        if (pattern == null) {
            this.pattern = new XPatternPaint();
        }
        if (outline == null) {
            this.outline = new XOutline();
        }
        this.point = null;
    }

    public XPatternOutline(XPatternPaint pattern, XOutline outline, XPoint point) {
        this.pattern = pattern;
        this.outline = outline;
        this.point = point;
        if (pattern == null) {
            this.pattern = new XPatternPaint();
        }
        if (outline == null) {
            this.outline = new XOutline();
        }
    }

    public String getString() {
        String bgc = Integer.toString(XColor.toInt(pattern.getBackgroundColor()));
        String pc = Integer.toString(XColor.toInt(pattern.getPatternColor()));
        String pd = Byte.toString(pattern.getPatternDensity());
        String ps = Byte.toString(pattern.getPatternStyle());
        String oc = Integer.toString(XColor.toInt(outline.getColor()));
        String od = Byte.toString(outline.getDensity());
        String os = Byte.toString(outline.getStyle());
        String ret = bgc + ";" + pc + ";" + pd + ";" + ps + ";" + oc + ";" + od + ";" + os;
        if (hasPoint()) {
            ret += ";" + XPoint.getString(this.point);
        }
        return ret;
    }

    public static XPatternOutline getPO(String settings) {
        int i = 0;
        String[] tok = settings.split(";", -1);
        String pbgColor = "";
        String pcolor = "";
        String pstyle = "";
        String pdensity = "";
        String ocolor = "";
        String ostyle = "";
        String odensity = "";
        if (i < tok.length) pbgColor = tok[i++];
        if (i < tok.length) pcolor = tok[i++];
        if (i < tok.length) pdensity = tok[i++];
        if (i < tok.length) pstyle = tok[i++];
        if (i < tok.length) ocolor = tok[i++];
        if (i < tok.length) odensity = tok[i++];
        if (i < tok.length) ostyle = tok[i++];
        XPatternPaint pat = new XPatternPaint(XColor.loadColor(Integer.parseInt(pbgColor)), XColor.loadColor(Integer.parseInt(pcolor)), Byte.parseByte(pstyle), Byte.parseByte(pdensity));
        XOutline out = new XOutline(XColor.loadColor(Integer.parseInt(ocolor)), Byte.parseByte(ostyle), Byte.parseByte(odensity));
        String ptype = null;
        String picontype = "-1";
        String piconname = "";
        String piconfilename = "";
        String piconimgloc = "";
        String pshapetype = "-1";
        String pshapesize = "";
        String pshapename = "";
        String pscalable = "";
        String pshapeunit = "";
        String psmooth = "";
        if (i < tok.length) ptype = tok[i++];
        if (i < tok.length) picontype = tok[i++];
        if (i < tok.length) piconname = tok[i++];
        if (i < tok.length) piconfilename = tok[i++];
        if (i < tok.length) piconimgloc = tok[i++];
        if (i < tok.length) pshapetype = tok[i++];
        if (i < tok.length) pshapesize = tok[i++];
        if (i < tok.length) pshapename = tok[i++];
        if (i < tok.length) pscalable = tok[i++];
        if (i < tok.length) pshapeunit = tok[i++];
        if (i < tok.length) psmooth = tok[i++];
        XPoint pt = null;
        if (ptype != null) {
            pt = new XPoint(ptype, picontype, piconname, piconfilename, piconimgloc, pshapetype, pshapesize, pshapename, pscalable, pshapeunit, psmooth);
        }
        return new XPatternOutline(pat, out, pt);
    }

    public void setPattern(XPatternPaint pattern) {
        this.pattern = pattern;
    }

    public void setOutline(XOutline outline) {
        this.outline = outline;
    }

    public void setPoint(XPoint point) {
        this.point = point;
    }

    public XPatternPaint getPattern() {
        return pattern;
    }

    public XOutline getOutline() {
        return outline;
    }

    public XPoint getPoint() {
        return point;
    }

    public Icon getIcon(int width, int height) {
        return new IconPattern(this, width, height);
    }

    class IconPattern implements Icon {

        XPatternOutline pattern;

        int width;

        int height;

        IconPattern(XPatternOutline pattern, int width, int height) {
            this.pattern = pattern;
            this.width = width;
            this.height = height;
        }

        public int getIconWidth() {
            return width;
        }

        public int getIconHeight() {
            return height;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (g instanceof Graphics2D) {
                pattern.paintShape((Graphics2D) g, new Rectangle(x, y, width, height));
            }
        }
    }

    public Color getColor() {
        if (pattern != null || pattern.getColor().getAlpha() != 0) {
            return pattern.getColor();
        } else {
            return outline.getColor();
        }
    }

    public void paintShape(Graphics2D g, Shape shape) {
        paintShape(g, shape, pattern, outline);
    }

    /**     *  alisaf: method to paint the polygon passed in as array of x,y coordinates.     *  This method gets called from the MultiPolygonFeature class.     *     *  @param g      The graphics object that will paint the polygon.     *  @param points The array of x,y coordinates of the shape to be painted.     */
    public void paintPaths(Graphics2D g, Point2D[][] points) {
        paintPaths(g, null, points, pattern, outline);
    }

    public void paintShape(Graphics2D g, Rectangle clip, Shape shape) {
        paintShape(g, clip, shape, pattern, outline);
    }

    public void drawShape(Graphics2D g, Rectangle clip, Shape shape) {
        drawShape(g, clip, shape, outline);
    }

    public void drawPaths(Graphics2D g, Rectangle clip, Point2D[][] points) {
        drawPaths(g, clip, points, outline);
    }

    public void drawShape(Graphics2D g, Shape shape) {
        drawShape(g, shape, outline);
    }

    public void drawPaths(Graphics2D g, Point2D[][] points) {
        drawPaths(g, null, points, outline);
    }

    public static void paintShape(Graphics2D g, Shape shape, XPatternPaint pattern, XOutline outline) {
        Rectangle clip = null;
        if (g.getClip() != null) {
            clip = g.getClip().getBounds();
        }
        paintShape(g, clip, shape, pattern, outline);
    }

    public Shape drawPoint(Graphics2D g, int x, int y) {
        if (point != null && point.getType() != XPoint.POINT_NONE) {
            return point.drawPoint(g, x, y, pattern, outline);
        } else if (outline != null && outline.getStyle() != XOutline.LINE_NONE) {
            outline.drawPoint(g, x, y);
        }
        return null;
    }

    public Shape drawPoint(Graphics2D g, Shape c, int x, int y, double scalefactor) {
        return drawPoint(g, c, null, x, y, XPoint.drawNormal, scalefactor);
    }

    public Shape drawPoint(Graphics2D g, Shape c, int x, int y) {
        return drawPoint(g, c, null, x, y);
    }

    public Shape drawPoint(Graphics2D g, Shape c, Shape s, int x, int y) {
        return drawPoint(g, c, s, x, y, XPoint.drawNormal);
    }

    public Shape drawPoint(Graphics2D g, Shape c, int x, int y, int drawtype) {
        return drawPoint(g, c, null, x, y, drawtype);
    }

    public Shape drawPoint(Graphics2D g, Shape c, Shape s, int x, int y, int drawtype) {
        return drawPoint(g, c, s, x, y, drawtype, 1.0d);
    }

    public Shape drawPoint(Graphics2D g, Shape c, Shape s, int x, int y, int drawtype, double scalefactor) {
        if (this.hasPoint()) {
            return point.drawPoint(g, (Rectangle) c, s, x, y, pattern, outline, drawtype, scalefactor);
        } else {
            this.paintShape(g, (Rectangle) c, s);
            return s;
        }
    }

    public void paintLegendIcon(Graphics2D g, Rectangle c) {
        if (this.hasPoint()) {
            point.paintLegendIcon(g, c, pattern, outline);
            return;
        } else {
            this.paintShape(g, c, c);
        }
    }

    /**     *  alisaf: method to paint the polygon passed in as array of x,y coordinates.     *     *  @param g      The graphics object that will paint the polygon.     *  @param clip   The rectangle within which the polygon will be painted.     *  @param points The array of x,y coordinates of the shape to be painted.     */
    public static void paintPaths(Graphics2D g, Rectangle clip, Point2D[][] points, XPatternPaint pattern, XOutline outline) {
        pattern.fillPaths(g, clip, points);
        if (outline != null && outline.getStyle() != XOutline.LINE_NONE) {
            outline.drawPaths(g, clip, points);
        }
    }

    public static void paintShape(Graphics2D g, Rectangle clip, Shape shape, XPatternPaint pattern, XOutline outline) {
        pattern.fillShape(g, clip, shape);
        if (outline != null && outline.getStyle() != XOutline.LINE_NONE) {
            outline.drawShape(g, clip, shape);
        }
    }

    public static void drawShape(Graphics2D g, Shape shape, XOutline outline) {
        Rectangle clip = null;
        if (g.getClip() != null) {
            clip = g.getClip().getBounds();
        }
        drawShape(g, clip, shape, outline);
    }

    public static void drawShape(Graphics2D g, Rectangle clip, Shape shape, XOutline outline) {
        if (outline != null && outline.getStyle() != XOutline.LINE_NONE) {
            outline.drawShape(g, clip, shape);
        }
    }

    public static void drawPaths(Graphics2D g, Rectangle clip, Point2D[][] points, XOutline outline) {
        if (outline != null && outline.getStyle() != XOutline.LINE_NONE) {
            outline.drawPaths(g, clip, points);
        }
    }

    public void paintPreview(Graphics2D g, Shape s) {
        if (this.hasPoint()) {
            point.paintPreview(g, s, pattern, outline);
            return;
        }
        Rectangle preview = (Rectangle) s;
        int thick = outline.getDensity();
        preview.setBounds(preview.x + thick / 2, preview.y + thick / 2, preview.width - thick, preview.height - thick);
        paintShape(g, preview);
    }

    public void doTransparent() {
        if (outline != null) {
            outline.doTransparent();
        }
        if (pattern != null) {
            pattern.doTransparent();
        }
    }

    public boolean isTransparent() {
        if ((outline == null || outline.isTransparent()) && (pattern == null || pattern.isTransparent())) {
            return true;
        }
        return false;
    }

    public void toXML(Document doc, Element element) {
        this.getPattern().toXML(doc, element);
        this.getOutline().toXML(doc, element);
        XPoint point = this.getPoint();
        if (point != null) point.toXML(doc, element);
    }
}
