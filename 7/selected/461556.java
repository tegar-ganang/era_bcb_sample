package GiciTransform;

import GiciException.*;

/**
 * This class receives an image and performs applies a discrete wavelet transform.<br>
 * Usage example:<br>
 * &nbsp; construct<br>
 * &nbsp; setParameters<br>
 * &nbsp; run<br>
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class InverseWaveletTransform {

    /**
	 * Original image samples (index meaning [z][y][x]).
	 * <p>
	 * All values allowed.
	 */
    float[][][] imageSamples = null;

    /**
	 * Number of image components.
	 * <p>
	 * Negative values are not allowed for this field.
	 */
    int zSize;

    /**
	 * Image height.
	 * <p>
	 * Negative values are not allowed for this field.
	 */
    int ySize;

    /**
	 * Image width.
	 * <p>
	 * Negative values are not allowed for this field.
	 */
    int xSize;

    /**
	 * Definition in {@link ForwardWaveletTransform#WTTypes}
	 */
    int[] WTTypes = null;

    /**
	 * DWT levels to apply for each component.
	 * <p>
	 * Negative values not allowed.
	 */
    int[] WTLevels = null;

    /**
	 * To know the order of the transform in the spatial dimentions for each component
	 * <p>
	 * Valid values are:<br>
	 *   <ul>
	 *     <li> 0 - Horizontal - Verical
	 *     <li> 1 - Vertical - Horizontal
	 *     <li> 2 - Only horizontal
	 *   </ul>
	 * 
	 */
    int[] WTOrder;

    /**
	 * To know if parameters are set.
	 * <p>
	 * True or false.
	 */
    boolean parametersSet = false;

    /**
	 * Indicates which is the resolution level being processed.
	 */
    int rLevel = 0;

    /**
	 * Constructor that receives the original image samples and initializes default values.
	 *
	 * @param imageSamples a 3D float array that contains image samples
	 */
    public InverseWaveletTransform(float[][][] imageSamples) {
        this.imageSamples = imageSamples;
        zSize = imageSamples.length;
    }

    /**
	 * Set the parameters used to apply the discrete wavelet transform, the order is set and cannot be selected..
	 *
	 * @param WTTypes definition in this class
	 * @param WTLevels definition in this class
	 */
    public void setParameters(int[] WTTypes, int[] WTLevels) {
        parametersSet = true;
        this.WTTypes = WTTypes;
        this.WTLevels = WTLevels;
        this.WTOrder = new int[zSize];
        for (int z = 0; z < zSize; z++) {
            WTOrder[z] = 1;
        }
    }

    /**
	 * Set the parameters used to apply the discrete wavelet transform when the order of the spatial dimentions can be chosen.
	 *
	 * @param WTTypes definition in this class
	 * @param WTLevels definition in this class
	 * @param WTOrder definition in this class 
	 */
    public void setParameters(int[] WTTypes, int[] WTLevels, int[] WTOrder) {
        parametersSet = true;
        this.WTTypes = WTTypes;
        this.WTLevels = WTLevels;
        this.WTOrder = WTOrder;
    }

    /**
	 * Verify Parameters defined in this class
	 *
	 * @param WTType defined in this class
	 * @param WTLevels defined in this class
	 * @param WTOrder definition in this class
	 *
	 * @return a boolean that indicates if the parameters are allowed
	 */
    public static boolean verifyParameters(int[] WTType, int[] WTLevels, int[] WTOrder) {
        for (int z = 0; z < WTType.length; z++) {
            if (WTType[z] < 0 || WTType[z] > 4) {
                return false;
            }
            if (WTLevels[z] < 0) {
                return false;
            }
            if (WTOrder[z] < 0 || WTOrder[z] > 2) {
                return false;
            }
        }
        return true;
    }

    /**
	 * Performs the discrete wavelete transform and returns the result image.
	 *
	 * @return a 3D float array that contains the DWT image
	 */
    public float[][][] run() throws Exception {
        if (!parametersSet) {
            throw new Exception("Discrete wavelet transform cannot run if parameters are not set.");
        }
        for (int z = 0; z < zSize; z++) {
            ySize = imageSamples[z].length;
            xSize = imageSamples[z][0].length;
            if ((WTTypes[z] != 0) && (WTLevels[z] > 0)) {
                int xSubBandSizes[] = new int[WTLevels[z]];
                int ySubBandSizes[] = new int[WTLevels[z]];
                xSubBandSizes[WTLevels[z] - 1] = xSize;
                ySubBandSizes[WTLevels[z] - 1] = ySize;
                for (int k = WTLevels[z] - 2; k >= 0; k--) {
                    xSubBandSizes[k] = xSubBandSizes[k + 1] / 2 + xSubBandSizes[k + 1] % 2;
                    ySubBandSizes[k] = ySubBandSizes[k + 1] / 2 + ySubBandSizes[k + 1] % 2;
                }
                for (int currentLevel = 0; currentLevel < WTLevels[z]; currentLevel++) {
                    rLevel = WTLevels[z] - currentLevel;
                    int xSubBandSize = xSubBandSizes[currentLevel];
                    int ySubBandSize = ySubBandSizes[currentLevel];
                    if (WTOrder[z] == 2) {
                        ySubBandSize = ySize;
                    }
                    if (WTOrder[z] == 0) {
                        for (int x = 0; x < xSubBandSize; x++) {
                            float currentColumn[] = new float[ySubBandSize];
                            for (int y = 0; y < ySubBandSize; y++) {
                                currentColumn[y] = imageSamples[z][y][x];
                            }
                            currentColumn = filtering(currentColumn, z);
                            for (int y = 0; y < ySubBandSize; y++) {
                                imageSamples[z][y][x] = currentColumn[y];
                            }
                        }
                    }
                    for (int y = 0; y < ySubBandSize; y++) {
                        float currentRow[] = new float[xSubBandSize];
                        for (int x = 0; x < xSubBandSize; x++) {
                            currentRow[x] = imageSamples[z][y][x];
                        }
                        currentRow = filtering(currentRow, z);
                        for (int x = 0; x < xSubBandSize; x++) {
                            imageSamples[z][y][x] = currentRow[x];
                        }
                    }
                    if (WTOrder[z] == 1 && WTOrder[z] != 2) {
                        for (int x = 0; x < xSubBandSize; x++) {
                            float currentColumn[] = new float[ySubBandSize];
                            for (int y = 0; y < ySubBandSize; y++) {
                                currentColumn[y] = imageSamples[z][y][x];
                            }
                            currentColumn = filtering(currentColumn, z);
                            for (int y = 0; y < ySubBandSize; y++) {
                                imageSamples[z][y][x] = currentColumn[y];
                            }
                        }
                    }
                }
            }
        }
        return (imageSamples);
    }

    /**
	 * This function selects the way to apply the filter
	 * selected depending on the size of the source
	 *
	 * @param src a float array of the image samples
	 * @param z the component determines the filter to apply
	 *
	 * @return a float array that contains the tranformed sources
	 *
	 * @throws ErrorException when unrecognized wavelet type is passed
	 */
    private float[] filtering(float[] src, int z) throws ErrorException {
        if (src.length == 1) {
            return src;
        }
        if (src.length % 2 == 0) {
            return (evenFiltering(src, z));
        } else {
            return (oddFiltering(src, z));
        }
    }

    /**
	 * This function applies the DWT filter to a source with even length.
	 *
	 * @param src a float array of the image samples
	 * @param z the component determines the filter to apply
	 * @return a float array that contains the tranformed sources
	 *
	 * @throws ErrorException when unrecognized wavelet type is passed
	 */
    private float[] evenFiltering(float[] src, int z) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k];
        }
        if (WTTypes[z] == 1) {
            dst[0] = dst[0] - (float) (Math.floor(((dst[1] + dst[1] + 2) / 4)));
            for (int k = 2; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
            }
            for (int k = 1; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor((dst[subbandSize - 2] + dst[subbandSize - 2]) / 2));
        } else if (WTTypes[z] == 2 || WTTypes[z] == 3) {
            final float alfa_97 = -1.586134342059924F;
            final float beta_97 = -0.052980118572961F;
            final float gamma_97 = 0.882911075530934F;
            final float delta_97 = 0.443506852043971F;
            final float nh_97, nl_97;
            if (WTTypes[z] == 2) {
                nh_97 = 1.230174104914001F;
                nl_97 = 1F / nh_97;
            } else {
                nl_97 = 1.14960430535816F;
                nh_97 = -1F / nl_97;
            }
            for (int k = 0; k < subbandSize; k += 2) {
                dst[k] = dst[k] / nl_97;
                dst[k + 1] = dst[k + 1] / nh_97;
            }
            dst[0] = dst[0] - delta_97 * (dst[1] + dst[1]);
            for (int k = 2; k < subbandSize; k += 2) {
                dst[k] = dst[k] - delta_97 * (dst[k - 1] + dst[k + 1]);
            }
            for (int k = 1; k < subbandSize - 2; k += 2) {
                dst[k] = dst[k] - gamma_97 * (dst[k - 1] + dst[k + 1]);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - gamma_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
            dst[0] = dst[0] - beta_97 * (dst[1] + dst[1]);
            for (int k = 2; k < subbandSize; k += 2) {
                dst[k] = dst[k] - beta_97 * (dst[k - 1] + dst[k + 1]);
            }
            for (int k = 1; k < subbandSize - 2; k += 2) {
                dst[k] = dst[k] - alfa_97 * (dst[k - 1] + dst[k + 1]);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - alfa_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
        } else if (WTTypes[z] == 4) {
            if (subbandSize >= 6) {
                final float alfa1 = (9F / 16F);
                final float alfa2 = (1F / 16F);
                final float beta = (1F / 4F);
                dst[0] = dst[0] + (float) (Math.floor(-beta * (dst[1] + dst[1]) + 0.5));
                for (int k = 2; k < subbandSize; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(-beta * (dst[k - 1] + dst[k + 1]) + 0.5));
                }
                dst[1] = dst[1] + (float) (Math.floor(alfa1 * (dst[0] + dst[2]) - alfa2 * (dst[2] + dst[4]) + 0.5));
                for (int k = 3; k < subbandSize - 3; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(alfa1 * (dst[k - 1] + dst[k + 1]) - alfa2 * (dst[k - 3] + dst[k + 3]) + 0.5));
                }
                dst[subbandSize - 3] = dst[subbandSize - 3] + (float) (Math.floor(alfa1 * (dst[subbandSize - 4] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 6] + dst[subbandSize - 2]) + 0.5));
                dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(alfa1 * (dst[subbandSize - 2] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 4] + dst[subbandSize - 4]) + 0.5));
            } else {
                throw new ErrorException("Size should be greater or equal than 6 in order to perform 9/7M");
            }
        } else if (WTTypes[z] == 5 || WTTypes[z] == 6) {
            final float alfa, beta, gamma, delta;
            if (WTTypes[z] == 6) {
                alfa = -1.58615986717275F;
                beta = -0.05297864003258F;
                gamma = 0.88293362717904F;
                delta = 0.44350482244527F;
            } else {
                alfa = -0.5F;
                beta = 0.25F;
                gamma = 0.F;
                delta = 0.F;
            }
            if (WTTypes[z] == 6) {
                dst[0] = dst[0] - (float) Math.floor(delta * (dst[1] + dst[1]) + 0.5);
                for (int k = 2; k < subbandSize; k += 2) {
                    dst[k] = dst[k] - (float) Math.floor(delta * (dst[k - 1] + dst[k + 1]) + 0.5);
                }
                for (int k = 1; k < subbandSize - 2; k += 2) {
                    dst[k] = dst[k] - (float) Math.floor(gamma * (dst[k - 1] + dst[k + 1]) + 0.5);
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - (float) Math.floor(gamma * (dst[subbandSize - 2] + dst[subbandSize - 2]) + 0.5);
            }
            dst[0] = dst[0] - (float) Math.floor(beta * (dst[1] + dst[1]) + 0.5);
            for (int k = 2; k < subbandSize; k += 2) {
                dst[k] = dst[k] - (float) Math.floor(beta * (dst[k - 1] + dst[k + 1]) + 0.5);
            }
            for (int k = 1; k < subbandSize - 2; k += 2) {
                dst[k] = dst[k] - (float) Math.floor(alfa * (dst[k - 1] + dst[k + 1]) + 0.5);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - (float) Math.floor(alfa * (dst[subbandSize - 2] + dst[subbandSize - 2]) + 0.5);
        } else if (WTTypes[z] == 7) {
            float sample1 = 0, sample2 = 0;
            float normFactor = (float) (Math.sqrt(2));
            for (int k = 0; k < subbandSize; k += 2) {
                sample1 = dst[k] + dst[k + 1];
                sample2 = dst[k] - dst[k + 1];
                dst[k] = sample1 * normFactor;
                dst[k + 1] = sample2 * normFactor;
            }
        } else if (WTTypes[z] == 8) {
            float s = 0;
            for (int k = 0; k < subbandSize; k += 2) {
                s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                dst[k] = dst[k + 1] + s;
                dst[k + 1] = s;
            }
        } else {
            throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return dst;
    }

    /**
	 * This function applies the DWT filter to a source with odd length.
	 *
	 * @param src a float array of the image samples
	 * @param z the component determines the filter to apply
	 *
	 * @return a float array that contains the tranformed sources
	 *
	 * @throws ErrorException when unrecognized wavelet type is passed
	 */
    private float[] oddFiltering(float[] src, int z) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k + 1];
        }
        dst[subbandSize - 1] = src[half];
        if (WTTypes[z] == 1) {
            dst[0] = dst[0] - (float) (Math.floor(((dst[1] + dst[1] + 2) / 4)));
            for (int k = 2; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - (float) (Math.floor((dst[subbandSize - 2] + dst[subbandSize - 2] + 2) / 4));
            for (int k = 1; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
            }
        } else if (WTTypes[z] == 2 || WTTypes[z] == 3) {
            final float alfa_97 = -1.586134342059924F;
            final float beta_97 = -0.052980118572961F;
            final float gamma_97 = 0.882911075530934F;
            final float delta_97 = 0.443506852043971F;
            final float nh_97, nl_97;
            if (WTTypes[z] == 2) {
                nh_97 = 1.230174104914001F;
                nl_97 = 1F / nh_97;
            } else {
                nh_97 = 1.14960430535816F;
                nl_97 = -1F / nh_97;
            }
            for (int k = 0; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] / nl_97;
                dst[k + 1] = dst[k + 1] / nh_97;
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] / nl_97;
            dst[0] = dst[0] - delta_97 * (dst[1] + dst[1]);
            for (int k = 2; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - delta_97 * (dst[k - 1] + dst[k + 1]);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - delta_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
            for (int k = 1; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - gamma_97 * (dst[k - 1] + dst[k + 1]);
            }
            dst[0] = dst[0] - beta_97 * (dst[1] + dst[1]);
            for (int k = 2; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - beta_97 * (dst[k - 1] + dst[k + 1]);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - beta_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
            for (int k = 1; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - alfa_97 * (dst[k - 1] + dst[k + 1]);
            }
        } else if (WTTypes[z] == 4) {
            throw new ErrorException("Integer 9/7M CCSDS Recommended is not implemented for odd signals.!!!");
        } else if (WTTypes[z] == 5 || WTTypes[z] == 6) {
            final float alfa, beta, gamma, delta;
            if (WTTypes[z] == 6) {
                alfa = -1.58615986717275F;
                beta = -0.05297864003258F;
                gamma = 0.88293362717904F;
                delta = 0.44350482244527F;
            } else {
                alfa = -0.5F;
                beta = 0.25F;
                gamma = 0.F;
                delta = 0.F;
            }
            if (WTTypes[z] == 6) {
                dst[0] = dst[0] - (float) Math.floor(delta * (dst[1] + dst[1]) + 0.5);
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) Math.floor(delta * (dst[k - 1] + dst[k + 1]) + 0.5);
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - (float) Math.floor(delta * (dst[subbandSize - 2] + dst[subbandSize - 2]) + 0.5);
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) Math.floor(gamma * (dst[k - 1] + dst[k + 1]) + 0.5);
                }
            }
            dst[0] = dst[0] - (float) Math.floor(beta * (dst[1] + dst[1]) + 0.5);
            for (int k = 2; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - (float) Math.floor(beta * (dst[k - 1] + dst[k + 1]) + 0.5);
            }
            dst[subbandSize - 1] = dst[subbandSize - 1] - (float) Math.floor(beta * (dst[subbandSize - 2] + dst[subbandSize - 2]) + 0.5);
            for (int k = 1; k < subbandSize - 1; k += 2) {
                dst[k] = dst[k] - (float) Math.floor(alfa * (dst[k - 1] + dst[k + 1]) + 0.5);
            }
        } else if (WTTypes[z] == 7) {
            float sample1 = 0, sample2 = 0;
            float normFactor = (float) (Math.sqrt(2));
            for (int k = 0; k < subbandSize - 1; k += 2) {
                sample1 = dst[k] + dst[k + 1];
                sample2 = dst[k] - dst[k + 1];
                dst[k] = sample1 * normFactor;
                dst[k + 1] = sample2 * normFactor;
            }
            dst[subbandSize - 1] = dst[subbandSize - 1];
        } else if (WTTypes[z] == 8) {
            float s = 0;
            for (int k = 0; k < subbandSize - 1; k += 2) {
                s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                dst[k] = dst[k + 1] + s;
                dst[k + 1] = s;
            }
            dst[subbandSize - 1] = dst[subbandSize - 1];
        } else {
            throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return (dst);
    }
}
