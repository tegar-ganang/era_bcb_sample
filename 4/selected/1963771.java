package org.mcisb.beacon.ui.pedro.file;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import pedro.model.RecordModel;
import pedro.model.RecordModelFactory;
import pedro.view.NavigationTreeNode;
import org.mcisb.beacon.analysis.*;
import org.mcisb.beacon.model.*;
import org.mcisb.beacon.ui.*;
import org.mcisb.util.xml.*;

/**
 * 
 * @author Neil Swainston
 */
public class TrackerParser extends ResultsParser {

    /**
	 * 
	 */
    private final Element root;

    private final File file;

    /**
	 *
	 * @param file
	 * @param recordModelFactory
	 * @param node
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
    public TrackerParser(File f, final RecordModelFactory recordModelFactory, final NavigationTreeNode node) throws SAXException, ParserConfigurationException, IOException {
        super(f, recordModelFactory, node);
        file = f;
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        root = document.getDocumentElement();
    }

    protected RecordModel getFileInfoRecordModel() {
        final RecordModel fileInfoRecordModel = recordModelFactory.createRecordModel("File_Info");
        final Element fileInfoElement = XmlUtils.getFirstElement(root, "File_Info");
        if (fileInfoElement != null) {
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "FileName");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Scan_Mode");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Stack_Size");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Scaling");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Zoom");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Position");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Objective");
            pedroUtils.addElement(fileInfoElement, fileInfoRecordModel, "Pixel_Time");
            final String TAG_NAME = "Item";
            final String NODE_NAME = "value";
            pedroUtils.createAndPopulateItemElements(recordModelFactory, fileInfoRecordModel, fileInfoElement, "Pinhole", TAG_NAME, NODE_NAME);
            pedroUtils.createAndPopulateItemElements(recordModelFactory, fileInfoRecordModel, fileInfoElement, "Filters", TAG_NAME, NODE_NAME);
            pedroUtils.createAndPopulateItemElements(recordModelFactory, fileInfoRecordModel, fileInfoElement, "Beam_Splitters", TAG_NAME, NODE_NAME);
            pedroUtils.createAndPopulateItemElements(recordModelFactory, fileInfoRecordModel, fileInfoElement, "Wavelength", TAG_NAME, NODE_NAME);
        }
        return fileInfoRecordModel;
    }

    protected RecordModel getTimeSeriesRecordModel() {
        final RecordModel timeSeriesRecordModel = recordModelFactory.createRecordModel("ResultTimeSeries");
        final Element timeSeriesElement = XmlUtils.getFirstElement(root, "ResultTimeSeries");
        if (timeSeriesElement != null) {
            final NodeList resultStates = timeSeriesElement.getElementsByTagName("ResultState");
            for (int i = 0; i < resultStates.getLength(); i++) {
                final Element resultStateElement = (Element) resultStates.item(i);
                final RecordModel resultStateRecordModel = recordModelFactory.createRecordModel("ResultState");
                pedroUtils.addElement(resultStateElement, resultStateRecordModel, "TimeStamp");
                final NodeList cells = resultStateElement.getElementsByTagName("Cell");
                for (int j = 0; j < cells.getLength(); j++) {
                    final Element cellElement = (Element) cells.item(j);
                    final RecordModel cellRecordModel = recordModelFactory.createRecordModel("Cell");
                    pedroUtils.addElement(cellElement, cellRecordModel, "name");
                    final NodeList cellularCompartments = cellElement.getElementsByTagName("CellularCompartment");
                    for (int k = 0; k < cellularCompartments.getLength(); k++) {
                        final Element cellularCompartmentElement = (Element) cellularCompartments.item(k);
                        final RecordModel cellularCompartmentRecordModel = recordModelFactory.createRecordModel("CellularCompartment");
                        pedroUtils.addElement(cellularCompartmentElement, cellularCompartmentRecordModel, "name");
                        pedroUtils.addElement(cellularCompartmentElement, cellularCompartmentRecordModel, "Centroid");
                        final NodeList cellProperties = cellElement.getElementsByTagName("CellProperty");
                        if (cellProperties != null) {
                            for (int l = 0; l < cellProperties.getLength(); l++) {
                                final Element cellPropertyElement = (Element) cellProperties.item(l);
                                final RecordModel cellPropertyRecordModel = recordModelFactory.createRecordModel("CellProperty");
                                pedroUtils.addElement(cellPropertyElement, cellPropertyRecordModel, "property_name");
                                pedroUtils.addElements(cellPropertyElement, cellPropertyRecordModel, recordModelFactory, "Channel", Arrays.asList(new String[] { "channel_name", "channel_intensity" }));
                                cellularCompartmentRecordModel.addChild("CellProperty", cellPropertyRecordModel, false);
                                cellularCompartmentRecordModel.updateDisplayName();
                            }
                        }
                        final Element polygon = XmlUtils.getFirstElement(cellularCompartmentElement, "Polygon");
                        if (polygon != null) {
                            final Element exteriorElement = XmlUtils.getFirstElement(polygon, "exterior");
                            final Element linearRingElement = XmlUtils.getFirstElement(exteriorElement, "LinearRing");
                            final RecordModel linearRingRecordModel = recordModelFactory.createRecordModel("LinearRing");
                            pedroUtils.addElement(linearRingElement, linearRingRecordModel, "coordinates");
                            final RecordModel exteriorRecordModel = recordModelFactory.createRecordModel("exterior");
                            exteriorRecordModel.addChild("LinearRing", linearRingRecordModel, false);
                            final RecordModel polygonRecordModel = recordModelFactory.createRecordModel("Polygon");
                            polygonRecordModel.addChild("exterior", exteriorRecordModel, false);
                            cellularCompartmentRecordModel.addChild("Polygon", polygonRecordModel, false);
                        }
                        cellRecordModel.addChild("CellularCompartment", cellularCompartmentRecordModel, false);
                        cellRecordModel.updateDisplayName();
                    }
                    resultStateRecordModel.addChild("Cell", cellRecordModel, false);
                    resultStateRecordModel.updateDisplayName();
                }
                timeSeriesRecordModel.addChild("ResultState", resultStateRecordModel, false);
                timeSeriesRecordModel.updateDisplayName();
            }
        }
        return timeSeriesRecordModel;
    }

    protected RecordModel getAnalysisResultsRecordModel(String[] plasmidNames) {
        TrackerResultsAnalyser ta = new TrackerResultsAnalyser(file);
        AnalysisResults ar = ta.getAnalysisResults();
        ArrayList cnames = new ArrayList();
        for (int i = 0; i < ar.getAnalysedCell().get(0).getAnalysedChannel().size(); i++) {
            cnames.add(ar.getAnalysedCell().get(0).getAnalysedChannel().get(i).getChannelName());
        }
        PlasmidChannelMatchDialog pcmd = new PlasmidChannelMatchDialog();
        String[] channelNames = (String[]) cnames.toArray(new String[cnames.size()]);
        String[] plasmidOrder = pcmd.getPlasmidOrder(channelNames, plasmidNames);
        final RecordModel analysisResultsRecordModel = recordModelFactory.createRecordModel("AnalysisResults");
        for (int i = 0; i < ar.getAnalysedCell().size(); i++) {
            AnalysedCell ac = ar.getAnalysedCell().get(i);
            RecordModel analysedCellRecordModel = recordModelFactory.createRecordModel("AnalysedCell");
            analysedCellRecordModel.setValue("CellName", ac.getCellName(), false);
            if (ac.getPreStimPeriod() != null) {
                analysedCellRecordModel.setValue("PreStimPeriod", ac.getPreStimPeriod().toString(), false);
            }
            for (int j = 0; j < ac.getAnalysedChannel().size(); j++) {
                AnalysedChannel aCh = ac.getAnalysedChannel().get(j);
                RecordModel analysedChannelRecordModel = recordModelFactory.createRecordModel("AnalysedChannel");
                analysedChannelRecordModel.setValue("ChannelName", aCh.getChannelName(), false);
                analysedChannelRecordModel.setValue("PlasmidName", plasmidOrder[j], false);
                for (int k = 0; k < aCh.getPeak().size(); k++) {
                    org.mcisb.beacon.model.Peak peak = aCh.getPeak().get(k);
                    RecordModel peakRecordModel = recordModelFactory.createRecordModel("Peak");
                    peakRecordModel.setValue("time", Double.toString(peak.getTime()), false);
                    peakRecordModel.setValue("value", Double.toString(peak.getValue()), false);
                    peakRecordModel.updateDisplayName();
                    analysedChannelRecordModel.addChild("Peak", peakRecordModel, false);
                    analysedChannelRecordModel.updateDisplayName();
                }
                if (aCh.getMaxPeak() != null) {
                    analysedChannelRecordModel.setValue("MaxPeak", aCh.getMaxPeak().toString(), false);
                }
                if (aCh.getPeriod() != null) {
                    analysedChannelRecordModel.setValue("Period", aCh.getPeriod().toString(), false);
                }
                if (aCh.getPeriodStdDev() != null) {
                    analysedChannelRecordModel.setValue("PeriodStdDev", aCh.getPeriodStdDev().toString(), false);
                }
                if (aCh.getDecayRate() != null) {
                    analysedChannelRecordModel.setValue("DecayRate", aCh.getDecayRate().toString(), false);
                }
                if (aCh.getFirstPeakTime() != null) {
                    analysedChannelRecordModel.setValue("FirstPeakTime", aCh.getFirstPeakTime().toString(), false);
                }
                analysedChannelRecordModel.updateDisplayName();
                analysedCellRecordModel.addChild("AnalysedChannel", analysedChannelRecordModel, false);
            }
            analysisResultsRecordModel.addChild("AnalysedCell", analysedCellRecordModel, false);
            analysisResultsRecordModel.updateDisplayName();
        }
        return analysisResultsRecordModel;
    }
}
