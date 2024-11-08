package de.grogra.imp3d.gl20;

import de.grogra.math.ChannelMap;

public abstract class GL20ResourceShaderFragment extends GL20Resource {

    /**
	 * position.x channel 
	 */
    public static final int GL20CHANNEL_POSITION_X = 0;

    /**
	 * position.y channel
	 */
    public static final int GL20CHANNEL_POSITION_Y = 1;

    /**
	 * position.z channel
	 */
    public static final int GL20CHANNEL_POSITION_Z = 2;

    /**
	 * normal.x channel
	 */
    public static final int GL20CHANNEL_NORMAL_X = 4;

    /**
	 * normal.y channel 
	 */
    public static final int GL20CHANNEL_NORMAL_Y = 5;

    /**
	 * normal.z channel
	 */
    public static final int GL20CHANNEL_NORMAL_Z = 6;

    /**
	 * derivate x du channel
	 */
    public static final int GL20CHANNEL_DERIVATE_X_DU = 8;

    /**
	 * derivate y du channel
	 */
    public static final int GL20CHANNEL_DERIVATE_Y_DU = 9;

    /**
	 * derivate z du channel
	 */
    public static final int GL20CHANNEL_DERIVATE_Z_DU = 10;

    /**
	 * derivate x dv channel
	 */
    public static final int GL20CHANNEL_DERIVATE_X_DV = 12;

    /**
	 * derivate y dv channel
	 */
    public static final int GL20CHANNEL_DERIVATE_Y_DV = 13;

    /**
	 * derivate z dv channel
	 */
    public static final int GL20CHANNEL_DERIVATE_Z_DV = 14;

    /**
	 * first derivate channel
	 */
    public static final int GL20CHANNEL_DERIVATE_MIN = GL20CHANNEL_DERIVATE_X_DU;

    /**
	 * last derivate channel
	 */
    public static final int GL20CHANNEL_DERIVATE_MAX = GL20CHANNEL_DERIVATE_Z_DV;

    /**
	 * texcoord.u channel
	 */
    public static final int GL20CHANNEL_TEXCOORD_U = 16;

    /**
	 * texcoord.v channel
	 */
    public static final int GL20CHANNEL_TEXCOORD_V = 17;

    /**
	 * texcoord.w channel
	 */
    public static final int GL20CHANNEL_TEXCOORD_W = 18;

    /**
	 * x channel
	 */
    public static final int GL20CHANNEL_X = 20;

    /**
	 * y channel
	 */
    public static final int GL20CHANNEL_Y = 21;

    /**
	 * z channel
	 */
    public static final int GL20CHANNEL_Z = 22;

    /**
	 * color.r channel
	 */
    public static final int GL20CHANNEL_R = 24;

    /**
	 * color.g channel
	 */
    public static final int GL20CHANNEL_G = 25;

    /**
	 * color.b channel
	 */
    public static final int GL20CHANNEL_B = 26;

    /**
	 * color.a channel
	 */
    public static final int GL20CHANNEL_A = 27;

    /**
	 * position.x channel bit
	 */
    public static final int GL20CHANNEL_POSITION_X_BIT = (1 << GL20CHANNEL_POSITION_X);

    /**
	 * position.y channel bit
	 */
    public static final int GL20CHANNEL_POSITION_Y_BIT = (1 << GL20CHANNEL_POSITION_Y);

    /**
	 * position.z channel bit
	 */
    public static final int GL20CHANNEL_POSITION_Z_BIT = (1 << GL20CHANNEL_POSITION_Z);

    /**
	 * position channels bitmask
	 */
    public static final int GL20CHANNEL_POSITION_BITS = (GL20CHANNEL_POSITION_X_BIT | GL20CHANNEL_POSITION_Y_BIT | GL20CHANNEL_POSITION_Z_BIT);

    /**
	 * normal.x channel bit
	 */
    public static final int GL20CHANNEL_NORMAL_X_BIT = (1 << GL20CHANNEL_NORMAL_X);

    /**
	 * normal.y channel bit
	 */
    public static final int GL20CHANNEL_NORMAL_Y_BIT = (1 << GL20CHANNEL_NORMAL_Y);

    /**
	 * normal.z channel bit
	 */
    public static final int GL20CHANNEL_NORMAL_Z_BIT = (1 << GL20CHANNEL_NORMAL_Z);

    /**
	 * normal channels bitmask
	 */
    public static final int GL20CHANNEL_NORMAL_BITS = (GL20CHANNEL_NORMAL_X_BIT | GL20CHANNEL_NORMAL_Y_BIT | GL20CHANNEL_NORMAL_Z_BIT);

    /**
	 * derivate x du channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_X_DU_BIT = (1 << GL20CHANNEL_DERIVATE_X_DU);

    /**
	 * derivate y du channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_Y_DU_BIT = (1 << GL20CHANNEL_DERIVATE_Y_DU);

    /**
	 * derivate z du channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_Z_DU_BIT = (1 << GL20CHANNEL_DERIVATE_Z_DU);

    /**
	 * derivate du channels bitmask
	 */
    public static final int GL20CHANNEL_DERIVATE_DU_BITS = (GL20CHANNEL_DERIVATE_X_DU_BIT | GL20CHANNEL_DERIVATE_Y_DU_BIT | GL20CHANNEL_DERIVATE_Z_DU_BIT);

    /**
	 * derivate x dv channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_X_DV_BIT = (1 << GL20CHANNEL_DERIVATE_X_DV);

    /**
	 * derivate y dv channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_Y_DV_BIT = (1 << GL20CHANNEL_DERIVATE_Y_DV);

    /**
	 * derivate z dv channel bit
	 */
    public static final int GL20CHANNEL_DERIVATE_Z_DV_BIT = (1 << GL20CHANNEL_DERIVATE_Z_DV);

    /**
	 * derivate dv channels bitmask
	 */
    public static final int GL20CHANNEL_DERIVATE_DV_BITS = (GL20CHANNEL_DERIVATE_X_DV_BIT | GL20CHANNEL_DERIVATE_Y_DV_BIT | GL20CHANNEL_DERIVATE_Z_DV_BIT);

    /**
	 * all derivate channels bitmask
	 */
    public static final int GL20CHANNEL_DERIVATE_BITS = (GL20CHANNEL_DERIVATE_DU_BITS | GL20CHANNEL_DERIVATE_DV_BITS);

    /**
	 * texcoord.u channel bit
	 */
    public static final int GL20CHANNEL_TEXCOORD_U_BIT = (1 << GL20CHANNEL_TEXCOORD_U);

    /**
	 * texcoord.v channel bit
	 */
    public static final int GL20CHANNEL_TEXCOORD_V_BIT = (1 << GL20CHANNEL_TEXCOORD_V);

    /**
	 * texcoord.w channel bit
	 */
    public static final int GL20CHANNEL_TEXCOORD_W_BIT = (1 << GL20CHANNEL_TEXCOORD_W);

    /**
	 * texcoord channels bitmask
	 */
    public static final int GL20CHANNEL_TEXCOORD_BITS = (GL20CHANNEL_TEXCOORD_U_BIT | GL20CHANNEL_TEXCOORD_V_BIT | GL20CHANNEL_TEXCOORD_W_BIT);

    /**
	 * x channel bit
	 */
    public static final int GL20CHANNEL_X_BIT = (1 << GL20CHANNEL_X);

    /**
	 * y channel bit
	 */
    public static final int GL20CHANNEL_Y_BIT = (1 << GL20CHANNEL_Y);

    /**
	 * z channel bit
	 */
    public static final int GL20CHANNEL_Z_BIT = (1 << GL20CHANNEL_Z);

    /**
	 * xyz channels bitmask
	 */
    public static final int GL20CHANNEL_XYZ_BITS = (GL20CHANNEL_X_BIT | GL20CHANNEL_Y_BIT | GL20CHANNEL_Z_BIT);

    /**
	 * color.r channel bit
	 */
    public static final int GL20CHANNEL_R_BIT = (1 << GL20CHANNEL_R);

    /**
	 * color.g channel bit
	 */
    public static final int GL20CHANNEL_G_BIT = (1 << GL20CHANNEL_G);

    /**
	 * color.b channel bit
	 */
    public static final int GL20CHANNEL_B_BIT = (1 << GL20CHANNEL_B);

    /**
	 * color.a channel bit
	 */
    public static final int GL20CHANNEL_A_BIT = (1 << GL20CHANNEL_A);

    /**
	 * rgb channels bitmask
	 */
    public static final int GL20CHANNEL_RGB_BITS = (GL20CHANNEL_R_BIT | GL20CHANNEL_G_BIT | GL20CHANNEL_B_BIT);

    /**
	 * rgba channels bitmask
	 */
    public static final int GL20CHANNEL_RGBA_BITS = (GL20CHANNEL_RGB_BITS | GL20CHANNEL_A_BIT);

    /**
	 * channel map attribute bit
	 */
    private static final int CHANNEL_MAP = 0x1;

    /**
	 * all changes that was made since last update
	 */
    private int changeMask = GL20Const.ALL_CHANGED;

    /**
	 * channel map attribute
	 */
    private ChannelMap channelMap = null;

    /**
	 * last channel map stamp
	 */
    private int channelMapStamp = -1;

    /**
	 * count of users for this <code>GL20ResourceShaderFragment</code>
	 */
    private int userCount = 0;

    protected GL20ResourceShaderFragment(int resourceClassType) {
        super(resourceClassType);
        assert ((resourceClassType & GL20Resource.GL20RESOURCE_CLASS_MASK) == GL20Resource.GL20RESOURCE_CLASS_SHADER);
    }

    /**
	 * set the equivalent <code>ChannelMap</code> to this <code>GL20ResourceShaderFragment</code>
	 * 
	 * @param channelMap the <code>ChannelMap</code> that should be the source of for this
	 * <code>GL20ResourceShaderFragment</code>
	 */
    public boolean setChannelMap(ChannelMap channelMap) {
        boolean returnValue = false;
        if (this.channelMap != channelMap) {
            this.channelMap = channelMap;
            changeMask |= CHANNEL_MAP;
            returnValue = true;
        } else returnValue = true;
        return returnValue;
    }

    /**
	 * return the equivalent <code>ChannelMap</code> to this <code>GL20ResourceShaderFragment</code>
	 * 
	 * @return the source <code>ChannelMap</code> for this <code>GL20ResourceShaderFragment</code>
	 */
    public ChannelMap getChannelMap() {
        return channelMap;
    }

    /**
	 * get the index of the scalar value for the <code>code</code> that was created
	 * 
	 * @param code the class where the shader code is stored finally
	 * @param channel the channel you want
	 * @return <code>-1</code> an error occurred
	 * otherwise the index of the scalar value. Test for the CONST_BIT to get the correct
	 * reference
	 */
    public int getScalarIndex(GL20GLSLCode code, int channel) {
        return -1;
    }

    /**
	 * get the index of the vector3 for the <code>code</code> that was created
	 * 
	 * @param code the class where the shader code is stored finally
	 * @param startChannel the channels you want, starting with the given channel
	 * @return <code>-1</code> an error occurred
	 * otherwise the index of the vector3. Test for the CONST_BIT to get the correct
	 * reference
	 */
    public int getVector3Index(GL20GLSLCode code, int startChannel) {
        return -1;
    }

    /**
	 * get the index of the vector4 for the <code>code</code> that was created
	 * 
	 * @param code the class where the shader code is stored finally
	 * @param startChannel the channels you want, starting with the given channel
	 * @return <code>-1</code> an error occurred
	 * otherwise the index of the vector4. Test for the CONST_BIT to get the correct
	 * reference
	 */
    public int getVector4Index(GL20GLSLCode code, int startChannel) {
        return -1;
    }

    /**
	 * check if this <code>GL20ResourceShaderFragment</code> affect the alpha
	 * channel
	 * 
	 * @return <code>true</code> this <code>GL20ResourceShader</code> will
	 * affect the alpha channel
	 * <code>false</code> this <code>GL20ResourceShader</code> will not
	 * affect the alpha channel
	 */
    public abstract boolean fragmentAffectOnAlpha();

    /**
	 * register one user of this <code>GL20ResourceShaderFragment</code>
	 */
    public final void registerUser() {
        userCount++;
    }

    /**
	 * unregister one user of this <code>GL20ResourceShaderFragment</code>.
	 * When no more user exists, this <code>GL20ResourceShaderFragment</code>
	 * will destroy itself.
	 */
    public final void unregisterUser() {
        userCount--;
        if (userCount == 0) destroy();
    }

    /**
	 * check if this <code>GL20ResourceShaderFragment</code> is up to date
	 * 
	 * @return <code>true</code> this <code>GL20ResourceShaderFragment</code> is up to date
	 * <code>false</code> this <code>GL20ResourceShaderFragment</code> is not up to date,
	 * need an <code>update()</code> call
	 */
    public boolean isUpToDate() {
        if (channelMap != null) if (channelMap.getStamp() != channelMapStamp) return false;
        return super.isUpToDate();
    }

    /**
	 * update this <code>GL20ResourceShaderFragment</code>
	 */
    public void update() {
        if (channelMap != null) channelMapStamp = channelMap.getStamp();
        super.update();
    }

    /**
	 * every <code>GL20ResourceShaderFragment</code> have to override this
	 * method when it contains objects that should be released
	 */
    public void destroy() {
    }
}
