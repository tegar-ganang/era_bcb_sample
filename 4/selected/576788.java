package gov.sns.apps.mpsmodemask;

import gov.sns.ca.*;

public class ChannelAccess {

    public double[] SwitchMachMd_get = new double[7];

    public double[] SwitchBmMd_get = new double[8];

    public double[] RTDLMachMd_get = new double[7];

    public double[] RTDLBmMd_get = new double[8];

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    private static int getValue(final Channel ch) {
        int val = 0;
        try {
            ch.requestConnection();
            val = ch.getValInt();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access for " + ch.getId());
            val = -1;
        } catch (GetException e) {
            System.err.println("Unable to get process variables.");
        }
        return val;
    }

    public int getPvValue(final String pv) {
        int val = 0;
        ChannelWrapper chs;
        chs = new ChannelWrapper(pv);
        return val;
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
}
