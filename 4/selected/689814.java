package org.skycastle.texture;

import java.awt.image.BufferedImage;

/**
 * A small utility class that holds a field and calculates and renders it to a buffer when requested.
 */
public final class FieldRenderer {

    private final Field myField = new FieldImpl(1, 1);

    private final String myRedChannelName;

    private final String myGreenChannelName;

    private final String myBlueChannelName;

    private final String myAlphaChannelName;

    private FieldCalculator myProceduralTexture = null;

    private RectangularAreaParameters myAreaParameters = null;

    /**
     * Creates a new FieldRenderer.
     * Remember to set the procedural texture and the parameters before rendering.
     */
    public FieldRenderer() {
        this(null);
    }

    /**
     * Creates a new FieldRenderer.
     * Remember to set the parameters before rendering.
     *
     * @param proceduralTexture the calculator used to calculate the fields to render.
     */
    public FieldRenderer(final FieldCalculator proceduralTexture) {
        this(proceduralTexture, "red", "green", "blue", "alpha");
    }

    /**
     * Creates a new FieldRenderer.
     * Remember to set the parameters before rendering.
     *
     * @param proceduralTexture the calculator used to calculate the fields to render.
     * @param redChannelName    the name of the channel that should be used for the red component of the target image.
     * @param greenChannelName  the name of the channel that should be used for the green component of the target image.
     * @param blueChannelName   the name of the channel that should be used for the blue component of the target image.
     * @param alphaChannelName  the name of the channel that should be used for the alpha component of the target image.
     */
    public FieldRenderer(final FieldCalculator proceduralTexture, final String redChannelName, final String greenChannelName, final String blueChannelName, final String alphaChannelName) {
        myProceduralTexture = proceduralTexture;
        myRedChannelName = redChannelName;
        myGreenChannelName = greenChannelName;
        myBlueChannelName = blueChannelName;
        myAlphaChannelName = alphaChannelName;
        myField.addChannel(myRedChannelName);
        myField.addChannel(myGreenChannelName);
        myField.addChannel(myBlueChannelName);
        myField.addChannel(myAlphaChannelName, 1.0f);
    }

    /**
     * @return the name of the channel that should be used for rendering the red channel of the image.
     */
    public String getRedChannelName() {
        return myRedChannelName;
    }

    /**
     * @return the name of the channel that should be used for rendering the green channel of the image.
     */
    public String getGreenChannelName() {
        return myGreenChannelName;
    }

    /**
     * @return the name of the channel that should be used for rendering the blue channel of the image.
     */
    public String getBlueChannelName() {
        return myBlueChannelName;
    }

    /**
     * @return the name of the channel that should be used for rendering the alpha channel of the image.
     */
    public String getAlphaChannelName() {
        return myAlphaChannelName;
    }

    /**
     * @return the calculator used to calculate the fields to render.
     */
    public FieldCalculator getProceduralTexture() {
        return myProceduralTexture;
    }

    /**
     * @param proceduralTexture the calculator used to calculate the fields to render.
     */
    public void setProceduralTexture(final FieldCalculator proceduralTexture) {
        myProceduralTexture = proceduralTexture;
    }

    /**
     * @return global parameters and parameters for the area to render.
     */
    public RectangularAreaParameters getAreaParameters() {
        return myAreaParameters;
    }

    /**
     * @param areaParameters global parameters and parameters for the area to render.
     */
    public void setAreaParameters(final RectangularAreaParameters areaParameters) {
        myAreaParameters = areaParameters;
    }

    /**
     * Calculates the field and renders it to the specified target.
     *
     * @param target the buffer to render the procedural exture to.
     */
    public void render(BufferedImage target) {
        if (canRender(target)) {
            resizeFieldIfNeeded(target);
            myProceduralTexture.calculateField(myField, myAreaParameters);
            ChannelUtils.renderChannelsToImage(target, myField.getChannel(myRedChannelName), myField.getChannel(myGreenChannelName), myField.getChannel(myBlueChannelName), myField.getChannel(myAlphaChannelName));
        }
    }

    /**
     * Calculates the field and renders it to the specified target.
     *
     * @param target         the buffer to render the procedural exture to.
     * @param areaParameters global parameters and parameters for the area to render.
     */
    public void render(BufferedImage target, RectangularAreaParameters areaParameters) {
        setAreaParameters(areaParameters);
        render(target);
    }

    /**
     * @return true if the render method will render something to the target.
     */
    public boolean canRender(BufferedImage target) {
        if (target == null) {
            return false;
        }
        final int width = target.getWidth();
        final int height = target.getHeight();
        return width > 0 && height > 0 && myProceduralTexture != null && myAreaParameters != null;
    }

    private void resizeFieldIfNeeded(final BufferedImage target) {
        final int width = target.getWidth();
        final int height = target.getHeight();
        if (myField.getXSize() != width || myField.getYSize() != height) {
            myField.resize(width, height);
        }
    }
}
