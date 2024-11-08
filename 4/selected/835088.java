package de.grogra.imp3d.gl20;

import javax.vecmath.Vector4f;
import de.grogra.math.ChannelMap;
import de.grogra.math.RGBColor;

class GL20ResourceShaderFragmentRGBColor extends GL20ResourceShaderFragment {

    /**
	 * color attribute bit
	 */
    private static final int COLOR = 0x1;

    /**
	 * all changed that was made since last update
	 */
    private int changeMask = GL20Const.ALL_CHANGED;

    /**
	 * color attribute
	 */
    private Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    public GL20ResourceShaderFragmentRGBColor() {
        super(GL20Resource.GL20RESOURCE_SHADERFRAGMENT_RGB);
    }

    public boolean setChannelMap(ChannelMap channelMap) {
        boolean returnValue = false;
        if (channelMap instanceof RGBColor) {
            RGBColor rgbChannelMap = (RGBColor) channelMap;
            setColor(new Vector4f(rgbChannelMap.x, rgbChannelMap.y, rgbChannelMap.z, 1.0f));
            returnValue = super.setChannelMap(channelMap);
        }
        return returnValue;
    }

    /**
	 * check if this <code>GL20ResourceShaderFragmentColor</code> affect the
	 * alpha value
	 * 
	 * @return <code>true</code> this <code>GL20ResourceShaderFragmentColor</code>
	 * affects on alpha value
	 * <code>false</code> this <code>GL20ResourceShaderFragmentColor</code> doesn't
	 * affects on alpha values 
	 */
    public boolean fragmentAffectOnAlpha() {
        return false;
    }

    /**
	 * get the color of this <code>GL20ResourceShaderFragmentColor</code>
	 * 
	 * @return the color of this <code>GL20ResourceShaderFragmentColor</code>
	 */
    public final Vector4f getColor() {
        return color;
    }

    /**
	 * set the color of this <code>GL20ResourceShaderFragmentColor</code>
	 * 
	 * @param color the color that this <code>GL20ResourceShaderFragmentColor</code>
	 * should have
	 */
    public final void setColor(Vector4f color) {
        if (this.color.equals(color) == false) {
            this.color.set(color);
            changeMask |= COLOR;
        }
    }

    public int getScalarIndex(GL20GLSLCode code, int channel) {
        int returnValue = -1;
        if ((channel < GL20CHANNEL_DERIVATE_MIN) || (channel > GL20CHANNEL_DERIVATE_MAX)) {
            int constIndex = -1;
            switch(channel & 0x3) {
                case 0:
                    constIndex = code.createConstScalar(color.x);
                    break;
                case 1:
                    constIndex = code.createConstScalar(color.y);
                    break;
                case 2:
                    constIndex = code.createConstScalar(color.z);
                    break;
                case 3:
                    constIndex = code.createConstScalar(1.0f);
                    break;
            }
            returnValue = constIndex | GL20GLSLCode.CONST_BIT;
        }
        return returnValue;
    }

    public int getVector3Index(GL20GLSLCode code, int startChannel) {
        int returnValue = -1;
        if ((startChannel + 2 < GL20CHANNEL_DERIVATE_MIN) || (startChannel > GL20CHANNEL_DERIVATE_MAX)) {
            int vector3Index = code.allocateTemporaryVector3();
            if (vector3Index != -1) {
                String source = new String(code.getVector3Name(vector3Index) + " = vec(");
                for (int i = 0; i < 3; i++) {
                    switch((startChannel + i) & 0x3) {
                        case 0:
                            source += ((Float) color.x).toString();
                            break;
                        case 1:
                            source += ((Float) color.y).toString();
                            break;
                        case 2:
                            source += ((Float) color.z).toString();
                            break;
                        case 3:
                            source += "1.0";
                            break;
                    }
                    if (i < 2) source += ","; else source += ");\n";
                }
                code.appendCode(source);
                returnValue = vector3Index;
            }
        }
        return returnValue;
    }

    public int getVector4Index(GL20GLSLCode code, int startChannel) {
        int returnValue = -1;
        if ((startChannel + 3 < GL20CHANNEL_DERIVATE_MIN) || (startChannel > GL20CHANNEL_DERIVATE_MAX)) {
            int vector4Index = code.allocateTemporaryVector4();
            if (vector4Index != -1) {
                String source = new String(code.getVector4Name(vector4Index) + " = vec4(");
                for (int i = 0; i < 4; i++) {
                    switch((startChannel + i) & 0x3) {
                        case 0:
                            source += ((Float) color.x).toString();
                            break;
                        case 1:
                            source += ((Float) color.y).toString();
                            break;
                        case 2:
                            source += ((Float) color.z).toString();
                            break;
                        case 3:
                            source += "1.0";
                            break;
                    }
                    if (i < 3) source += ","; else source += ");\n";
                }
                code.appendCode(source);
                returnValue = vector4Index;
            }
        }
        return returnValue;
    }

    /**
	 * check if this <code>GL20ResourceShaderFragmentColor</code> is up to date
	 * 
	 * @return <code>true</code> this <code>GL20ResourceShaderFragmentColor</code> is
	 * up to date
	 * <code>false</code> this <code>GL20ResourceShaderFragmentColor</code> isn't up
	 * to date, need update via <code>update()</code>
	 */
    public boolean isUpToDate() {
        if (changeMask != 0) return false; else return super.isUpToDate();
    }

    /**
	 * update this <code>GL20ResourceShaderFragmentColor</code>
	 */
    public void update() {
        setChannelMap(getChannelMap());
        if (changeMask != 0) {
            changeMask = 0;
        }
        super.update();
    }

    /**
	 * destroy this <code>GL20ResourceShaderFragmentColor</code>
	 */
    public void destroy() {
        super.destroy();
    }
}
