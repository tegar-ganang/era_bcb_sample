package gov.sns.apps.orbitdisplay;

import gov.sns.xal.smf.Accelerator;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.data.XMLDataManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.cosylab.databush.utilities.Orientation;

/**
 * Helper class for listening to the correlation notices and distributing the
 * new orbit values to the BeamAnalyzerBean.
 *
 * @author <a href="mailto:anze.zupanc@cosylab.com">Anze Zupanc</a>
 */
public class OrbitDisplayEngine {

    private File accelMasterFile = null;

    private Accelerator acc;

    private OrbitDisplayAll orbitDisplay;

    private AcceleratorSeq selectedAccSeq = null;

    private OrbitDisplayChannelCorrelator channelCorrelator = null;

    /**
	 * Returns current accelerator.
	 *
	 * @return Accelerator
	 */
    public Accelerator getAccelerator() {
        return acc;
    }

    /**
	 * Setts new accelerator.
	 *
	 * @param accelerator
	 */
    public void setAccelerator(Accelerator accelerator) {
        acc = accelerator;
    }

    /**
	 * Creates a new OrbitDisplayEngine object and setts OrbitDisplayAll
	 *	
	 * @param od OrbitDisplayAll
	 */
    public OrbitDisplayEngine(OrbitDisplayAll od) {
        setOrbitDisplay(od);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
    public OrbitDisplayAll getOrbitDisplay() {
        return orbitDisplay;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param display
	 */
    public void setOrbitDisplay(OrbitDisplayAll display) {
        orbitDisplay = display;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public List getAllAccSeq() {
        return getAccelerator().getAllSeqs();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
    public AcceleratorSeq getSelectedAccSeq() {
        return selectedAccSeq;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param file DOCUMENT ME!
	 */
    public void setAccelerator(File file) {
        if (file.equals(accelMasterFile)) {
            return;
        } else {
            accelMasterFile = file;
            setAccelerator(XMLDataManager.acceleratorWithPath(accelMasterFile.getPath()));
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param seq
	 */
    public void setSelectedAccSeq(AcceleratorSeq seq) {
        selectedAccSeq = seq;
        update();
    }

    private void update() {
        int avrgX = 10;
        int avrgY = 10;
        if (getOrbitDisplay().getOrbitDisplayP().getBeamAnalyzerBean() != null) {
            avrgX = getOrbitDisplay().getOrbitDisplayP().getBeamAnalyzerBean().getAveragingCount(Orientation.HORIZONTAL);
            avrgY = getOrbitDisplay().getOrbitDisplayP().getBeamAnalyzerBean().getAveragingCount(Orientation.VERTICAL);
        }
        BeamAnalyzerBean babean = new BeamAnalyzerBean(selectedAccSeq);
        babean.setAveragingCount(Orientation.HORIZONTAL, avrgX);
        babean.setAveragingCount(Orientation.VERTICAL, avrgY);
        getOrbitDisplay().getOrbitDisplayP().init(babean);
        if (selectedAccSeq != null) {
            correlate(babean);
        }
    }

    private void correlate(BeamAnalyzerBean babean) {
        if (getChannelCorrelator() == null) {
            setChannelCorrelator(new OrbitDisplayChannelCorrelator(1.e7));
            getChannelCorrelator().setBeamAnalyzerBean(babean);
            getChannelCorrelator().addBPMs(babean.getSequenceBPMList());
            getChannelCorrelator().startMonitoring();
            getChannelCorrelator().addListener(getChannelCorrelator().getCorrelationListner());
            reportOnConnection(babean);
        } else {
            getChannelCorrelator().stopMonitoring();
            getChannelCorrelator().removeAllChannels();
            getChannelCorrelator().removeListener(getChannelCorrelator().getCorrelationListner());
            getChannelCorrelator().setBeamAnalyzerBean(babean);
            getChannelCorrelator().addBPMs(babean.getSequenceBPMList());
            getChannelCorrelator().startMonitoring();
            getChannelCorrelator().addListener(getChannelCorrelator().getCorrelationListner());
            reportOnConnection(babean);
        }
    }

    private void reportOnConnection(BeamAnalyzerBean babean) {
        if (!getChannelCorrelator().isNoBadBPMs()) {
            ArrayList bad = getChannelCorrelator().getBadBPMs();
            Iterator iter = bad.iterator();
            ArrayList names = new ArrayList(bad.size());
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if (name.endsWith("xAvg")) {
                    name = name.replaceAll(":xAvg", "");
                    if (!names.contains(name)) names.add(name);
                } else if (name.endsWith("yAvg")) {
                    name = name.replaceAll(":yAvg", "");
                    if (!names.contains(name)) names.add(name);
                }
            }
            if (getChannelCorrelator().getGoodBPMs().isEmpty()) {
                getOrbitDisplay().getReportArea().append("SORRY! Could not connect to any of the BPM in selected sequence.\n");
                babean.removeNotConnectableBPMs(names);
                return;
            }
            babean.removeNotConnectableBPMs(names);
            Iterator it = names.iterator();
            getOrbitDisplay().getReportArea().append("Could not connect to following BPMs :\n");
            while (it.hasNext()) {
                getOrbitDisplay().getReportArea().append(" - " + (String) it.next() + "\n");
            }
        } else babean.setConnectableBPMs();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
    private OrbitDisplayChannelCorrelator getChannelCorrelator() {
        return channelCorrelator;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param correlator
	 */
    private void setChannelCorrelator(OrbitDisplayChannelCorrelator correlator) {
        channelCorrelator = correlator;
    }
}
