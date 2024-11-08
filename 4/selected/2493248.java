package chequeredflag.data.track;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.awt.*;
import chequeredflag.data.f1gp.CosLookupTable;
import chequeredflag.data.f1gp.F1GPMath;

/**
 *
 * @author Klaus
 */
public class Track {

    /** Creates a new instance of Track */
    public Track() {
        m_baBackground = new byte[4096];
        m_FileHeader = new TrackFileHeader();
        m_Objects = new TrackObjects();
        m_DataHeader = new TrackDataHeader();
        m_TrackSegments = new TrackSegments();
        m_CCLine = new CCLine();
        m_CCSetup = new CCSetup();
        m_PitlaneSegments = new TrackSegments();
        m_Footer = new Footer();
        m_fLayoutMode = true;
    }

    public boolean load(File file) {
        boolean loadSuccess = true;
        long lPos;
        FileChannel fc;
        try {
            m_File = file;
            FileInputStream fis = new FileInputStream(file);
            fc = fis.getChannel();
            fis.read(m_baBackground);
            m_FileHeader.load(fis);
            m_Objects.load(fis, m_FileHeader.m_nTrackDataOffset);
            m_DataHeader.load(fis);
            m_TrackSegments.load(fis);
            m_CCLine.load(fis);
            m_CCSetup.load(fis);
            m_PitlaneSegments.load(fis);
            m_PitlaneSegments.remove(m_PitlaneSegments.size() - 1);
            lPos = m_PitlaneSegments.size();
            lPos = fc.position();
            int nCount = m_Footer.load(fis);
            m_nLapNumIndex = nCount - 10;
            fis.close();
        } catch (Exception exceptionError) {
            loadSuccess = false;
        }
        ;
        if (loadSuccess == false) {
            return false;
        } else {
            calculateTrackLayout();
            calculateCCLine();
            return true;
        }
    }

    public int save() {
        return save(m_File);
    }

    public int save(File file) {
        int nBytesWritten = 0;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(m_baBackground);
            nBytesWritten = m_baBackground.length;
            nBytesWritten += m_FileHeader.save(fos);
            nBytesWritten += m_Objects.save(fos);
            nBytesWritten += m_DataHeader.save(fos);
            nBytesWritten += m_TrackSegments.save(fos);
            nBytesWritten += m_CCLine.save(fos);
            nBytesWritten += m_CCSetup.save(fos);
            nBytesWritten += m_PitlaneSegments.save(fos);
            m_Footer.setChecksum(0);
            nBytesWritten += m_Footer.save(fos);
            FileChannel fc = fos.getChannel();
            long lPos = fc.position();
            m_FileHeader.setChecksumOffset(lPos - 4);
            fc.position(4104);
            m_FileHeader.saveChecksumOffset(fos);
            fos.close();
            calculateChecksum(file);
        } catch (IOException ioe) {
        }
        return nBytesWritten;
    }

    protected void calculateChecksum(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            try {
                long lLength = raf.length();
                int nChecksum1 = 0, nChecksum2 = 0;
                int nChar;
                for (long lPos = 0; lPos < lLength - 4; lPos++) {
                    nChar = raf.read();
                    nChecksum1 += nChar;
                    nChecksum2 = ((nChecksum2 << 3) & 0x0FFF8) + ((nChecksum2 >> 13) & 0x07);
                    nChecksum2 = nChecksum2 + nChar;
                }
                nChecksum1 &= 0x0FFFF;
                nChecksum2 &= 0x0FFFF;
                raf.write(nChecksum1 & 0x0FF);
                raf.write((nChecksum1 >> 8) & 0x0FF);
                raf.write(nChecksum2 & 0x0FF);
                raf.write((nChecksum2 >> 8) & 0x0FF);
                raf.close();
            } catch (IOException ioe) {
            }
        } catch (FileNotFoundException fnfe) {
        }
    }

    /** Methods to get several parts of the track object */
    public TrackDataHeader getTrackDataHeader() {
        return m_DataHeader;
    }

    public TrackSegments getTrackSegments() {
        return m_TrackSegments;
    }

    public TrackSegments getPitlaneSegments() {
        return m_PitlaneSegments;
    }

    public CCLine getCCLine() {
        return m_CCLine;
    }

    public CCSetup getCCSetup() {
        return m_CCSetup;
    }

    public TrackObjects getTrackObjects() {
        return m_Objects;
    }

    public boolean getLayoutMode() {
        return m_fLayoutMode;
    }

    public boolean setLayoutMode(boolean fLayoutMode) {
        boolean fOldLayoutMode = m_fLayoutMode;
        m_fLayoutMode = fLayoutMode;
        return fLayoutMode;
    }

    /** Data members */
    protected File m_File;

    protected byte m_baBackground[];

    protected int m_nLapNumIndex;

    protected TrackFileHeader m_FileHeader;

    protected TrackObjects m_Objects;

    protected TrackDataHeader m_DataHeader;

    protected TrackSegments m_TrackSegments;

    protected CCLine m_CCLine;

    protected CCSetup m_CCSetup;

    protected TrackSegments m_PitlaneSegments;

    protected Footer m_Footer;

    protected boolean m_fLayoutMode;

    static final double s_dWIDTHSCALE = 0.0047625;

    static final double s_dANGLE_SCALE = (2 * Math.PI) / 65536;

    static final double s_dRADIUS_SCALE = 1 / 128.0;

    public void calculateTrackLayout() {
        int nStartPosX, nStartPosY;
        nStartPosX = m_DataHeader.getStartPos(0) << 3;
        nStartPosY = m_DataHeader.getStartPos(1) << 3;
        m_TrackSegments.calculateTrackLayout(m_DataHeader.getStartWidth(), m_DataHeader.getStartAngle(), nStartPosX, nStartPosY, m_fLayoutMode);
        m_PitlaneSegments.calculatePitlaneLayout(m_TrackSegments, m_DataHeader.getPitSide(), m_fLayoutMode);
    }

    ;

    /**
        Intersect ccLine segment with end of track segment.
        Parameters: CCline segment
                    point and angle of track at end of CCLine segment.
        Calculate end point and angle for ccLineSegment and store into segment.
    */
    public void intersect(CCLineSegment ccLineSegment, double dTrackPosX, double dTrackPosY, double dTrackAngle) {
        double dRadius = ccLineSegment.getRadius();
        double dCClineAngle = ccLineSegment.getAngleStart();
        if (ccLineSegment.isStraight()) {
            ccLineSegment.setPosXCenter(0.0);
            ccLineSegment.setPosYCenter(0.0);
            ccLineSegment.setAngleStart(ccLineSegment.getAngleStart() - s_dANGLE_SCALE * ccLineSegment.getShift());
            ccLineSegment.setShiftSegment(null);
            double dAux;
            dAux = Math.sin(ccLineSegment.getAngleStart()) * Math.sin(dTrackAngle) + Math.cos(ccLineSegment.getAngleStart()) * Math.cos(dTrackAngle);
            if (Math.abs(dAux) < 1e-10) {
                ccLineSegment.setS(100.0);
                ccLineSegment.setLength(ccLineSegment.getTlu());
            } else {
                ccLineSegment.setLength((Math.cos(dTrackAngle) * (dTrackPosY - ccLineSegment.getPosYStart())) - (Math.sin(dTrackAngle) * (dTrackPosX - ccLineSegment.getPosXStart())));
                ccLineSegment.setS((-Math.sin(ccLineSegment.getAngleStart()) * (dTrackPosY - ccLineSegment.getPosYStart()) - Math.cos(ccLineSegment.getAngleStart()) * (dTrackPosX - ccLineSegment.getPosXStart())) / dAux);
            }
            ccLineSegment.setAngleEnd(ccLineSegment.getAngleStart());
            ccLineSegment.setPosXEnd(dTrackPosX + ccLineSegment.getS() * Math.cos(dTrackAngle));
            ccLineSegment.setPosYEnd(dTrackPosY + ccLineSegment.getS() * Math.sin(dTrackAngle));
        } else {
            int i;
            double dS = 0.0;
            if (ccLineSegment.getShift() > 0) {
                CCLineSegment ccLineAux = new CCLineSegment(1);
                ccLineAux.setRadius(0.0);
                ccLineAux.setPosXCenter(0.0);
                ccLineAux.setPosYCenter(0.0);
                ccLineAux.setPosXStart(ccLineSegment.getPosXStart());
                ccLineAux.setPosYStart(ccLineSegment.getPosYStart());
                ccLineAux.setAngleStart(ccLineSegment.getAngleStart());
                ccLineAux.setAngleEnd(ccLineSegment.getAngleStart());
                double dLength = ccLineSegment.getShift() / 256.0;
                ccLineSegment.setLength(dLength);
                ccLineSegment.setS(0.0);
                ccLineAux.setPosXEnd(ccLineAux.getPosXStart() - dLength * Math.sin(ccLineAux.getAngleStart()));
                ccLineAux.setPosYEnd(ccLineAux.getPosYStart() + dLength * Math.cos(ccLineAux.getAngleStart()));
                ccLineSegment.setShiftSegment(ccLineAux);
                ccLineSegment.setPosXStart(ccLineAux.getPosXEnd());
                ccLineSegment.setPosYStart(ccLineAux.getPosYEnd());
            } else {
                ccLineSegment.setShiftSegment(null);
            }
            ccLineSegment.setPosXCenter(ccLineSegment.getPosXStart() + ccLineSegment.getRadius() * Math.cos(ccLineSegment.getAngleStart()));
            ccLineSegment.setPosYCenter(ccLineSegment.getPosYStart() + ccLineSegment.getRadius() * Math.sin(ccLineSegment.getAngleStart()));
            double adAlfa[] = new double[2];
            double adS[] = new double[2];
            double dCotTrackAngle, dA, dC, dF, dF2;
            if (Math.abs(Math.cos(dTrackAngle)) < Math.abs(Math.sin(dTrackAngle))) {
                dCotTrackAngle = Math.cos(dTrackAngle) / Math.sin(dTrackAngle);
                dA = 1.0 / Math.sqrt(1 + dCotTrackAngle * dCotTrackAngle);
                dC = dA * (dTrackPosX - ccLineSegment.getPosXCenter() + dCotTrackAngle * (ccLineSegment.getPosYCenter() - dTrackPosY)) / ccLineSegment.getRadius();
                dC = -dC;
                dF = Math.atan2(-dA, dCotTrackAngle * dA);
                if (Math.abs(dC) > 1.0) {
                    ccLineSegment.setS(100.0);
                    ccLineSegment.setAngleEnd(ccLineSegment.getAngleStart());
                    ccLineSegment.setLength(1.0);
                    ccLineSegment.setPosXEnd(ccLineSegment.getPosXStart() - Math.sin(ccLineSegment.getAngleStart()));
                    ccLineSegment.setPosYEnd(ccLineSegment.getPosYStart() + Math.cos(ccLineSegment.getAngleStart()));
                    ccLineSegment.setValid(false);
                    return;
                } else {
                    dF2 = Math.asin(dC);
                }
                adAlfa[0] = -dF2 - dF;
                adS[0] = (ccLineSegment.getPosYCenter() - dTrackPosY - ccLineSegment.getRadius() * Math.sin(adAlfa[0])) / Math.sin(dTrackAngle);
                adAlfa[1] = Math.PI + dF2 - dF;
                adS[1] = (ccLineSegment.getPosYCenter() - dTrackPosY - ccLineSegment.getRadius() * Math.sin(adAlfa[1])) / Math.sin(dTrackAngle);
            } else {
                double dTanTrackAngle;
                dTanTrackAngle = Math.tan(dTrackAngle);
                dA = 1.0 / Math.sqrt(1 + dTanTrackAngle * dTanTrackAngle);
                dC = dA * (dTrackPosY - ccLineSegment.getPosYCenter() + dTanTrackAngle * (ccLineSegment.getPosXCenter() - dTrackPosX)) / ccLineSegment.getRadius();
                dC = -dC;
                dF = Math.atan2(dTanTrackAngle * dA, -dA);
                if (Math.abs(dC) > 1.0) {
                    ccLineSegment.setS(100.0);
                    ccLineSegment.setLength(1.0);
                    ccLineSegment.setAngleEnd(ccLineSegment.getAngleStart());
                    ccLineSegment.setPosXEnd(ccLineSegment.getPosXStart() - Math.sin(ccLineSegment.getAngleStart()));
                    ccLineSegment.setPosYEnd(ccLineSegment.getPosYStart() + Math.cos(ccLineSegment.getAngleStart()));
                    ccLineSegment.setValid(false);
                    return;
                }
                dF2 = Math.asin(dC);
                adAlfa[0] = -dF2 - dF;
                adS[0] = (ccLineSegment.getPosXCenter() - dTrackPosX - ccLineSegment.getRadius() * Math.cos(adAlfa[0])) / Math.cos(dTrackAngle);
                adAlfa[1] = Math.PI + dF2 - dF;
                adS[1] = (ccLineSegment.getPosXCenter() - dTrackPosX - ccLineSegment.getRadius() * Math.cos(adAlfa[1])) / Math.cos(dTrackAngle);
            }
            for (i = 0; i < 2; i++) {
                while ((adAlfa[i] - ccLineSegment.getAngleStart()) > (2.0 * Math.PI)) {
                    adAlfa[i] -= 2.0 * Math.PI;
                }
                while ((adAlfa[i] - ccLineSegment.getAngleStart()) <= (-2.0 * Math.PI)) {
                    adAlfa[i] += 2.0 * Math.PI;
                }
                if (ccLineSegment.turnsRight()) {
                    while (-adAlfa[i] <= -ccLineSegment.getAngleStart()) {
                        adAlfa[i] -= 2.0 * Math.PI;
                    }
                } else {
                    while (adAlfa[i] <= ccLineSegment.getAngleStart()) {
                        adAlfa[i] += 2.0 * Math.PI;
                    }
                }
            }
            if (((ccLineSegment.getPosYStart() - dTrackPosY) * Math.cos(dTrackAngle) - (ccLineSegment.getPosXStart() - dTrackPosX) * Math.sin(dTrackAngle)) > 0.0) {
                if ((ccLineSegment.turnsRight() && (-adAlfa[0] < -adAlfa[1])) || (ccLineSegment.turnsLeft() && (adAlfa[0] < adAlfa[1]))) {
                    ccLineSegment.setAngleEnd(adAlfa[1]);
                    ccLineSegment.setS(adS[1]);
                } else {
                    ccLineSegment.setAngleEnd(adAlfa[0]);
                    ccLineSegment.setS(adS[0]);
                }
            } else {
                if ((ccLineSegment.turnsRight() && (-adAlfa[0] < -adAlfa[1])) || (ccLineSegment.turnsLeft() && (adAlfa[0] < adAlfa[1]))) {
                    ccLineSegment.setAngleEnd(adAlfa[0]);
                    ccLineSegment.setS(adS[0]);
                } else {
                    ccLineSegment.setAngleEnd(adAlfa[1]);
                    ccLineSegment.setS(adS[1]);
                }
            }
            ccLineSegment.setLength(Math.abs((ccLineSegment.getAngleEnd() - ccLineSegment.getAngleStart()) * ccLineSegment.getRadius()));
            ccLineSegment.setPosXEnd(dTrackPosX + ccLineSegment.getS() * Math.cos(dTrackAngle));
            ccLineSegment.setPosYEnd(dTrackPosY + ccLineSegment.getS() * Math.sin(dTrackAngle));
        }
    }

    public void calculateCCLine() {
        try {
            if (m_CCLine.size() == 0) return;
            CCLineSegment ccLineSegment;
            TrackSegment trackSegment;
            int nCumulatedTrackTlu = 0, nCumulatedCCLineTlu = 0;
            int nTrackSegmentIndex = 1, nCCLineSegmentIndex = 1;
            double dXPos, dYPos, dOffset, dAngleStart, dAngleEnd, dRadius;
            ccLineSegment = m_CCLine.getAt(nCCLineSegmentIndex++);
            nCumulatedCCLineTlu = ccLineSegment.getTlu();
            trackSegment = m_TrackSegments.getAt(nTrackSegmentIndex++);
            nCumulatedTrackTlu = trackSegment.getTlu();
            dXPos = trackSegment.getPosXStart();
            dYPos = trackSegment.getPosYStart();
            dOffset = ccLineSegment.getParam(0) / 1024.0;
            dAngleStart = trackSegment.getAngleStart() * s_dANGLE_SCALE;
            ccLineSegment.setAngleStart(dAngleStart);
            ccLineSegment.setPosXStart(dXPos + Math.cos(dAngleStart) * dOffset);
            ccLineSegment.setPosYStart(dYPos + Math.sin(dAngleStart) * dOffset);
            do {
                dXPos = ccLineSegment.getPosXStart();
                dYPos = ccLineSegment.getPosYStart();
                dAngleStart = ccLineSegment.getAngleStart();
                dRadius = ccLineSegment.calculateRadius();
                while (nCumulatedCCLineTlu > nCumulatedTrackTlu) {
                    trackSegment = m_TrackSegments.getAt(nTrackSegmentIndex++);
                    if (trackSegment == null) {
                        nCumulatedTrackTlu = nCumulatedCCLineTlu;
                        nTrackSegmentIndex = nTrackSegmentIndex - 3;
                        if (nTrackSegmentIndex < 1) nTrackSegmentIndex = 1;
                        trackSegment = m_TrackSegments.getAt(nTrackSegmentIndex);
                    } else {
                        nCumulatedTrackTlu += trackSegment.getTlu();
                    }
                }
                double dTrackAngle = trackSegment.getAngleEnd() - (nCumulatedCCLineTlu - nCumulatedTrackTlu) * trackSegment.getCurvature();
                dTrackAngle = dTrackAngle * s_dANGLE_SCALE;
                double dTrackPosX;
                double dTrackPosY;
                if (trackSegment.getCurvature() == 0) {
                    dTrackPosX = trackSegment.getPosXStart() - (nCumulatedCCLineTlu - nCumulatedTrackTlu + trackSegment.getTlu()) * Math.sin(dTrackAngle);
                    dTrackPosY = trackSegment.getPosYStart() + (nCumulatedCCLineTlu - nCumulatedTrackTlu + trackSegment.getTlu()) * Math.cos(dTrackAngle);
                } else {
                    dTrackPosX = trackSegment.getPosXCenter() - trackSegment.getRadius() * Math.cos(dTrackAngle);
                    dTrackPosY = trackSegment.getPosYCenter() - trackSegment.getRadius() * Math.sin(dTrackAngle);
                }
                intersect(ccLineSegment, dTrackPosX, dTrackPosY, dTrackAngle);
                if (nCCLineSegmentIndex <= m_CCLine.size()) {
                    dXPos = ccLineSegment.getPosXEnd();
                    dYPos = ccLineSegment.getPosYEnd();
                    dAngleEnd = ccLineSegment.getAngleEnd();
                    ccLineSegment = m_CCLine.getAt(nCCLineSegmentIndex++);
                    nCumulatedCCLineTlu += ccLineSegment.getTlu();
                    ccLineSegment.setPosXStart(dXPos);
                    ccLineSegment.setPosYStart(dYPos);
                    ccLineSegment.setAngleStart(dAngleEnd);
                } else ccLineSegment = null;
            } while (ccLineSegment != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        calculateGameCCLine();
    }

    private int length, radius;

    private short shiftx;

    private short wTmpAngleZ;

    private short wSegPosX;

    /**
      CCLine calculations retrieved from game by Rene.
    */
    protected void calculateGameCCLine() {
        if (m_fLayoutMode) return;
        int nTrackSegNum = 0;
        wTmpAngleZ = m_TrackSegments.getSegAt(nTrackSegNum).wAngleZ;
        CCLineSegment cclineSeg;
        int nCCLineSector = 0;
        for (Enumeration e = m_CCLine.elements(); e.hasMoreElements(); ) {
            cclineSeg = (CCLineSegment) e.nextElement();
            int nShift;
            int nParam = 0;
            nCCLineSector++;
            if ((cclineSeg.m_nType & 0x80) != 0) wSegPosX = (short) cclineSeg.m_nParam[nParam++];
            nShift = cclineSeg.m_nParam[nParam++];
            radius = cclineSeg.m_nParam[nParam++];
            if ((cclineSeg.m_nType & 0x40) != 0) {
                radius = (radius << 16) | (cclineSeg.m_nParam[nParam++] & 0x0FFFF);
            }
            radius <<= 3;
            if (radius != 0) shiftx = (short) (nShift << 2); else {
                shiftx = 0;
                wTmpAngleZ += (short) nShift;
            }
            length = ((cclineSeg.m_nType & 0x3f) << 8) | cclineSeg.m_nTlu;
            Seg seg = m_TrackSegments.getSegAt(nTrackSegNum);
            ProcessCCLineSector(seg);
            try {
                for (int i = 0; i < length; i++) {
                    seg.wCCLineRAngle = wTmpAngleZ - seg.wAngleZ;
                    seg.wCCLine = wSegPosX;
                    seg.m_nCCLineSector = nCCLineSector;
                    nTrackSegNum++;
                    if (nTrackSegNum >= m_TrackSegments.nSegNumber) nTrackSegNum = 1;
                    seg = m_TrackSegments.getSegAt(nTrackSegNum);
                    ProcessCCLineSegment(seg);
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    /**
        ProcessCCLineSector
    */
    private int tmpX = 0, tmpY = 0;

    private int tmpCos = 0, tmpSin = 0;

    private int tmp1 = 0, tmp2 = 0, tmp5 = 0, tmp6 = 0;

    private void ProcessCCLineSector(Seg pSeg) {
        short wSegPosY = (short) ((wSegPosX * pSeg.wAngleZChangeMulHalfPI) >> 15);
        tmpSin = F1GPMath.LookupSin(pSeg.wAngleZ);
        tmpCos = F1GPMath.LookupCos(pSeg.wAngleZ);
        tmp5 = ((wSegPosY * tmpCos) - (wSegPosX * tmpSin)) >> 14;
        tmp6 = ((wSegPosX * tmpCos) + (wSegPosY * tmpSin)) >> 14;
        tmp5 += pSeg.getPosY();
        tmp6 += pSeg.getPosX();
        tmp5 += (F1GPMath.LookupCos((short) wTmpAngleZ) * shiftx) >> 14;
        tmpY = tmp5;
        tmp6 += (F1GPMath.LookupSin((short) wTmpAngleZ) * shiftx) >> 14;
        tmpX = tmp6;
        if (radius != 0) {
            int nAngle;
            if (radius >= 0) nAngle = wTmpAngleZ + 0x4000; else nAngle = wTmpAngleZ - 0x4000;
            sinAndCosBig(nAngle);
            long ll = (long) tmpSin * (long) Math.abs(radius);
            tmpX += (ll >> 30);
            ll = (long) tmpCos * (long) Math.abs(radius);
            tmpY += (ll >> 30);
        }
    }

    /**
        ProcessCCLineSegment.
        Uses "Globals" tmpX, tmpY.
        Modifies "Globals" wTmpAngleZ, wSegPosX.
        Calculate position and angle of ccLine at next track Seg start.
    */
    void ProcessCCLineSegment(Seg seg) {
        int nXDiff = tmpX - seg.getPosX();
        int nYDiff = tmpY - seg.getPosY();
        int invPI = 0x517D;
        int a1 = seg.wAngleZ - (((seg.wAngleZChangeMulHalfPI >> 1) * invPI) >> 15);
        tmpCos = a1;
        double dAngleDeg = ((double) a1 / 0x10000) * 360.0;
        sinAndCosBig(tmpCos);
        tmp5 = (int) ((((long) tmpCos * (long) nXDiff) - ((long) tmpSin * (long) nYDiff)) >> 30);
        tmp6 = (int) ((((long) tmpCos * (long) nYDiff) + ((long) tmpSin * (long) nXDiff)) >> 30);
        if (radius == 0) {
            int tmp = wTmpAngleZ - a1;
            tmp1 = F1GPMath.LookupSinbig((short) tmp);
            tmp2 = F1GPMath.LookupCosbig((short) tmp);
            long lTemp = (long) tmp1 * (long) tmp6;
            if (tmp2 == 0) {
                tmp6 = (int) (lTemp & (long) 0x0000FFFF);
            } else {
                tmp6 = (int) (lTemp / ((long) tmp2));
            }
            wSegPosX = (short) (tmp5 - tmp6);
        } else {
            long ll = (((long) radius * (long) radius) - ((long) tmp6 * (long) tmp6));
            tmp1 = (int) F1GPMath.sqrt64(ll);
            if (radius < 0) tmp1 = -tmp1;
            wSegPosX = (short) (tmp5 - tmp1);
            int r = radius;
            if (r < 0) r = -r;
            for (; r >= 0x7F00; r >>= 1) {
                tmp1 >>= 1;
                tmp6 >>= 1;
            }
            int a2 = F1GPMath.LookupAtan2(tmp1, tmp6);
            if (radius < 0) a2 = a2 + 0x4000; else a2 = a2 - 0x4000;
            wTmpAngleZ = (short) (a1 + a2);
        }
    }

    private void getOppositeEdgeLength() {
        long tmp = 0x1000000000000000l - ((long) tmpCos * (long) tmpCos);
        if (tmp < 0) tmp = 0;
        tmpSin = (int) F1GPMath.sqrt64(tmp);
    }

    /**
        Calculate 32Bit Sin and Cos from tmpCos (16 bit angle value).
        If Cos is below 0.5, Sin is read from the lookup tables and Cos is
        calculated from it.
        Otherwise Cos is read from lookup table and Sin is calculated from it.
        Returns results in tmpSin and tmpCos (shifted by 30 bits).
    */
    private void sinAndCosBig(int nAngle) {
        short oldCos = (short) nAngle;
        short index = (short) ((-nAngle) + (short) 0x4000);
        if (index < 0) index = (short) -index;
        int i = (index >> 2) & 0x3FFE;
        short val = CosLookupTable.get(i / 2);
        if (val < 0) val = (short) -val;
        if (val < 0x2000) {
            tmpCos = F1GPMath.LookupSinbig((short) nAngle);
            if (oldCos < 0) oldCos = (short) -oldCos;
            int j = (oldCos >> 2) & 0x3FFE;
            oldCos = CosLookupTable.get(j / 2);
            getOppositeEdgeLength();
            if (oldCos < 0) tmpSin = -tmpSin;
            int temp = tmpSin;
            tmpSin = tmpCos;
            tmpCos = temp;
        } else {
            tmpCos = F1GPMath.LookupCosbig((short) nAngle);
            oldCos = (short) (-oldCos + 0x4000);
            if (oldCos < 0) oldCos = (short) -oldCos;
            int j = (oldCos >> 2) & 0x3FFE;
            oldCos = CosLookupTable.get(j / 2);
            getOppositeEdgeLength();
            if (oldCos < 0) tmpSin = -tmpSin;
        }
    }

    /**
        Returns rectangle that contains all track graphics
        based on F1GP in-game calculations (Seg objects).
    */
    public Rectangle getF1GPBoundingRectangle() {
        double dMinX, dMinY, dMaxX, dMaxY, dX, dY;
        TrackSegments trackSegments = getTrackSegments();
        Seg seg;
        seg = trackSegments.getSegAt(1);
        dMinX = seg.getPosX();
        dMaxX = dMinX;
        dMinY = seg.getPosY();
        dMaxY = dMinY;
        for (int i = 1; i <= trackSegments.getMaxTrackSegIndex(); i++) {
            seg = trackSegments.getSegAt(i);
            dX = seg.getPosX();
            if (dX > dMaxX) dMaxX = dX;
            if (dX < dMinX) dMinX = dX;
            dY = seg.getPosY();
            if (dY > dMaxY) dMaxY = dY;
            if (dY < dMinY) dMinY = dY;
        }
        Rectangle r = new Rectangle(new Double(dMinX).intValue(), new Double(dMinY).intValue(), new Double(dMaxX - dMinX).intValue(), new Double(dMaxY - dMinY).intValue());
        return r;
    }

    public Rectangle getBoundingRectangle() {
        double dMinX, dMinY, dMaxX, dMaxY, dX, dY;
        TrackSegments trackSegments = getTrackSegments();
        TrackSegment trackSegment;
        trackSegment = trackSegments.getAt(1);
        dMinX = trackSegment.getPosXStart();
        dMaxX = dMinX;
        dMinY = trackSegment.getPosYStart();
        dMaxY = dMinY;
        for (int i = 1; i <= trackSegments.size(); i++) {
            trackSegment = trackSegments.getAt(i);
            dX = trackSegment.getPosXStart();
            if (dX > dMaxX) dMaxX = dX;
            if (dX < dMinX) dMinX = dX;
            dY = trackSegment.getPosYStart();
            if (dY > dMaxY) dMaxY = dY;
            if (dY < dMinY) dMinY = dY;
            dX = trackSegment.getPosXEnd();
            if (dX > dMaxX) dMaxX = dX;
            if (dX < dMinX) dMinX = dX;
            dY = trackSegment.getPosYEnd();
            if (dY > dMaxY) dMaxY = dY;
            if (dY < dMinY) dMinY = dY;
        }
        Rectangle r = new Rectangle(new Double(dMinX).intValue(), new Double(dMinY).intValue(), new Double(dMaxX - dMinX).intValue(), new Double(dMaxY - dMinY).intValue());
        return r;
    }
}
