package org.yaoqiang.graph.canvas;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.yaoqiang.graph.util.Constants;
import com.mxgraph.canvas.mxBasicCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxBase64;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**
 * YSvgCanvas
 * 
 * @author Shi Yaoqiang(shi_yaoqiang@yahoo.com)
 */
public class YSvgCanvas extends mxBasicCanvas {

    /**
	 * Holds the HTML document that represents the canvas.
	 */
    protected Document document;

    /**
	 * Used internally for looking up elements. Workaround for getElementById not working.
	 */
    private Map<String, Element> gradients = new Hashtable<String, Element>();

    /**
	 * Used internally for looking up images.
	 */
    private Map<String, Element> images = new Hashtable<String, Element>();

    /**
	 * 
	 */
    protected Element defs = null;

    /**
	 * Specifies if images should be embedded as base64 encoded strings. Default is false.
	 */
    protected boolean embedded = false;

    /**
	 * Constructs a new SVG canvas for the specified dimension and scale.
	 */
    public YSvgCanvas() {
        this(null);
    }

    /**
	 * Constructs a new SVG canvas for the specified bounds, scale and background color.
	 */
    public YSvgCanvas(Document document) {
        setDocument(document);
    }

    /**
	 * 
	 */
    public void appendSvgElement(Element node) {
        if (document != null) {
            document.getDocumentElement().appendChild(node);
        }
    }

    /**
	 * 
	 */
    protected Element getDefsElement() {
        if (defs == null) {
            defs = document.createElement("defs");
            Element svgNode = document.getDocumentElement();
            if (svgNode.hasChildNodes()) {
                svgNode.insertBefore(defs, svgNode.getFirstChild());
            } else {
                svgNode.appendChild(defs);
            }
        }
        return defs;
    }

    /**
	 * 
	 */
    public Element getGradientElement(String start, String end, String direction) {
        String id = getGradientId(start, end, direction);
        Element gradient = gradients.get(id);
        if (gradient == null) {
            gradient = createGradientElement(start, end, direction);
            gradient.setAttribute("id", "g" + (gradients.size() + 1));
            getDefsElement().appendChild(gradient);
            gradients.put(id, gradient);
        }
        return gradient;
    }

    /**
	 * 
	 */
    public Element getGlassGradientElement() {
        String id = "mx-glass-gradient";
        Element glassGradient = gradients.get(id);
        if (glassGradient == null) {
            glassGradient = document.createElement("linearGradient");
            glassGradient.setAttribute("x1", "0%");
            glassGradient.setAttribute("y1", "0%");
            glassGradient.setAttribute("x2", "0%");
            glassGradient.setAttribute("y2", "100%");
            Element stop1 = document.createElement("stop");
            stop1.setAttribute("offset", "0%");
            stop1.setAttribute("style", "stop-color:#ffffff;stop-opacity:0.9");
            glassGradient.appendChild(stop1);
            Element stop2 = document.createElement("stop");
            stop2.setAttribute("offset", "100%");
            stop2.setAttribute("style", "stop-color:#ffffff;stop-opacity:0.1");
            glassGradient.appendChild(stop2);
            glassGradient.setAttribute("id", "g" + (gradients.size() + 1));
            getDefsElement().appendChild(glassGradient);
            gradients.put(id, glassGradient);
        }
        return glassGradient;
    }

    /**
	 * 
	 */
    protected Element createGradientElement(String start, String end, String direction) {
        Element gradient = document.createElement("linearGradient");
        gradient.setAttribute("x1", "0%");
        gradient.setAttribute("y1", "0%");
        gradient.setAttribute("x2", "0%");
        gradient.setAttribute("y2", "0%");
        if (direction == null || direction.equals(mxConstants.DIRECTION_SOUTH)) {
            gradient.setAttribute("y2", "100%");
        } else if (direction.equals(mxConstants.DIRECTION_EAST)) {
            gradient.setAttribute("x2", "100%");
        } else if (direction.equals(mxConstants.DIRECTION_NORTH)) {
            gradient.setAttribute("y1", "100%");
        } else if (direction.equals(mxConstants.DIRECTION_WEST)) {
            gradient.setAttribute("x1", "100%");
        }
        Element stop = document.createElement("stop");
        stop.setAttribute("offset", "0%");
        stop.setAttribute("style", "stop-color:" + start);
        gradient.appendChild(stop);
        stop = document.createElement("stop");
        stop.setAttribute("offset", "100%");
        stop.setAttribute("style", "stop-color:" + end);
        gradient.appendChild(stop);
        return gradient;
    }

    /**
	 * 
	 */
    public String getGradientId(String start, String end, String direction) {
        if (start.startsWith("#")) {
            start = start.substring(1);
        }
        if (end.startsWith("#")) {
            end = end.substring(1);
        }
        start = start.toLowerCase();
        end = end.toLowerCase();
        String dir = null;
        if (direction == null || direction.equals(mxConstants.DIRECTION_SOUTH)) {
            dir = "south";
        } else if (direction.equals(mxConstants.DIRECTION_EAST)) {
            dir = "east";
        } else {
            String tmp = start;
            start = end;
            end = tmp;
            if (direction.equals(mxConstants.DIRECTION_NORTH)) {
                dir = "south";
            } else if (direction.equals(mxConstants.DIRECTION_WEST)) {
                dir = "east";
            }
        }
        return "mx-gradient-" + start + "-" + end + "-" + dir;
    }

    /**
	 * Returns true if the given string ends with .png, .jpg or .gif.
	 */
    protected boolean isImageResource(String src) {
        return src != null && (src.toLowerCase().endsWith(".png") || src.toLowerCase().endsWith(".jpg") || src.toLowerCase().endsWith(".gif"));
    }

    /**
	 * 
	 */
    protected InputStream getResource(String src) {
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new URL(src).openStream());
        } catch (Exception e1) {
            stream = getClass().getResourceAsStream(src);
        }
        return stream;
    }

    /**
	 * @throws IOException
	 * 
	 */
    protected String createDataUrl(String src) throws IOException {
        String result = null;
        InputStream inputStream = isImageResource(src) ? getResource(src) : null;
        if (inputStream != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
            byte[] bytes = new byte[512];
            int readBytes;
            while ((readBytes = inputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, readBytes);
            }
            String format = "png";
            int dot = src.lastIndexOf('.');
            if (dot > 0 && dot < src.length()) {
                format = src.substring(dot + 1);
            }
            result = "data:image/" + format + ";base64," + mxBase64.encodeToString(outputStream.toByteArray(), false);
        }
        return result;
    }

    /**
	 * 
	 */
    protected Element getEmbeddedImageElement(String src) {
        Element img = images.get(src);
        if (img == null) {
            img = document.createElement("svg");
            img.setAttribute("width", "100%");
            img.setAttribute("height", "100%");
            Element inner = document.createElement("image");
            inner.setAttribute("width", "100%");
            inner.setAttribute("height", "100%");
            images.put(src, img);
            if (!src.startsWith("data:image/")) {
                try {
                    String tmp = createDataUrl(src);
                    if (tmp != null) {
                        src = tmp;
                    }
                } catch (IOException e) {
                }
            }
            inner.setAttributeNS(mxConstants.NS_XLINK, "xlink:href", src);
            img.appendChild(inner);
            img.setAttribute("id", "i" + (images.size()));
            getDefsElement().appendChild(img);
        }
        return img;
    }

    /**
	 * 
	 */
    protected Element createImageElement(double x, double y, double w, double h, String src, boolean aspect, boolean flipH, boolean flipV, boolean embedded) {
        Element elem = null;
        if (embedded) {
            elem = document.createElement("use");
            Element img = getEmbeddedImageElement(src);
            elem.setAttributeNS(mxConstants.NS_XLINK, "xlink:href", "#" + img.getAttribute("id"));
        } else {
            elem = document.createElement("image");
            elem.setAttributeNS(mxConstants.NS_XLINK, "xlink:href", src);
        }
        elem.setAttribute("x", String.valueOf(x));
        elem.setAttribute("y", String.valueOf(y));
        elem.setAttribute("width", String.valueOf(w));
        elem.setAttribute("height", String.valueOf(h));
        if (aspect) {
            elem.setAttribute("preserveAspectRatio", "xMidYMid");
        } else {
            elem.setAttribute("preserveAspectRatio", "none");
        }
        double sx = 1;
        double sy = 1;
        double dx = 0;
        double dy = 0;
        if (flipH) {
            sx *= -1;
            dx = -w - 2 * x;
        }
        if (flipV) {
            sy *= -1;
            dy = -h - 2 * y;
        }
        String transform = "";
        if (sx != 1 || sy != 1) {
            transform += "scale(" + sx + " " + sy + ") ";
        }
        if (dx != 0 || dy != 0) {
            transform += "translate(" + dx + " " + dy + ") ";
        }
        if (transform.length() > 0) {
            elem.setAttribute("transform", transform);
        }
        return elem;
    }

    /**
	 * 
	 */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
	 * Returns a reference to the document that represents the canvas.
	 * 
	 * @return Returns the document.
	 */
    public Document getDocument() {
        return document;
    }

    /**
	 * 
	 */
    public void setEmbedded(boolean value) {
        embedded = value;
    }

    /**
	 * 
	 */
    public boolean isEmbedded() {
        return embedded;
    }

    public Object drawCell(mxCellState state) {
        Map<String, Object> style = state.getStyle();
        mxCell cell = (mxCell) state.getCell();
        Element elem = null;
        if (state.getAbsolutePointCount() > 1) {
            List<mxPoint> pts = state.getAbsolutePoints();
            pts = mxUtils.translatePoints(pts, translate.x, translate.y);
            elem = drawLine(pts, style);
            float opacity = mxUtils.getFloat(style, mxConstants.STYLE_OPACITY, 100);
            if (opacity != 100) {
                String value = String.valueOf(opacity / 100);
                elem.setAttribute("fill-opacity", value);
                elem.setAttribute("stroke-opacity", value);
            }
        } else {
            int x = (int) state.getX() + translate.x;
            int y = (int) state.getY() + translate.y;
            int w = (int) state.getWidth();
            int h = (int) state.getHeight();
            if (!mxUtils.getString(style, mxConstants.STYLE_SHAPE, "").equals(mxConstants.SHAPE_SWIMLANE)) {
                elem = drawShape(x, y, w, h, style);
            } else {
                int start = (int) Math.round(mxUtils.getInt(style, mxConstants.STYLE_STARTSIZE, mxConstants.DEFAULT_STARTSIZE) * scale);
                Map<String, Object> cloned = new Hashtable<String, Object>(style);
                cloned.remove(mxConstants.STYLE_FILLCOLOR);
                cloned.remove(mxConstants.STYLE_ROUNDED);
                if (mxUtils.isTrue(style, mxConstants.STYLE_HORIZONTAL, true)) {
                    elem = drawShape(x, y, w, start, style);
                    drawShape(x, y + start, w, h - start, cloned);
                } else {
                    elem = drawShape(x, y, start, h, style);
                    drawShape(x + start, y, w - start, h, cloned);
                }
            }
        }
        if (cell.getId() != null && elem != null) {
            elem.setAttribute("id", cell.getId());
        }
        return elem;
    }

    public Object drawLabel(String label, mxCellState state, boolean html) {
        mxRectangle bounds = state.getLabelBounds();
        if (drawLabels && bounds != null) {
            int x = (int) bounds.getX() + translate.x;
            int y = (int) bounds.getY() + translate.y;
            int w = (int) bounds.getWidth();
            int h = (int) bounds.getHeight();
            Map<String, Object> style = state.getStyle();
            return drawText(label, x, y, w, h, style);
        }
        return null;
    }

    /**
	 * Draws the shape specified with the STYLE_SHAPE key in the given style.
	 * 
	 * @param x
	 *            X-coordinate of the shape.
	 * @param y
	 *            Y-coordinate of the shape.
	 * @param w
	 *            Width of the shape.
	 * @param h
	 *            Height of the shape.
	 * @param style
	 *            Style of the the shape.
	 */
    public Element drawShape(int x, int y, int w, int h, Map<String, Object> style) {
        String fillColor = mxUtils.getString(style, mxConstants.STYLE_FILLCOLOR, "none");
        if (fillColor.startsWith("ff")) {
            fillColor = "#" + fillColor.substring(2);
        }
        String gradientColor = mxUtils.getString(style, mxConstants.STYLE_GRADIENTCOLOR, "none");
        if (gradientColor.startsWith("ff")) {
            gradientColor = "#" + gradientColor.substring(2);
        }
        String strokeColor = mxUtils.getString(style, mxConstants.STYLE_STROKECOLOR, "none");
        if (strokeColor.startsWith("ff")) {
            strokeColor = "#" + strokeColor.substring(2);
        }
        float strokeWidth = (float) (mxUtils.getFloat(style, mxConstants.STYLE_STROKEWIDTH, 1) * scale);
        float opacity = mxUtils.getFloat(style, mxConstants.STYLE_OPACITY, 100);
        int inset = (int) ((3 + strokeWidth) * scale);
        String shape = mxUtils.getString(style, mxConstants.STYLE_SHAPE, "");
        String type = mxUtils.getString(style, Constants.STYLE_TYPE, "");
        String loop = mxUtils.getString(style, Constants.STYLE_LOOP_IMAGE, " ");
        String compensation = mxUtils.getString(style, Constants.STYLE_COMPENSATION_IMAGE, " ");
        String adHoc = mxUtils.getString(style, Constants.STYLE_ADHOC_IMAGE, " ");
        String img = getImageForStyle(style);
        double imgWidth = 16 * scale;
        double imgHeight = 16 * scale;
        int spacing = 5;
        mxRectangle imageBounds = new mxRectangle();
        Element elem = null;
        Element background = null;
        if (shape.equals(Constants.SHAPE_GATEWAY)) {
            elem = document.createElement("g");
            background = document.createElement("path");
            String d = "M " + (x + w / 2) + " " + y + " L " + (x + w) + " " + (y + h / 2) + " L " + (x + w / 2) + " " + (y + h) + " L " + x + " " + (y + h / 2);
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);
            if (type.equals(Constants.GATEWAY_STYLE_INCLUSIVE)) {
                inset = (int) Math.round(11 * scale);
                Element foreground = document.createElement("ellipse");
                foreground.setAttribute("fill", "none");
                foreground.setAttribute("stroke", strokeColor);
                foreground.setAttribute("stroke-width", String.valueOf(strokeWidth * 3));
                foreground.setAttribute("cx", String.valueOf(x + w / 2));
                foreground.setAttribute("cy", String.valueOf(y + h / 2));
                foreground.setAttribute("rx", String.valueOf(w / 2 - inset));
                foreground.setAttribute("ry", String.valueOf(h / 2 - inset));
                elem.appendChild(foreground);
            } else if (type.equals(Constants.GATEWAY_STYLE_EXCLUSIVE)) {
                int px = (int) Math.round(x + w * 0.65);
                int py = (int) Math.round(y + w * 0.25);
                int p2x = (int) Math.round(x + w * 0.35);
                int p2y = (int) Math.round(y + w * 0.75);
                Element foreground = document.createElement("path");
                d = " M " + px + " " + py + " L " + p2x + " " + p2y + " M " + p2x + " " + py + " L " + px + " " + p2y;
                foreground.setAttribute("d", d);
                foreground.setAttribute("fill", "none");
                foreground.setAttribute("stroke", strokeColor);
                foreground.setAttribute("stroke-width", String.valueOf(strokeWidth * 3));
                elem.appendChild(foreground);
            } else if (type.equals(Constants.GATEWAY_STYLE_EVENT)) {
                inset = (int) Math.round(10 * scale);
                Element foreground = document.createElement("ellipse");
                foreground.setAttribute("fill", "none");
                foreground.setAttribute("stroke", strokeColor);
                foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));
                foreground.setAttribute("cx", String.valueOf(x + w / 2));
                foreground.setAttribute("cy", String.valueOf(y + h / 2));
                foreground.setAttribute("rx", String.valueOf(w / 2 - inset));
                foreground.setAttribute("ry", String.valueOf(h / 2 - inset));
                elem.appendChild(foreground);
                if (!mxUtils.isTrue(style, Constants.STYLE_INSTANTIATE, false)) {
                    inset = (int) Math.round(12 * scale);
                    Element outer = document.createElement("ellipse");
                    outer.setAttribute("fill", "none");
                    outer.setAttribute("stroke", strokeColor);
                    outer.setAttribute("stroke-width", String.valueOf(strokeWidth));
                    outer.setAttribute("cx", String.valueOf(x + w / 2));
                    outer.setAttribute("cy", String.valueOf(y + h / 2));
                    outer.setAttribute("rx", String.valueOf(w / 2 - inset));
                    outer.setAttribute("ry", String.valueOf(h / 2 - inset));
                    elem.appendChild(outer);
                }
                int px = Math.round(x + w / 2);
                int py = (int) Math.round(y + w * 0.34);
                int p2x = (int) Math.round(x + w * 0.65);
                int p2y = (int) Math.round(y + w * 0.45);
                int p3x = (int) Math.round(x + w * 0.6);
                int p3y = (int) Math.round(y + w * 0.63);
                int p4x = (int) Math.round(x + w * 0.4);
                int p5x = (int) Math.round(x + w * 0.35);
                Element inner = document.createElement("path");
                d = " M " + px + " " + py + " L " + p2x + " " + p2y + " L " + p3x + " " + p3y + " L " + p4x + " " + p3y + " L " + p5x + " " + p2y;
                inner.setAttribute("d", d + " Z");
                inner.setAttribute("fill", "none");
                inner.setAttribute("stroke", strokeColor);
                inner.setAttribute("stroke-width", String.valueOf(strokeWidth));
                elem.appendChild(inner);
            } else if (type.equals(Constants.GATEWAY_STYLE_PARALLEL)) {
                int px = (int) Math.round(x + w * 0.3);
                int py = (int) Math.round(y + w * 0.3);
                int p2x = (int) Math.round(x + w * 0.5);
                int p2y = (int) Math.round(y + w * 0.5);
                int p3x = (int) Math.round(x + w * 0.7);
                int p3y = (int) Math.round(y + w * 0.7);
                Element outer = document.createElement("path");
                d = " M " + px + " " + p2y + " L " + p3x + " " + p2y + " M " + p2x + " " + py + " L " + p2x + " " + p3y;
                outer.setAttribute("d", d);
                outer.setAttribute("fill", "none");
                outer.setAttribute("stroke", strokeColor);
                outer.setAttribute("stroke-width", String.valueOf(strokeWidth * 5));
                elem.appendChild(outer);
                if (mxUtils.isTrue(style, Constants.STYLE_INSTANTIATE, false)) {
                    inset = (int) Math.round(9 * scale);
                    Element foreground = document.createElement("ellipse");
                    foreground.setAttribute("fill", "none");
                    foreground.setAttribute("stroke", strokeColor);
                    foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));
                    foreground.setAttribute("cx", String.valueOf(x + w / 2));
                    foreground.setAttribute("cy", String.valueOf(y + h / 2));
                    foreground.setAttribute("rx", String.valueOf(w / 2 - inset));
                    foreground.setAttribute("ry", String.valueOf(h / 2 - inset));
                    elem.appendChild(foreground);
                    Element inner = document.createElement("path");
                    d = " M " + px + " " + p2y + " L " + p3x + " " + p2y + " M " + p2x + " " + py + " L " + p2x + " " + p3y;
                    inner.setAttribute("d", d);
                    inner.setAttribute("fill", fillColor);
                    inner.setAttribute("stroke", strokeColor);
                    inner.setAttribute("stroke-width", String.valueOf(strokeWidth * 2));
                    elem.appendChild(inner);
                }
            } else if (type.equals(Constants.GATEWAY_STYLE_COMPLEX)) {
                int px = (int) Math.round(x + w * 0.7);
                int py = (int) Math.round(y + w * 0.3);
                int p2x = (int) Math.round(x + w * 0.3);
                int p2y = (int) Math.round(y + w * 0.7);
                int p3x = (int) Math.round(x + w * 0.2);
                int p3y = (int) Math.round(y + w * 0.8);
                int p4x = (int) Math.round(x + w * 0.8);
                int p4y = (int) Math.round(y + w * 0.5);
                int p5x = (int) Math.round(x + w * 0.5);
                int p5y = (int) Math.round(y + w * 0.2);
                Element inner = document.createElement("path");
                d = " M " + px + " " + py + " L " + p2x + " " + p2y + " M " + p2x + " " + py + " L " + px + " " + p2y + " M " + p3x + " " + p4y + " L " + p4x + " " + p4y + " M " + p5x + " " + p5y + " L " + p5x + " " + p3y;
                inner.setAttribute("d", d);
                inner.setAttribute("fill", "none");
                inner.setAttribute("stroke", strokeColor);
                inner.setAttribute("stroke-width", String.valueOf(strokeWidth * 4));
                elem.appendChild(inner);
            }
        } else if (shape.equals(Constants.SHAPE_EVENT)) {
            elem = document.createElement("g");
            background = document.createElement("ellipse");
            background.setAttribute("cx", String.valueOf(x + w / 2));
            background.setAttribute("cy", String.valueOf(y + h / 2));
            background.setAttribute("rx", String.valueOf(w / 2));
            background.setAttribute("ry", String.valueOf(h / 2));
            elem.appendChild(background);
            if (type.equals(Constants.EVENT_STYLE_INTERMEDIATE)) {
                Element foreground = document.createElement("ellipse");
                foreground.setAttribute("fill", "none");
                foreground.setAttribute("stroke", strokeColor);
                foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));
                foreground.setAttribute("cx", String.valueOf(x + w / 2));
                foreground.setAttribute("cy", String.valueOf(y + h / 2));
                foreground.setAttribute("rx", String.valueOf(w / 2 - inset));
                foreground.setAttribute("ry", String.valueOf(h / 2 - inset));
                elem.appendChild(foreground);
            }
            if (img != null) {
                imgWidth = 22 * scale;
                imgHeight = 22 * scale;
                imageBounds.setRect(x + (w - imgWidth) / 2, y + (h - imgHeight) / 2, imgWidth, imgHeight);
                Element imageElement = createImageElement(imageBounds.getX(), imageBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight(), img, false, false, false, isEmbedded());
                elem.appendChild(imageElement);
            }
        } else if (shape.equals(Constants.SHAPE_CONVERSATION_NODE)) {
            elem = document.createElement("g");
            background = document.createElement("path");
            String d = "M " + (x + 0.25 * w) + " " + y + " L " + (x + 0.75 * w) + " " + y + " L " + (x + w) + " " + (y + 0.5 * h) + " L " + (x + 0.75 * w) + " " + (y + h) + " L " + (x + 0.25 * w) + " " + (y + h) + " L " + x + " " + (y + 0.5 * h);
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);
            if (img != null) {
                imageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth, imgHeight);
                Element imageElement = createImageElement(imageBounds.getX(), imageBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight(), img, false, false, false, isEmbedded());
                elem.appendChild(imageElement);
            }
        } else if (shape.equals(Constants.SHAPE_DATAOBJECT)) {
            elem = document.createElement("g");
            background = document.createElement("path");
            double dy = Math.min(40, Math.floor(w / 3));
            String d = " M " + x + " " + y + " L " + x + " " + (y + h) + " L " + (x + w) + " " + (y + h) + " L " + (x + w) + " " + (y + dy) + " L " + (x + w - dy) + " " + y;
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);
            Element foreground = document.createElement("path");
            d = " M " + (x + w - dy) + " " + y + " L " + (x + w - dy) + " " + (y + dy) + " L " + (x + w) + " " + (y + dy);
            foreground.setAttribute("d", d);
            foreground.setAttribute("fill", "none");
            foreground.setAttribute("stroke", strokeColor);
            foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));
            elem.appendChild(foreground);
            if (img != null) {
                imageBounds.setRect(x + (w - imgWidth) / 4, y, imgWidth, imgHeight);
                Element imageElement = createImageElement(imageBounds.getX(), imageBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight(), img, false, false, false, isEmbedded());
                elem.appendChild(imageElement);
            }
        } else if (shape.equals(Constants.SHAPE_DATASTORE)) {
            elem = document.createElement("g");
            background = document.createElement("path");
            double dy = Math.min(40, Math.floor(h / 5));
            String d = " M " + x + " " + (y + dy) + " C " + x + " " + (y - dy / 3) + " " + (x + w) + " " + (y - dy / 3) + " " + (x + w) + " " + (y + dy) + " L " + (x + w) + " " + (y + h - dy) + " C " + (x + w) + " " + (y + h + dy / 3) + " " + x + " " + (y + h + dy / 3) + " " + x + " " + (y + h - dy);
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);
            Element foreground = document.createElement("path");
            d = "M " + x + " " + (y + dy) + " C " + x + " " + (y + 2 * dy) + " " + (x + w) + " " + (y + 2 * dy) + " " + (x + w) + " " + (y + dy) + " " + "M " + x + " " + (y + 1.5 * dy) + " C " + x + " " + (y + 2.5 * dy) + " " + (x + w) + " " + (y + 2.5 * dy) + " " + (x + w) + " " + (y + 1.5 * dy) + " " + "M " + x + " " + (y + 2 * dy) + " C " + x + " " + (y + 3 * dy) + " " + (x + w) + " " + (y + 3 * dy) + " " + (x + w) + " " + (y + 2 * dy);
            foreground.setAttribute("d", d);
            foreground.setAttribute("fill", "none");
            foreground.setAttribute("stroke", strokeColor);
            foreground.setAttribute("stroke-width", String.valueOf(strokeWidth));
            elem.appendChild(foreground);
        } else if (shape.equals(Constants.SHAPE_ANNOTATION)) {
            double a = 20 * scale;
            elem = document.createElement("g");
            background = document.createElement("path");
            String d = "M " + x + " " + y + " L " + x + " " + (y + h) + " L " + (x + w) + " " + (y + h) + " L " + (x + w) + " " + y;
            background.setAttribute("d", d + " Z");
            elem.appendChild(background);
            Element foreground = document.createElement("path");
            d = "M " + (x + a) + " " + y + " L " + x + " " + y + " L " + x + " " + (y + h) + " L " + (x + a) + " " + (y + h);
            foreground.setAttribute("fill", "none");
            foreground.setAttribute("stroke", strokeColor);
            foreground.setAttribute("stroke-width", String.valueOf(strokeWidth * 3));
            foreground.setAttribute("d", d);
            elem.appendChild(foreground);
        } else if (shape.equals(mxConstants.SHAPE_IMAGE)) {
            if (img != null) {
                boolean flipH = mxUtils.isTrue(style, mxConstants.STYLE_IMAGE_FLIPH, false);
                boolean flipV = mxUtils.isTrue(style, mxConstants.STYLE_IMAGE_FLIPV, false);
                elem = createImageElement(x, y, w, h, img, PRESERVE_IMAGE_ASPECT, flipH, flipV, isEmbedded());
            }
        } else {
            background = document.createElement("rect");
            elem = background;
            elem.setAttribute("x", String.valueOf(x));
            elem.setAttribute("y", String.valueOf(y));
            elem.setAttribute("width", String.valueOf(w));
            elem.setAttribute("height", String.valueOf(h));
            if (mxUtils.isTrue(style, mxConstants.STYLE_ROUNDED, false) || mxUtils.isTrue(style, Constants.STYLE_BORDER, false)) {
                String r = "8.25";
                elem.setAttribute("rx", r);
                elem.setAttribute("ry", r);
            }
            if (shape.equals(Constants.SHAPE_PARTICIPANT_BAND) && mxUtils.isTrue(style, Constants.STYLE_BORDER, false)) {
                elem.setAttribute("height", String.valueOf(h + h / 2));
                String position = mxUtils.getString(style, Constants.STYLE_POSITION, "top");
                if (!position.equals("top")) {
                    elem.setAttribute("y", String.valueOf(y - h / 2));
                }
                if (mxUtils.isTrue(style, Constants.ACTIVITY_STYLE_LOOP_MI, false)) {
                    elem = document.createElement("g");
                    elem.appendChild(background);
                    Element imageElement = createImageElement(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth, imgHeight, img, false, false, false, isEmbedded());
                    elem.appendChild(imageElement);
                }
            } else if (shape.equals(Constants.SHAPE_TASK) || shape.equals(Constants.SHAPE_SUBPROCESS)) {
                elem = document.createElement("g");
                elem.appendChild(background);
                mxRectangle loopImageBounds = new mxRectangle();
                mxRectangle compensationImageBounds = new mxRectangle();
                mxRectangle adHocImageBounds = new mxRectangle();
                if (shape.equals(Constants.SHAPE_TASK)) {
                    if (mxUtils.isTrue(style, Constants.STYLE_INSTANTIATE, false)) {
                        Element instantiate = document.createElement("ellipse");
                        instantiate.setAttribute("fill", "none");
                        instantiate.setAttribute("stroke", strokeColor);
                        instantiate.setAttribute("stroke-width", String.valueOf(strokeWidth));
                        instantiate.setAttribute("cx", String.valueOf(13));
                        instantiate.setAttribute("cy", String.valueOf(11));
                        instantiate.setAttribute("rx", String.valueOf(10));
                        instantiate.setAttribute("ry", String.valueOf(10));
                        elem.appendChild(instantiate);
                    }
                    if (img != null) {
                        imageBounds.setRect(x + spacing, y + spacing - 2, imgWidth, imgHeight);
                    }
                    if (!loop.equals(" ") || !compensation.equals(" ")) {
                        if (!loop.equals(" ") && !compensation.equals(" ")) {
                            loopImageBounds.setRect(x + (w / 2 - imgWidth), y + h - imgHeight, imgWidth, imgHeight);
                            compensationImageBounds.setRect(x + w / 2, y + h - imgHeight, imgWidth, imgHeight);
                        } else {
                            loopImageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth, imgHeight);
                            compensationImageBounds = loopImageBounds;
                        }
                    }
                } else if (shape.equals(Constants.SHAPE_SUBPROCESS)) {
                    imgWidth = 32 * scale;
                    if (type.equals(Constants.SUBPROCESS_STYLE_TRANSACTION)) {
                        Element innerRect = document.createElement("rect");
                        innerRect.setAttribute("x", String.valueOf(x + inset));
                        innerRect.setAttribute("y", String.valueOf(y + inset));
                        innerRect.setAttribute("width", String.valueOf(w - 2 * inset));
                        innerRect.setAttribute("height", String.valueOf(h - 2 * inset));
                        innerRect.setAttribute("rx", "8");
                        innerRect.setAttribute("ry", "8");
                        innerRect.setAttribute("fill", "none");
                        innerRect.setAttribute("stroke", strokeColor);
                        innerRect.setAttribute("stroke-width", String.valueOf(strokeWidth));
                        elem.appendChild(innerRect);
                    }
                    if (!mxUtils.isTrue(style, Constants.STYLE_CALL, false) && (w > Constants.FOLDED_SUBPROCESS_WIDTH * scale + 1)) {
                        if (type.equals(Constants.SUBPROCESS_STYLE_ADHOC)) {
                            if (!loop.equals(" ") || !compensation.equals(" ")) {
                                if (!loop.equals(" ") && !compensation.equals(" ")) {
                                    loopImageBounds.setRect(x + (2 * w - 3 * imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds.setRect(loopImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    adHocImageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                } else {
                                    loopImageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds = loopImageBounds;
                                    adHocImageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                }
                            } else {
                                adHocImageBounds.setRect(x + (2 * w - imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                            }
                        } else {
                            if (!loop.equals(" ") || !compensation.equals(" ")) {
                                if (!loop.equals(" ") && !compensation.equals(" ")) {
                                    loopImageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds.setRect(loopImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                } else {
                                    loopImageBounds.setRect(x + (2 * w - imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds = loopImageBounds;
                                }
                            }
                        }
                    } else {
                        if (type.equals(Constants.SUBPROCESS_STYLE_ADHOC)) {
                            if (!loop.equals(" ") || !compensation.equals(" ")) {
                                if (!loop.equals(" ") && !compensation.equals(" ")) {
                                    loopImageBounds.setRect(x + (w / 2 - imgWidth), y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds.setRect(loopImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    imageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    adHocImageBounds.setRect(imageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                } else {
                                    loopImageBounds.setRect(x + (2 * w - 3 * imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds = loopImageBounds;
                                    imageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    adHocImageBounds.setRect(imageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                }
                            } else {
                                imageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                adHocImageBounds.setRect(imageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                            }
                        } else {
                            if (!loop.equals(" ") || !compensation.equals(" ")) {
                                if (!loop.equals(" ") && !compensation.equals(" ")) {
                                    loopImageBounds.setRect(x + (2 * w - 3 * imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds.setRect(loopImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    imageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                } else {
                                    loopImageBounds.setRect(x + (w - imgWidth) / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                    compensationImageBounds = loopImageBounds;
                                    imageBounds.setRect(compensationImageBounds.getX() + imgWidth / 2, y + h - imgHeight, imgWidth / 2, imgHeight);
                                }
                            } else {
                                imageBounds.setRect(x + (2 * w - imgWidth) / 4, y + h - imgHeight, imgWidth / 2, imgHeight);
                            }
                        }
                    }
                }
                if (img != null) {
                    Element imageElement = createImageElement(imageBounds.getX(), imageBounds.getY(), imageBounds.getWidth(), imageBounds.getHeight(), img, false, false, false, isEmbedded());
                    elem.appendChild(imageElement);
                }
                if (!loop.equals(" ")) {
                    Element loopImageElement = createImageElement(loopImageBounds.getX(), loopImageBounds.getY(), loopImageBounds.getWidth(), loopImageBounds.getHeight(), loop, false, false, false, isEmbedded());
                    elem.appendChild(loopImageElement);
                }
                if (!compensation.equals(" ")) {
                    Element compensationImageElement = createImageElement(compensationImageBounds.getX(), compensationImageBounds.getY(), compensationImageBounds.getWidth(), compensationImageBounds.getHeight(), compensation, false, false, false, isEmbedded());
                    elem.appendChild(compensationImageElement);
                }
                if (!adHoc.equals(" ")) {
                    Element adHocImageElement = createImageElement(adHocImageBounds.getX(), adHocImageBounds.getY(), adHocImageBounds.getWidth(), adHocImageBounds.getHeight(), adHoc, false, false, false, isEmbedded());
                    elem.appendChild(adHocImageElement);
                }
            } else if (shape.equals(mxConstants.SHAPE_LABEL)) {
                if (mxUtils.isTrue(style, mxConstants.STYLE_GLASS, false)) {
                    double size = 0.4;
                    Element glassOverlay = document.createElement("path");
                    glassOverlay.setAttribute("fill", "url(#" + getGlassGradientElement().getAttribute("id") + ")");
                    String d = "m " + (x - strokeWidth) + "," + (y - strokeWidth) + " L " + (x - strokeWidth) + "," + (y + h * size) + " Q " + (x + w * 0.5) + "," + (y + h * 0.7) + " " + (x + w + strokeWidth) + "," + (y + h * size) + " L " + (x + w + strokeWidth) + "," + (y - strokeWidth) + " z";
                    glassOverlay.setAttribute("stroke-width", String.valueOf(strokeWidth / 2));
                    glassOverlay.setAttribute("d", d);
                    elem.appendChild(glassOverlay);
                }
            }
        }
        double rotation = mxUtils.getDouble(style, mxConstants.STYLE_ROTATION);
        int cx = x + w / 2;
        int cy = y + h / 2;
        Element bg = background;
        if (bg == null) {
            bg = elem;
        }
        if (!bg.getNodeName().equalsIgnoreCase("use") && !bg.getNodeName().equalsIgnoreCase("image")) {
            if (!fillColor.equalsIgnoreCase("none") && !gradientColor.equalsIgnoreCase("none")) {
                String direction = mxUtils.getString(style, mxConstants.STYLE_GRADIENT_DIRECTION);
                Element gradient = getGradientElement(fillColor, gradientColor, direction);
                if (gradient != null) {
                    bg.setAttribute("fill", "url(#" + gradient.getAttribute("id") + ")");
                }
            } else {
                bg.setAttribute("fill", fillColor);
            }
            bg.setAttribute("stroke", strokeColor);
            bg.setAttribute("stroke-width", String.valueOf(strokeWidth));
            Element shadowElement = null;
            if (mxUtils.isTrue(style, mxConstants.STYLE_SHADOW, false) && !shape.equals(Constants.SHAPE_PARTICIPANT_BAND) && !fillColor.equals("none")) {
                shadowElement = (Element) bg.cloneNode(true);
                shadowElement.setAttribute("transform", mxConstants.SVG_SHADOWTRANSFORM);
                shadowElement.setAttribute("fill", mxConstants.W3C_SHADOWCOLOR);
                shadowElement.setAttribute("stroke", mxConstants.W3C_SHADOWCOLOR);
                shadowElement.setAttribute("stroke-width", String.valueOf(strokeWidth));
                if (rotation != 0) {
                    shadowElement.setAttribute("transform", "rotate(" + rotation + "," + cx + "," + cy + ") " + mxConstants.SVG_SHADOWTRANSFORM);
                }
                if (opacity != 100) {
                    String value = String.valueOf(opacity / 100);
                    shadowElement.setAttribute("fill-opacity", value);
                    shadowElement.setAttribute("stroke-opacity", value);
                }
                appendSvgElement(shadowElement);
            }
        }
        if (rotation != 0) {
            elem.setAttribute("transform", elem.getAttribute("transform") + " rotate(" + rotation + "," + cx + "," + cy + ")");
        }
        if (opacity != 100) {
            String value = String.valueOf(opacity / 100);
            elem.setAttribute("fill-opacity", value);
            elem.setAttribute("stroke-opacity", value);
        }
        if (mxUtils.isTrue(style, mxConstants.STYLE_DASHED) || type.equals(Constants.SUBPROCESS_STYLE_EVENT)) {
            String pattern = mxUtils.getString(style, mxConstants.STYLE_DASH_PATTERN, "3, 3");
            elem.setAttribute("stroke-dasharray", pattern);
        }
        appendSvgElement(elem);
        return elem;
    }

    /**
	 * Draws the given lines as segments between all points of the given list of mxPoints.
	 * 
	 * @param pts
	 *            List of points that define the line.
	 * @param style
	 *            Style to be used for painting the line.
	 */
    public Element drawLine(List<mxPoint> pts, Map<String, Object> style) {
        Element group = document.createElement("g");
        Element path = document.createElement("path");
        boolean rounded = mxUtils.isTrue(style, mxConstants.STYLE_ROUNDED, false);
        String strokeColor = mxUtils.getString(style, mxConstants.STYLE_STROKECOLOR);
        if (strokeColor.startsWith("ff")) {
            strokeColor = "#" + strokeColor.substring(2);
        }
        float tmpStroke = (mxUtils.getFloat(style, mxConstants.STYLE_STROKEWIDTH, 1));
        float strokeWidth = (float) (tmpStroke * scale);
        if (strokeColor != null && strokeWidth > 0) {
            Object marker = style.get(mxConstants.STYLE_STARTARROW);
            mxPoint pt = pts.get(1);
            mxPoint p0 = pts.get(0);
            mxPoint offset = null;
            if (marker != null) {
                float size = (mxUtils.getFloat(style, mxConstants.STYLE_STARTSIZE, mxConstants.DEFAULT_MARKERSIZE));
                offset = drawMarker(group, marker, pt, p0, size, tmpStroke, strokeColor);
            } else {
                double dx = pt.getX() - p0.getX();
                double dy = pt.getY() - p0.getY();
                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                double nx = dx * strokeWidth / dist;
                double ny = dy * strokeWidth / dist;
                offset = new mxPoint(nx / 2, ny / 2);
            }
            if (offset != null) {
                p0 = (mxPoint) p0.clone();
                p0.setX(p0.getX() + offset.getX());
                p0.setY(p0.getY() + offset.getY());
                offset = null;
            }
            marker = style.get(mxConstants.STYLE_ENDARROW);
            pt = pts.get(pts.size() - 2);
            mxPoint pe = pts.get(pts.size() - 1);
            if (marker != null) {
                float size = (mxUtils.getFloat(style, mxConstants.STYLE_ENDSIZE, mxConstants.DEFAULT_MARKERSIZE));
                offset = drawMarker(group, marker, pt, pe, size, tmpStroke, strokeColor);
            } else {
                double dx = pt.getX() - p0.getX();
                double dy = pt.getY() - p0.getY();
                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                double nx = dx * strokeWidth / dist;
                double ny = dy * strokeWidth / dist;
                offset = new mxPoint(nx / 2, ny / 2);
            }
            if (offset != null) {
                pe = (mxPoint) pe.clone();
                pe.setX(pe.getX() + offset.getX());
                pe.setY(pe.getY() + offset.getY());
                offset = null;
            }
            double arcSize = mxConstants.LINE_ARCSIZE * scale;
            pt = p0;
            String d = "M " + pt.getX() + " " + pt.getY();
            for (int i = 1; i < pts.size() - 1; i++) {
                mxPoint tmp = pts.get(i);
                double dx = pt.getX() - tmp.getX();
                double dy = pt.getY() - tmp.getY();
                if ((rounded && i < pts.size() - 1) && (dx != 0 || dy != 0)) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double nx1 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny1 = dy * Math.min(arcSize, dist / 2) / dist;
                    double x1 = tmp.getX() + nx1;
                    double y1 = tmp.getY() + ny1;
                    d += " L " + x1 + " " + y1;
                    mxPoint next = pts.get(i + 1);
                    dx = next.getX() - tmp.getX();
                    dy = next.getY() - tmp.getY();
                    dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                    double nx2 = dx * Math.min(arcSize, dist / 2) / dist;
                    double ny2 = dy * Math.min(arcSize, dist / 2) / dist;
                    double x2 = tmp.getX() + nx2;
                    double y2 = tmp.getY() + ny2;
                    d += " Q " + tmp.getX() + " " + tmp.getY() + " " + x2 + " " + y2;
                    tmp = new mxPoint(x2, y2);
                } else {
                    d += " L " + tmp.getX() + " " + tmp.getY();
                }
                pt = tmp;
            }
            d += " L " + pe.getX() + " " + pe.getY();
            path.setAttribute("d", d);
            path.setAttribute("stroke", strokeColor);
            path.setAttribute("fill", "none");
            path.setAttribute("stroke-width", String.valueOf(strokeWidth));
            if (mxUtils.isTrue(style, mxConstants.STYLE_DASHED)) {
                String pattern = mxUtils.getString(style, mxConstants.STYLE_DASH_PATTERN, "3, 3");
                path.setAttribute("stroke-dasharray", pattern);
            }
            group.appendChild(path);
            appendSvgElement(group);
        }
        return group;
    }

    /**
	 * Draws the specified marker as a child path in the given parent.
	 */
    public mxPoint drawMarker(Element parent, Object type, mxPoint p0, mxPoint pe, float size, float strokeWidth, String color) {
        mxPoint offset = null;
        double dx = pe.getX() - p0.getX();
        double dy = pe.getY() - p0.getY();
        double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        double absSize = size * scale;
        double nx = dx * absSize / dist;
        double ny = dy * absSize / dist;
        pe = (mxPoint) pe.clone();
        pe.setX(pe.getX() - nx * strokeWidth / (2 * size));
        pe.setY(pe.getY() - ny * strokeWidth / (2 * size));
        nx *= 0.5 + strokeWidth / 2;
        ny *= 0.5 + strokeWidth / 2;
        Element path = document.createElement("path");
        path.setAttribute("stroke-width", String.valueOf(strokeWidth * scale));
        path.setAttribute("stroke", color);
        path.setAttribute("fill", "none");
        String d = null;
        if (type.equals(mxConstants.ARROW_CLASSIC)) {
            d = "M " + pe.getX() + " " + pe.getY() + " L " + (pe.getX() - nx - ny / 2) + " " + (pe.getY() - ny + nx / 2) + " L " + (pe.getX() - nx * 3 / 4) + " " + (pe.getY() - ny * 3 / 4) + " L " + (pe.getX() + ny / 2 - nx) + " " + (pe.getY() - ny - nx / 2) + " z";
            path.setAttribute("fill", color);
        } else if (type.equals(Constants.ARROW_STYLE_OPEN_BLOCK)) {
            d = "M " + pe.getX() + " " + pe.getY() + " L " + (pe.getX() - nx - ny / 2) + " " + (pe.getY() - ny + nx / 2) + " L " + (pe.getX() + ny / 2 - nx) + " " + (pe.getY() - ny - nx / 2) + " z";
        } else if (type.equals(mxConstants.ARROW_OPEN)) {
            nx *= 1.2;
            ny *= 1.2;
            d = "M " + (pe.getX() - nx - ny / 2) + " " + (pe.getY() - ny + nx / 2) + " L " + (pe.getX() - nx / 6) + " " + (pe.getY() - ny / 6) + " L " + (pe.getX() + ny / 2 - nx) + " " + (pe.getY() - ny - nx / 2) + " M " + pe.getX() + " " + pe.getY();
        } else if (type.equals(Constants.ARROW_STYLE_OPEN_OVAL)) {
            nx *= 1.2;
            ny *= 1.2;
            absSize *= 1.2;
            d = "M " + (pe.getX() - ny / 2) + " " + (pe.getY() + nx / 2) + " a " + (absSize / 2) + " " + (absSize / 2) + " 0  1,1 " + (nx / 8) + " " + (ny / 8) + " z";
        } else if (type.equals(Constants.ARROW_STYLE_OPEN_DIAMOND)) {
            nx *= 1.8;
            ny *= 1.8;
            int a = (int) (8 * scale);
            if (nx > 0 || ny > 0) {
                a = (int) (-8 * scale);
            }
            if (ny == 0) {
                d = "M " + (pe.getX() + nx / 2 - a) + " " + pe.getY() + " L " + (pe.getX() - a) + " " + (pe.getY() + nx / 3) + " L " + (pe.getX() - nx / 2 - a) + " " + pe.getY() + " L " + (pe.getX() + ny / 2 - a) + " " + (pe.getY() - nx / 3) + " z";
            } else {
                d = "M " + (pe.getX() + nx / 3) + " " + (pe.getY() - a) + " L " + pe.getX() + " " + (pe.getY() + nx / 2 - a) + " L " + (pe.getX() - nx / 3) + " " + (pe.getY() - a) + " L " + pe.getX() + " " + (pe.getY() - nx / 2 - a) + " z";
            }
        } else if (type.equals(Constants.ARROW_STYLE_SLASH)) {
            nx *= 1.8;
            ny *= 1.8;
            int a = (int) (-8 * scale);
            if (nx > 0 || ny > 0) {
                a = (int) (8 * scale);
            }
            if (ny == 0) {
                d = "M " + (pe.getX() - a) + " " + (pe.getY() + nx / 3) + " L " + (pe.getX() - 2 * a) + " " + (pe.getY() - nx / 3) + " L " + (pe.getX() - a) + " " + (pe.getY() + nx / 3) + " L " + (pe.getX() - 2 * a) + " " + (pe.getY() - nx / 3) + " z";
            } else {
                d = "M " + (pe.getX() + ny / 3) + " " + (pe.getY() - a) + " L " + (pe.getX() - ny / 3) + " " + (pe.getY() - 2 * a) + " L " + (pe.getX() + ny / 3) + " " + (pe.getY() - a) + " L " + (pe.getX() - ny / 3) + " " + (pe.getY() - 2 * a) + " z";
            }
        }
        if (d != null) {
            path.setAttribute("d", d);
            parent.appendChild(path);
        }
        return offset;
    }

    /**
	 * Draws the specified text either using drawHtmlString or using drawString.
	 * 
	 * @param text
	 *            Text to be painted.
	 * @param x
	 *            X-coordinate of the text.
	 * @param y
	 *            Y-coordinate of the text.
	 * @param w
	 *            Width of the text.
	 * @param h
	 *            Height of the text.
	 * @param style
	 *            Style to be used for painting the text.
	 */
    public Object drawText(String text, int x, int y, int w, int h, Map<String, Object> style) {
        Element elem = null;
        String fontColor = mxUtils.getString(style, mxConstants.STYLE_FONTCOLOR, "black");
        if (fontColor.startsWith("ff")) {
            fontColor = "#" + fontColor.substring(2);
        }
        String fontFamily = mxUtils.getString(style, mxConstants.STYLE_FONTFAMILY, mxConstants.DEFAULT_FONTFAMILIES);
        int fontSize = (int) (mxUtils.getInt(style, mxConstants.STYLE_FONTSIZE, mxConstants.DEFAULT_FONTSIZE) * scale);
        if (text != null && text.length() > 0) {
            float strokeWidth = (float) (mxUtils.getFloat(style, mxConstants.STYLE_STROKEWIDTH, 1) * scale);
            float opacity = mxUtils.getFloat(style, mxConstants.STYLE_TEXT_OPACITY, 100);
            String bg = mxUtils.getString(style, mxConstants.STYLE_LABEL_BACKGROUNDCOLOR);
            String border = mxUtils.getString(style, mxConstants.STYLE_LABEL_BORDERCOLOR);
            String transform = null;
            if (!mxUtils.isTrue(style, mxConstants.STYLE_HORIZONTAL, true)) {
                double cx = x + w / 2;
                double cy = y + h / 2;
                transform = "rotate(270 " + cx + " " + cy + ")";
            }
            if (bg != null || border != null) {
                Element background = document.createElement("rect");
                background.setAttribute("x", String.valueOf(x));
                background.setAttribute("y", String.valueOf(y));
                background.setAttribute("width", String.valueOf(w));
                background.setAttribute("height", String.valueOf(h));
                if (bg != null) {
                    if (bg.startsWith("ff")) {
                        bg = "#" + bg.substring(2);
                    }
                    background.setAttribute("fill", bg);
                } else {
                    background.setAttribute("fill", "none");
                }
                if (border != null) {
                    if (border.startsWith("ff")) {
                        border = "#" + border.substring(2);
                    }
                    background.setAttribute("stroke", border);
                } else {
                    background.setAttribute("stroke", "none");
                }
                background.setAttribute("stroke-width", String.valueOf(strokeWidth));
                if (opacity != 100) {
                    String value = String.valueOf(opacity / 100);
                    background.setAttribute("fill-opacity", value);
                    background.setAttribute("stroke-opacity", value);
                }
                if (transform != null) {
                    background.setAttribute("transform", transform);
                }
                appendSvgElement(background);
            }
            String align = mxUtils.getString(style, mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
            String anchor = "start";
            if (align.equals(mxConstants.ALIGN_RIGHT)) {
                anchor = "end";
                x += w - mxConstants.LABEL_INSET * scale;
            } else if (align.equals(mxConstants.ALIGN_CENTER)) {
                anchor = "middle";
                x += w / 2;
            } else {
                x += mxConstants.LABEL_INSET * scale;
            }
            int fontStyle = mxUtils.getInt(style, mxConstants.STYLE_FONTSTYLE);
            String weight = ((fontStyle & mxConstants.FONT_BOLD) == mxConstants.FONT_BOLD) ? "bold" : "normal";
            String uline = ((fontStyle & mxConstants.FONT_UNDERLINE) == mxConstants.FONT_UNDERLINE) ? "underline" : "none";
            String[] lines = text.split("\n");
            y += fontSize + (h - lines.length * (fontSize + mxConstants.LINESPACING)) / 2 - 2;
            for (int i = 0; i < lines.length; i++) {
                elem = document.createElement("text");
                elem.setAttribute("x", String.valueOf(x));
                elem.setAttribute("y", String.valueOf(y));
                elem.setAttribute("font-weight", weight);
                elem.setAttribute("font-decoration", uline);
                if ((fontStyle & mxConstants.FONT_ITALIC) == mxConstants.FONT_ITALIC) {
                    elem.setAttribute("font-style", "italic");
                }
                elem.setAttribute("font-size", String.valueOf(fontSize));
                elem.setAttribute("font-family", fontFamily);
                elem.setAttribute("fill", fontColor);
                if (opacity != 100) {
                    String value = String.valueOf(opacity / 100);
                    elem.setAttribute("fill-opacity", value);
                    elem.setAttribute("stroke-opacity", value);
                }
                elem.setAttribute("text-anchor", anchor);
                if (transform != null) {
                    elem.setAttribute("transform", transform);
                }
                elem.appendChild(document.createTextNode(lines[i]));
                appendSvgElement(elem);
                y += fontSize + mxConstants.LINESPACING;
            }
        }
        return elem;
    }
}
