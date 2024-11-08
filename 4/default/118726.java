import java.lang.reflect.Constructor;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import ie.omk.smpp.message.*;
import ie.omk.smpp.util.GSMConstants;
import ie.omk.smpp.util.SMPPDate;

/** Incredibly simple test that checks the reported length is the same as the
  * actual length of the packet.
  * This test class instantiates each SMPP packet class and writes it to a
  * ByteArrayOutputStream. It compares the length of the stream to the value
  * returned by SMPPPacket.getCommandLen. If they differ, you'll see a
  * FAILED (1) message.
  * It then creates a ByteArrayInputStream from the bytes in the output stream
  * created in the first step and calls SMPPPacket.readPacket on that stream.
  * It then writes the NEW packet to another ByteArrayOutputStream and
  * compares the length of that new output stream to the <b>original</b>
  * getCommandLen value. If it differs, you'll see a FAILED (2) message.
  * If any form of exception occurs, you will see a dump of the 2 byte array
  * output streams and the stack trace of the exception.
  * XXX todo: tests that put values in the packet fields!
  */
public class Test_Sizes {

    static Class[] cs = { BindReceiver.class, BindReceiverResp.class, BindTransmitter.class, BindTransmitterResp.class, CancelSM.class, CancelSMResp.class, DeliverSM.class, DeliverSMResp.class, EnquireLink.class, EnquireLinkResp.class, GenericNack.class, ParamRetrieve.class, ParamRetrieveResp.class, QueryLastMsgs.class, QueryLastMsgsResp.class, QueryMsgDetails.class, QueryMsgDetailsResp.class, QuerySM.class, QuerySMResp.class, ReplaceSM.class, ReplaceSMResp.class, SubmitMulti.class, SubmitMultiResp.class, SubmitSM.class, SubmitSMResp.class, Unbind.class, UnbindResp.class };

    public static void main(String[] args) {
        try {
            smeAddressCheck();
            nullCheck();
            filledCheck();
        } catch (Exception x) {
            x.printStackTrace(System.err);
        }
    }

    static void smeAddressCheck() throws java.lang.Exception {
        try {
            SmeAddress sme = new SmeAddress();
            System.out.print(sme.getClass().getName() + ": ");
            int i = checkSize(sme);
            switch(i) {
                case 0:
                    System.out.print("pass.");
                    break;
                case 1:
                case 2:
                    System.out.print("FAILED (" + i + ")");
                    break;
            }
            System.out.print("\n");
            sme = new SmeAddress(GSMConstants.GSM_TON_INTERNATIONAL, GSMConstants.GSM_NPI_PRIVATE, "77827475");
            System.out.print(sme.getClass().getName() + ": ");
            i = checkSize(sme);
            switch(i) {
                case 0:
                    System.out.print("pass.");
                    break;
                case 1:
                case 2:
                    System.out.print("FAILED (" + i + ")");
                    break;
            }
            System.out.print("\n");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    static int checkSize(SmeAddress sme) throws java.io.IOException, ie.omk.smpp.SMPPException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len = sme.size();
        sme.writeTo(out);
        byte[] b = out.toByteArray();
        if (b.length != len) return (1);
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        SmeAddress reread = new SmeAddress(in);
        if (reread.size() != len) return (2);
        return (0);
    }

    static void nullCheck() throws java.lang.Exception {
        int loop = 0;
        SMPPPacket[] obj = new SMPPPacket[cs.length];
        System.out.println("\n\n========= Simple null field check ==========");
        try {
            Constructor con;
            Class[] argTypes = { int.class };
            Object[] args = { new Integer(4) };
            for (loop = 0; loop < cs.length; loop++) {
                con = cs[loop].getConstructor(argTypes);
                obj[loop] = (SMPPPacket) con.newInstance(args);
            }
        } catch (Exception x) {
            System.out.print(cs[loop].getName() + ":\n    ");
            x.printStackTrace();
        }
        runTestOnArray(obj);
        System.out.println("\n\n============================================");
    }

    static void filledCheck() throws java.lang.Exception {
        int loop = 0;
        String n = null;
        SMPPPacket[] obj = new SMPPPacket[cs.length];
        System.out.println("\n\n========= Filled in field check ==========");
        try {
            BindReceiver br = new BindReceiver(6);
            n = br.getClass().getName();
            obj[loop++] = br;
            br.setSystemId("sysId");
            br.setSystemType("sysType");
            br.setPassword("passwd");
            br.setInterfaceVersion(0x33);
            br.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534[1-3]"));
            BindReceiverResp brr = new BindReceiverResp(6);
            n = brr.getClass().getName();
            obj[loop++] = brr;
            brr.setSystemId("smscId");
            BindTransmitter bt = new BindTransmitter(6);
            n = bt.getClass().getName();
            bt.setSystemId("sysId");
            bt.setSystemType("sysType");
            bt.setPassword("passwd");
            bt.setInterfaceVersion(0x33);
            bt.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534[1-3]"));
            obj[loop++] = bt;
            BindTransmitterResp btr = new BindTransmitterResp(6);
            n = btr.getClass().getName();
            obj[loop++] = btr;
            btr.setSystemId("smscId");
            CancelSM cm = new CancelSM(6);
            n = cm.getClass().getName();
            obj[loop++] = cm;
            cm.setServiceType("svcTp");
            cm.setMessageId("deadbeef");
            cm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            cm.setDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            CancelSMResp cmr = new CancelSMResp(6);
            n = cmr.getClass().getName();
            obj[loop++] = cmr;
            DeliverSM dm = new DeliverSM(6);
            n = dm.getClass().getName();
            obj[loop++] = dm;
            dm.setServiceType("svcTp");
            dm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            dm.setDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293213"));
            dm.setPriority(true);
            dm.setDeliveryTime(new SMPPDate());
            dm.setExpiryTime(new SMPPDate());
            dm.setRegistered(true);
            dm.setReplaceIfPresent(true);
            dm.setMessageText("This is a short message");
            DeliverSMResp dmr = new DeliverSMResp(6);
            n = dmr.getClass().getName();
            obj[loop++] = dmr;
            EnquireLink el = new EnquireLink(6);
            n = el.getClass().getName();
            obj[loop++] = el;
            EnquireLinkResp elr = new EnquireLinkResp(6);
            n = elr.getClass().getName();
            obj[loop++] = elr;
            GenericNack gn = new GenericNack(6);
            n = gn.getClass().getName();
            obj[loop++] = gn;
            ParamRetrieve pr = new ParamRetrieve(6);
            n = pr.getClass().getName();
            obj[loop++] = pr;
            pr.setParamName("getParam");
            ParamRetrieveResp prr = new ParamRetrieveResp(6);
            n = prr.getClass().getName();
            obj[loop++] = prr;
            prr.setParamValue("paramValueCanBeQuiteLong");
            QueryLastMsgs qlm = new QueryLastMsgs(6);
            n = qlm.getClass().getName();
            obj[loop++] = qlm;
            qlm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            qlm.setMsgCount(45);
            QueryLastMsgsResp qlmr = new QueryLastMsgsResp(6);
            n = qlmr.getClass().getName();
            obj[loop++] = qlmr;
            qlmr.addMessageId("23423423");
            qlmr.addMessageId("23423425");
            qlmr.addMessageId("23423424");
            qlmr.addMessageId("23323422");
            qlmr.addMessageId("33324522");
            qlmr.addMessageId("33323464");
            qlmr.addMessageId("33323232");
            qlmr.addMessageId("33323452");
            QueryMsgDetails qmd = new QueryMsgDetails(6);
            n = qmd.getClass().getName();
            obj[loop++] = qmd;
            qmd.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            qmd.setMessageId("aaeeffe");
            qmd.setSmLength(155);
            QueryMsgDetailsResp qmdr = new QueryMsgDetailsResp(6);
            n = qmdr.getClass().getName();
            obj[loop++] = qmdr;
            qmdr.setServiceType("svcTp");
            qmdr.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            qmdr.addDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293213"));
            qmdr.setPriority(true);
            qmdr.setDeliveryTime(new SMPPDate());
            qmdr.setExpiryTime(new SMPPDate());
            qmdr.setRegistered(true);
            qmdr.setMessageText("This is a short message");
            qmdr.setMessageId("deadbeef");
            qmdr.setFinalDate(new SMPPDate());
            qmdr.setMessageStatus(9);
            qmdr.setErrorCode(2);
            QuerySM qm = new QuerySM(6);
            n = qm.getClass().getName();
            obj[loop++] = qm;
            qm.setMessageId("aaaaaaab");
            qm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            QuerySMResp qmr = new QuerySMResp(6);
            n = qmr.getClass().getName();
            obj[loop++] = qmr;
            qmr.setMessageId("aaaaabba");
            qmr.setFinalDate(new SMPPDate());
            qmr.setMessageStatus(1);
            qmr.setErrorCode(4);
            ReplaceSM rm = new ReplaceSM(6);
            n = rm.getClass().getName();
            obj[loop++] = rm;
            rm.setMessageId("cafecafe");
            rm.setServiceType("svcTp");
            rm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            rm.setDeliveryTime(new SMPPDate());
            rm.setExpiryTime(new SMPPDate());
            rm.setRegistered(true);
            rm.setMessageText("This is a short message");
            ReplaceSMResp rmr = new ReplaceSMResp(6);
            n = rmr.getClass().getName();
            obj[loop++] = rmr;
            SubmitMulti sml = new SubmitMulti(6);
            n = sml.getClass().getName();
            obj[loop++] = sml;
            sml.setServiceType("svcTp");
            sml.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            sml.addDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293211"));
            sml.addDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293212"));
            sml.addDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293213"));
            sml.setPriority(true);
            sml.setDeliveryTime(new SMPPDate());
            sml.setExpiryTime(new SMPPDate());
            sml.setRegistered(true);
            sml.setReplaceIfPresent(false);
            sml.setMessageText("This is a short message, multi destinations");
            SubmitMultiResp smlr = new SubmitMultiResp(6);
            n = smlr.getClass().getName();
            obj[loop++] = smlr;
            smlr.setMessageId("213ffeaa");
            smlr.addSmeToTable(new SmeAddress_e(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111", 0));
            smlr.addSmeToTable(new SmeAddress_e(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293211", 3));
            smlr.addSmeToTable(new SmeAddress_e(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293212", 0));
            smlr.addSmeToTable(new SmeAddress_e(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293213", 5));
            SubmitSM sm = new SubmitSM(6);
            n = sm.getClass().getName();
            obj[loop++] = sm;
            sm.setServiceType("svcTp");
            sm.setSource(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "65534111"));
            sm.setDestination(new SmeAddress(GSMConstants.GSM_TON_UNKNOWN, GSMConstants.GSM_NPI_UNKNOWN, "991293213"));
            sm.setPriority(true);
            sm.setDeliveryTime(new SMPPDate());
            sm.setExpiryTime(new SMPPDate());
            sm.setRegistered(true);
            sm.setReplaceIfPresent(true);
            sm.setMessageText("This is a short message");
            SubmitSMResp smr = new SubmitSMResp(6);
            n = smr.getClass().getName();
            obj[loop++] = smr;
            smr.setMessageId("12e53af");
            Unbind ub = new Unbind(6);
            n = ub.getClass().getName();
            obj[loop++] = ub;
            UnbindResp ubr = new UnbindResp(6);
            n = ubr.getClass().getName();
            obj[loop++] = ubr;
        } catch (Exception x) {
            System.out.print(n + ":\n    ");
            x.printStackTrace();
        }
        runTestOnArray(obj);
        System.out.println("\n\n============================================");
    }

    static void runTestOnArray(Object[] obj) throws java.lang.Exception {
        for (int i = 0; i < cs.length; i++) {
            System.out.print(obj[i].getClass().getName() + ": ");
            ByteArrayOutputStream bo1 = null, bo2 = null;
            try {
                SMPPPacket pak = (SMPPPacket) obj[i];
                if (pak == null) continue;
                bo1 = new ByteArrayOutputStream();
                bo2 = new ByteArrayOutputStream();
                pak.writeTo(bo1);
                SMPPPacket.readPacket(new ByteArrayInputStream(bo1.toByteArray())).writeTo(bo2);
                int ret = checkSize(pak);
                if (ret == 0) {
                    System.out.print("pass.");
                } else {
                    System.out.print("FAILED (" + ret + ")");
                    switch(ret) {
                        case 1:
                            System.out.print("\n    getCommandLen() = " + pak.getCommandLen());
                            System.out.print("\n    bytes: " + TestUtils.showBytes(bo1.toByteArray()));
                            break;
                        case 2:
                            System.out.print("\n    getCommandLen() = " + pak.getCommandLen());
                            System.out.print("\n    bytes 1: " + TestUtils.showBytes(bo1.toByteArray()) + "\n    bytes 2: " + TestUtils.showBytes(bo2.toByteArray()));
                            break;
                    }
                }
            } catch (Exception x) {
                System.out.print("FAILED..exception." + "\n    bytes1: " + TestUtils.showBytes(bo1.toByteArray()) + "\n    bytes2: " + TestUtils.showBytes(bo1.toByteArray()) + "\n");
                x.printStackTrace();
            }
            System.out.print("\n");
        }
    }

    static int checkSize(SMPPPacket pak) throws java.io.IOException, ie.omk.smpp.SMPPException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len = pak.getCommandLen();
        pak.writeTo(out);
        byte[] b = out.toByteArray();
        if (b.length != len) return (1);
        ByteArrayInputStream in = new ByteArrayInputStream(b);
        SMPPPacket reread = pak.readPacket(in);
        if (reread.getCommandLen() != len) return (2);
        return (0);
    }
}
