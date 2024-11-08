package shu.cms.measure.meterapi;

import com.jacob.com.*;
import shu.cms.measure.meterapi.ca210api.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 * �PCA210���������O
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class CA210API {

    public static enum Probe {

        Universal, SmallUniversal
    }

    public static final double getAccuracy(Probe probe, double luminance) {
        double[][] accuracy = null;
        switch(probe) {
            case Universal:
                accuracy = ACCURACY_ARRAY;
                break;
            case SmallUniversal:
                accuracy = SMALL_ACCURACY_ARRAY;
                break;
        }
        int size = accuracy.length;
        for (int x = 0; x < size; x++) {
            double[] data = accuracy[x];
            if (luminance >= data[0] && luminance < data[1]) {
                return data[2];
            }
        }
        return -1;
    }

    private static final double[][] ACCURACY_ARRAY = new double[][] { { 0.1, 5, 0.008 }, { 5, 40, 0.005 }, { 160, 160, 0.002 }, { 40, 1000, 0.003 } };

    private static final double[][] SMALL_ACCURACY_ARRAY = new double[][] { { 0.3, 15, 0.008 }, { 15, 120, 0.005 }, { 160, 160, 0.002 }, { 120, 3000, 0.003 } };

    private static final double[][] REPEATABILITY_ARRAY = new double[][] { { 0.1, 0.2, 0.015 }, { 0.2, 0.5, 0.008 }, { 0.5, 2, 0.003 }, { 2.0, 1000, 0.001 } };

    private static final double[][] SMALL_REPEATABILITY_ARRAY = new double[][] { { 0.3, 0.6, 0.015 }, { 0.6, 1.5, 0.008 }, { 1.5, 6.0, 0.003 }, { 6.0, 3000, 0.001 } };

    /**
   * ��o�W��ѤW�����Ʃ�
   * @param probe Probe
   * @param luminance double
   * @return double delta xy
   */
    public static final double getxyRepeatabilityBySpecification(Probe probe, double luminance) {
        double[][] repeatability = null;
        switch(probe) {
            case Universal:
                repeatability = REPEATABILITY_ARRAY;
                break;
            case SmallUniversal:
                repeatability = SMALL_REPEATABILITY_ARRAY;
                break;
        }
        int size = repeatability.length;
        for (int x = 0; x < size; x++) {
            double[] data = repeatability[x];
            if (luminance >= data[0] && luminance < data[1]) {
                return data[2];
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        Ca200 ca = new Ca200();
        ca.autoConnect();
        ca.release();
        System.exit(0);
    }

    public CA210API() {
        System.setProperty("com.jacob.autogc", "true");
        ComThread.InitMTA(true);
        ca = new Ca200();
        ca.autoConnect();
        ica = ca.getSingleCa();
        probe = ica.getSingleProbe();
        memory = ica.getMemory();
    }

    public void close() {
        memory.release();
        memory = null;
        probe.release();
        probe = null;
        ica.release();
        ca.release();
        ica = null;
        ca = null;
        System.gc();
    }

    protected Ca200 ca;

    protected ICa ica;

    protected IProbe probe;

    protected IMemory memory;

    public void calibrate() {
        ica.calZero();
    }

    public float[] triggerMeasurement() {
        ica.measure();
        float[] result = new float[] { probe.getX(), probe.getY(), probe.getZ() };
        return result;
    }

    public static enum AveragingMode {

        SLOW(0), FAST(1), AUTO(2);

        AveragingMode(int value) {
            this.value = value;
        }

        int value;

        public static final AveragingMode getAveragingMode(int mode) {
            switch(mode) {
                case 0:
                    return SLOW;
                case 1:
                    return FAST;
                case 2:
                    return AUTO;
                default:
                    return null;
            }
        }
    }

    public AveragingMode getAveragingMode() {
        int mode = ica.getAveragingMode();
        return AveragingMode.getAveragingMode(mode);
    }

    public void setAveragingMode(AveragingMode mode) {
        ica.setAveragingMode(mode.value);
    }

    public void setCalStandard(CalStandard calStandard) {
        ica.setCalStandard(calStandard.value);
    }

    public CalStandard getCalStandard() {
        int std = ica.getCalStandard();
        return CalStandard.getCalStandard(std);
    }

    public static enum CalStandard {

        CT6500K(1), CT9300K(2);

        CalStandard(int value) {
            this.value = value;
        }

        int value;

        public static final CalStandard getCalStandard(int mode) {
            switch(mode) {
                case 1:
                    return CT6500K;
                case 2:
                    return CT9300K;
                default:
                    return null;
            }
        }
    }

    public void setChannelNO(int no) {
        memory.setChannelNO(no);
    }

    public int getChannelNO() {
        return memory.getChannelNO();
    }

    public static enum RemoteMode {

        OFF(0), ON(1), LOCKED(2);

        RemoteMode(int value) {
            this.value = value;
        }

        int value;

        public static final RemoteMode getRemoteMode(int mode) {
            switch(mode) {
                case 0:
                    return OFF;
                case 1:
                    return ON;
                case 2:
                    return LOCKED;
                default:
                    return null;
            }
        }
    }

    public void setRemoteMode(RemoteMode mode) {
        ica.setRemoteMode(mode.value);
    }

    public static enum SyncMode {

        NTSC(0), PAL(1), EXT(2), UNIV(3), INT(-1);

        SyncMode(int value) {
            this.value = value;
        }

        int value;

        public static final SyncMode getSyncMode(int mode) {
            switch(mode) {
                case 0:
                    return NTSC;
                case 1:
                    return PAL;
                case 2:
                    return EXT;
                case 3:
                    return UNIV;
                default:
                    return INT;
            }
        }
    }

    public SyncMode getSyncMode() {
        return SyncMode.getSyncMode((int) ica.getSyncMode());
    }

    public void setSyncMode(SyncMode syncMode) {
        ica.setSyncMode(syncMode.value);
    }

    public void setSyncMode(float frequency) {
        ica.setSyncMode(frequency);
    }
}
