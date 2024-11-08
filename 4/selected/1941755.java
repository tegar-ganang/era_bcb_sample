package de.grogra.imp3d.gl20;

import de.grogra.math.ChannelMap;
import de.grogra.math.Graytone;

public class GL20ResourceShaderFragmentGraytone extends GL20ResourceShaderFragment {

    /**
	 * value attribute bit
	 */
    private static final int VALUE = 0x1;

    /**
	 * all changes that was made since last update
	 */
    private int changeMask = GL20Const.ALL_CHANGED;

    /**
	 * value attribute
	 */
    private float value;

    public GL20ResourceShaderFragmentGraytone() {
        super(GL20Resource.GL20RESOURCE_SHADERFRAGMENT_GRAYTONE);
        value = 0.5f;
    }

    public GL20ResourceShaderFragmentGraytone(float grayValue) {
        super(GL20Resource.GL20RESOURCE_SHADERFRAGMENT_GRAYTONE);
        value = grayValue;
    }

    public boolean setChannelMap(ChannelMap channelMap) {
        boolean returnValue = false;
        if (channelMap instanceof Graytone) {
            Graytone graytone = (Graytone) channelMap;
            setValue(graytone.getValue());
            returnValue = super.setChannelMap(channelMap);
        }
        return returnValue;
    }

    public boolean fragmentAffectOnAlpha() {
        return false;
    }

    public int getScalarIndex(GL20GLSLCode code, int channel) {
        int returnValue = -1;
        if ((channel < GL20CHANNEL_DERIVATE_MIN) || (channel > GL20CHANNEL_DERIVATE_MAX)) {
            int constIndex = 0;
            switch(channel & 3) {
                case 0:
                case 1:
                case 2:
                    constIndex = code.createConstScalar(value);
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
            int tempVector = code.allocateTemporaryVector3();
            if (tempVector != -1) {
                String source = new String(code.getTemporaryVector3Name(tempVector) + " = vec3(");
                for (int i = 0; i < 3; i++) {
                    switch((startChannel + i) & 0x3) {
                        case 0:
                        case 1:
                        case 2:
                            source += ((Float) value).toString();
                            break;
                        case 3:
                            source += "1.0";
                            break;
                    }
                    if (i < 2) source += ","; else source += ");\n";
                }
                code.appendCode(source);
                returnValue = tempVector;
            }
        }
        return returnValue;
    }

    public int getVector4Index(GL20GLSLCode code, int startChannel) {
        int returnValue = -1;
        if ((startChannel + 3 < GL20CHANNEL_DERIVATE_MIN) || (startChannel > GL20CHANNEL_DERIVATE_MAX)) {
            int tempVector = code.allocateTemporaryVector4();
            if (tempVector != -1) {
                String source = new String(code.getTemporaryVector4Name(tempVector) + " = vec4(");
                for (int i = 0; i < 4; i++) {
                    switch((startChannel + i) & 0x3) {
                        case 0:
                        case 1:
                        case 2:
                            source += ((Float) value).toString();
                            break;
                        case 3:
                            source += "1.0";
                            break;
                    }
                    if (i < 3) source += ","; else source += ");\n";
                }
                code.appendCode(source);
                returnValue = tempVector;
            }
        }
        return returnValue;
    }

    public final float getValue() {
        return value;
    }

    public final void setValue(float value) {
        if (this.value != value) {
            this.value = value;
            changeMask |= VALUE;
        }
    }

    public boolean isUpToDate() {
        if (changeMask != 0) return false; else return super.isUpToDate();
    }

    public void update() {
        setChannelMap(getChannelMap());
        if (changeMask != 0) {
            changeMask = 0;
        }
        super.update();
    }
}
