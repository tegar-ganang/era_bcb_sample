package GiciTransform;

import GiciException.*;

/**
 * This class receives an arbitrary piece of a 3D transformed image and performs one level of the corresponded 3D discrete inverse wavelet transform. Usage example:<br>
 * &nbsp; construct<br>
 * &nbsp; setParameters<br>
 * &nbsp; run<br>
 * &nbsp; getImageSamples<br> 
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0
 */
public class InverseDWTCore {

    /**
	 * Image samples (index meaning [z][y][x]).
	 */
    float[][][] imageSamples = null;

    /**
	 * Definition in {@link ForwardDWTCore#WTTypes}
	 */
    int WTTypes;

    /**
	 * Definition in {@link ForwardDWTCore#WTAxis}
	 */
    int WTAxis;

    /**
	 * Definition in {@link ForwardDWTCore#WTZRegionStart}
	 */
    int WTZRegionStart;

    /**
	 * Definition in {@link ForwardDWTCore#WTZRegionEnd}
	 */
    int WTZRegionEnd;

    /**
	 * Definition in {@link ForwardDWTCore#WTYRegionStart}
	 */
    int WTYRegionStart;

    /**
	 * Definition in {@link ForwardDWTCore#WTYRegionEnd}
	 */
    int WTYRegionEnd;

    /**
	 * Definition in {@link ForwardDWTCore#WTXRegionStart}
	 */
    int WTXRegionStart;

    /**
	 * Definition in {@link ForwardDWTCore#WTXRegionEnd}
	 */
    int WTXRegionEnd;

    /**
	 * To know if parameters are set.
	 * <p>
	 * True indicates that they are set otherwise false.
	 */
    boolean parametersSet = false;

    /**
	 * Constructor, receives the transformed image samples.
	 *
	 * @param imageSamples definition in {@link #imageSamples}
	 */
    public InverseDWTCore(float[][][] imageSamples) {
        this.imageSamples = imageSamples;
    }

    /**
	 * Set the parameters used to apply the discrete inverse wavelet transform, compact use, the transform is going to be applied by default on the whole image over the requested axis.
	 *
	 * @param WTTypes definition in {@link #WTTypes}
	 * @param WTAxis definition in {@link #WTAxis}
	 */
    public void setParameters(int WTTypes, int WTAxis) {
        parametersSet = true;
        this.WTTypes = WTTypes;
        this.WTAxis = WTAxis;
        this.WTZRegionStart = 0;
        this.WTYRegionStart = 0;
        this.WTXRegionStart = 0;
        this.WTZRegionEnd = imageSamples.length;
        this.WTYRegionEnd = imageSamples[0].length;
        this.WTXRegionEnd = imageSamples[0][0].length;
    }

    /**
	 * Set the parameters used to apply the discrete inverse wavelet transform.
	 *
	 * @param WTTypes definition in {@link #WTTypes}
	 * @param WTAxis definition in {@link #WTAxis}
	 * @param WTZRegionStart definition in {@link #WTZRegionStart}
	 * @param WTZRegionEnd definition in {@link #WTZRegionEnd}
	 * @param WTYRegionStart definition in {@link #WTYRegionStart}
	 * @param WTYRegionEnd definition in {@link #WTYRegionEnd}
	 * @param WTXRegionStart definition in {@link #WTXRegionStart}
	 * @param WTXRegionEnd definition in {@link #WTXRegionEnd}
	 */
    public void setParameters(int WTTypes, int WTAxis, int WTZRegionStart, int WTZRegionEnd, int WTYRegionStart, int WTYRegionEnd, int WTXRegionStart, int WTXRegionEnd) {
        parametersSet = true;
        this.WTTypes = WTTypes;
        this.WTAxis = WTAxis;
        this.WTZRegionStart = WTZRegionStart;
        this.WTZRegionEnd = WTZRegionEnd;
        this.WTYRegionStart = WTYRegionStart;
        this.WTYRegionEnd = WTYRegionEnd;
        this.WTXRegionStart = WTXRegionStart;
        this.WTXRegionEnd = WTXRegionEnd;
    }

    /**
	 * Used to set (or reset) the image over which the detransform is going to be applied.
	 *
	 * @param imageSamples definition in {@link #imageSamples}
	 */
    public void setImageSamples(float[][][] imageSamples) {
        this.imageSamples = imageSamples;
    }

    /**
	 * Used to get the 3DWT detransformed image.
	 *
	 * @return the DWT image
	 */
    public float[][][] getImageSamples() {
        return (imageSamples);
    }

    /**
	 * Performs the discrete inverse wavelete transform and returns the result image.
	 *
	 * @throws ErrorException when parameters are not set, wavelet type is unrecognized or trying to detransform over an unimplemented axis
	 */
    public void run() throws ErrorException {
        if (!parametersSet) {
            throw new ErrorException("Parameters not set.");
        }
        if (WTTypes != 0) {
            int zSize = WTZRegionEnd - WTZRegionStart + 1;
            int ySize = WTYRegionEnd - WTYRegionStart + 1;
            int xSize = WTXRegionEnd - WTXRegionStart + 1;
            switch(WTAxis) {
                case 0:
                    for (int z = 0; z < zSize; z++) {
                        for (int y = 0; y < ySize; y++) {
                            float currentLine[] = new float[xSize];
                            for (int x = 0; x < xSize; x++) {
                                currentLine[x + WTXRegionStart] = imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart];
                            }
                            currentLine = filtering(currentLine, WTTypes, WTXRegionStart);
                            for (int x = 0; x < xSize; x++) {
                                imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart] = currentLine[x + WTXRegionStart];
                            }
                        }
                    }
                    break;
                case 1:
                    for (int z = 0; z < zSize; z++) {
                        for (int x = 0; x < xSize; x++) {
                            float currentLine[] = new float[ySize];
                            for (int y = 0; y < ySize; y++) {
                                currentLine[y + WTYRegionStart] = imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart];
                            }
                            currentLine = filtering(currentLine, WTTypes, WTYRegionStart);
                            for (int y = 0; y < ySize; y++) {
                                imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart] = currentLine[y + WTYRegionStart];
                            }
                        }
                    }
                    break;
                case 2:
                    for (int y = 0; y < ySize; y++) {
                        for (int x = 0; x < xSize; x++) {
                            float currentLine[] = new float[zSize];
                            for (int z = 0; z < zSize; z++) {
                                currentLine[z + WTZRegionStart] = imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart];
                            }
                            currentLine = filtering(currentLine, WTTypes, WTZRegionStart);
                            for (int z = 0; z < zSize; z++) {
                                imageSamples[z + WTZRegionStart][y + WTYRegionStart][x + WTXRegionStart] = currentLine[z + WTZRegionStart];
                            }
                        }
                    }
                    break;
                default:
                    throw new ErrorException("Unimplemented Axis");
            }
        }
        parametersSet = false;
    }

    /**
	 * This function selects the way to apply the filter depending on the phase and the size of the source.
	 *
	 * @param src a float array of the image samples
	 * @param WTTypes Filter to apply
	 * @param WTLRegionStart The starting coordinate of the original line to transform, used to determine the starting phase.
	 * @return a float array that contains the detransformed sources
	 *
	 * @throws ErrorException when wavelet type is unrecognized
	 */
    private float[] filtering(float[] src, int WTTypes, int WTLRegionStart) throws ErrorException {
        float[] unfiltered = null;
        float[] unfiltered_prim = null;
        if (src.length == 1 && WTTypes != 4) {
            unfiltered = src;
        } else {
            float[] src_prim = null;
            if (WTTypes == 4 && src.length < 6) {
                src_prim = coefExpansion(src);
            } else {
                src_prim = src;
            }
            if (src_prim.length % 2 == 0 && WTLRegionStart % 2 == 0) {
                unfiltered_prim = evenEvenFiltering(src_prim, WTTypes);
            } else if (src_prim.length % 2 == 0 && WTLRegionStart % 2 == 1) {
                unfiltered_prim = evenOddFiltering(src_prim, WTTypes);
            } else if (src_prim.length % 2 == 1 && WTLRegionStart % 2 == 0) {
                unfiltered_prim = oddEvenFiltering(src_prim, WTTypes);
            } else if (src_prim.length % 2 == 1 && WTLRegionStart % 2 == 1) {
                unfiltered_prim = oddOddFiltering(src_prim, WTTypes);
            } else {
                unfiltered_prim = src_prim;
            }
            if (WTTypes == 4 && src.length < 6) {
                unfiltered = coefUnexpansion(unfiltered_prim, src.length);
            } else {
                unfiltered = unfiltered_prim;
            }
        }
        return (unfiltered);
    }

    /**
	 * This function applies the DWT filter to a source with even length and even phase.
	 *
	 * @param src a float array of the transformed image samples
	 * @param WTTypes Filter to apply
	 * @return a float array that contains the detransformed sources
	 *
	 * @throws ErrorException when wavelet type is unrecognized
	 */
    private float[] evenEvenFiltering(float[] src, int WTTypes) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k];
        }
        switch(WTTypes) {
            case 1:
                dst[0] = dst[0] - (float) (Math.floor(((dst[1] + dst[1] + 2) / 4)));
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
                }
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor((dst[subbandSize - 2] + dst[subbandSize - 2]) / 2));
                break;
            case 2:
            case 3:
                final float alfa_97 = -1.586134342059924F;
                final float beta_97 = -0.052980118572961F;
                final float gamma_97 = 0.882911075530934F;
                final float delta_97 = 0.443506852043971F;
                final float nh_97, nl_97;
                if (WTTypes == 2) {
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
                break;
            case 4:
                if (subbandSize >= 6) {
                    final float alfa1 = (9F / 16F);
                    final float alfa2 = (1F / 16F);
                    final float beta = (1F / 4F);
                    dst[0] = dst[0] + (float) (Math.floor(-beta * (dst[1] + dst[1]) + 0.5F));
                    for (int k = 2; k < subbandSize; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(-beta * (dst[k - 1] + dst[k + 1]) + 0.5F));
                    }
                    dst[1] = dst[1] + (float) (Math.floor(alfa1 * (dst[0] + dst[2]) - alfa2 * (dst[2] + dst[4]) + 0.5F));
                    for (int k = 3; k < subbandSize - 3; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(alfa1 * (dst[k - 1] + dst[k + 1]) - alfa2 * (dst[k - 3] + dst[k + 3]) + 0.5F));
                    }
                    dst[subbandSize - 3] = dst[subbandSize - 3] + (float) (Math.floor(alfa1 * (dst[subbandSize - 4] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 6] + dst[subbandSize - 2]) + 0.5F));
                    dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(alfa1 * (dst[subbandSize - 2] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 4] + dst[subbandSize - 4]) + 0.5F));
                } else {
                    throw new ErrorException("Size should be greater or equal than 6 in order to perform 9/7M");
                }
                break;
            case 7:
                float sample1 = 0, sample2 = 0;
                float normFactor = (float) (Math.sqrt(2));
                for (int k = 0; k < subbandSize; k += 2) {
                    sample1 = dst[k] + dst[k + 1];
                    sample2 = dst[k] - dst[k + 1];
                    dst[k] = sample1 * normFactor;
                    dst[k + 1] = sample2 * normFactor;
                }
                break;
            case 8:
                float s = 0;
                for (int k = 0; k < subbandSize; k += 2) {
                    s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                    dst[k] = dst[k + 1] + s;
                    dst[k + 1] = s;
                }
                break;
            default:
                throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return (dst);
    }

    /**
	 * This function applies the DWT filter to a source with even length and odd phase.
	 *
	 * @param src a float array of the image samples
	 * @param WTTypes Filter to apply
	 * @return a float array that contains the detransformed sources
	 *
	 * @throws ErrorException when wavelet type is unrecognized
	 */
    private float[] evenOddFiltering(float[] src, int WTTypes) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k];
        }
        switch(WTTypes) {
            case 1:
                dst[subbandSize - 1] = dst[subbandSize - 1] - (float) (Math.floor((dst[subbandSize - 2] + dst[subbandSize - 2] + 2) / 4));
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
                }
                dst[0] = dst[0] + (float) (Math.floor(((dst[1] + dst[1]) / 2)));
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
                }
                break;
            case 2:
            case 3:
                final float alfa_97 = -1.586134342059924F;
                final float beta_97 = -0.052980118572961F;
                final float gamma_97 = 0.882911075530934F;
                final float delta_97 = 0.443506852043971F;
                final float nh_97, nl_97;
                if (WTTypes == 2) {
                    nh_97 = 1.230174104914001F;
                    nl_97 = 1F / nh_97;
                } else {
                    nl_97 = 1.14960430535816F;
                    nh_97 = -1F / nl_97;
                }
                for (int k = 0; k < subbandSize; k += 2) {
                    dst[k] = dst[k] / nh_97;
                    dst[k + 1] = dst[k + 1] / nl_97;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - delta_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - delta_97 * (dst[k - 1] + dst[k + 1]);
                }
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - gamma_97 * (dst[k - 1] + dst[k + 1]);
                }
                dst[0] = dst[0] - gamma_97 * (dst[1] + dst[1]);
                dst[subbandSize - 1] = dst[subbandSize - 1] - beta_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - beta_97 * (dst[k - 1] + dst[k + 1]);
                }
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - alfa_97 * (dst[k - 1] + dst[k + 1]);
                }
                dst[0] = dst[0] - alfa_97 * (dst[1] + dst[1]);
                break;
            case 4:
                if (subbandSize >= 6) {
                    final float alfa1 = (9F / 16F);
                    final float alfa2 = (1F / 16F);
                    final float beta = (1F / 4F);
                    dst[0] = dst[0] + (float) (Math.floor(-beta * (dst[1] + dst[1]) + 0.5F));
                    for (int k = 2; k < subbandSize; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(-beta * (dst[k - 1] + dst[k + 1]) + 0.5F));
                    }
                    dst[1] = dst[1] + (float) (Math.floor(alfa1 * (dst[0] + dst[2]) - alfa2 * (dst[2] + dst[4]) + 0.5F));
                    for (int k = 3; k < subbandSize - 3; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(alfa1 * (dst[k - 1] + dst[k + 1]) - alfa2 * (dst[k - 3] + dst[k + 3]) + 0.5F));
                    }
                    dst[subbandSize - 3] = dst[subbandSize - 3] + (float) (Math.floor(alfa1 * (dst[subbandSize - 4] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 6] + dst[subbandSize - 2]) + 0.5F));
                    dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(alfa1 * (dst[subbandSize - 2] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 4] + dst[subbandSize - 4]) + 0.5F));
                } else {
                    throw new ErrorException("Size should be greater or equal than 6 in order to perform 9/7M");
                }
                break;
            case 7:
                float sample1 = 0, sample2 = 0;
                float normFactor = (float) (Math.sqrt(2));
                for (int k = 0; k < subbandSize; k += 2) {
                    sample1 = dst[k] + dst[k + 1];
                    sample2 = dst[k] - dst[k + 1];
                    dst[k] = sample1 * normFactor;
                    dst[k + 1] = sample2 * normFactor;
                }
                break;
            case 8:
                float s = 0;
                for (int k = 0; k < subbandSize; k += 2) {
                    s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                    dst[k] = dst[k + 1] + s;
                    dst[k + 1] = s;
                }
                break;
            default:
                throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return (dst);
    }

    /**
	 * This function applies the DWT filter to a source with odd length and even phase.
	 *
	 * @param src a float array of the image samples
	 * @param WTTypes Filter to apply
	 * @return a float array that contains the detransformed sources
	 *
	 * @throws ErrorException when wavelet type is unrecognized
	 */
    private float[] oddEvenFiltering(float[] src, int WTTypes) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k + 1];
        }
        dst[subbandSize - 1] = src[half];
        switch(WTTypes) {
            case 1:
                dst[0] = dst[0] - (float) (Math.floor(((dst[1] + dst[1] + 2) / 4)));
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - (float) (Math.floor((dst[subbandSize - 2] + dst[subbandSize - 2] + 2) / 4));
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
                }
                break;
            case 2:
            case 3:
                final float alfa_97 = -1.586134342059924F;
                final float beta_97 = -0.052980118572961F;
                final float gamma_97 = 0.882911075530934F;
                final float delta_97 = 0.443506852043971F;
                final float nh_97, nl_97;
                if (WTTypes == 2) {
                    nh_97 = 1.230174104914001F;
                    nl_97 = 1F / nh_97;
                } else {
                    nl_97 = 1.14960430535816F;
                    nh_97 = -1F / nl_97;
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
                break;
            case 4:
                if (subbandSize >= 6) {
                    final float alfa1 = (9F / 16F);
                    final float alfa2 = (1F / 16F);
                    final float beta = (1F / 4F);
                    dst[0] = dst[0] + (float) (Math.floor(-beta * (dst[1] + dst[1]) + 0.5));
                    for (int k = 2; k < subbandSize - 1; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(-beta * (dst[k - 1] + dst[k + 1]) + 0.5));
                    }
                    dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(-beta * (dst[subbandSize - 2] + dst[subbandSize - 2]) + 0.5));
                    dst[1] = dst[1] + (float) (Math.floor(alfa1 * (dst[0] + dst[2]) - alfa2 * (dst[2] + dst[4]) + 0.5));
                    for (int k = 3; k < subbandSize - 3; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(alfa1 * (dst[k - 1] + dst[k + 1]) - alfa2 * (dst[k - 3] + dst[k + 3]) + 0.5));
                    }
                    dst[subbandSize - 2] = dst[subbandSize - 2] + (float) (Math.floor(alfa1 * (dst[subbandSize - 3] + dst[subbandSize - 1]) - alfa2 * (dst[subbandSize - 5] + dst[subbandSize - 1]) + 0.5));
                } else {
                    throw new ErrorException("Size should be greater or equal than 6 in order to perform 9/7M");
                }
                break;
            case 7:
                float sample1 = 0, sample2 = 0;
                float normFactor = (float) (Math.sqrt(2));
                for (int k = 0; k < subbandSize - 1; k += 2) {
                    sample1 = dst[k] + dst[k + 1];
                    sample2 = dst[k] - dst[k + 1];
                    dst[k] = sample1 * normFactor;
                    dst[k + 1] = sample2 * normFactor;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1];
                break;
            case 8:
                float s = 0;
                for (int k = 0; k < subbandSize - 1; k += 2) {
                    s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                    dst[k] = dst[k + 1] + s;
                    dst[k + 1] = s;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1];
                break;
            default:
                throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return (dst);
    }

    /**
	 * This function applies the DWT filter to a source with odd length and odd phase.
	 *
	 * @param src a float array of the image samples
	 * @param WTTypes Filter to apply
	 * @return a float array that contains the detransformed sources
	 *
	 * @throws ErrorException when wavelet type is unrecognized
	 */
    private float[] oddOddFiltering(float[] src, int WTTypes) throws ErrorException {
        int subbandSize = src.length;
        int half = subbandSize / 2;
        float dst[] = new float[subbandSize];
        for (int k = 0; k < half; k++) {
            dst[2 * k] = src[k];
            dst[2 * k + 1] = src[half + k + 1];
        }
        dst[subbandSize - 1] = src[half];
        switch(WTTypes) {
            case 1:
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - (float) (Math.floor(((dst[k - 1] + dst[k + 1] + 2) / 4)));
                }
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] + (float) (Math.floor(((dst[k - 1] + dst[k + 1]) / 2)));
                }
                dst[0] = dst[0] + (float) (Math.floor(((dst[1] + dst[1]) / 2)));
                dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(((dst[subbandSize - 2] + dst[subbandSize - 2]) / 2)));
                break;
            case 2:
            case 3:
                final float alfa_97 = -1.586134342059924F;
                final float beta_97 = -0.052980118572961F;
                final float gamma_97 = 0.882911075530934F;
                final float delta_97 = 0.443506852043971F;
                final float nh_97, nl_97;
                if (WTTypes == 2) {
                    nh_97 = 1.230174104914001F;
                    nl_97 = 1F / nh_97;
                } else {
                    nl_97 = 1.14960430535816F;
                    nh_97 = -1F / nl_97;
                }
                for (int k = 0; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] / nh_97;
                    dst[k + 1] = dst[k + 1] / nl_97;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] / nh_97;
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - delta_97 * (dst[k - 1] + dst[k + 1]);
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - gamma_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
                dst[0] = dst[0] - gamma_97 * (dst[1] + dst[1]);
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - gamma_97 * (dst[k - 1] + dst[k + 1]);
                }
                for (int k = 1; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - beta_97 * (dst[k - 1] + dst[k + 1]);
                }
                dst[subbandSize - 1] = dst[subbandSize - 1] - alfa_97 * (dst[subbandSize - 2] + dst[subbandSize - 2]);
                dst[0] = dst[0] - alfa_97 * (dst[1] + dst[1]);
                for (int k = 2; k < subbandSize - 1; k += 2) {
                    dst[k] = dst[k] - alfa_97 * (dst[k - 1] + dst[k + 1]);
                }
                break;
            case 4:
                if (subbandSize >= 6) {
                    final float alfa1 = (9F / 16F);
                    final float alfa2 = (1F / 16F);
                    final float beta = (1F / 4F);
                    for (int k = 1; k < subbandSize; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(-beta * (dst[k - 1] + dst[k + 1]) + 0.5));
                    }
                    dst[0] = dst[0] + (float) (Math.floor(alfa1 * (dst[1] + dst[1]) - alfa2 * (dst[3] + dst[3]) + 0.5));
                    dst[2] = dst[2] + (float) (Math.floor(alfa1 * (dst[1] + dst[3]) - alfa2 * (dst[1] + dst[5]) + 0.5));
                    for (int k = 4; k < subbandSize - 3; k += 2) {
                        dst[k] = dst[k] + (float) (Math.floor(alfa1 * (dst[k - 1] + dst[k + 1]) - alfa2 * (dst[k - 3] + dst[k + 3]) + 0.5));
                    }
                    dst[subbandSize - 3] = dst[subbandSize - 3] + (float) (Math.floor(alfa1 * (dst[subbandSize - 4] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 6] + dst[subbandSize - 2]) + 0.5));
                    dst[subbandSize - 1] = dst[subbandSize - 1] + (float) (Math.floor(alfa1 * (dst[subbandSize - 2] + dst[subbandSize - 2]) - alfa2 * (dst[subbandSize - 4] + dst[subbandSize - 4]) + 0.5));
                } else {
                    throw new ErrorException("Size should be greater or equal than 6 in order to perform 9/7M");
                }
                break;
            case 7:
                float sample1 = 0, sample2 = 0;
                float normFactor = (float) (Math.sqrt(2));
                for (int k = 0; k < subbandSize - 1; k += 2) {
                    sample1 = dst[k] + dst[k + 1];
                    sample2 = dst[k] - dst[k + 1];
                    dst[k] = sample1 * normFactor;
                    dst[k + 1] = sample2 * normFactor;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1];
                break;
            case 8:
                float s = 0;
                for (int k = 0; k < subbandSize - 1; k += 2) {
                    s = dst[k] - (float) Math.floor(dst[k + 1] / 2);
                    dst[k] = dst[k + 1] + s;
                    dst[k + 1] = s;
                }
                dst[subbandSize - 1] = dst[subbandSize - 1];
                break;
            default:
                throw new ErrorException("Unrecognized wavelet transform type.");
        }
        return (dst);
    }

    /**
	 * This function expands the source array length to transform if it's length is less than 6.
	 *
	 * @param src a float array of the image samples
	 * @return an extended float array with the image samples with length size of 6.
	 * @throws ErrorException when the src length is out of the range: 1 to 5 or undefined.
	 */
    private float[] coefExpansion(float[] src) throws ErrorException {
        float[] extended = new float[6];
        if (src == null) {
            throw new ErrorException("The source array is null");
        }
        switch(src.length) {
            case 1:
                for (int i = 0; i < 6; i++) {
                    extended[i] = src[0];
                }
                break;
            case 2:
                for (int i = 0; i < 6; i += 2) {
                    extended[i] = src[0];
                }
                for (int i = 1; i < 6; i += 2) {
                    extended[i] = src[1];
                }
                break;
            case 3:
                for (int i = 0; i < 3; i++) {
                    extended[i] = src[i];
                }
                for (int i = 3; i < 5; i++) {
                    extended[i] = src[4 - i];
                }
                extended[5] = src[1];
                break;
            case 4:
                for (int i = 0; i < 4; i++) {
                    extended[i] = src[i];
                }
                for (int i = 4; i < 6; i++) {
                    extended[i] = src[6 - i];
                }
                break;
            case 5:
                for (int i = 0; i < 5; i++) {
                    extended[i] = src[i];
                }
                extended[5] = src[3];
                break;
            default:
                throw new ErrorException("The source array length is out of range 1 - 5");
        }
        return (extended);
    }

    /**
	 * This function recovers a filtered array with original length from an expanded filtered source array.
	 *
	 * @param filt a float array of the image samples
	 * @param origSize the original length of the array
	 * @return a recovered float array with the transformed image samples with it's original length.
	 * @throws ErrorException when filt length is different of 6 or undefined and when the original length does not correspond to a valid source.
	 */
    private float[] coefUnexpansion(float[] filt, int origSize) throws ErrorException {
        float[] recovered = new float[origSize];
        if (filt == null) {
            throw new ErrorException("The filtered array is null!");
        }
        if (filt.length != 6) {
            throw new ErrorException("The filtered array does not have size of 6");
        }
        switch(origSize) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                for (int i = 0; i < origSize; i++) {
                    recovered[i] = filt[i];
                }
                break;
            default:
                throw new ErrorException("The array original length is out of the range 1 - 5");
        }
        return (recovered);
    }
}
