package jp.jparc.apps.bbc;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.JOptionPane;
import gov.sns.ca.Channel;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.PutException;
import gov.sns.tools.LinearFit;
import gov.sns.tools.plot.*;
import gov.sns.tools.scan.SecondEdition.AvgController;
import gov.sns.tools.scan.SecondEdition.MeasuredValue;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.smf.impl.MagnetMainSupply;

/**
 * This class contains the components internal to the Scanning procedure.
 * @author  jdg
 */
public class AnalysisStuff {

    /** the document this belongs to */
    protected BbcDocument theDoc;

    /** horizontal analysis data ready flag */
    protected boolean analysisDataXReady = false;

    /** vertical analysis data ready flag */
    protected boolean analysisDataYReady = false;

    /** model for selected sequence*/
    protected Scenario theModel;

    /** array list to record the slope, intercept and fitting error of horizontal downloadstream bpms */
    protected ArrayList[] downstreamBPMSlopeX;

    /** array list to record the slope, intercept and fitting error of vertical downloadstream bpms */
    protected ArrayList[] downstreamBPMSlopeY;

    /** array list to record the slope, intercept and fitting error of horizontal upstream bpm */
    protected ArrayList upstreamBPMSlopeX = new ArrayList();

    /** array list to record the slope, intercept and fitting error of vertical upstream bpm */
    protected ArrayList upstreamBPMSlopeY = new ArrayList();

    /** array list to record the average position of horizontal center BPM */
    protected ArrayList centerBPMPosAvgX = new ArrayList();

    protected ArrayList centerBPMPosAvgSigmaX = new ArrayList();

    /** array list to record the average position of vertical center BPM */
    protected ArrayList centerBPMPosAvgY = new ArrayList();

    protected ArrayList centerBPMPosAvgSigmaY = new ArrayList();

    /** array list to record the slope, intercept and fitting error of the horizontal centeral bpm */
    protected ArrayList bpmCenterPosX = new ArrayList();

    /** array list to record the slope, intercept and fitting error of the vertical centeral bpm */
    protected ArrayList bpmCenterPosY = new ArrayList();

    protected ArrayList bpmCenterPosXFitted = new ArrayList();

    protected ArrayList bpmCenterPosYFitted = new ArrayList();

    protected ArrayList stFieldXFitted = new ArrayList();

    protected ArrayList stFieldYFitted = new ArrayList();

    protected ArrayList stFieldXFitPrms = new ArrayList();

    protected ArrayList stFieldYFitPrms = new ArrayList();

    /** best bpm center */
    protected double bestBpmCenter = 0;

    /** best bpm center error */
    protected double bestBpmCenterErr = 0;

    private int nParamVals = 0;

    /** selected BPM2 numbers */
    private int bpm2Numbers = 0;

    protected boolean stXFieldReady = false;

    protected boolean stYFieldReady = false;

    protected double stXFieldVal = 0.0;

    protected double stYFieldVal = 0.0;

    protected Vector setStValVX = new Vector();

    protected Vector setStValVY = new Vector();

    protected String bestSteerCenterStrX;

    protected String bestSteerCenterStrY;

    protected String bestBPMCenterStrX;

    protected String bestBPMCenterStrY;

    private int bestBPMNumberX = 0;

    private int bestBPMNumberY = 0;

    /** Create an object */
    public AnalysisStuff(BbcDocument doc) {
        theDoc = doc;
    }

    /** initialize the analysis stuff, after the setup + scan stuff is done */
    protected void init(FunctionGraphsJPanel graphAnalysis, MeasuredValue bpm1PosMV, MeasuredValue[] bpm2PosMV, MeasuredValue bpm3PosMV) {
        if (bpm1PosMV != null && bpm1PosMV.getNumberOfDataContainers() != 0) {
            if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
                downstreamBPMSlopeX = new ArrayList[bpm2PosMV.length];
                for (int i = 0; i < bpm2PosMV.length; i++) downstreamBPMSlopeX[i] = new ArrayList();
            } else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
                downstreamBPMSlopeY = new ArrayList[bpm2PosMV.length];
                for (int i = 0; i < bpm2PosMV.length; i++) downstreamBPMSlopeY[i] = new ArrayList();
            }
            updateAnalysisData(graphAnalysis, bpm1PosMV, bpm2PosMV, bpm3PosMV);
            if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) analysisDataXReady = true; else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) analysisDataYReady = true;
            theDoc.myWindow().plotMeasuredData(graphAnalysis, bpm1PosMV, bpm2PosMV, bpm3PosMV);
            theDoc.setHasChanges(true);
        } else {
            JOptionPane.showMessageDialog(theDoc.myWindow(), "Can not update alalysis data because measured valued of the Centeral BPM is empty.");
        }
    }

    /** this method takes raw data from the scan Stuff and puts it into the analysis
    * containers. The data analysis is fitted using Linear with LSM method.
    */
    protected void updateAnalysisData(FunctionGraphsJPanel graphAnalysis, MeasuredValue bpm1PosMV, MeasuredValue[] bpm2PosMV, MeasuredValue bpm3PosMV) {
        clearData(graphAnalysis);
        nParamVals = bpm1PosMV.getNumberOfDataContainers();
        bpm2Numbers = bpm2PosMV.length;
        AvgController avgCntr = theDoc.scanStuff.avgCntrX;
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            avgCntr = theDoc.scanStuff.avgCntrY;
        }
        for (int i = 0; i < bpm2Numbers; i++) {
            if (bpm2PosMV[i].getNumberOfDataContainers() != nParamVals) {
                Toolkit.getDefaultToolkit().beep();
                String errText = "Opps, The number of parametric scans is different for the some of the BPM2s' measured value and BPM1's.\nDid the scan complete?";
                theDoc.myWindow().errorText.setText(errText);
                System.err.println(errText);
                return;
            }
        }
        if (theDoc.BPM3 != null && bpm3PosMV.getNumberOfDataContainers() != nParamVals) {
            Toolkit.getDefaultToolkit().beep();
            String errText = "Opps, The number of horizontal parametric scans is different for the some of the BPM3's measured value and BPM1's.\nDid the scan complete?";
            theDoc.myWindow().errorText.setText(errText);
            System.err.println(errText);
            return;
        }
        StringBuffer analysisText = new StringBuffer("Analysis Results:\n");
        for (int i = 0; i < nParamVals; i++) {
            BasicGraphData bgdBPM1Pos = bpm1PosMV.getDataContainer(i);
            BasicGraphData[] bgdBPM2Pos = new BasicGraphData[bpm2PosMV.length];
            for (int bpm2Count = 0; bpm2Count < bpm2Numbers; bpm2Count++) bgdBPM2Pos[bpm2Count] = bpm2PosMV[bpm2Count].getDataContainer(i);
            BasicGraphData bgdBPM3Pos = null;
            if (theDoc.BPM3 != null) bgdBPM3Pos = bpm3PosMV.getDataContainer(i);
            analysisText.append("\nCorrector Step " + i + "\n");
            double bpm1PosAvg = 0;
            double bpm1PosAvgSigma = 0;
            double bpm1PosSum2 = 0.0;
            double bpm1PosSum = 0.0;
            int nMeasures = bgdBPM1Pos.getNumbOfPoints();
            for (int counter = 0; counter < nMeasures; counter++) {
                bpm1PosSum += bgdBPM1Pos.getY(counter);
                bpm1PosSum2 += bgdBPM1Pos.getY(counter) * bgdBPM1Pos.getY(counter);
            }
            if (nMeasures > 0) {
                bpm1PosAvg = bpm1PosSum / nMeasures;
                bpm1PosAvgSigma = Math.sqrt(Math.abs(bpm1PosSum2 - nMeasures * bpm1PosAvg * bpm1PosAvg) / nMeasures);
            }
            if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
                centerBPMPosAvgX.add(new Double(bpm1PosAvg));
                centerBPMPosAvgSigmaX.add(new Double(bpm1PosAvgSigma));
            } else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
                centerBPMPosAvgY.add(new Double(bpm1PosAvg));
                centerBPMPosAvgSigmaY.add(new Double(bpm1PosAvgSigma));
            }
            for (int bpm2Num = 0; bpm2Num < bpm2Numbers; bpm2Num++) {
                double[] bpm2SlopeIntercept = new double[3];
                double bpm2SlopeSigma = 0.0;
                double bpm2InterceptSigma = 0.0;
                if (avgCntr.isOn() && avgCntr.getAvgNumber() > 1) {
                    WeightedLinearFit linearFit = new WeightedLinearFit();
                    for (int np = 0; np < bgdBPM2Pos[bpm2Num].getNumbOfPoints(); np++) {
                        linearFit.addSample(bgdBPM2Pos[bpm2Num].getX(np), bgdBPM2Pos[bpm2Num].getY(np), bgdBPM2Pos[bpm2Num].getErr(np));
                    }
                    bpm2SlopeIntercept[0] = linearFit.getSlope();
                    bpm2SlopeSigma = linearFit.getSlopeSigma();
                    bpm2SlopeIntercept[1] = linearFit.getIntercept();
                    bpm2InterceptSigma = linearFit.getInterceptSigma();
                    bpm2SlopeIntercept[2] = Math.sqrt(linearFit.getMeanSquareOrdinateError());
                } else {
                    LinearFit linearFit = new LinearFit();
                    for (int np = 0; np < bgdBPM2Pos[bpm2Num].getNumbOfPoints(); np++) {
                        linearFit.addSample(bgdBPM2Pos[bpm2Num].getX(np), bgdBPM2Pos[bpm2Num].getY(np));
                    }
                    bpm2SlopeIntercept[0] = linearFit.getSlope();
                    bpm2SlopeIntercept[1] = linearFit.getIntercept();
                    bpm2SlopeIntercept[2] = Math.sqrt(linearFit.getMeanSquareOrdinateError());
                }
                analysisText.append("Downstream BPM No." + bpm2Num + ": Slope = " + prettyString(bpm2SlopeIntercept[0]) + "; Intercept = " + prettyString(bpm2SlopeIntercept[1]) + "; Fitting Error = " + prettyString(bpm2SlopeIntercept[2]) + "\n");
                if (avgCntr.isOn() && avgCntr.getAvgNumber() > 1) {
                    analysisText.append("Slpoe Sigma = " + prettyString(bpm2SlopeSigma) + "; Intercept Sigma = " + prettyString(bpm2InterceptSigma) + "\n");
                }
                if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) downstreamBPMSlopeX[bpm2Num].add(bpm2SlopeIntercept); else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) downstreamBPMSlopeY[bpm2Num].add(bpm2SlopeIntercept);
            }
            if (theDoc.BPM3 != null) {
                double[] bpm3SlopeIntercept = new double[3];
                LinearFit linearFit = new LinearFit();
                for (int np = 0; np < bgdBPM3Pos.getNumbOfPoints(); np++) {
                    linearFit.addSample(bgdBPM3Pos.getX(np), bgdBPM3Pos.getY(np));
                }
                bpm3SlopeIntercept[0] = linearFit.getSlope();
                bpm3SlopeIntercept[1] = linearFit.getIntercept();
                bpm3SlopeIntercept[2] = Math.sqrt(linearFit.getMeanSquareOrdinateError());
                analysisText.append("Upstream BPM: " + "Slope = " + prettyString(bpm3SlopeIntercept[0]) + "; Intercept = " + prettyString(bpm3SlopeIntercept[1]) + "; Error = " + prettyString(bpm3SlopeIntercept[2]) + "\n");
                if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) upstreamBPMSlopeX.add(bpm3SlopeIntercept); else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) upstreamBPMSlopeY.add(bpm3SlopeIntercept);
            }
        }
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) theDoc.myWindow().analysisTextAreaX.setText(analysisText.toString()); else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) theDoc.myWindow().analysisTextAreaY.setText(analysisText.toString());
        System.out.println(analysisText.toString());
    }

    protected String prettyString(double num) {
        DecimalFormat fieldFormat;
        if (Math.abs(num) > 10000. || Math.abs(num) < 0.001) fieldFormat = new DecimalFormat("0.000E0"); else fieldFormat = new DecimalFormat("#####.######");
        return fieldFormat.format(num);
    }

    /** clear the containers of data */
    private void clearData(FunctionGraphsJPanel graphAnalysis) {
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
            for (int i = 0; i < theDoc.BPM2.length; i++) downstreamBPMSlopeX[i].clear();
            upstreamBPMSlopeX.clear();
            centerBPMPosAvgX.clear();
            centerBPMPosAvgSigmaX.clear();
            bpmCenterPosX.clear();
            bpmCenterPosXFitted.clear();
            stFieldXFitted.clear();
            stXFieldReady = false;
            stXFieldVal = 0.0;
            stFieldXFitPrms.clear();
            setStValVX.clear();
            analysisDataXReady = false;
        } else if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            for (int i = 0; i < theDoc.BPM2.length; i++) downstreamBPMSlopeY[i].clear();
            upstreamBPMSlopeY.clear();
            centerBPMPosAvgY.clear();
            centerBPMPosAvgSigmaY.clear();
            bpmCenterPosY.clear();
            bpmCenterPosYFitted.clear();
            stFieldYFitted.clear();
            stYFieldReady = false;
            stYFieldVal = 0.0;
            stFieldYFitPrms.clear();
            setStValVY.clear();
            analysisDataYReady = false;
        }
    }

    /** refresh both measured and model plot data */
    protected void plotUpdate(FunctionGraphsJPanel graphAnalysis, MeasuredValue bpm1PosMV, MeasuredValue[] bpm2PosMV, MeasuredValue bpm3PosMV) {
        theDoc.myWindow().plotMeasuredData(graphAnalysis, bpm1PosMV, bpm2PosMV, bpm3PosMV);
    }

    protected void solve(FunctionGraphsJPanel graphAnalysis) {
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
            bpmCenterPosX.clear();
            if (analysisDataXReady) {
                double bestCandidate = Double.MAX_VALUE;
                bestBPMNumberX = 0;
                bestBPMCenterStrX = "Horizontal Center for " + theDoc.BPM1.getId() + "\n";
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeX.length; j++) {
                    double[] fitParams = new double[5];
                    WeightedLinearFit bpmCenter = new WeightedLinearFit();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                        System.out.println(theDoc.BPM2[i].getId());
                        System.out.println("Slope of Downstream;" + "\t\tCenter BPM Position");
                        bpmCenter.addSample(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) (theDoc.analysisStuff.centerBPMPosAvgX.get(i)), (Double) (theDoc.analysisStuff.centerBPMPosAvgSigmaX.get(i)));
                        System.out.println("X = " + ((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0] + "; Y = " + (theDoc.analysisStuff.centerBPMPosAvgX.get(i)));
                        System.out.println();
                    }
                    fitParams[0] = bpmCenter.getSlope();
                    fitParams[1] = bpmCenter.getIntercept();
                    fitParams[2] = Math.sqrt(bpmCenter.getMeanSquareOrdinateError());
                    fitParams[3] = bpmCenter.getSlopeSigma();
                    fitParams[4] = bpmCenter.getInterceptSigma();
                    ArrayList fittedArray = new ArrayList();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                        fittedArray.add(bpmCenter.estimateY(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0]));
                    }
                    bpmCenterPosXFitted.add(fittedArray);
                    System.out.println("Result: " + (j + 1));
                    System.out.println("Slope = " + fitParams[0] + "; Sigma = " + fitParams[3] + "\nIntercept = " + fitParams[1] + "; Sigma = " + fitParams[4] + "\nFitting Error = " + fitParams[2] + "\n\n");
                    if (Math.abs(bpmCenter.getSlope()) < bestCandidate) {
                        bestCandidate = Math.abs(bpmCenter.getSlope());
                        bestBpmCenter = fitParams[1];
                        bestBpmCenterErr = fitParams[4];
                        bestBPMNumberX = j;
                    }
                    bpmCenterPosX.add(fitParams);
                }
                bestBPMCenterStrX += "Best response @  " + theDoc.BPM2[bestBPMNumberX].getId();
                bestBPMCenterStrX += ":  " + "\nCenteral position (mm) = " + bestBpmCenter + "\nSigma Error (mm) = +- " + bestBpmCenterErr;
                theDoc.myWindow().resultTextAreaX.setText(bestBPMCenterStrX);
                System.out.println("Horizontal Center for All BPMs");
                for (int i = 0; i < bpmCenterPosX.size(); i++) {
                    System.out.println(theDoc.BPM2[i].getId() + ":  " + "Centeral position (mm) = " + ((double[]) bpmCenterPosX.get(i))[1] + " +/- " + ((double[]) bpmCenterPosX.get(i))[4]);
                }
                System.out.println();
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            bpmCenterPosY.clear();
            if (analysisDataYReady) {
                double bestCandidate = Double.MAX_VALUE;
                bestBPMNumberY = 0;
                bestBPMCenterStrY = "Vertical Center for " + theDoc.BPM1.getId() + "\n";
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeY.length; j++) {
                    double[] fitParams = new double[5];
                    WeightedLinearFit bpmCenter = new WeightedLinearFit();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgY.size(); i++) {
                        System.out.println(theDoc.BPM2[i].getId());
                        System.out.println("Slope of Downstream;" + "\t\tCenter BPM Position");
                        bpmCenter.addSample(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) (theDoc.analysisStuff.centerBPMPosAvgY.get(i)), (Double) (theDoc.analysisStuff.centerBPMPosAvgSigmaY.get(i)));
                        System.out.println("X = " + ((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0] + "; Y = " + (theDoc.analysisStuff.centerBPMPosAvgY.get(i)));
                        System.out.println();
                    }
                    fitParams[0] = bpmCenter.getSlope();
                    fitParams[1] = bpmCenter.getIntercept();
                    fitParams[2] = Math.sqrt(bpmCenter.getMeanSquareOrdinateError());
                    fitParams[3] = bpmCenter.getSlopeSigma();
                    fitParams[4] = bpmCenter.getInterceptSigma();
                    ArrayList fittedArray = new ArrayList();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgY.size(); i++) {
                        fittedArray.add(bpmCenter.estimateY(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0]));
                    }
                    bpmCenterPosYFitted.add(fittedArray);
                    System.out.println("Result: " + (j + 1));
                    System.out.println("Slope = " + fitParams[0] + "; Sigma = " + fitParams[3] + "\nIntercept = " + fitParams[1] + "; Sigma = " + fitParams[4] + "\nFitting Error = " + fitParams[2]);
                    if (Math.abs(bpmCenter.getSlope()) < bestCandidate) {
                        bestCandidate = Math.abs(bpmCenter.getSlope());
                        bestBpmCenter = fitParams[1];
                        bestBpmCenterErr = fitParams[4];
                        bestBPMNumberY = j;
                    }
                    bpmCenterPosY.add(fitParams);
                }
                bestBPMCenterStrY += "Best response @  " + theDoc.BPM2[bestBPMNumberY].getId();
                bestBPMCenterStrY += ":  " + "\nCenteral position (mm) = " + bestBpmCenter + "\nSigma Error (mm) = +- " + bestBpmCenterErr;
                theDoc.myWindow().resultTextAreaY.setText(bestBPMCenterStrY);
                System.out.println("Vertical Center for All BPMs");
                for (int i = 0; i < bpmCenterPosY.size(); i++) {
                    System.out.println(theDoc.BPM2[i].getId() + ":  " + "Centeral position (mm) = " + ((double[]) bpmCenterPosY.get(i))[1] + " +/- " + ((double[]) bpmCenterPosY.get(i))[4]);
                }
                System.out.println();
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
    }

    protected void findSteerField(FunctionGraphsJPanel graphAnalysis) {
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
            if (analysisDataXReady) {
                double lowLimitVal = theDoc.scanStuff.scanControllerX.getParamLowLimit();
                double uppLimitVal = theDoc.scanStuff.scanControllerX.getParamUppLimit();
                if (lowLimitVal == uppLimitVal) {
                    stXFieldReady = true;
                    stXFieldVal = lowLimitVal;
                    return;
                }
                double stepVal = theDoc.scanStuff.scanControllerX.getParamStep();
                double setStVal = lowLimitVal;
                setStValVX = new Vector();
                setStValVX.add(setStVal);
                while (setStVal < uppLimitVal) {
                    setStVal += stepVal;
                    setStValVX.add(setStVal);
                }
                setStValVX.add(uppLimitVal);
                double bestCandidate = Double.MAX_VALUE;
                int bestBPMNumber = 0;
                bestSteerCenterStrX = "Seeking Horizontal Steering Magnet Field.\n ";
                bestSteerCenterStrX += "BPM Center for " + theDoc.BPM1.getId() + "\n";
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeX.length; j++) {
                    double[] fitParams = new double[3];
                    LinearFit bpmCenter = new LinearFit();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                        bpmCenter.addSample(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) setStValVX.get(i));
                        System.out.println("X = " + ((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0] + "; Y = " + setStValVX.get(i));
                    }
                    fitParams[0] = bpmCenter.getSlope();
                    fitParams[1] = bpmCenter.getIntercept();
                    fitParams[2] = Math.sqrt(bpmCenter.getMeanSquareOrdinateError());
                    ArrayList fittedArray = new ArrayList();
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                        fittedArray.add(bpmCenter.estimateY(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0]));
                    }
                    stFieldXFitted.add(fittedArray);
                    System.out.println("Slope = " + fitParams[0] + "\nIntercept = " + fitParams[1] + "\nFitting Error = " + fitParams[2]);
                    if (j == bestBPMNumberX) {
                        stXFieldVal = fitParams[1];
                    }
                    stFieldXFitPrms.add(fitParams);
                }
                bestSteerCenterStrX += "Best response @  " + theDoc.BPM2[bestBPMNumberX].getId();
                bestSteerCenterStrX += ":  " + "\nMagnet field of Steering magnet = " + stXFieldVal;
                theDoc.myWindow().resultTextAreaX.setText(bestSteerCenterStrX);
                stXFieldReady = true;
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            bpmCenterPosY.clear();
            if (analysisDataYReady) {
                double lowLimitVal = theDoc.scanStuff.scanControllerY.getParamLowLimit();
                double uppLimitVal = theDoc.scanStuff.scanControllerY.getParamUppLimit();
                if (lowLimitVal == uppLimitVal) {
                    stYFieldReady = true;
                    stYFieldVal = lowLimitVal;
                    return;
                }
                double stepVal = theDoc.scanStuff.scanControllerY.getParamStep();
                double setStVal = lowLimitVal;
                setStValVY = new Vector();
                setStValVY.add(setStVal);
                while (setStVal < uppLimitVal) {
                    setStVal += stepVal;
                    setStValVY.add(setStVal);
                }
                setStValVY.add(uppLimitVal);
                bestSteerCenterStrY = "Seeking Vertical Steering Magnet Field.\n ";
                bestSteerCenterStrY += "BPM Center for " + theDoc.BPM1.getId() + "\n";
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeY.length; j++) {
                    double[] fitParams = new double[3];
                    LinearFit bpmCenter = new LinearFit();
                    for (int i = 0; i < theDoc.analysisStuff.downstreamBPMSlopeY[j].size(); i++) {
                        bpmCenter.addSample(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) setStValVY.get(i));
                        System.out.println("X = " + ((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0] + "; Y = " + setStValVY.get(i));
                    }
                    fitParams[0] = bpmCenter.getSlope();
                    fitParams[1] = bpmCenter.getIntercept();
                    fitParams[2] = Math.sqrt(bpmCenter.getMeanSquareOrdinateError());
                    ArrayList fittedArray = new ArrayList();
                    for (int i = 0; i < theDoc.analysisStuff.downstreamBPMSlopeY[j].size(); i++) {
                        fittedArray.add(bpmCenter.estimateY(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0]));
                    }
                    stFieldYFitted.add(fittedArray);
                    System.out.println("Slope = " + fitParams[0] + "\nIntercept = " + fitParams[1] + "\nFitting Error = " + fitParams[2]);
                    if (j == bestBPMNumberY) {
                        stYFieldVal = fitParams[1];
                    }
                    stFieldYFitPrms.add(fitParams);
                }
                bestSteerCenterStrY += "Best response @  " + theDoc.BPM2[bestBPMNumberY].getId();
                bestSteerCenterStrY += ":  " + "\nMagnet field of Steering magnet = " + stYFieldVal;
                theDoc.myWindow().resultTextAreaY.setText(bestSteerCenterStrY);
                stYFieldReady = true;
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
    }

    protected void setSteerField(boolean isSetToSTX) {
        if (isSetToSTX) {
            Channel stXChan = theDoc.correctorH.getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
            try {
                stXChan.putVal(stXFieldVal);
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (PutException e) {
                e.printStackTrace();
            }
        } else {
            Channel stYChan = theDoc.correctorV.getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
            try {
                stYChan.putVal(stYFieldVal);
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (PutException e) {
                e.printStackTrace();
            }
        }
    }
}
