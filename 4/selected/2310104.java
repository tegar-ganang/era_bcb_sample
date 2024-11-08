package gov.sns.xal.model.pvlogger;

import gov.sns.ca.Channel;
import gov.sns.tools.ArrayValue;
import gov.sns.tools.database.ConnectionDictionary;
import gov.sns.tools.database.ConnectionPreferenceController;
import gov.sns.tools.pvlogger.ChannelSnapshot;
import gov.sns.tools.pvlogger.MachineSnapshot;
import gov.sns.tools.pvlogger.SqlStateStore;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.sync.SynchronizationException;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.impl.CurrentMonitor;
import gov.sns.xal.smf.impl.Electromagnet;
import gov.sns.xal.smf.impl.MagnetMainSupply;
import gov.sns.xal.smf.impl.MagnetTrimSupply;
import gov.sns.xal.smf.impl.TrimmedQuadrupole;
import gov.sns.xal.smf.impl.qualify.AndTypeQualifier;
import gov.sns.xal.smf.impl.qualify.NotTypeQualifier;
import gov.sns.xal.smf.impl.qualify.OrTypeQualifier;
import gov.sns.xal.smf.proxy.ElectromagnetPropertyAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides an interface for online model with PV logger data source.
 * 
 * @version 0.1 03 Jan 2005
 * @author Paul Chu
 */
public class PVLoggerDataSource {

    MachineSnapshot mss;

    Map<String, Double> qPVMap;

    Map<String, Double> qPSPVMap;

    ChannelSnapshot[] css;

    AcceleratorSeq myAccSeq;

    /**
	 * @param id
	 *            the PV logger ID
	 */
    public PVLoggerDataSource(long id) {
        ConnectionDictionary dict = ConnectionDictionary.defaultDictionary();
        SqlStateStore store;
        if (dict != null) {
            store = new SqlStateStore(dict);
        } else {
            ConnectionPreferenceController.displayPathPreferenceSelector();
            dict = ConnectionDictionary.defaultDictionary();
            store = new SqlStateStore(dict);
        }
        mss = store.fetchMachineSnapshot(id);
        css = mss.getChannelSnapshots();
        qPVMap = getQuadMap();
        qPSPVMap = getQuadPSMap();
    }

    public HashMap<String, Double> getQuadMap() {
        HashMap<String, Double> pvMap = new HashMap<String, Double>();
        for (int i = 0; i < css.length; i++) {
            if ((css[i].getPV().indexOf("MON:G") > -1) || (css[i].getPV().indexOf("SET:G") > -1)) {
                double[] val = css[i].getValue();
                pvMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return pvMap;
    }

    public HashMap<String, Double> getQuadPSMap() {
        HashMap<String, Double> pvMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            if ((css[i].getPV().indexOf("MON:G") > -1) || (css[i].getPV().indexOf("SET:G") > -1)) {
                double[] val = css[i].getValue();
                pvMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return pvMap;
    }

    public HashMap<String, Double> getBPMXMap() {
        HashMap<String, Double> bpmXMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            if (css[i].getPV().indexOf("MON:X") > -1) {
                double[] val = css[i].getValue();
                bpmXMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return bpmXMap;
    }

    public HashMap<String, Double> getBPMYMap() {
        HashMap<String, Double> bpmYMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            if (css[i].getPV().indexOf("MON:Y") > -1) {
                double[] val = css[i].getValue();
                bpmYMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return bpmYMap;
    }

    public HashMap<String, Double> getBPMAmpMap() {
        HashMap<String, Double> bpmYMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            if (css[i].getPV().indexOf(":amplitudeAvg") > -1) {
                double[] val = css[i].getValue();
                bpmYMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return bpmYMap;
    }

    public HashMap<String, Double> getBPMPhaseMap() {
        HashMap<String, Double> bpmYMap = new HashMap<String, Double>();
        ChannelSnapshot[] css = mss.getChannelSnapshots();
        for (int i = 0; i < css.length; i++) {
            if (css[i].getPV().indexOf(":phaseAvg") > -1) {
                double[] val = css[i].getValue();
                bpmYMap.put(css[i].getPV(), new Double(val[0]));
            }
        }
        return bpmYMap;
    }

    /**
	 * set the model lattice with PV logger data source
	 * 
	 * @param seq
	 *            accelerator sequence
	 * @param scenario
	 *            Model Scenario object
	 * @return a new scenario with lattice from PV logger data
	 */
    public Scenario setModelSource(AcceleratorSeq seq, Scenario scenario) {
        myAccSeq = seq;
        AndTypeQualifier atq = new AndTypeQualifier();
        atq.and("Q");
        OrTypeQualifier otq1 = new OrTypeQualifier();
        otq1.or("DCH");
        otq1.or("DCV");
        OrTypeQualifier otq = new OrTypeQualifier();
        otq.or("PMQH");
        otq.or("PMQV");
        NotTypeQualifier ntq = new NotTypeQualifier(otq);
        otq1.or(atq);
        List<AcceleratorNode> allMags = seq.getNodesWithQualifier(otq1);
        List<AcceleratorNode> mags = AcceleratorSeq.filterNodesByStatus(allMags, true);
        for (int i = 0; i < mags.size(); i++) {
            Electromagnet quad = (Electromagnet) mags.get(i);
            String pvName = "";
            double val = quad.getDesignField();
            double pol = quad.getPolarity();
            if (quad.useFieldReadback()) {
                Channel chan = quad.getChannel(Electromagnet.FIELD_RB_HANDLE);
                pvName = chan.channelName();
                if (qPVMap.containsKey(pvName)) {
                    val = qPVMap.get(pvName).doubleValue();
                    val = val * pol;
                } else {
                    Channel chan2 = quad.getMainSupply().getChannel("psFieldRB");
                    String pvName2 = chan2.channelName();
                    if (qPSPVMap.containsKey(pvName2)) {
                        val = qPSPVMap.get(pvName2).doubleValue();
                        val = val * pol;
                    } else {
                        chan2 = quad.getMainSupply().getChannel("fieldSet");
                        pvName2 = chan2.channelName();
                        if (qPSPVMap.containsKey(pvName2)) {
                            val = qPSPVMap.get(pvName2).doubleValue();
                            val = val * pol;
                        } else System.out.println(pvName2 + " has no value");
                    }
                }
            } else {
                Channel chan = quad.getMainSupply().getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
                pvName = chan.channelName();
                if (qPVMap.containsKey(pvName)) {
                    val = qPVMap.get(pvName).doubleValue();
                    val = val * pol;
                    if (quad instanceof TrimmedQuadrupole) {
                        Channel chan1 = ((TrimmedQuadrupole) quad).getTrimSupply().getChannel(MagnetTrimSupply.FIELD_SET_HANDLE);
                        String pvName1 = chan1.channelName();
                        if (qPVMap.containsKey(pvName1)) {
                            double trimVal = Math.abs(qPVMap.get(pvName1).doubleValue());
                            trimVal = chan1.getValueTransform().convertFromRaw(ArrayValue.doubleStore(trimVal)).doubleValue();
                            if (pvName1.indexOf("ShntC") > -1) {
                                val = val - trimVal;
                            } else {
                                val = val + trimVal;
                            }
                        }
                    }
                } else {
                    pvName = quad.getChannel(Electromagnet.FIELD_RB_HANDLE).channelName();
                    if (qPVMap.containsKey(pvName)) {
                        val = qPVMap.get(pvName).doubleValue();
                    }
                }
            }
            scenario.setModelInput(quad, ElectromagnetPropertyAccessor.PROPERTY_FIELD, val);
        }
        try {
            scenario.resync();
        } catch (SynchronizationException e) {
            System.out.println(e);
        }
        return scenario;
    }

    public void setAccelSequence(AcceleratorSeq seq) {
        myAccSeq = seq;
    }

    /**
	 * get the beam current in mA, we use the first available BCM in the
	 * sequence. If the first in the sequence is not available, use MEBT BCM02.
	 * If it's also not available, then default to 20mA
	 * 
	 * @return beam current
	 */
    public double getBeamCurrent() {
        double current = 20.;
        List<AcceleratorNode> bcms = myAccSeq.getAllNodesOfType("BCM");
        List<AcceleratorNode> allBCMs = AcceleratorSeq.filterNodesByStatus(bcms, true);
        if (myAccSeq.getAllNodesOfType("BCM").size() > 0) {
            String firstBCM = ((CurrentMonitor) allBCMs.get(0)).getId();
            for (int i = 0; i < css.length; i++) {
                if (css[i].getPV().indexOf(firstBCM) > -1 && css[i].getPV().indexOf(":currentMax") > -1) {
                    current = css[i].getValue()[0];
                    return current;
                } else if (css[i].getPV().equals("MEBT_Diag:BCM02:currentMax")) {
                    current = css[i].getValue()[0];
                    return current;
                }
            }
        }
        return current;
    }

    /**
	 * get the beam current in mA, use the BCM specified here. If it's not
	 * available, use 20mA as default
	 * 
	 * @param bcm
	 *            the BCM you want the beam current reading from
	 * @return beam current
	 */
    public double getBeamCurrent(String bcm) {
        double current = 20;
        for (int i = 0; i < css.length; i++) {
            if (css[i].getPV().indexOf(bcm) > -1 && css[i].getPV().indexOf(":currentMax") > -1) {
                current = css[i].getValue()[0];
                return current;
            }
        }
        return current;
    }

    /**
	 * get all the channel snapshots.
	 * 
	 * @return channel snapshots in array
	 */
    public ChannelSnapshot[] getChannelSnapshots() {
        return css;
    }
}
