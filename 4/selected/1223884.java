package gov.sns.apps.lossviewer;

import java.util.*;
import java.net.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.HashMap;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.data.*;
import gov.sns.application.*;
import gov.sns.xal.model.*;
import gov.sns.ca.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.tools.messaging.*;
import gov.sns.xal.smf.impl.qualify.*;

/**
 * GenDocument is a custom XALDocument for loss viewing application 
 *
 * @version   0.1 12/1/2003
 * @author  cp3
 * @author  Sarah Cousineau
 */
public class GenDocument extends AcceleratorDocument implements SettingListener, DataListener {

    /**
     * The document for the text pane in the main window.
     */
    protected PlainDocument textDocument;

    protected Lattice lattice = null;

    /** the name of the xml file containing the accelerator */
    protected String theProbeFile;

    /** Create a new empty document */
    public GenDocument() {
        this(null);
        initDTL();
        initCCL();
        initSCL();
        initHEBT();
        initLDmp();
        initIDmp();
        initRing();
        initRTBT();
        initEDmp();
        initSumPVs();
        initAvgPVs();
        initScalePVs();
        initMPSPVs();
    }

    /** 
     * Create a new document loaded from the URL file 
     * @param url The URL of the file to load into the new document.
     */
    public GenDocument(java.net.URL url) {
        setSource(url);
        if (url != null) {
            try {
                System.out.println("Opening document: " + url.toString());
                DataAdaptor documentAdaptor = XmlDataAdaptor.adaptorForUrl(url, false);
                update(documentAdaptor.childAdaptor("GenDocument"));
                setHasChanges(false);
            } catch (Exception exception) {
                exception.printStackTrace();
                displayError("Open Failed!", "Open failed due to an internal exception!", exception);
            }
        }
        if (url == null) return;
    }

    /**
     * Make a main window by instantiating the my custom window.  Set the text 
     * pane to use the textDocument variable as its document.
     */
    public void makeMainWindow() {
        mainWindow = new GenWindow(this);
    }

    /**
     * Convenience method for getting the main window cast to the proper 
     * subclass of XalWindow.  This allows me to avoid casting the window 
     * every time I reference it.
     * @return The main window cast to its dynamic runtime class
     */
    public GenWindow myWindow() {
        return (GenWindow) mainWindow;
    }

    /** 
     * Customize any special button commands.
     */
    protected void customizeCommands(Commander commander) {
    }

    /**
     * Save the document to the specified URL.
     * @url The URL to which the document should be saved.
     */
    public void saveDocumentAs(java.net.URL url) {
        try {
            XmlDataAdaptor documentAdaptor = XmlDataAdaptor.newEmptyDocumentAdaptor();
            documentAdaptor.writeNode(this);
            documentAdaptor.writeToUrl(url);
            setHasChanges(false);
        } catch (XmlDataAdaptor.WriteException exception) {
            exception.printStackTrace();
            displayError("Save Failed!", "Save failed due to an internal write exception!", exception);
        } catch (Exception exception) {
            exception.printStackTrace();
            displayError("Save Failed!", "Save failed due to an internal exception!", exception);
        }
    }

    /** 
     * dataLabel() provides the name used to identify the class in an 
     * external data source.
     * @return The tag for this data node.
     */
    public String dataLabel() {
        return "GenDocument";
    }

    /**
     * Instructs the receiver to update its data based on the given adaptor.
     * @param adaptor The data adaptor corresponding to this object's data 
     * node.
     */
    public void update(DataAdaptor adaptor) {
    }

    /**
     * When called this method indicates that a setting has changed in 
     * the source.
     * @param source The source whose setting has changed.
     */
    public void settingChanged(Object source) {
        setHasChanges(true);
    }

    /**
     * Instructs the receiver to write its data to the adaptor for external
     * storage.
     * @param adaptor The data adaptor corresponding to this object's data 
     * node.
     */
    public void write(DataAdaptor adaptor) {
    }

    public Accelerator accl = new Accelerator();

    public ArrayList dtl1agents, dtl2agents, dtl3agents, dtl4agents, dtl5agents, dtl6agents;

    public ArrayList alldtlagents;

    public ArrayList alldtlgroups;

    public ArrayList allconnecteddtlagents;

    public ArrayList allconnecteddtlgroups;

    public AcceleratorSeqCombo dtlseq;

    public ArrayList ccl1agents, ccl2agents, ccl3agents, ccl4agents;

    public ArrayList allcclagents;

    public ArrayList allcclgroups;

    public ArrayList allconnectedcclagents;

    public ArrayList allconnectedcclgroups;

    public AcceleratorSeqCombo cclseq;

    public ArrayList sclmedagents, sclhighagents;

    public ArrayList allsclagents;

    public ArrayList allsclgroups;

    public ArrayList allconnectedsclagents;

    public ArrayList allconnectedsclgroups;

    public AcceleratorSeqCombo sclseq;

    public ArrayList ldmpagents;

    public ArrayList allldmpagents;

    public ArrayList allldmpgroups;

    public ArrayList allconnectedldmpagents;

    public ArrayList allconnectedldmpgroups;

    public AcceleratorSeqCombo ldmpseq;

    public ArrayList hebt1agents, hebt2agents;

    public ArrayList allhebtagents;

    public ArrayList allhebtgroups;

    public ArrayList allconnectedhebtagents;

    public ArrayList allconnectedhebtgroups;

    public AcceleratorSeqCombo hebtseq;

    public ArrayList idmpagents;

    public ArrayList allidmpagents;

    public ArrayList allidmpgroups;

    public ArrayList allconnectedidmpagents;

    public ArrayList allconnectedidmpgroups;

    public AcceleratorSeqCombo idmpseq;

    public ArrayList ring1agents, ring2agents, ring3agents, ring4agents, ring5agents;

    public ArrayList allringagents;

    public ArrayList allringgroups;

    public ArrayList allconnectedringagents;

    public ArrayList allconnectedringgroups;

    public AcceleratorSeqCombo ringseq;

    public ArrayList rtbt1agents, rtbt2agents;

    public ArrayList allrtbtagents;

    public ArrayList allrtbtgroups;

    public ArrayList allconnectedrtbtagents;

    public ArrayList allconnectedrtbtgroups;

    public AcceleratorSeqCombo rtbtseq;

    public ArrayList edmpagents;

    public ArrayList alledmpagents;

    public ArrayList alledmpgroups;

    public ArrayList allconnectededmpagents;

    public ArrayList allconnectededmpgroups;

    public AcceleratorSeqCombo edmpseq;

    public double thresholdunlatch;

    public double thresholdlatch;

    public boolean usetimer = false;

    public int ndtlagents;

    public int ndtlconnectedagents;

    public static int ndtlgroups;

    public int ndtlconnectedgroups;

    public HashMap dtllabels;

    public int ncclagents;

    public int ncclconnectedagents;

    public static int ncclgroups;

    public int ncclconnectedgroups;

    public HashMap ccllabels;

    public int nsclagents;

    public int nsclconnectedagents;

    public static int nsclgroups;

    public int nsclconnectedgroups;

    public HashMap scllabels;

    public int nldmpagents;

    public int nldmpconnectedagents;

    public static int nldmpgroups;

    public int nldmpconnectedgroups;

    public HashMap ldmplabels;

    public int nidmpagents;

    public int nidmpconnectedagents;

    public static int nidmpgroups;

    public int nidmpconnectedgroups;

    public HashMap idmplabels;

    public int nhebtagents;

    public int nhebtconnectedagents;

    public static int nhebtgroups;

    public int nhebtconnectedgroups;

    public HashMap hebtlabels;

    public int nringagents;

    public int nringconnectedagents;

    public static int nringgroups;

    public int nringconnectedgroups;

    public HashMap ringlabels;

    public int nrtbtagents;

    public int nrtbtconnectedagents;

    public static int nrtbtgroups;

    public int nrtbtconnectedgroups;

    public HashMap rtbtlabels;

    public int nedmpagents;

    public int nedmpconnectedagents;

    public static int nedmpgroups;

    public int nedmpconnectedgroups;

    public HashMap edmplabels;

    public HashMap allagentsmap = new HashMap();

    public HashMap backgroundstats = new HashMap();

    public HashMap backgroundref = new HashMap();

    public HashMap referencestate = new HashMap();

    public HashMap currentrefstate = new HashMap();

    public boolean usebackground = false;

    public ChannelAgent dtlsumagent;

    public ChannelAgent cclsumagent;

    public ChannelAgent sclsumagent;

    public ChannelAgent hebtsumagent;

    public ChannelAgent ringsumagent;

    public ChannelAgent rtbtsumagent;

    public ChannelAgent ldmpsumagent;

    public ChannelAgent idmpsumagent;

    public ChannelAgent edmpsumagent;

    public ChannelAgent dtlscaleagent;

    public ChannelAgent cclscaleagent;

    public ChannelAgent sclscaleagent;

    public ChannelAgent hebtscaleagent;

    public ChannelAgent ringscaleagent;

    public ChannelAgent rtbtscaleagent;

    public ChannelAgent ldmpscaleagent;

    public ChannelAgent idmpscaleagent;

    public ChannelAgent edmpscaleagent;

    public ChannelAgent dtlavgagent;

    public ChannelAgent cclavgagent;

    public ChannelAgent sclavgagent;

    public ChannelAgent hebtavgagent;

    public ChannelAgent ringavgagent;

    public ChannelAgent rtbtavgagent;

    public ChannelAgent ldmpavgagent;

    public ChannelAgent idmpavgagent;

    public ChannelAgent edmpavgagent;

    public ChannelAgent dtlmpsagent;

    public ChannelAgent cclmpsagent;

    public ChannelAgent sclmpsagent;

    public ChannelAgent hebtmpsagent;

    public ChannelAgent ringmpsagent;

    public ChannelAgent rtbtmpsagent;

    public ChannelAgent ldmpmpsagent;

    public ChannelAgent idmpmpsagent;

    public ChannelAgent edmpmpsagent;

    public double dtlsum = 1.0;

    public double cclsum = 1.0;

    public double sclsum = 1.0;

    public double hebtsum = 1.0;

    public double ringsum = 1.0;

    public double rtbtsum = 1.0;

    public double ldmpsum = 1.0;

    public double idmpsum = 1.0;

    public double edmpsum = 1.0;

    public double dtlscalefac = 1.0;

    public double cclscalefac = 1.0;

    public double sclscalefac = 1.0;

    public double hebtscalefac = 1.0;

    public double ringscalefac = 1.0;

    public double rtbtscalefac = 1.0;

    public double ldmpscalefac = 1.0;

    public double idmpscalefac = 1.0;

    public double edmpscalefac = 1.0;

    public double dtlavg = 1.0;

    public double cclavg = 1.0;

    public double sclavg = 1.0;

    public double hebtavg = 1.0;

    public double ringavg = 1.0;

    public double rtbtavg = 1.0;

    public double ldmpavg = 1.0;

    public double idmpavg = 1.0;

    public double edmpavg = 1.0;

    public int dtlseverity = 0;

    public int cclseverity = 0;

    public int sclseverity = 0;

    public int hebtseverity = 0;

    public int ringseverity = 0;

    public int rtbtseverity = 0;

    public int ldmpseverity = 0;

    public int idmpseverity = 0;

    public int edmpseverity = 0;

    public void initSumPVs() {
        dtlsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("DTL_Diag:CalcL_BLM:Sum"));
        cclsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("CCL_Diag:CalcL_BLM:Sum"));
        sclsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("SCL_Diag:CalcL_BLM:Sum"));
        ringsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("Ring_Diag:CalcL_BLM:Sum"));
        hebtsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("HEBT_Diag:CalcL_BLM:Sum"));
        rtbtsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("RTBT_Diag:CalcL_BLM:Sum"));
        ldmpsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("LDmp_Diag:CalcL_BLM:Sum"));
        idmpsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("IDmp_Diag:CalcL_BLM:Sum"));
        edmpsumagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("EDmp_Diag:CalcL_BLM:Sum"));
        dtlsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                dtlsum = value;
            }
        });
        cclsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                cclsum = value;
            }
        });
        sclsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                sclsum = value;
            }
        });
        hebtsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                hebtsum = value;
            }
        });
        ringsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ringsum = value;
            }
        });
        rtbtsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                rtbtsum = value;
            }
        });
        ldmpsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ldmpsum = value;
            }
        });
        idmpsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                idmpsum = value;
            }
        });
        edmpsumagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                edmpsum = value;
            }
        });
    }

    public void initScalePVs() {
        dtlscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("DTL_Diag:CalcL_BLM:SumCur"));
        cclscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("CCL_Diag:CalcL_BLM:SumCur"));
        sclscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("SCL_Diag:CalcL_BLM:SumCur"));
        ringscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("Ring_Diag:CalcL_BLM:SumCur"));
        hebtscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("HEBT_Diag:CalcL_BLM:SumCur"));
        rtbtscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("RTBT_Diag:CalcL_BLM:SumCur"));
        ldmpscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("LDmp_Diag:CalcL_BLM:SumCur"));
        idmpscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("IDmp_Diag:CalcL_BLM:SumCur"));
        edmpscaleagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("EDmp_Diag:CalcL_BLM:SumCur"));
        dtlscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                dtlscalefac = value;
            }
        });
        cclscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                cclscalefac = value;
            }
        });
        sclscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                sclscalefac = value;
            }
        });
        hebtscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                hebtscalefac = value;
            }
        });
        ringscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ringscalefac = value;
            }
        });
        rtbtscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                rtbtscalefac = value;
            }
        });
        ldmpscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ldmpscalefac = value;
            }
        });
        idmpscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                idmpscalefac = value;
            }
        });
        edmpscaleagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                edmpscalefac = value;
            }
        });
    }

    public void initAvgPVs() {
        dtlavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("DTL_Diag:CalcL_BLM:Avg"));
        cclavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("CCL_Diag:CalcL_BLM:Avg"));
        sclavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("SCL_Diag:CalcL_BLM:Avg"));
        ringavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("Ring_Diag:CalcL_BLM:Avg"));
        hebtavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("HEBT_Diag:CalcL_BLM:Avg"));
        rtbtavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("RTBT_Diag:CalcL_BLM:Avg"));
        ldmpavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("LDmp_Diag:CalcL_BLM:Avg"));
        idmpavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("IDmp_Diag:CalcL_BLM:Avg"));
        edmpavgagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("EDmp_Diag:CalcL_BLM:Avg"));
        dtlavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                dtlavg = value;
            }
        });
        cclavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                cclavg = value;
            }
        });
        sclavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                sclavg = value;
            }
        });
        hebtavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                hebtavg = value;
            }
        });
        ringavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ringavg = value;
            }
        });
        rtbtavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                rtbtavg = value;
            }
        });
        ldmpavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ldmpavg = value;
            }
        });
        idmpavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                idmpavg = value;
            }
        });
        edmpavgagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                idmpavg = value;
            }
        });
    }

    public void initMPSPVs() {
        dtlmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("DTL_Diag:BLM:Alarm"));
        cclmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("CCL_Diag:BLM:Alarm"));
        sclmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("SCL_Diag:BLM:Alarm"));
        ringmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("Ring_Diag:BLM_LB:Alarm"));
        hebtmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("HEBT_Diag:BLM:Alarm"));
        rtbtmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("RTBT_Diag:BLM_ALarm"));
        ldmpmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("LDmp_Diag:BLM:Alarm"));
        idmpmpsagent = new ChannelAgent(ChannelFactory.defaultFactory().getChannel("IDmp_Diag:BLM:Alarm"));
        dtlmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                dtlseverity = severity;
            }
        });
        cclmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                cclseverity = severity;
            }
        });
        sclmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                sclseverity = severity;
            }
        });
        hebtmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                hebtseverity = severity;
            }
        });
        ringmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ringseverity = severity;
            }
        });
        rtbtmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                rtbtseverity = severity;
            }
        });
        ldmpmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                ldmpseverity = severity;
            }
        });
        idmpmpsagent.addReadbackListener(new ReadbackListener() {

            public void updateReadback(Object sender, String name, double value, int severity) {
                idmpseverity = severity;
            }
        });
    }

    public void initDTL() {
        AcceleratorSeq dtl1, dtl2, dtl3, dtl4, dtl5, dtl6;
        this.loadDefaultAccelerator();
        accl = this.getAccelerator();
        dtllabels = new HashMap();
        dtl1 = accl.getSequence("DTL1");
        dtl1agents = createAgents(dtl1);
        dtl2 = accl.getSequence("DTL2");
        dtl2agents = createAgents(dtl2);
        dtl3 = accl.getSequence("DTL3");
        dtl3agents = createAgents(dtl3);
        dtl4 = accl.getSequence("DTL4");
        dtl4agents = createAgents(dtl4);
        dtl5 = accl.getSequence("DTL5");
        dtl5agents = createAgents(dtl5);
        dtl6 = accl.getSequence("DTL6");
        dtl6agents = createAgents(dtl6);
        ArrayList dtls = new ArrayList();
        dtls.add(dtl1);
        dtls.add(dtl2);
        dtls.add(dtl3);
        dtls.add(dtl4);
        dtls.add(dtl5);
        dtls.add(dtl6);
        dtlseq = new AcceleratorSeqCombo("DTL", dtls);
        alldtlagents = new ArrayList();
        alldtlgroups = new ArrayList();
        allconnecteddtlagents = new ArrayList();
        allconnecteddtlgroups = new ArrayList();
        String sect = new String("DTL");
        makeSectionLists(dtl1agents, sect, "1", alldtlagents, alldtlgroups, dtllabels);
        makeSectionLists(dtl2agents, sect, "2", alldtlagents, alldtlgroups, dtllabels);
        makeSectionLists(dtl3agents, sect, "3", alldtlagents, alldtlgroups, dtllabels);
        makeSectionLists(dtl4agents, sect, "4", alldtlagents, alldtlgroups, dtllabels);
        makeSectionLists(dtl5agents, sect, "5", alldtlagents, alldtlgroups, dtllabels);
        makeSectionLists(dtl6agents, sect, "6", alldtlagents, alldtlgroups, dtllabels);
        makeConnectedLists(dtl1agents, "1", allconnecteddtlgroups);
        makeConnectedLists(dtl2agents, "2", allconnecteddtlgroups);
        makeConnectedLists(dtl3agents, "3", allconnecteddtlgroups);
        makeConnectedLists(dtl4agents, "4", allconnecteddtlgroups);
        makeConnectedLists(dtl5agents, "5", allconnecteddtlgroups);
        makeConnectedLists(dtl6agents, "6", allconnecteddtlgroups);
        ndtlagents = alldtlagents.size();
        ndtlgroups = alldtlgroups.size();
        ndtlconnectedagents = allconnecteddtlagents.size();
        ndtlconnectedgroups = allconnecteddtlgroups.size();
    }

    public void initCCL() {
        AcceleratorSeq ccl1, ccl2, ccl3, ccl4;
        ccllabels = new HashMap();
        ccl1 = accl.getSequence("CCL1");
        ccl1agents = createAgents(ccl1);
        ccl2 = accl.getSequence("CCL2");
        ccl2agents = createAgents(ccl2);
        ccl3 = accl.getSequence("CCL3");
        ccl3agents = createAgents(ccl3);
        ccl4 = accl.getSequence("CCL4");
        ccl4agents = createAgents(ccl4);
        ArrayList ccls = new ArrayList();
        ccls.add(ccl1);
        ccls.add(ccl2);
        ccls.add(ccl3);
        ccls.add(ccl4);
        cclseq = new AcceleratorSeqCombo("CCL", ccls);
        allcclagents = new ArrayList();
        allcclgroups = new ArrayList();
        allconnectedcclagents = new ArrayList();
        allconnectedcclgroups = new ArrayList();
        String sect = new String("CCL");
        makeSectionLists(ccl1agents, sect, "1", allcclagents, allcclgroups, ccllabels);
        makeSectionLists(ccl2agents, sect, "2", allcclagents, allcclgroups, ccllabels);
        makeSectionLists(ccl3agents, sect, "3", allcclagents, allcclgroups, ccllabels);
        makeSectionLists(ccl4agents, sect, "4", allcclagents, allcclgroups, ccllabels);
        makeConnectedLists(ccl1agents, "1", allconnectedcclgroups);
        makeConnectedLists(ccl2agents, "2", allconnectedcclgroups);
        makeConnectedLists(ccl3agents, "3", allconnectedcclgroups);
        makeConnectedLists(ccl4agents, "4", allconnectedcclgroups);
        ncclagents = allcclagents.size();
        ncclgroups = allcclgroups.size();
        ncclconnectedagents = allconnectedcclagents.size();
        ncclconnectedgroups = allconnectedcclgroups.size();
    }

    public void initSCL() {
        AcceleratorSeq sclmed, sclhigh;
        scllabels = new HashMap();
        sclmed = accl.getSequence("SCLMed");
        sclmedagents = createAgents(sclmed);
        sclhigh = accl.getSequence("SCLHigh");
        sclhighagents = createAgents(sclhigh);
        ArrayList scls = new ArrayList();
        scls.add(sclmed);
        scls.add(sclhigh);
        sclseq = new AcceleratorSeqCombo("SCL", scls);
        allsclagents = new ArrayList();
        allsclgroups = new ArrayList();
        allconnectedsclagents = new ArrayList();
        allconnectedsclgroups = new ArrayList();
        String sect = new String("SCL");
        makeSectionLists(sclmedagents, sect, "Med", allsclagents, allsclgroups, scllabels);
        makeSectionLists(sclhighagents, sect, "High", allsclagents, allsclgroups, scllabels);
        makeConnectedLists(sclmedagents, "Med", allconnectedsclgroups);
        makeConnectedLists(sclhighagents, "High", allconnectedsclgroups);
        nsclagents = allsclagents.size();
        nsclgroups = allsclgroups.size();
        nsclconnectedagents = allconnectedsclagents.size();
        nsclconnectedgroups = allconnectedsclgroups.size();
    }

    public void initLDmp() {
        AcceleratorSeq ldmp;
        ldmplabels = new HashMap();
        ldmp = accl.getSequence("LDmp");
        ldmpagents = createAgents(ldmp);
        ArrayList ldmps = new ArrayList();
        ldmps.add(ldmp);
        ldmpseq = new AcceleratorSeqCombo("LDmp", ldmps);
        allldmpagents = new ArrayList();
        allldmpgroups = new ArrayList();
        allconnectedldmpagents = new ArrayList();
        allconnectedldmpgroups = new ArrayList();
        String sect = new String("LDmp");
        makeSectionLists(ldmpagents, sect, "", allldmpagents, allldmpgroups, ldmplabels);
        makeConnectedLists(ldmpagents, "", allconnectedldmpgroups);
        nldmpagents = allldmpagents.size();
        nldmpgroups = allldmpgroups.size();
        nldmpconnectedagents = allconnectedldmpagents.size();
        nldmpconnectedgroups = allconnectedldmpgroups.size();
    }

    public void initHEBT() {
        AcceleratorSeq hebt1, hebt2;
        hebtlabels = new HashMap();
        hebt1 = accl.getSequence("HEBT1");
        hebt1agents = createAgents(hebt1);
        hebt2 = accl.getSequence("HEBT2");
        hebt2agents = createAgents(hebt2);
        ArrayList hebt = new ArrayList();
        hebt.add(hebt1);
        hebt.add(hebt2);
        hebtseq = new AcceleratorSeqCombo("HEBT", hebt);
        allhebtagents = new ArrayList();
        allhebtgroups = new ArrayList();
        allconnectedhebtagents = new ArrayList();
        allconnectedhebtgroups = new ArrayList();
        String sect = new String("HEBT");
        makeSectionLists(hebt1agents, sect, "1", allhebtagents, allhebtgroups, hebtlabels);
        makeSectionLists(hebt2agents, sect, "2", allhebtagents, allhebtgroups, hebtlabels);
        makeConnectedLists(hebt1agents, "1", allconnectedhebtgroups);
        makeConnectedLists(hebt2agents, "2", allconnectedhebtgroups);
        nhebtagents = allhebtagents.size();
        nhebtgroups = allhebtgroups.size();
        nhebtconnectedagents = allconnectedhebtagents.size();
        nhebtconnectedgroups = allconnectedhebtgroups.size();
    }

    public void initIDmp() {
        AcceleratorSeq idmp;
        idmplabels = new HashMap();
        idmp = accl.getSequence("IDmp+");
        idmpagents = createAgents(idmp);
        ArrayList idmps = new ArrayList();
        idmps.add(idmp);
        idmpseq = new AcceleratorSeqCombo("IDmp+", idmps);
        allidmpagents = new ArrayList();
        allidmpgroups = new ArrayList();
        allconnectedidmpagents = new ArrayList();
        allconnectedidmpgroups = new ArrayList();
        String sect = new String("IDmp+");
        makeSectionLists(idmpagents, sect, "", allidmpagents, allidmpgroups, idmplabels);
        makeConnectedLists(idmpagents, "", allconnectedidmpgroups);
        nidmpagents = allidmpagents.size();
        nidmpgroups = allidmpgroups.size();
        nidmpconnectedagents = allconnectedidmpagents.size();
        nidmpconnectedgroups = allconnectedidmpgroups.size();
    }

    public void initRing() {
        AcceleratorSeq ring1, ring2, ring3, ring4, ring5;
        ringlabels = new HashMap();
        ring1 = accl.getSequence("Ring1");
        ring1agents = createAgents(ring1);
        ring2 = accl.getSequence("Ring2");
        ring2agents = createAgents(ring2);
        ring3 = accl.getSequence("Ring3");
        ring3agents = createAgents(ring3);
        ring4 = accl.getSequence("Ring4");
        ring4agents = createAgents(ring4);
        ring5 = accl.getSequence("Ring5");
        ring5agents = createAgents(ring5);
        ArrayList ring = new ArrayList();
        ring.add(ring1);
        ring.add(ring2);
        ring.add(ring3);
        ring.add(ring4);
        ring.add(ring5);
        ringseq = new AcceleratorSeqCombo("RING", ring);
        allringagents = new ArrayList();
        allringgroups = new ArrayList();
        allconnectedringagents = new ArrayList();
        allconnectedringgroups = new ArrayList();
        String sect = new String("Ring");
        makeSectionLists(ring1agents, sect, "1", allringagents, allringgroups, ringlabels);
        makeSectionLists(ring2agents, sect, "2", allringagents, allringgroups, ringlabels);
        makeSectionLists(ring3agents, sect, "3", allringagents, allringgroups, ringlabels);
        makeSectionLists(ring4agents, sect, "4", allringagents, allringgroups, ringlabels);
        makeSectionLists(ring5agents, sect, "5", allringagents, allringgroups, ringlabels);
        makeConnectedLists(ring1agents, "1", allconnectedringgroups);
        makeConnectedLists(ring2agents, "2", allconnectedringgroups);
        makeConnectedLists(ring3agents, "3", allconnectedringgroups);
        makeConnectedLists(ring4agents, "4", allconnectedringgroups);
        makeConnectedLists(ring5agents, "5", allconnectedringgroups);
        nringagents = allringagents.size();
        nringgroups = allringgroups.size();
        nringconnectedagents = allconnectedringagents.size();
        nringconnectedgroups = allconnectedringgroups.size();
    }

    public void initRTBT() {
        AcceleratorSeq rtbt1, rtbt2;
        rtbtlabels = new HashMap();
        rtbt1 = accl.getSequence("RTBT1");
        rtbt1agents = createAgents(rtbt1);
        rtbt2 = accl.getSequence("RTBT2");
        rtbt2agents = createAgents(rtbt2);
        ArrayList rtbt = new ArrayList();
        rtbt.add(rtbt1);
        rtbt.add(rtbt2);
        rtbtseq = new AcceleratorSeqCombo("RTBT", rtbt);
        allrtbtagents = new ArrayList();
        allrtbtgroups = new ArrayList();
        allconnectedrtbtagents = new ArrayList();
        allconnectedrtbtgroups = new ArrayList();
        String sect = new String("RTBT");
        makeSectionLists(rtbt1agents, sect, "1", allrtbtagents, allrtbtgroups, rtbtlabels);
        makeSectionLists(rtbt2agents, sect, "2", allrtbtagents, allrtbtgroups, rtbtlabels);
        makeConnectedLists(rtbt1agents, "1", allconnectedrtbtgroups);
        makeConnectedLists(rtbt2agents, "2", allconnectedrtbtgroups);
        nrtbtagents = allrtbtagents.size();
        nrtbtgroups = allrtbtgroups.size();
        nrtbtconnectedagents = allconnectedrtbtagents.size();
        nrtbtconnectedgroups = allconnectedrtbtgroups.size();
    }

    public void initEDmp() {
        AcceleratorSeq edmp;
        edmplabels = new HashMap();
        edmp = accl.getSequence("EDmp");
        edmpagents = createAgents(edmp);
        ArrayList edmps = new ArrayList();
        edmps.add(edmp);
        edmpseq = new AcceleratorSeqCombo("EDmp", edmps);
        alledmpagents = new ArrayList();
        alledmpgroups = new ArrayList();
        allconnectededmpagents = new ArrayList();
        allconnectededmpgroups = new ArrayList();
        String sect = new String("EDmp");
        makeSectionLists(edmpagents, sect, "", alledmpagents, alledmpgroups, edmplabels);
        makeConnectedLists(edmpagents, "", allconnectededmpgroups);
        nedmpagents = alledmpagents.size();
        nedmpgroups = alledmpgroups.size();
        nedmpconnectedagents = allconnectededmpagents.size();
        nedmpconnectedgroups = allconnectededmpgroups.size();
    }

    public ArrayList createAgents(AcceleratorSeq asequence) {
        AndTypeQualifier and_qualifier = new AndTypeQualifier();
        NotTypeQualifier not_qualifier = new NotTypeQualifier("ND");
        and_qualifier.and("BLM");
        and_qualifier.and(not_qualifier);
        ArrayList blms = (ArrayList) asequence.getNodesWithQualifier(and_qualifier);
        ArrayList blmagents = new ArrayList();
        Iterator itr = blms.iterator();
        while (itr.hasNext()) {
            BlmAgent agent = new BlmAgent(asequence, (BLM) itr.next());
            if (agent.isOkay()) {
                blmagents.add((BlmAgent) agent);
            }
            System.out.println("This is BLM is " + agent.name() + " in sequence " + asequence + ", status is " + agent.isOkay());
        }
        return blmagents;
    }

    public void makeSectionLists(ArrayList blmagentlist, String section, String label, ArrayList allsectionagents, ArrayList allsectiongroups, HashMap sectionlabels) {
        if (blmagentlist.size() > 0) {
            allsectionagents.addAll(blmagentlist);
            allsectiongroups.add(blmagentlist);
            sectionlabels.put(blmagentlist, label);
            String name = new String(section + label);
            allagentsmap.put(new String(name), blmagentlist);
        }
    }

    public void makeConnectedLists(ArrayList blmagentlist, String label, ArrayList allconnectedsectiongroups) {
        final Set connectedblms = new HashSet();
        Iterator itr = blmagentlist.iterator();
        while (itr.hasNext()) {
            final BlmAgent agent = (BlmAgent) itr.next();
            agent.addLossChannelConnectionListener(new ConnectionListener() {

                public void connectionMade(Channel aChannel) {
                    synchronized (connectedblms) {
                        connectedblms.add(agent);
                    }
                }

                public void connectionDropped(Channel aChannel) {
                    synchronized (connectedblms) {
                        connectedblms.remove(agent);
                    }
                }
            });
            if (agent.isConnected()) {
                synchronized (connectedblms) {
                    connectedblms.add(agent);
                }
            }
        }
        allconnectedsectiongroups.add(connectedblms);
    }

    public ArrayList getConnectedDTLGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnecteddtlgroups.size(); i++) {
            Set blmagentset = (Set) allconnecteddtlgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedCCLGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedcclgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedcclgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedSCLGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedsclgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedsclgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedHEBTGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedhebtgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedhebtgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedRingGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedringgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedringgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedRTBTGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedrtbtgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedrtbtgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedLDmpGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedldmpgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedldmpgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public ArrayList getConnectedIDmpGroups() {
        ArrayList copy = new ArrayList();
        for (int i = 0; i < allconnectedidmpgroups.size(); i++) {
            Set blmagentset = (Set) allconnectedidmpgroups.get(i);
            synchronized (blmagentset) {
                copy.add(new ArrayList(blmagentset));
            }
        }
        return copy;
    }

    public void setCurrentState(int state) {
        if (state == 0) {
            currentrefstate = referencestate;
        } else {
            currentrefstate = backgroundref;
        }
    }

    public void setBackgroundStats(HashMap stats) {
        backgroundstats = stats;
    }

    public void setBackground(HashMap ref) {
        backgroundref = ref;
    }

    public void setReferenceState(HashMap ref) {
        referencestate = ref;
    }

    public HashMap getBackground() {
        return backgroundref;
    }

    public HashMap getReference() {
        return referencestate;
    }

    public HashMap getCurrentRef() {
        return currentrefstate;
    }
}
