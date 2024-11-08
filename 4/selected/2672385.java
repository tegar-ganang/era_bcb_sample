package rendava;

import java.awt.*;

/**
 * Used for displaying HTML content when a <font> tag is active. 
 * Also can be used for displaying plain text using default values. 
 * 
 * @author Ben Gomm
 * @version 0.92
 *
 */
public class DisplayFont extends SimpleTag {

    private String fontFace = "Courier";

    private String fontColour;

    private Color fColor = new Color(0, 0, 0);

    private int fontSize = 12;

    private String text;

    private String oneChar = "w";

    private int fontStyle1 = 0;

    private int fontStyle2 = 0;

    int charsFit;

    int renderPoint = 0;

    int renderSpace = 0;

    String nextLine = null;

    /**
	 * renderInfo writes to the Renderer's graphics to display text with different fonts. 
	 * Font attributes should be set before calling this method if we are not rendering plain text.
	 * @param renderer The renderer that is rendering the page.
	 */
    public void renderInfo(Renderer renderer) {
        System.out.println("!!RENDER INFO CALLED!!");
        Graphics g = renderer.getGraphics();
        g.setFont(new Font(fontFace, fontStyle1 + fontStyle2, fontSize));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(fColor);
        if (renderer.title) {
            System.out.println("Found title tag");
            renderer.getGUI().setTitle("Rendava -  " + text);
            return;
        }
        if (text != null) {
            int tempX = renderer.getX();
            System.out.println("X is " + tempX);
            int stringWidth = fm.stringWidth(text);
            int stringHeight = fm.getHeight();
            int tempRoom = (renderer.getCanvasWidth() - renderer.getX());
            System.out.println("String width is: " + stringWidth);
            System.out.println("Room left is " + tempRoom);
            if ((renderer.getCanvasWidth() - renderer.getX()) > stringWidth) {
                System.out.println("Rendering string straight away as we can fit it in the window");
                System.out.println("String is: " + text);
                g.drawString(text, renderer.getX(), renderer.getY());
                renderer.setX(renderer.getX() + stringWidth);
            } else {
                while (renderPoint < text.length()) {
                    int charsLeft = text.length() - renderPoint;
                    renderSpace = renderer.getCanvasWidth() - renderer.getX();
                    charsFit = Math.round(renderSpace / fm.stringWidth(oneChar));
                    System.out.println("We have room for: " + charsFit);
                    if (charsFit < charsLeft) {
                        System.out.println("Text left to render is larger than space left");
                        int tempXc = renderer.getX();
                        System.out.println("X is " + tempXc);
                        if (text.charAt((renderPoint + charsFit)) == ' ') {
                            nextLine = text.substring(renderPoint, (renderPoint + charsFit));
                            System.out.println("nextLine = " + nextLine);
                            System.out.println("No need for space alterartion");
                            g.drawString(nextLine, renderer.getX(), renderer.getY());
                            renderer.setX(0);
                            renderer.setY(renderer.getY() + stringHeight);
                            renderPoint = renderPoint + charsFit;
                        } else {
                            while (text.charAt((renderPoint + charsFit)) != ' ') {
                                charsFit--;
                            }
                            nextLine = text.substring(renderPoint, (renderPoint + charsFit));
                            System.out.println("nextLine = " + nextLine);
                            System.out.println("Split at space");
                            g.drawString(nextLine, renderer.getX(), renderer.getY());
                            renderer.setX(0);
                            renderer.setY(renderer.getY() + stringHeight);
                            renderPoint = renderPoint + charsFit;
                        }
                    }
                    if (charsFit > charsLeft) {
                        System.out.println("Text left to render is smaller than space left");
                        int tempXb = renderer.getX();
                        System.out.println("X is " + tempXb);
                        nextLine = text.substring(renderPoint, text.length());
                        System.out.println("nextLine = " + nextLine);
                        g.drawString(nextLine, renderer.getX(), renderer.getY());
                        renderPoint = renderPoint + charsFit;
                        int substringWidth = fm.stringWidth(nextLine);
                        renderer.setX(renderer.getX() + substringWidth);
                    }
                }
                int tempXc = renderer.getX();
                System.out.println("X is " + tempXc);
                System.out.println("END OF LOOP REACHED!");
            }
        }
    }

    /**
	 * Returns the current font face.
	 * @return current Font face
	 */
    public String getFontFace() {
        return fontFace;
    }

    /**
	 * Returns the current font size
	 * @return current font size
	 */
    public int getFontsize() {
        return fontSize;
    }

    public String getFontColour() {
        return fontColour;
    }

    public void setFontFace(String fontFace) {
        this.fontFace = fontFace;
    }

    public void setSize(int size) {
        fontSize = size;
        System.out.println("Size set to " + fontSize);
    }

    public void setFontColour(String colour) {
        fontColour = colour;
        System.out.println("Colour set to " + fontColour);
        if (fontColour.equals("red")) {
            fColor = new Color(255, 0, 0);
        }
        if (fontColour.equals("black")) {
            fColor = new Color(0, 0, 0);
        }
        if (fontColour.equals("blue")) {
            fColor = new Color(0, 0, 255);
        }
        if (fontColour.equals("green")) {
            fColor = new Color(0, 255, 0);
        }
    }

    /**
	 * Sets the value of the text to be rendered
	 * @param text A String of characters to be displayed
	 */
    public void setText(String text) {
        this.text = text;
    }

    /**
	 * Sets the font style to either plain (0), bold(1) or italic(2). Additive with setFontStyle2()
	 * @param style
	 */
    public void setFontStyle1(int style) {
        fontStyle1 = style;
    }

    /**
	 * Sets the font style to either plain (0), bold(1) or italic(2) additive with setFontStyle1()
	 * @param style
	 */
    public void setFontStyle2(int style) {
        fontStyle2 = style;
    }

    public String closeTag() {
        return "</font>";
    }

    public void setBold() {
        setFontStyle1(1);
    }

    public void setItalic() {
        setFontStyle2(2);
    }

    /**
	 * Sets the font to plain. This may be unnecessary as plain is default.
	 */
    public void setPlain() {
        setFontStyle1(0);
        setFontStyle2(0);
    }

    public String getText() {
        return text;
    }
}
