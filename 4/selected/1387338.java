package gov.sns.apps.mpsblmlimits;

import gov.sns.ca.*;
import java.util.*;
import javax.swing.*;

public class ChannelAccess {

    public static int[] MPSReadyToTest_get = { -1, -1, -1, -1, -1 };

    public static int[] MPSHPRFCheckTrips_get = new int[10];

    public static int[] MPSblmPVs_get = new int[12];

    public double[] SwitchMachMd_get = new double[7];

    public double[] SwitchBmMd_get = new double[8];

    public double[] RTDLMachMd_get = new double[7];

    public double[] RTDLBmMd_get = new double[8];

    public static Map MPSblmMap;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    private static void setValue(Channel ch, int val) {
        try {
            ch.putVal(val);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        return;
    }

    private static void setValue(Channel ch, float val) {
        try {
            ch.putVal(val);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        return;
    }

    private static int getValue(final Channel ch) {
        int val = 0;
        try {
            ch.requestConnection();
            val = ch.getValInt();
            ch.flushIO();
        } catch (ConnectionException e) {
            JOptionPane.showMessageDialog(getMainWindow(), "Unable to connect to channel access for " + ch.getId(), "Unknown PV", JOptionPane.ERROR_MESSAGE);
            val = -1;
        } catch (GetException e) {
            System.err.println("Unable to get process variables.");
        }
        return val;
    }

    private static float getFloatVal(final Channel ch) {
        float fval = 0;
        try {
            ch.requestConnection();
            fval = ch.getValFlt();
            ch.flushIO();
        } catch (ConnectionException e) {
            JOptionPane.showMessageDialog(getMainWindow(), "Unable to connect to channel access for " + ch.getId(), "Unknown PV", JOptionPane.ERROR_MESSAGE);
            fval = -1;
        } catch (GetException e) {
            System.err.println("Unable to get process variables.");
        }
        return fval;
    }

    public Map getMPSblmMap() {
        return MPSblmMap;
    }

    public int[] getMPSblmPvs(final String initPvStr, final String orgPvStr) {
        String[] chNames = { "", "", "", "", "", "", "", "", "", "" };
        MPSblmMap = new LinkedHashMap();
        String chName = orgPvStr + ":DbgHVBias";
        chNames[0] = chName;
        Channel ch_1 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgHVBiasRb";
        chNames[1] = chName;
        Channel ch_2 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgHVCurrentRb";
        chNames[2] = chName;
        Channel ch_3 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgAFEFirstStageGainRb";
        chNames[3] = chName;
        Channel ch_4 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgSlowPulseLossRb";
        chNames[4] = chName;
        Channel ch_5 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgMPSPulseLossLimit";
        chNames[5] = chName;
        Channel ch_6 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":DbgMPS600PulsesLossLimit";
        chNames[6] = chName;
        Channel ch_7 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":nom_trip";
        chNames[7] = chName;
        Channel ch_8 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":tune_trip";
        chNames[8] = chName;
        Channel ch_9 = ChannelFactory.defaultFactory().getChannel(chName);
        chName = orgPvStr + ":user_trip";
        chNames[9] = chName;
        Channel ch_10 = ChannelFactory.defaultFactory().getChannel(chName);
        MPSblmPVs_get[0] = getValue(ch_1);
        MPSblmPVs_get[1] = getValue(ch_2);
        MPSblmPVs_get[2] = getValue(ch_3);
        MPSblmPVs_get[3] = getValue(ch_4);
        MPSblmPVs_get[4] = getValue(ch_5);
        MPSblmPVs_get[5] = getValue(ch_6);
        MPSblmPVs_get[6] = getValue(ch_7);
        MPSblmPVs_get[7] = getValue(ch_8);
        MPSblmPVs_get[8] = getValue(ch_9);
        MPSblmPVs_get[9] = getValue(ch_10);
        String str;
        for (int i = 0; i < 10; i++) {
            str = "" + MPSblmPVs_get[i];
            MPSblmMap.put(chNames[i], str);
        }
        return MPSblmPVs_get;
    }

    public double[] getSwitchBmMdPVs() {
        Channel ch_1 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:10uSec.RVAL");
        Channel ch_2 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:50uSec.RVAL");
        Channel ch_3 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:100uSec.RVAL");
        Channel ch_4 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:1mSec.RVAL");
        Channel ch_5 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:FullPwr.RVAL");
        Channel ch_6 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:Off.RVAL");
        Channel ch_7 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:StandBy.RVAL");
        Channel ch_8 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_BmMd:MPSTest.RVAL");
        SwitchBmMd_get[0] = getValue(ch_1);
        SwitchBmMd_get[1] = getValue(ch_2);
        SwitchBmMd_get[2] = getValue(ch_3);
        SwitchBmMd_get[3] = getValue(ch_4);
        SwitchBmMd_get[4] = getValue(ch_5);
        SwitchBmMd_get[5] = getValue(ch_6);
        SwitchBmMd_get[6] = getValue(ch_7);
        SwitchBmMd_get[7] = getValue(ch_8);
        return SwitchBmMd_get;
    }

    public double[] getRTDLBmMdPVs() {
        Channel ch_1 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:10uSec.RVAL");
        Channel ch_2 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:50uSec.RVAL");
        Channel ch_3 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:100uSec.RVAL");
        Channel ch_4 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:1mSec.RVAL");
        Channel ch_5 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:FullPwr.RVAL");
        Channel ch_6 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:Off.RVAL");
        Channel ch_7 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:StandBy.RVAL");
        Channel ch_8 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_BmMd:MPSTest.RVAL");
        RTDLBmMd_get[0] = getValue(ch_1);
        RTDLBmMd_get[1] = getValue(ch_2);
        RTDLBmMd_get[2] = getValue(ch_3);
        RTDLBmMd_get[3] = getValue(ch_4);
        RTDLBmMd_get[4] = getValue(ch_5);
        RTDLBmMd_get[5] = getValue(ch_6);
        RTDLBmMd_get[6] = getValue(ch_7);
        RTDLBmMd_get[7] = getValue(ch_8);
        return RTDLBmMd_get;
    }

    public double[] getRTDLMachMdPVs() {
        Channel ch_1 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:MEBT_BS.RVAL");
        Channel ch_2 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:CCL_BS.RVAL");
        Channel ch_3 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:LinDmp.RVAL");
        Channel ch_4 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:InjDmp.RVAL");
        Channel ch_5 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:Ring.RVAL");
        Channel ch_6 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:ExtDmp.RVAL");
        Channel ch_7 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:RTDL_MachMd:Tgt.RVAL");
        RTDLMachMd_get[0] = getValue(ch_1);
        RTDLMachMd_get[1] = getValue(ch_2);
        RTDLMachMd_get[2] = getValue(ch_3);
        RTDLMachMd_get[3] = getValue(ch_4);
        RTDLMachMd_get[4] = getValue(ch_5);
        RTDLMachMd_get[5] = getValue(ch_6);
        RTDLMachMd_get[6] = getValue(ch_7);
        return RTDLMachMd_get;
    }

    public double[] getSwitchMachMdPVs() {
        Channel ch_1 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:MEBT_BS.RVAL");
        Channel ch_2 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:CCL_BS.RVAL");
        Channel ch_3 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:LinDmp.RVAL");
        Channel ch_4 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:InjDmp.RVAL");
        Channel ch_5 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:Ring.RVAL");
        Channel ch_6 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:ExtDmp.RVAL");
        Channel ch_7 = ChannelFactory.defaultFactory().getChannel("ICS_MPS:Switch_MachMd:Tgt.RVAL");
        SwitchMachMd_get[0] = getValue(ch_1);
        SwitchMachMd_get[1] = getValue(ch_2);
        SwitchMachMd_get[2] = getValue(ch_3);
        SwitchMachMd_get[3] = getValue(ch_4);
        SwitchMachMd_get[4] = getValue(ch_5);
        SwitchMachMd_get[5] = getValue(ch_6);
        SwitchMachMd_get[6] = getValue(ch_7);
        return SwitchMachMd_get;
    }

    public static MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }
}
