package ch.unizh.ini.jaer.projects.opticalflow.mdc2d;

import net.sf.jaer.util.jama.Matrix;
import ch.unizh.ini.jaer.projects.opticalflow.*;
import java.lang.Math.*;

/**
 * Packs data returned from optical flow sensor.
 * The different methods to extract motion out of raw data are implemented in this
 * class

 * @author reto
 */
public class MotionDataMDC2D extends MotionData {

    public static final int LMC1 = BIT5, LMC2 = BIT6, ON_CHIP_ADC = BIT7;

    private float[][] lmc1;

    private float[][] lmc2;

    private int channel;

    public static int globalScaleFactor = 10;

    private int numLocalToAverageForGlobal;

    /** Creates a new instance of MotionData */
    public MotionDataMDC2D(Chip2DMotion setchip) {
        super(setchip);
        lmc1 = new float[chip.getSizeX()][chip.getSizeY()];
        lmc2 = new float[chip.getSizeX()][chip.getSizeY()];
        NUM_PASTMOTIONDATA = 50;
    }

    public static float thresh = 0;

    public static float match = 0;

    public static int temporalAveragesNum = 5;

    @Override
    protected void fillPh() {
        this.setPh(extractRawChannel(0));
        this.fillAdditional();
    }

    @Override
    protected void fillUxUy() {
        int algorithm = MDC2D.getMotionMethod();
        channel = MDC2D.getChannelForMotionAlgorithm();
        numLocalToAverageForGlobal = chip.NUM_MOTION_PIXELS;
        switch(algorithm) {
            case MDC2D.NORMAL_OPTICFLOW:
                this.calculateMotion_gradientBased();
                this.localUxUy_temporalAverage(3);
                this.globalUxUy_averageLocal();
                this.globalUxUy_temporalAverage(temporalAveragesNum);
                break;
            case MDC2D.SRINIVASAN:
                this.setUx(zero());
                this.setUy(zero());
                this.calculateMotion_srinivasan();
                this.globalUxUy_temporalAverage(temporalAveragesNum);
                break;
            case MDC2D.LOCAL_SRINIVASAN:
                this.setUx(zero());
                this.setUy(zero());
                this.calculateMotion_localSrinivasan(4);
                this.localUxUy_temporalAverage(3);
                this.globalUxUy_averageLocal();
                this.globalUxUy_temporalAverage(temporalAveragesNum);
                break;
            case MDC2D.TIME_OF_TRAVEL:
                float thresholdContrast = thresh / 100;
                float matchContrast = match / 100;
                this.calculateMotion_timeOfTravel_absValue(thresholdContrast, matchContrast);
                this.globalUxUy_averageLocal();
                this.globalUxUy_temporalAverage(temporalAveragesNum);
                break;
            case MDC2D.RANDOM:
                this.calculateMotion_random();
                this.globalUxUy_temporalAverage(temporalAveragesNum);
                this.globalUxUy_averageLocal();
                break;
        }
    }

    @Override
    protected void fillMinMax() {
        minph = 1;
        minux = 1;
        minuy = 1;
        maxph = 0;
        maxux = 0;
        maxuy = 0;
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                float a = ux[i][j];
                if (ux[i][j] < minux) minux = ux[i][j];
                if (ux[i][j] > maxux) maxux = ux[i][j];
                if (uy[i][j] < minuy) minuy = uy[i][j];
                if (uy[i][j] > maxuy) maxux = uy[i][j];
                if (ph[i][j] < minph) minph = ph[i][j];
                if (ph[i][j] > maxph) maxph = ph[i][j];
            }
        }
    }

    @Override
    protected void fillAdditional() {
        this.lmc1 = extractRawChannel(1);
        this.lmc2 = extractRawChannel(2);
    }

    /**
     * NORMAL OPTIC FLOW algorithm
     * This is the implementation of a gradient based optical flow algorithm.
     * As additional constraint it is assumed that the correct flow is perpen-
     * dicular to the pixel orientation.
     * this results in the equations ux=-(Ix *It)/(Ix^2 +Iy^2)
     *                           and uy=-(Iy *It)/(Ix^2 +Iy^2)
     * with Ix=dI/dt, Iy=dI/dy, It=dI/dt
     */
    protected void calculateMotion_gradientBased() {
        float dIdx;
        float dIdy;
        float dIdt;
        float[][] raw = this.extractRawChannel(channel);
        float[][] past = this.getPastMotionData()[0].extractRawChannel(channel);
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                if (j == 0 || i == 0 || j == chip.NUM_COLUMNS - 1 || i == chip.NUM_ROWS - 1) {
                    ux[j][i] = (float) 0;
                    uy[j][i] = (float) 0;
                } else {
                    dIdx = (raw[j][i + 1] - raw[j][i - 1]) / 2;
                    dIdy = (raw[j + 1][i] - raw[j - 1][i]) / 2;
                    long dt = getTimeCapturedMs() - getPastMotionData()[0].getTimeCapturedMs();
                    dIdt = (raw[j][i] - past[j][i]) / (float) dt;
                    if (dIdx * dIdx + dIdy * dIdy != 0 && dt != 0) {
                        ux[j][i] = -dIdx * dIdt / (dIdx * dIdx + dIdy * dIdy);
                        uy[j][i] = -dIdy * dIdt / (dIdx * dIdx + dIdy * dIdy);
                    } else {
                        ux[j][i] = 0;
                        uy[j][i] = 0;
                    }
                }
            }
        }
    }

    /**
     * OPTIC FLOW ALGORITHM BY SRINIVASAN 
     * This assumes that the brightness I(t,x,y) is a approximately a linear
     * combination of x=-n...n I(t-1,x+-x, y+-x).
     * Rotation is not calculated and should not appear in the image.
     * The algorithm computes a global motion.
     */
    protected void calculateMotion_srinivasan() {
        float[][] raw = this.extractRawChannel(channel);
        float[][] past = this.getPastMotionData()[0].extractRawChannel(channel);
        Matrix A = new Matrix(new double[2][2]);
        Matrix b = new Matrix(new double[2][1]);
        float a11 = 0, a12 = 0;
        float a21 = 0, a22 = 0;
        float b1 = 0, b2 = 0;
        float f1, f2, f3, f4;
        try {
            for (int x = 1; x < chip.NUM_COLUMNS - 1; x++) {
                for (int y = 1; y < chip.NUM_ROWS - 1; y++) {
                    f1 = past[y][x + 1];
                    f2 = past[y][x - 1];
                    f3 = past[y + 1][x];
                    f4 = past[y - 1][x];
                    a11 += (f2 - f1) * (f2 - f1);
                    a12 += (f4 - f3) * (f2 - f1);
                    a21 += (f4 - f3) * (f2 - f1);
                    a22 += (f4 - f3) * (f4 - f3);
                    b1 += 2 * (raw[y][x] - past[y][x]) * (f2 - f1);
                    b2 += 2 * (raw[y][x] - past[y][x]) * (f4 - f3);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
        }
        A.set(0, 0, a11);
        A.set(0, 1, a12);
        A.set(1, 0, a21);
        A.set(1, 1, a22);
        b.set(0, 0, b1);
        b.set(1, 0, b2);
        long dt = getTimeCapturedMs() - getPastMotionData()[0].getTimeCapturedMs();
        try {
            Matrix x = A.solve(b);
            if (dt != 0) {
                this.setGlobalX((float) x.get(0, 0) / (float) dt * globalScaleFactor);
                this.setGlobalY((float) x.get(1, 0) / (float) dt * globalScaleFactor);
            } else {
                this.setGlobalX((0));
                this.setGlobalY((0));
            }
        } catch (Exception e) {
            this.setGlobalX(0);
            this.setGlobalY(0);
            System.out.println("Matrix decomposition failed. No global motion vector computed");
        }
    }

    /**
     * OPTIC FLOW ALGORITHM BY SRINIVASAN INCLUDING ROTATION
     * This assumes that the brightness I(t,x,y) is a approximately a linear
     * combination of x=-n...n I(t-1,x+-x, y+-x).
     *
     * The algorithm computes a global motion.
     */
    protected void calculateMotion_srinivasan_inclRot() {
        float phi = 45;
        phi = (float) Math.toRadians(phi);
        float[][] raw = this.extractRawChannel(channel);
        float[][] past = this.getPastMotionData()[0].extractRawChannel(channel);
        Matrix A = new Matrix(new double[3][3]);
        Matrix b = new Matrix(new double[3][1]);
        float a11 = 0, a12 = 0, a13 = 0;
        float a21 = 0, a22 = 0, a23 = 0;
        float a31 = 0, a32 = 0, a33 = 0;
        float b1 = 0, b2 = 0, b3 = 0;
        float f1, f2, f3, f4, f5, f6;
        double xprime, yprime;
        try {
            for (int x = 1; x < chip.NUM_COLUMNS - 1; x++) {
                for (int y = 1; y < chip.NUM_ROWS - 1; y++) {
                    f1 = past[y][x + 1];
                    f2 = past[y][x - 1];
                    f3 = past[y + 1][x];
                    f4 = past[y - 1][x];
                    xprime = x * Math.cos(phi) + y * Math.sin(phi);
                    yprime = x * Math.sin(phi) + y * Math.cos(phi);
                    xprime = x * Math.cos(-phi) + y * Math.sin(-phi);
                    yprime = x * Math.sin(-phi) + y * Math.cos(-phi);
                    a11 += (f2 - f1) * (f2 - f1);
                    a12 += (f4 - f3) * (f2 - f1);
                    a21 += (f4 - f3) * (f2 - f1);
                    a22 += (f4 - f3) * (f4 - f3);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
        }
        long dt = getTimeCapturedMs() - getPastMotionData()[0].getTimeCapturedMs();
        try {
            Matrix x = A.solve(b);
            if (dt != 0) {
                this.setGlobalX((float) x.get(0, 0) / (float) dt * globalScaleFactor);
                this.setGlobalY((float) x.get(1, 0) / (float) dt * globalScaleFactor);
            } else {
                this.setGlobalX((0));
                this.setGlobalY((0));
            }
        } catch (Exception e) {
            this.setGlobalX(0);
            this.setGlobalY(0);
            System.out.println("Matrix decomposition failed. No global motion vector computed");
        }
    }

    /**
     * LOCAL OPTIC FLOW ALGORITHM BY SRINIVASAN
     * This assumes that the brightness I(t,x,y) is a approximately a linear
     * combination of x=-n...n I(t-1,x+-x, y+-x).
     * Rotation is not calculated and should not appear in the image.
     * The algorithm computes is the same as in calculateMotion_srinivasan. The
     * only difference is that here the picture is split into sections for which
     * motion is calculated separatly using the srinivasan algorithm.
     */
    protected void calculateMotion_localSrinivasan(int divideSideBy) {
        float[][] globalRaw = this.extractRawChannel(channel);
        float[][] globalPast = this.getPastMotionData()[0].extractRawChannel(channel);
        int div = divideSideBy;
        int subarrayLength = chip.NUM_COLUMNS / div;
        float[][] localRaw = new float[subarrayLength][subarrayLength];
        float[][] localPast = new float[subarrayLength][subarrayLength];
        Matrix A = new Matrix(new double[2][2]);
        Matrix b = new Matrix(new double[2][1]);
        float a11 = 0, a12 = 0;
        float a21 = 0, a22 = 0;
        float b1 = 0, b2 = 0;
        long dt = getTimeCapturedMs() - getPastMotionData()[0].getTimeCapturedMs();
        for (int i = 0; i < div; i++) {
            for (int j = 0; j < div; j++) {
                for (int c = 0; c < subarrayLength; c++) {
                    for (int d = 0; d < subarrayLength; d++) {
                        localRaw[c][d] = globalRaw[j * subarrayLength + c][i * subarrayLength + d];
                        localPast[c][d] = globalPast[j * subarrayLength + c][i * subarrayLength + d];
                    }
                }
                for (int x = 1; x < subarrayLength - 1; x++) {
                    for (int y = 1; y < subarrayLength - 1; y++) {
                        a11 += (localPast[y][x - 1] - localPast[y][x + 1]) * (localPast[y][x - 1] - localPast[y][x + 1]);
                        a12 += (localPast[y - 1][x] - localPast[y + 1][x]) * (localPast[y][x - 1] - localPast[y][x + 1]);
                        a21 += (localPast[y - 1][x] - localPast[y + 1][x]) * (localPast[y][x - 1] - localPast[y][x + 1]);
                        a22 += (localPast[y - 1][x] - localPast[y + 1][x]) * (localPast[y - 1][x] - localPast[y + 1][x]);
                        b1 += 2 * (localRaw[y][x] - localPast[y][x]) * (localPast[y][x - 1] - localPast[y][x + 1]);
                        b2 += 2 * (localRaw[y][x] - localPast[y][x]) * (localPast[y - 1][x] - localPast[y + 1][x]);
                    }
                }
                A.set(0, 0, a11);
                A.set(0, 1, a12);
                A.set(1, 0, a21);
                A.set(1, 1, a22);
                b.set(0, 0, b1);
                b.set(1, 0, b2);
                try {
                    Matrix x = A.solve(b);
                    if (dt != 0) {
                        this.ux[(int) ((j + 0.5) * subarrayLength)][(int) ((i + 0.5) * subarrayLength)] = ((float) x.get(0, 0) / (float) dt * subarrayLength * subarrayLength);
                        this.uy[(int) ((j + 0.5) * subarrayLength)][(int) ((i + 0.5) * subarrayLength)] = ((float) x.get(1, 0) / (float) dt * subarrayLength * subarrayLength);
                    } else {
                        ;
                    }
                } catch (Exception e) {
                    ;
                    System.out.println("Matrix decomposition failed. ");
                }
            }
        }
    }

    /**
     * Looks for pixels with high spatial contrast in horizontal or vertical direction.
     * Lower contrasts are ignored and also not taken into account for the global
     * motion. Channel is only lmc1 since this is a amplified and differentiated version
     * of the photoreceptor stage. Its feature is a high reaction to spatial contrasts.
     */
    protected void calculateMotion_timeOfTravel_absValue(float thresholdPercentage, float contrastMatch) {
        numLocalToAverageForGlobal = chip.NUM_MOTION_PIXELS;
        int ttChannel = 1;
        int numPastFrame = 5;
        float[][] raw = this.extractRawChannel(ttChannel);
        float[][][] past = new float[numPastFrame][chip.NUM_ROWS][chip.NUM_COLUMNS];
        long dt;
        if (thresholdPercentage > 0.5) {
            thresholdPercentage = (float) 0.5;
            System.out.println("WARNING: The threshold percentage in Time of Travel method is high. Thus a large number of pixel will be analyzed, what possibly decreases reliability");
        }
        float max = 0, min = 1;
        for (int i = 0; i < chip.NUM_ROWS; i++) {
            for (int j = 0; j < chip.NUM_COLUMNS; j++) {
                if (raw[j][i] < min) min = raw[j][i];
                if (raw[j][i] > max) max = raw[j][i];
            }
        }
        try {
            for (int i = 0; i < numPastFrame; i++) {
                past[i] = this.getPastMotionData()[0].extractRawChannel(ttChannel);
            }
            int offset = 3;
            float localValue;
            for (int i = 0; i < chip.NUM_COLUMNS; i++) {
                for (int j = 0; j < chip.NUM_ROWS; j++) {
                    if (i < offset || j < offset || i >= chip.NUM_ROWS - offset || j >= chip.NUM_COLUMNS - offset) {
                        ux[j][i] = 0;
                        uy[j][i] = 0;
                        this.numLocalToAverageForGlobal--;
                    } else {
                        thresholdPercentage = (float) 0.1;
                        localValue = raw[j][i];
                        if (localValue < max - (max - min) * thresholdPercentage && localValue > min + (max - min) * thresholdPercentage) {
                            ux[j][i] = 0;
                            uy[j][i] = 0;
                            this.numLocalToAverageForGlobal--;
                        } else {
                            ux[j][i] = 1;
                            uy[j][i] = 1;
                            {
                                boolean match = false;
                                int cnt = 0;
                                while (!match) {
                                    if (Math.abs(past[cnt][j][i + offset] / localValue - 1) < contrastMatch) {
                                        dt = this.getTimeCapturedMs() - this.pastMotionData[cnt].getTimeCapturedMs();
                                        if (dt == 0) {
                                            ux[j][i] = 0;
                                        } else {
                                            ux[j][i] = -1 / (float) dt;
                                            System.out.println("ux=" + ux[j][i]);
                                        }
                                        match = true;
                                    }
                                    if (Math.abs(past[cnt][j][i - offset] / localValue - 1) < contrastMatch) {
                                        dt = this.getTimeCapturedMs() - this.pastMotionData[cnt].getTimeCapturedMs();
                                        if (dt == 0) {
                                            ux[j][i] = 0;
                                        } else {
                                            ux[j][i] = (1 / (float) dt + ux[j][i]) / 2;
                                            System.out.println("ux=" + ux[j][i]);
                                        }
                                        match = true;
                                    }
                                    cnt++;
                                    if (cnt >= numPastFrame - 1) {
                                        ux[j][i] = 0;
                                        break;
                                    }
                                }
                            }
                            {
                                boolean match = false;
                                int cnt = 0;
                                while (!match) {
                                    if (Math.abs(past[cnt][j + offset][i] / localValue - 1) < contrastMatch) {
                                        dt = this.getTimeCapturedMs() - this.pastMotionData[cnt].getTimeCapturedMs();
                                        if (dt == 0) {
                                            uy[j][i] = 0;
                                        } else {
                                            uy[j][i] = -1 / (float) dt;
                                            System.out.println("uy=" + uy[j][i]);
                                        }
                                        match = true;
                                    }
                                    if (Math.abs(past[cnt][j - offset][i] / localValue - 1) < contrastMatch) {
                                        dt = this.getTimeCapturedMs() - this.pastMotionData[cnt].getTimeCapturedMs();
                                        if (dt == 0) {
                                            uy[j][i] = 0;
                                        } else {
                                            uy[j][i] = (1 / (float) dt + uy[j][i]) / 2;
                                            System.out.println("uy=" + uy[j][i]);
                                        }
                                        match = true;
                                    }
                                    cnt++;
                                    if (cnt >= numPastFrame - 1) {
                                        uy[j][i] = 0;
                                        if (ux[j][i] == 0) numLocalToAverageForGlobal--;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            System.out.println(e + " tried to access past data, which doesnt exist yet. Ignore and wait some frames");
        }
    }

    /**
//    protected void calculateMotion_timeOfTravel(float thresholdContrast, float contrastMatch){
//        numLocalToAverageForGlobal=chip.NUM_MOTION_PIXELS;
//        int ttChannel=1;//use the lmc1 channel for this method.
//        int numPastFrame=5;
//        float[][] raw=this.extractRawChannel(ttChannel);
//        float[][][] past = new float[numPastFrame][chip.NUM_ROWS][chip.NUM_COLUMNS];
//        long dt;
//        
//        //calculate the max and min of the raw data
//        float max=0, min=0;
//        for(int i=0;i<chip.NUM_ROWS;i++){
//            for(int j=0;j<chip.NUM_COLUMNS;j++){
//                if (raw[j][i]<min) min=raw[j][i];
//                if (raw[j][i]>max) max=raw[j][i];
//            }
//        }
//
//        try { //to handle nullpointerException which is thrown when less pastMotionData then required are available. This is only the case at startup
//            //get past Frames
//            for(int i=0; i<numPastFrame; i++){
//                past[i]=this.getPastMotionData()[0].extractRawChannel(ttChannel);
//            }
//            float localContrastX;
//            float localContrastY;
//            for(int i=0; i<chip.NUM_COLUMNS;i++){
//                for(int j=0; j<chip.NUM_ROWS; j++){
//                    //at the borders its not possible to calculate motion because one would get ArrayOutOfBoundException. So set it to 0
//                    if(i==0||j==0||i>chip.NUM_ROWS-3||j>chip.NUM_COLUMNS-3){
//                        ux[j][i]=0;
//                        uy[j][i]=0;
////                        this.numLocalToAverageForGlobal--;
//                    }else{
//                        {//First look for motion in x direction
//                            localContrastX=raw[j][i]/raw[j][i+1];
//                            if(localContrastX>thresholdContrast){ //low contrast: ignore pixel
//                                ux[j][i]=0;
////                                this.numLocalToAverageForGlobal--;
//                            }else{ //contrast is high. look for the same contrast in x direction. Look only at the next pixel to the left and right but at different frames
//                                boolean match=false;
//                                int cnt=0;
//                                while(!match){
//                                    if(Math.abs(past[cnt][j][i+1-1] / localContrastX  -1)<contrastMatch){ //if contrast at neighbor at t-1 is within +-contrastMatch% of the contrast at the current pixel at t this is considered as match
//                                       dt=this.getTimeCapturedMs()-this.pastMotionData[cnt].getTimeCapturedMs();
//                                       if(dt==0){
//                                           ux[j][i]=0;
//                                       }else {
//                                            ux[j][i]=1/dt;
//                                       } // 1pixel in dt since we only look at one neighboring pixel
//                                       match=true;
//                                    } else if(Math.abs(past[cnt][j][i+1+1] / localContrastX  -1)<contrastMatch){
//                                       dt=this.getTimeCapturedMs()-this.pastMotionData[cnt].getTimeCapturedMs();
//                                       if(dt==0){
//                                            ux[j][i]=0;
//                                       }else{
//                                          ux[j][i]=-1/dt; // 1pixel in dt since we only look at one neighboring pixel
//                                       match=true;
//                                       }
//                                    }
//                                    cnt++;
//                                    if(cnt>=numPastFrame-1){
//                                        ux[j][i]=0;
//                                        break;
//                                    }
//                                }
//                            }
//                         }
//                         {//First look for motion in y direction
//                            localContrastY=raw[j][i]/raw[j+1][i];
//                            if(localContrastX<thresholdContrast){ //low contrast: ignore pixel
//                                ux[j][i]=0;
////                                this.numLocalToAverageForGlobal--;
//                            }else{ //contrast is high. look for the same contrast in x direction. Look only at the next pixel to the left and right but at different frames
//                                boolean match=false;
//                                int cnt=0;
//                                while(!match){
//                                    if(Math.abs(past[cnt][j+1-1][i] / localContrastX  -1)<contrastMatch){ //if contrast at neighbor at t-1 is within +-contrastMatch% of the contrast at the current pixel at t this is considered as match
//                                       dt=this.getTimeCapturedMs()-this.pastMotionData[cnt].getTimeCapturedMs();
//                                       if(dt==0){
//                                            uy[j][i]=0;
////                                            if(ux[j][i]==0) this.numLocalToAverageForGlobal--;
//                                       }else {
//                                           uy[j][i] = 1 / dt;// 1pixel in dt since we only look at one neighboring pixel
//                                           match=true;
//                                       } 
//                                    } else if(Math.abs(past[cnt][j+1+1][i] / localContrastX  -1)<contrastMatch){
//                                       dt=this.getTimeCapturedMs()-this.pastMotionData[cnt].getTimeCapturedMs();
//                                       if(dt==0){
//                                            uy[j][i]=0;
////                                            if(ux[j][i]==0) this.numLocalToAverageForGlobal--;
//                                       }else {
//                                           uy[j][i]=-1/dt; // 1pixel in dt since we only look at one neighboring pixel
//                                           match=true;
//                                       }
//                                    }
//                                    cnt++;
//                                    if(cnt==numPastFrame-1){
//                                        uy[j][i]=0;
//                                        break;
//                                    }
//                                }
//                            }
//                         }  System.out.println("ux="+ux[j][i]); System.out.println("uy="+uy[j][i]);
//                    }
//                }
//            }
//
//        } catch(NullPointerException e) {
//            System.out.println(e+" tried to access past data, which doesnt exist yet. Ignore and wait some frames");
//        }
//        System.out.println("numValid"+this.numLocalToAverageForGlobal);
//    }
    
     * 
*/
    private void pixel_setInvalid(int j, int i) {
        ux[j][i] = 0;
        uy[j][i] = 0;
        this.numLocalToAverageForGlobal--;
    }

    /**
     * RANDOM MOTION
     * This generates random values between -1 and 1 for ux and uy.
     * By itself not very interesting, but useful for testing.
     */
    private void calculateMotion_random() {
        float range = (float) 0.1;
        setUx(randomizeArray(getUx(), -range, range));
        setUy(randomizeArray(getUy(), -range, range));
    }

    /**
     * Implements a difference off gaussian filter with a center of one pixel
     * and souuound of the 8 neighboring pixels. Center has weight +1, while
     * each pixel of the surround has weight -1/8.
     */
    private float[][] filter_DOG(float[][] arrayToFilter) {
        int maxi = arrayToFilter.length;
        int maxj = arrayToFilter[1].length;
        float[][] filtered = new float[maxj][maxi];
        for (int i = 0; i < maxi; i++) {
            for (int j = 0; j < maxj; j++) {
                if (j == 0 || i == 0 || j == maxj - 1 || i == maxi - 1) {
                    filtered[j][i] = (float) 0;
                } else {
                    filtered[j][i] = arrayToFilter[j][i] - (float) 0.1 * (arrayToFilter[j - 1][i - 1] + arrayToFilter[j - 1][i] + arrayToFilter[j - 1][i + 1] + arrayToFilter[j][i - 1] + arrayToFilter[j][i + 1] + arrayToFilter[j + 1][i - 1] + arrayToFilter[j + 1][i] + arrayToFilter[j + 1][i + 1]);
                }
            }
        }
        return filtered;
    }

    protected void globalUxUy_averageLocal() {
        float globalUx = 0;
        float globalUy = 0;
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                globalUx += getUx()[i][j];
                globalUy += getUy()[i][j];
            }
        }
        if (numLocalToAverageForGlobal != 0) {
            globalUx /= this.numLocalToAverageForGlobal;
            this.setGlobalX(globalUx * globalScaleFactor);
            globalUy /= this.numLocalToAverageForGlobal;
            this.setGlobalY(globalUy * globalScaleFactor);
        } else {
            globalUx = 0;
            globalUy = 0;
        }
    }

    protected void globalUxUy_temporalAverage(int num) {
        if (num > this.NUM_PASTMOTIONDATA) num = this.NUM_PASTMOTIONDATA;
        for (int i = 0; i < num; i++) {
            try {
                globalX += pastMotionData[i].getGlobalX();
                globalY += pastMotionData[i].getGlobalY();
            } catch (NullPointerException e) {
                num--;
                System.out.println(e + ":can be ignored. When more frames captured its ok");
            }
        }
        globalX /= num + 1;
        globalY /= num + 1;
    }

    protected void localUxUy_temporalAverage(int num) {
        if (num > this.NUM_PASTMOTIONDATA) num = this.NUM_PASTMOTIONDATA;
        for (int i = 0; i < num; i++) {
            try {
                for (int j = 0; j < chip.NUM_COLUMNS; j++) {
                    for (int k = 0; k < chip.NUM_ROWS; k++) {
                        ux[j][k] += pastMotionData[i].getUx()[j][k];
                        uy[j][k] += pastMotionData[i].getUy()[j][k];
                    }
                }
            } catch (NullPointerException e) {
                num--;
                System.out.println(e + ":can be ignored. When more frames captured its ok");
            }
        }
        for (int j = 0; j < chip.NUM_COLUMNS; j++) {
            for (int k = 0; k < chip.NUM_ROWS; k++) {
                ux[j][k] /= num + 1;
                uy[j][k] /= num + 1;
            }
        }
    }

    /**
     * Goes through the ux and uy arrays simultaniously. At each pixel it compares
     * the (ux,uy) vector to the 8 neighboring ones. To each of the neighbors the
     * cosine of the angle between the two vectors is computed. This is taken as
     * correlation measure. The cosines from the current pixel to all its neighbors
     * are avereged. If the average is smaller then a given value the motion at this
     * pixel (ux and uy) is considered invalid. The value of the pixel is set to 0.
     */
    protected void localUxUy_filterByCorrelation(double limitCorrelation) {
        numLocalToAverageForGlobal = chip.NUM_MOTION_PIXELS;
        float[][] copyux = new float[chip.NUM_COLUMNS][chip.NUM_ROWS], copyuy = new float[chip.NUM_COLUMNS][chip.NUM_ROWS];
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                copyux[j][i] = ux[j][i];
                copyuy[j][i] = uy[j][i];
            }
        }
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                if (i == 0 || j == 0 || i == chip.NUM_COLUMNS - 1 || j == chip.NUM_ROWS - 1) {
                    pixel_setInvalid(j, i);
                } else {
                    double u1x, u1y, u2x, u2y;
                    double cos = 0;
                    for (int m = -1; m < 2; m++) {
                        for (int n = -1; n < 2; n++) {
                            if (i != j) {
                                u1x = copyux[i][j];
                                u1y = copyuy[i][j];
                                u2x = copyux[i + m][j + n];
                                u2y = copyuy[i + m][j + n];
                                double scalarprod = (u1x * u2x + u1y * u2y);
                                double u1Norm = Math.sqrt(u1x * u1x + u1y * u1y);
                                double u2Norm = Math.sqrt(u2x * u2x + u2y * u2y);
                                if ((u1x == 0 && u1y == 0) || (u2x == 0 && u2y == 0)) {
                                    cos += 0;
                                } else cos += scalarprod / (u1Norm * u2Norm);
                            }
                        }
                    }
                    cos /= 8;
                    if (cos < limitCorrelation) {
                        pixel_setInvalid(j, i);
                    }
                }
            }
        }
    }

    /**
     * Goes through the ux and uy arrays simultaniously. At each pixel it computes
     * average of the pixel itself and its 8 neighbors. The average is the value
     * assigned for the pixel in the center.
     */
    protected void localUxUy_spatialAverage() {
        numLocalToAverageForGlobal = chip.NUM_MOTION_PIXELS;
        float[][] copyux = new float[chip.NUM_COLUMNS][chip.NUM_ROWS], copyuy = new float[chip.NUM_COLUMNS][chip.NUM_ROWS];
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                copyux[j][i] = ux[j][i];
                copyuy[j][i] = uy[j][i];
            }
        }
        for (int i = 0; i < chip.NUM_COLUMNS; i++) {
            for (int j = 0; j < chip.NUM_ROWS; j++) {
                if (i == 0 || j == 0 || i == chip.NUM_COLUMNS - 1 || j == chip.NUM_ROWS - 1) {
                    pixel_setInvalid(j, i);
                } else {
                    ux[j][i] = 0;
                    for (int v = -1; v <= 1; v++) {
                        for (int w = -1; w <= 1; w++) {
                            ux[j][i] += copyux[j + v][i + w];
                            uy[j][i] += copyuy[j + v][i + w];
                        }
                    }
                    ux[j][i] /= 9;
                    uy[j][i] /= 9;
                }
            }
        }
    }

    @Override
    protected void updateContents() {
        setContents(getContents() | MotionData.GLOBAL_X | MotionData.GLOBAL_Y | MotionData.UX | MotionData.UY);
    }

    public float[][] zero() {
        int maxPos = chip.NUM_COLUMNS;
        float[][] channelData = new float[maxPos][maxPos];
        for (int x = 0; x < maxPos; x++) {
            for (int y = 0; y < maxPos; y++) {
                channelData[y][x] = (float) 0;
            }
        }
        return channelData;
    }
}
