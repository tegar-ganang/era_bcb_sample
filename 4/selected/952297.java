package org.ponder.groovy.builders.pdf.itext;

import groovy.lang.GroovyObjectSupport;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.groovy.builder.stack.NodeContext;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * This allows each node or group of nodes to have a particular context for relative area calculations
 * and text placement.
 * 
 * @author elponderador
 */
public class ItextContext extends GroovyObjectSupport implements NodeContext {

    private static final ThreadLocal<ItextContext> context = new ThreadLocal<ItextContext>();

    public static ItextContext getCurrentContext() {
        return context.get();
    }

    protected ItextContext parent;

    protected BaseFont font;

    protected float fontSize;

    protected String fontFamily;

    protected String style;

    protected Color fill = Color.BLACK;

    protected Color stroke = Color.BLACK;

    protected float leading = 1;

    protected boolean translateY = true;

    protected float top = 0;

    protected float left = 0;

    protected float height;

    protected float width;

    protected float horizontalTextSpacing = 5;

    protected float textOffset = 3;

    protected int textAlignment = PdfContentByte.ALIGN_LEFT;

    protected float textRotation = 0;

    protected float currentTextX;

    protected float currentTextY;

    protected int pageNumber = 0;

    protected Document template;

    protected com.lowagie.text.Document document;

    protected PdfWriter writer;

    protected PdfContentByte cb;

    protected Map<String, Object> contextVariables = new HashMap<String, Object>();

    public ItextContext(ItextContext parent) {
        this(parent, parent.top, parent.left, parent.height, parent.width);
    }

    public ItextContext(ItextContext parent, float top, float left, float height, float width) {
        this(parent.template, parent, parent.document, parent.writer, top, left, height, width);
    }

    public ItextContext(Document template, com.lowagie.text.Document document, PdfWriter writer, boolean translateY) {
        this(template, null, document, writer, document.getPageSize().getTop(), document.getPageSize().getLeft(), document.getPageSize().getHeight(), document.getPageSize().getWidth());
        this.translateY = true;
    }

    public ItextContext(Document template, ItextContext parent, com.lowagie.text.Document document, PdfWriter writer, float top, float left, float height, float width) {
        this.template = template;
        this.setFont(template.getFontFamily(), template.getFontSize());
        this.parent = parent;
        this.document = document;
        this.writer = writer;
        this.height = height;
        this.width = width;
        this.top = top;
        this.left = left;
        this.currentTextX = 0;
        this.currentTextY = 30;
        if (this.parent != null) {
            this.cb = parent.cb;
            this.translateY = parent.translateY;
        }
    }

    public PdfContentByte getContentByte() {
        return this.cb;
    }

    public Object set(String name, Object value) {
        this.contextVariables.put(name, value);
        return value;
    }

    public Object get(String name) {
        if (this.getMetaClass().getMetaProperty(name) != null) return this.getProperty(name);
        Object value = this.contextVariables.containsKey(name) ? this.contextVariables.get(name) : (this.parent == null ? null : this.parent.get(name));
        return value == null ? null : value;
    }

    public boolean isSet(String key) {
        return this.contextVariables.containsKey(key);
    }

    public Object remove(String key) {
        return this.contextVariables.remove(key);
    }

    public void applyTemplate(PdfTemplate template, float x, float y, float rotation, float scaleX, float scaleY) {
        if (rotation != 0) {
            double radians = getRadians(rotation);
            this.cb.addTemplate(template, (float) (scaleX != 1 ? (scaleX * Math.cos(radians)) : Math.cos(radians)), (float) (scaleY != 1 ? (scaleY * Math.sin(radians)) : Math.sin(radians)), -(float) (scaleX != 1 ? (scaleX * Math.sin(radians)) : Math.sin(radians)), (float) (scaleY != 1 ? (scaleY * Math.cos(radians)) : Math.cos(radians)), x, y);
        } else if (scaleX != 1 || scaleY != 1) {
            this.cb.addTemplate(template, scaleX, 0, 0, scaleY, x, y);
        } else this.cb.addTemplate(template, x, y);
    }

    public double getRadians(float degrees) {
        return degrees * (Math.PI / 180);
    }

    public PdfTemplate createTemplate(PdfReader reader, int page) {
        return writer.getImportedPage(reader, page);
    }

    public float getTextOffset() {
        return textOffset;
    }

    public void setTextOffset(float textOffset) {
        this.textOffset = textOffset;
    }

    public int getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(int textAlignment) {
        this.textAlignment = textAlignment;
    }

    public float getTextRotation() {
        return textRotation;
    }

    public void setTextRotation(float textRotation) {
        this.textRotation = textRotation;
    }

    public void print(Object data, float x, float y, int alignment, float rotation) {
        cb.beginText();
        cb.setFontAndSize(this.font, this.fontSize);
        cb.showTextAligned(alignment, String.valueOf(data), this.getRealX(x), this.getRealY(y), rotation);
        cb.endText();
    }

    public void print(Object data, boolean appendNewLine) {
        String[] lines = String.valueOf(data).split("\n");
        for (int l = 0; l < lines.length; l++) {
            if (l > 0) this.newLine();
            this.print(data, this.currentTextX + this.textOffset, this.currentTextY, this.textAlignment, this.textRotation);
            this.currentTextX += cb.getEffectiveStringWidth(lines[l], false);
        }
        if (appendNewLine) this.newLine();
    }

    public void newLine() {
        this.currentTextX = this.left;
        this.currentTextY += (this.fontSize + this.horizontalTextSpacing);
    }

    public void setStrokColor(Color color) {
        this.stroke = color;
        this.cb.setColorStroke(color);
    }

    public void setFillColor(Color color) {
        this.fill = color;
        this.cb.setColorFill(color);
    }

    public void stroke(Color color) {
        this.setStrokColor(color);
        this.cb.stroke();
    }

    public void fill(Color color) {
        this.cb.setColorFill(color);
        this.cb.fill();
        this.cb.setColorFill(this.fill);
    }

    public void fillStroke(Color stroke, Color fill) {
        this.setStrokColor(stroke);
        this.cb.setColorFill(fill);
        this.cb.fillStroke();
        this.cb.setColorFill(this.fill);
    }

    public Rectangle getContextArea() {
        return new Rectangle(this.left, this.top + this.height, this.left + this.width, this.top);
    }

    public void rectangle(Rectangle rectangle) {
        this.rectangle(rectangle.getLeft(), rectangle.getTop(), rectangle.getWidth(), rectangle.getHeight());
    }

    public void rectangle(float x, float y, float width, float height) {
        this.cb.rectangle(this.getRealX(x), this.getRealY(y), width, this.getRealHeight(height));
    }

    public void line(float x, float y, float toX, float toY, float dash) {
        if (dash != -1) this.cb.setLineDash(dash, dash);
        this.cb.moveTo(this.getRealX(x), this.getRealY(y));
        this.cb.lineTo(this.getRealX(toX), this.getRealY(toY));
        this.cb.stroke();
    }

    public void newPage() {
        this.document.newPage();
        this.pageNumber++;
        if (this.pageNumber == 1) {
            this.cb = this.writer.getDirectContent();
            this.initialize();
        }
        this.setDefaultFont();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public float getRealX(float x) {
        return this.left + x;
    }

    public float getRealY(float y) {
        return this.translateY ? this.top - y : y;
    }

    public float getRealHeight(float height) {
        return this.translateY ? -height : height;
    }

    public float getTop() {
        return top;
    }

    public float getLeft() {
        return left;
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public BaseFont getFont() {
        return font;
    }

    public void setFont(BaseFont font) {
        this.font = font;
    }

    public float getHorizontalTextSpacing() {
        return horizontalTextSpacing;
    }

    public void setHorizontalTextSpacing(float horizontalTextSpacing) {
        this.horizontalTextSpacing = horizontalTextSpacing;
    }

    public boolean isTranslateY() {
        return translateY;
    }

    public void setTranslateY(boolean translateY) {
        this.translateY = translateY;
    }

    public float getFontSize() {
        return fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFont(String family, float size) {
        if (family == null) {
            family = BaseFont.HELVETICA;
        }
        this.fontFamily = family;
        FontFactory.registerDirectories();
        int fs = Font.NORMAL;
        if (style != null) {
            if (style.contains("bold")) fs = Font.BOLD;
            if (style.contains("italic")) fs |= Font.ITALIC;
            if (style.contains("underline")) fs |= Font.UNDERLINE;
            if (style.contains("strikethrough")) fs |= Font.STRIKETHRU;
        }
        this.font = FontFactory.getFont(this.fontFamily, fontSize, fs).getBaseFont();
        if (this.font == null) {
            try {
                this.font = BaseFont.createFont(this.fontFamily, BaseFont.CP1250, true);
            } catch (DocumentException e) {
                throw ThrowableManagerRegistry.caught(e);
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }
        this.fontSize = size;
        if (this.pageNumber > 0) this.cb.setFontAndSize(font, fontSize);
    }

    public void startContext() {
        this.resumeContext();
    }

    public void resumeContext() {
        this.setDefaultFont();
        if (this.cb != null) this.initialize();
        context.set(this);
    }

    public void stopContext() {
        if (this.parent != null) this.parent.resumeContext();
    }

    protected float charSpace = -1;

    protected float wordSpace = -1;

    private void initialize() {
        if (charSpace == -1) charSpace = this.cb.getCharacterSpacing(); else this.cb.setCharacterSpacing(charSpace);
        if (wordSpace == -1) wordSpace = this.cb.getWordSpacing(); else this.cb.setWordSpacing(wordSpace);
        if (leading == -1) leading = this.cb.getLeading(); else this.cb.setLeading(leading);
    }

    private void setDefaultFont() {
        if (this.pageNumber > 0) cb.setFontAndSize(font, this.fontSize);
    }
}
