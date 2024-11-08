package jmp123.decoder;

public final class Layer3 implements ILayer123 {

    private static Header objHeader;

    private static Synthesis objFilter;

    private static BitStream objInBitStream;

    private static int intWhichChannel;

    private static int intMaxGr;

    private static int intChannels;

    private static int intFirstChannel;

    private static int intLastChannel;

    private static int[] intSfbIdxLong;

    private static int[] intSfbIdxShort;

    private static boolean boolIntensityStereo;

    public Layer3(BitStream bs, Header h, Synthesis filter, int wch) {
        objInBitStream = bs;
        objHeader = h;
        intChannels = objHeader.getChannels();
        intWhichChannel = wch;
        intMaxGr = (objHeader.getVersion() == Header.MPEG1) ? 2 : 1;
        objFilter = filter;
        objSI = new SideInfo();
        objHuffBits = new HuffmanBits(objInBitStream);
        objSideBS = new BitStream(36);
        scfL = new int[2][23];
        scfS = new int[2][3][13];
        is = new int[32 * 18 + 4];
        xr = new float[2][32][18];
        intWidthLong = new int[22];
        intWidthShort = new int[13];
        floatRawOut = new float[36];
        floatPrevBlck = new float[2][32][18];
        cs = new float[] { 0.857492925712f, 0.881741997318f, 0.949628649103f, 0.983314592492f, 0.995517816065f, 0.999160558175f, 0.999899195243f, 0.999993155067f };
        ca = new float[] { -0.5144957554270f, -0.4717319685650f, -0.3133774542040f, -0.1819131996110f, -0.0945741925262f, -0.0409655828852f, -0.0141985685725f, -0.00369997467375f };
        int i;
        floatPowIS = new float[8207];
        for (i = 0; i < 8207; i++) floatPowIS[i] = (float) Math.pow(i, 4.0 / 3.0);
        floatPow2 = new float[256 + 118 + 4];
        for (i = -256; i < 118 + 4; i++) floatPow2[i + 256] = (float) Math.pow(2.0, -0.25 * (i + 210));
        if (intChannels == 2) switch(intWhichChannel) {
            case Decoder.CH_LEFT:
                intFirstChannel = intLastChannel = 0;
                break;
            case Decoder.CH_RIGHT:
                intFirstChannel = intLastChannel = 1;
                break;
            case Decoder.CH_BOTH:
            default:
                intFirstChannel = 0;
                intLastChannel = 1;
                break;
        } else intFirstChannel = intLastChannel = 0;
        int intSfreq = objHeader.getSampleFrequency();
        intSfreq += (objHeader.getVersion() == Header.MPEG1) ? 0 : ((objHeader.getVersion() == Header.MPEG2) ? 3 : 6);
        switch(intSfreq) {
            case 0:
                intSfbIdxLong = new int[] { 0, 4, 8, 12, 16, 20, 24, 30, 36, 44, 52, 62, 74, 90, 110, 134, 162, 196, 238, 288, 342, 418, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 16, 22, 30, 40, 52, 66, 84, 106, 136, 192 };
                break;
            case 1:
                intSfbIdxLong = new int[] { 0, 4, 8, 12, 16, 20, 24, 30, 36, 42, 50, 60, 72, 88, 106, 128, 156, 190, 230, 276, 330, 384, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 16, 22, 28, 38, 50, 64, 80, 100, 126, 192 };
                break;
            case 2:
                intSfbIdxLong = new int[] { 0, 4, 8, 12, 16, 20, 24, 30, 36, 44, 54, 66, 82, 102, 126, 156, 194, 240, 296, 364, 448, 550, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 16, 22, 30, 42, 58, 78, 104, 138, 180, 192 };
                break;
            case 3:
                intSfbIdxLong = new int[] { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 18, 24, 32, 42, 56, 74, 100, 132, 174, 192 };
                break;
            case 4:
                intSfbIdxLong = new int[] { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 114, 136, 162, 194, 232, 278, 330, 394, 464, 540, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 136, 180, 192 };
                break;
            case 5:
                intSfbIdxLong = new int[] { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
                break;
            case 6:
                intSfbIdxLong = new int[] { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
                break;
            case 7:
                intSfbIdxLong = new int[] { 0, 6, 12, 18, 24, 30, 36, 44, 54, 66, 80, 96, 116, 140, 168, 200, 238, 284, 336, 396, 464, 522, 576 };
                intSfbIdxShort = new int[] { 0, 4, 8, 12, 18, 26, 36, 48, 62, 80, 104, 134, 174, 192 };
                break;
            case 8:
                intSfbIdxLong = new int[] { 0, 12, 24, 36, 48, 60, 72, 88, 108, 132, 160, 192, 232, 280, 336, 400, 476, 566, 568, 570, 572, 574, 576 };
                intSfbIdxShort = new int[] { 0, 8, 16, 24, 36, 52, 72, 96, 124, 160, 162, 164, 166, 192 };
                break;
        }
        for (i = 0; i < 22; i++) intWidthLong[i] = intSfbIdxLong[i + 1] - intSfbIdxLong[i];
        for (i = 0; i < 13; i++) intWidthShort[i] = intSfbIdxShort[i + 1] - intSfbIdxShort[i];
        boolIntensityStereo = objHeader.isIStereo();
        if (boolIntensityStereo) {
            if (objHeader.getVersion() == Header.MPEG1) is_coef = new float[] { 0.0f, 0.211324865f, 0.366025404f, 0.5f, 0.633974596f, 0.788675135f, 1.0f }; else lsf_is_coef = new float[][] { { 0.840896415f, 0.707106781f, 0.594603558f, 0.5f, 0.420448208f, 0.353553391f, 0.297301779f, 0.25f, 0.210224104f, 0.176776695f, 0.148650889f, 0.125f, 0.105112052f, 0.088388348f, 0.074325445f }, { 0.707106781f, 0.5f, 0.353553391f, 0.25f, 0.176776695f, 0.125f, 0.088388348f, 0.0625f, 0.044194174f, 0.03125f, 0.022097087f, 0.015625f, 0.011048543f, 0.0078125f, 0.005524272f } };
        }
        if (objHeader.getVersion() != Header.MPEG1) {
            i_slen2 = new int[256];
            n_slen2 = new int[512];
            slen_tab2 = new byte[][][] { { { 6, 5, 5, 5 }, { 6, 5, 7, 3 }, { 11, 10, 0, 0 }, { 7, 7, 7, 0 }, { 6, 6, 6, 3 }, { 8, 8, 5, 0 } }, { { 9, 9, 9, 9 }, { 9, 9, 12, 6 }, { 18, 18, 0, 0 }, { 12, 12, 12, 0 }, { 12, 9, 9, 6 }, { 15, 12, 9, 0 } }, { { 6, 9, 9, 9 }, { 6, 9, 12, 6 }, { 15, 18, 0, 0 }, { 6, 15, 12, 0 }, { 6, 12, 9, 6 }, { 6, 18, 9, 0 } } };
            int j, k, l, n;
            for (i = 0; i < 5; i++) for (j = 0; j < 6; j++) for (k = 0; k < 6; k++) {
                n = k + j * 6 + i * 36;
                i_slen2[n] = i | (j << 3) | (k << 6) | (3 << 12);
            }
            for (i = 0; i < 4; i++) for (j = 0; j < 4; j++) for (k = 0; k < 4; k++) {
                n = k + j * 4 + i * 16;
                i_slen2[n + 180] = i | (j << 3) | (k << 6) | (4 << 12);
            }
            for (i = 0; i < 4; i++) for (j = 0; j < 3; j++) {
                n = j + i * 3;
                i_slen2[n + 244] = i | (j << 3) | (5 << 12);
                n_slen2[n + 500] = i | (j << 3) | (2 << 12) | (1 << 15);
            }
            for (i = 0; i < 5; i++) for (j = 0; j < 5; j++) for (k = 0; k < 4; k++) for (l = 0; l < 4; l++) {
                n = l + k * 4 + j * 16 + i * 80;
                n_slen2[n] = i | (j << 3) | (k << 6) | (l << 9);
            }
            for (i = 0; i < 5; i++) for (j = 0; j < 5; j++) for (k = 0; k < 4; k++) {
                n = k + j * 4 + i * 20;
                n_slen2[n + 400] = i | (j << 3) | (k << 6) | (1 << 12);
            }
        }
    }

    private static SideInfo objSI;

    private BitStream objSideBS;

    private boolean getSideInfo() throws Exception {
        int ch, gr;
        objSideBS.resetIndex();
        objSideBS.append(objHeader.getSideInfoSize());
        if (objHeader.getVersion() == Header.MPEG1) {
            objSI.main_data_begin = objSideBS.getBits9(9);
            if (intChannels == 1) objSideBS.getBits9(5); else objSideBS.getBits9(3);
            for (ch = 0; ch < intChannels; ch++) {
                int[] scfsi = objSI.ch[ch].scfsi;
                scfsi[0] = objSideBS.get1Bit();
                scfsi[1] = objSideBS.get1Bit();
                scfsi[2] = objSideBS.get1Bit();
                scfsi[3] = objSideBS.get1Bit();
            }
            for (gr = 0; gr < 2; gr++) {
                for (ch = 0; ch < intChannels; ch++) {
                    Layer3.GRInfo s = objSI.ch[ch].gr[gr];
                    s.part2_3_length = objSideBS.getBits17(12);
                    s.big_values = objSideBS.getBits9(9);
                    s.global_gain = objSideBS.getBits9(8);
                    s.scalefac_compress = objSideBS.getBits9(4);
                    s.window_switching_flag = objSideBS.get1Bit();
                    if ((s.window_switching_flag) != 0) {
                        s.block_type = objSideBS.getBits9(2);
                        s.mixed_block_flag = objSideBS.get1Bit();
                        s.table_select[0] = objSideBS.getBits9(5);
                        s.table_select[1] = objSideBS.getBits9(5);
                        s.subblock_gain[0] = objSideBS.getBits9(3);
                        s.subblock_gain[1] = objSideBS.getBits9(3);
                        s.subblock_gain[2] = objSideBS.getBits9(3);
                        if (s.block_type == 0) return false; else if (s.block_type == 2 && s.mixed_block_flag == 0) s.region0_count = 8; else s.region0_count = 7;
                        s.region1_count = 20 - s.region0_count;
                    } else {
                        s.table_select[0] = objSideBS.getBits9(5);
                        s.table_select[1] = objSideBS.getBits9(5);
                        s.table_select[2] = objSideBS.getBits9(5);
                        s.region0_count = objSideBS.getBits9(4);
                        s.region1_count = objSideBS.getBits9(3);
                        s.block_type = 0;
                    }
                    s.preflag = objSideBS.get1Bit();
                    s.scalefac_scale = objSideBS.get1Bit();
                    s.count1table_select = objSideBS.get1Bit();
                }
            }
        } else {
            objSI.main_data_begin = objSideBS.getBits9(8);
            if (intChannels == 1) objSideBS.get1Bit(); else objSideBS.getBits9(2);
            for (ch = 0; ch < intChannels; ch++) {
                Layer3.GRInfo s = objSI.ch[ch].gr[0];
                s.part2_3_length = objSideBS.getBits17(12);
                s.big_values = objSideBS.getBits9(9);
                s.global_gain = objSideBS.getBits9(8);
                s.scalefac_compress = objSideBS.getBits9(9);
                s.window_switching_flag = objSideBS.get1Bit();
                if ((s.window_switching_flag) != 0) {
                    s.block_type = objSideBS.getBits9(2);
                    s.mixed_block_flag = objSideBS.get1Bit();
                    s.table_select[0] = objSideBS.getBits9(5);
                    s.table_select[1] = objSideBS.getBits9(5);
                    s.subblock_gain[0] = objSideBS.getBits9(3);
                    s.subblock_gain[1] = objSideBS.getBits9(3);
                    s.subblock_gain[2] = objSideBS.getBits9(3);
                    if (s.block_type == 0) return false; else if (s.block_type == 2 && s.mixed_block_flag == 0) s.region0_count = 8; else {
                        s.region0_count = 7;
                        s.region1_count = 20 - s.region0_count;
                    }
                } else {
                    s.table_select[0] = objSideBS.getBits9(5);
                    s.table_select[1] = objSideBS.getBits9(5);
                    s.table_select[2] = objSideBS.getBits9(5);
                    s.region0_count = objSideBS.getBits9(4);
                    s.region1_count = objSideBS.getBits9(3);
                    s.block_type = 0;
                    s.mixed_block_flag = 0;
                }
                s.scalefac_scale = objSideBS.get1Bit();
                s.count1table_select = objSideBS.get1Bit();
            }
        }
        return true;
    }

    private static int[][] scfL;

    private static int[][][] scfS;

    private static int[] i_slen2;

    private static int[] n_slen2;

    private static byte[][][] slen_tab2;

    private void getScaleFactors_2(final int ch, final int gr) {
        byte[] pnt;
        int i, j, k, slen, n = 0, scf = 0;
        int l[] = null, s[][] = null;
        boolean i_stereo = objHeader.isIStereo();
        GRInfo gr_info = (objSI.ch[ch].gr[gr]);
        rzero_bandL = 0;
        l = scfL[ch];
        s = scfS[ch];
        if ((ch > 0) && i_stereo) slen = i_slen2[gr_info.scalefac_compress >> 1]; else slen = n_slen2[gr_info.scalefac_compress];
        gr_info.preflag = (slen >> 15) & 0x1;
        gr_info.part2_bits = 0;
        if (gr_info.block_type == 2) {
            n++;
            if ((gr_info.mixed_block_flag) != 0) n++;
            pnt = slen_tab2[n][(slen >> 12) & 0x7];
            for (i = 0; i < 4; i++) {
                int num = slen & 0x7;
                slen >>= 3;
                if (num != 0) {
                    for (j = 0; j < pnt[i]; j += 3) {
                        for (k = 0; k < 3; k++) s[k][scf] = objInBitStream.getBits17(num);
                        scf++;
                    }
                    gr_info.part2_bits += pnt[i] * num;
                } else {
                    for (j = 0; j < pnt[i]; j += 3) {
                        for (k = 0; k < 3; k++) s[k][scf] = 0;
                        scf++;
                    }
                }
            }
            n = (n << 1) + 1;
            for (i = 0; i < n; i += 3) {
                for (k = 0; k < 3; k++) s[k][scf] = 0;
                scf++;
            }
        } else {
            pnt = slen_tab2[n][(slen >> 12) & 0x7];
            for (i = 0; i < 4; i++) {
                int num = slen & 0x7;
                slen >>= 3;
                if (num != 0) {
                    for (j = 0; j < pnt[i]; j++) l[scf++] = objInBitStream.getBits17(num);
                    gr_info.part2_bits += pnt[i] * num;
                } else {
                    for (j = 0; j < pnt[i]; j++) l[scf++] = 0;
                }
            }
            n = (n << 1) + 1;
            for (i = 0; i < n; i++) l[scf++] = 0;
        }
    }

    private static final int slen0[] = { 0, 0, 0, 0, 3, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 };

    private static final int slen1[] = { 0, 1, 2, 3, 0, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 3 };

    private void getScaleFactors_1(final int ch, final int gr) {
        GRInfo gr_info = objSI.ch[ch].gr[gr];
        int scale_comp = gr_info.scalefac_compress;
        int length0 = slen0[scale_comp];
        int length1 = slen1[scale_comp];
        int sfb, window;
        int l[] = null;
        int s[][] = null;
        l = scfL[ch];
        s = scfS[ch];
        gr_info.part2_bits = 0;
        if (gr_info.window_switching_flag != 0 && gr_info.block_type == 2) {
            if (gr_info.mixed_block_flag != 0) {
                gr_info.part2_bits = 17 * length0 + 18 * length1;
                for (sfb = 0; sfb < 8; sfb++) l[sfb] = objInBitStream.getBits9(length0);
                for (sfb = 3; sfb < 6; sfb++) for (window = 0; window < 3; window++) s[window][sfb] = objInBitStream.getBits9(length0);
                for (sfb = 6; sfb < 12; sfb++) for (window = 0; window < 3; window++) s[window][sfb] = objInBitStream.getBits9(length1);
            } else {
                gr_info.part2_bits = 18 * (length0 + length1);
                for (sfb = 0; sfb < 6; sfb++) for (window = 0; window < 3; window++) s[window][sfb] = objInBitStream.getBits9(length0);
                for (sfb = 6; sfb < 12; sfb++) for (window = 0; window < 3; window++) s[window][sfb] = objInBitStream.getBits9(length1);
            }
        } else {
            int si_t[] = objSI.ch[ch].scfsi;
            if (gr == 0) {
                gr_info.part2_bits = 10 * (length0 + length1) + length0;
                for (sfb = 0; sfb < 11; sfb++) l[sfb] = objInBitStream.getBits9(length0);
                for (sfb = 11; sfb < 21; sfb++) l[sfb] = objInBitStream.getBits9(length1);
            } else {
                gr_info.part2_bits = 0;
                if (si_t[0] == 0) {
                    for (sfb = 0; sfb < 6; sfb++) l[sfb] = objInBitStream.getBits9(length0);
                    gr_info.part2_bits += 6 * length0;
                }
                if (si_t[1] == 0) {
                    for (sfb = 6; sfb < 11; sfb++) l[sfb] = objInBitStream.getBits9(length0);
                    gr_info.part2_bits += 5 * length0;
                }
                if (si_t[2] == 0) {
                    for (sfb = 11; sfb < 16; sfb++) l[sfb] = objInBitStream.getBits9(length1);
                    gr_info.part2_bits += 5 * length1;
                }
                if (si_t[3] == 0) {
                    for (sfb = 16; sfb < 21; sfb++) l[sfb] = objInBitStream.getBits9(length1);
                    gr_info.part2_bits += 5 * length1;
                }
            }
        }
    }

    private static int[] rzero_index = new int[2];

    private static int[] is;

    private static HuffmanBits objHuffBits;

    private void huffmanDecoder(final int ch, final int gr) {
        GRInfo s = objSI.ch[ch].gr[gr];
        int r1, r2;
        if (s.window_switching_flag != 0) {
            int v = objHeader.getVersion();
            if (v == Header.MPEG1 || (v == Header.MPEG2 && s.block_type == 2)) {
                s.region1Start = 36;
                s.region2Start = 576;
            } else {
                if (v == Header.MPEG25) {
                    if (s.block_type == 2 && s.mixed_block_flag == 0) s.region1Start = intSfbIdxLong[6]; else s.region1Start = intSfbIdxLong[8];
                    s.region2Start = 576;
                } else {
                    s.region1Start = 54;
                    s.region2Start = 576;
                }
            }
        } else {
            r1 = s.region0_count + 1;
            r2 = r1 + s.region1_count + 1;
            if (r2 > intSfbIdxLong.length - 1) {
                r2 = intSfbIdxLong.length - 1;
            }
            s.region1Start = intSfbIdxLong[r1];
            s.region2Start = intSfbIdxLong[r2];
        }
        rzero_index[ch] = objHuffBits.decode(ch, s, is);
    }

    private static float[][][] xr;

    private static float[] floatPow2;

    private static float[] floatPowIS;

    private static int[] intWidthLong;

    private static int[] intWidthShort;

    private static int rzero_bandL;

    private static int[] rzero_bandS = new int[3];

    private final int[] pretab = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 3, 2, 0 };

    private void requantizer(int ch, int gr, float[][] xr) {
        int bi, wi, band, width, pre, v, is_idx = 0, sb = 0, ss = 0;
        float requ_v;
        int sb_start, ss_start;
        final GRInfo gr_info = objSI.ch[ch].gr[gr];
        final int preflag = gr_info.preflag;
        final int shift = 1 + gr_info.scalefac_scale;
        final int l[] = scfL[ch];
        final int s[][] = scfS[ch];
        final int[] sub_gain = gr_info.subblock_gain;
        sub_gain[0] <<= 3;
        sub_gain[1] <<= 3;
        sub_gain[2] <<= 3;
        int pow2_idx = 256 - gr_info.global_gain;
        if (objHeader.isMSStereo()) pow2_idx += 2;
        if ((gr_info.window_switching_flag == 1) && (gr_info.block_type == 2)) {
            if (gr_info.mixed_block_flag == 1) {
                rzero_bandS[0] = rzero_bandS[1] = rzero_bandS[2] = 2;
                rzero_bandL = -1;
                for (band = 0; band < 8; band++) {
                    pre = (preflag == 0) ? 0 : pretab[band];
                    requ_v = floatPow2[pow2_idx + ((l[band] + pre) << shift)];
                    width = intWidthLong[band];
                    for (bi = 0; bi < width; bi++) {
                        v = is[is_idx];
                        if (v < 0) {
                            xr[sb][ss] = -requ_v * floatPowIS[-v];
                            rzero_bandL = band;
                        } else if (v > 0) {
                            xr[sb][ss] = requ_v * floatPowIS[v];
                            rzero_bandL = band;
                        } else xr[sb][ss] = 0;
                        is_idx++;
                        if (++ss == 18) {
                            ss = 0;
                            sb++;
                        }
                    }
                }
                sb_start = 2;
                ss_start = 0;
                for (band = 3; band < 12; band++) {
                    width = intWidthShort[band];
                    for (wi = 0; wi < 3; wi++) {
                        requ_v = floatPow2[pow2_idx + sub_gain[wi] + (s[wi][band] << shift)];
                        sb = sb_start;
                        ss = ss_start + wi;
                        if (ss >= 18) {
                            ss -= 18;
                            sb++;
                        }
                        for (bi = 0; bi < width; bi++) {
                            v = is[is_idx];
                            if (v < 0) {
                                xr[sb][ss] = -requ_v * floatPowIS[-v];
                                rzero_bandS[wi] = band;
                            } else if (v > 0) {
                                xr[sb][ss] = requ_v * floatPowIS[v];
                                rzero_bandS[wi] = band;
                            } else xr[sb][ss] = 0;
                            is_idx++;
                            ss += 3;
                            if (ss >= 18) {
                                ss -= 18;
                                sb++;
                            }
                        }
                    }
                    ss -= 2;
                    if (ss < 0) {
                        ss = 0;
                        sb--;
                    }
                    sb_start = sb;
                    ss_start = ss;
                }
            } else {
                rzero_bandS[0] = rzero_bandS[1] = rzero_bandS[2] = rzero_bandL = -1;
                sb_start = ss_start = 0;
                for (band = 0; band < 12; band++) {
                    width = intWidthShort[band];
                    for (wi = 0; wi < 3; wi++) {
                        requ_v = floatPow2[pow2_idx + sub_gain[wi] + (s[wi][band] << shift)];
                        sb = sb_start;
                        ss = ss_start + wi;
                        if (ss >= 18) {
                            ss -= 18;
                            sb++;
                        }
                        for (bi = 0; bi < width; bi++) {
                            v = is[is_idx];
                            if (v < 0) {
                                xr[sb][ss] = -requ_v * floatPowIS[-v];
                                rzero_bandS[wi] = band;
                            } else if (v > 0) {
                                xr[sb][ss] = requ_v * floatPowIS[v];
                                rzero_bandS[wi] = band;
                            } else xr[sb][ss] = 0;
                            is_idx++;
                            ss += 3;
                            if (ss >= 18) {
                                ss -= 18;
                                sb++;
                            }
                        }
                    }
                    ss -= 2;
                    if (ss < 0) {
                        ss = 0;
                        sb--;
                    }
                    sb_start = sb;
                    ss_start = ss;
                }
            }
            rzero_bandS[0]++;
            rzero_bandS[1]++;
            rzero_bandS[2]++;
            rzero_bandL++;
        } else {
            rzero_bandL = -1;
            for (band = 0; band < 21; band++) {
                pre = (preflag == 0) ? 0 : pretab[band];
                requ_v = floatPow2[pow2_idx + ((l[band] + pre) << shift)];
                width = intWidthLong[band];
                for (bi = 0; bi < width; bi++) {
                    v = is[is_idx];
                    if (v < 0) {
                        xr[sb][ss] = -requ_v * floatPowIS[-v];
                        rzero_bandL = band;
                    } else if (v > 0) {
                        xr[sb][ss] = requ_v * floatPowIS[v];
                        rzero_bandL = band;
                    } else xr[sb][ss] = 0;
                    is_idx++;
                    if (++ss == 18) {
                        ss = 0;
                        sb++;
                    }
                }
            }
            rzero_bandL++;
        }
        for (; sb < 32; sb++) {
            for (; ss < 18; ss++) xr[sb][ss] = 0;
            ss = 0;
        }
    }

    private void ms_stereo() {
        int sb, ss;
        float tmp0, tmp1;
        int rzero_xr = (rzero_index[0] > rzero_index[1]) ? rzero_index[0] : rzero_index[1];
        int rzero_sb = (rzero_xr + 17) / 18;
        for (sb = 0; sb < rzero_sb; sb++) for (ss = 0; ss < 18; ss++) {
            tmp0 = xr[0][sb][ss];
            tmp1 = xr[1][sb][ss];
            xr[0][sb][ss] = tmp0 + tmp1;
            xr[1][sb][ss] = tmp0 - tmp1;
        }
        rzero_index[0] = rzero_index[1] = rzero_xr;
    }

    private static float[][] lsf_is_coef;

    private static float[] is_coef;

    private void is_lines_1(int is_pos, int idx0, int max_width, int idx_step) {
        float xr0;
        int sb32 = idx0 / 18;
        int ss18 = idx0 % 18;
        for (int w = max_width; w > 0; w--) {
            xr0 = xr[0][sb32][ss18];
            xr[0][sb32][ss18] = xr0 * is_coef[is_pos];
            xr[1][sb32][ss18] = xr0 * is_coef[6 - is_pos];
            ss18 += idx_step;
            if (ss18 >= 18) {
                ss18 -= 18;
                sb32++;
            }
        }
    }

    private void is_lines_2(int tab2, int is_pos, int idx0, int max_width, int idx_step) {
        float xr0;
        int sb32 = idx0 / 18;
        int ss18 = idx0 % 18;
        for (int w = max_width; w > 0; w--) {
            xr0 = xr[0][sb32][ss18];
            if (is_pos == 0) xr[1][sb32][ss18] = xr0; else {
                if ((is_pos & 1) == 0) xr[1][sb32][ss18] = xr0 * lsf_is_coef[tab2][(is_pos - 1) >> 1]; else {
                    xr[0][sb32][ss18] = xr0 * lsf_is_coef[tab2][(is_pos - 1) >> 1];
                    xr[1][sb32][ss18] = xr0;
                }
            }
            ss18 += idx_step;
            if (ss18 >= 18) {
                ss18 -= 18;
                sb32++;
            }
        }
    }

    private void i_stereo(final int gr) {
        if (objSI.ch[0].gr[gr].mixed_block_flag != objSI.ch[1].gr[gr].mixed_block_flag || objSI.ch[0].gr[gr].block_type != objSI.ch[1].gr[gr].block_type) return;
        GRInfo gr_info = objSI.ch[1].gr[gr];
        int is_p, idx, sfb;
        if (objHeader.getVersion() == Header.MPEG1) {
            if (gr_info.block_type == 2) {
                int w3;
                for (w3 = 0; w3 < 3; w3++) {
                    sfb = rzero_bandS[w3];
                    for (; sfb < 12; sfb++) {
                        idx = 3 * intSfbIdxShort[sfb] + w3;
                        is_p = scfS[1][w3][sfb];
                        if (is_p >= 7) continue;
                        is_lines_1(is_p, idx, intWidthShort[sfb], 3);
                    }
                }
            } else {
                for (sfb = rzero_bandL; sfb <= 21; sfb++) {
                    is_p = scfL[1][sfb];
                    if (is_p < 7) is_lines_1(is_p, intSfbIdxLong[sfb], intWidthLong[sfb], 1);
                }
            }
        } else {
            final int tab2 = gr_info.scalefac_compress & 0x1;
            if (gr_info.block_type == 2) {
                int w3;
                for (w3 = 0; w3 < 3; w3++) {
                    sfb = rzero_bandS[w3];
                    for (; sfb < 12; sfb++) {
                        idx = 3 * intSfbIdxShort[sfb] + w3;
                        is_p = scfS[1][w3][sfb];
                        is_lines_2(tab2, scfS[1][w3][sfb], idx, intWidthShort[sfb], 3);
                    }
                }
            } else {
                for (sfb = rzero_bandL; sfb <= 21; sfb++) is_lines_2(tab2, scfL[1][sfb], intSfbIdxLong[sfb], intWidthLong[sfb], 1);
            }
        }
    }

    private static float[] ca, cs;

    private void antialias(final int ch, final int gr) {
        GRInfo gr_info = (objSI.ch[ch].gr[gr]);
        int sb, ss, sblim = 0;
        float bu, bd;
        if (gr_info.block_type == 2) {
            if (gr_info.mixed_block_flag == 0) return;
            sblim = 1;
        } else sblim = (rzero_index[ch] - 1) / 18;
        for (sb = 0; sb < sblim; sb++) for (ss = 0; ss < 8; ss++) {
            bu = xr[ch][sb][17 - ss];
            bd = xr[ch][sb + 1][ss];
            xr[ch][sb][17 - ss] = bu * cs[ss] - bd * ca[ss];
            xr[ch][sb + 1][ss] = bd * cs[ss] + bu * ca[ss];
        }
    }

    private static final float[][] floatWinIMDCT = { { 0.0322824f, 0.1072064f, 0.2014143f, 0.3256164f, 0.5f, 0.7677747f, 1.2412229f, 2.3319514f, 7.7441506f, -8.4512568f, -3.0390580f, -1.9483297f, -1.4748814f, -1.2071068f, -1.0327232f, -0.9085211f, -0.8143131f, -0.7393892f, -0.6775254f, -0.6248445f, -0.5787917f, -0.5376016f, -0.5f, -0.4650284f, -0.4319343f, -0.4000996f, -0.3689899f, -0.3381170f, -0.3070072f, -0.2751725f, -0.2420785f, -0.2071068f, -0.1695052f, -0.1283151f, -0.0822624f, -0.0295815f }, { 0.0322824f, 0.1072064f, 0.2014143f, 0.3256164f, 0.5f, 0.7677747f, 1.2412229f, 2.3319514f, 7.7441506f, -8.4512568f, -3.0390580f, -1.9483297f, -1.4748814f, -1.2071068f, -1.0327232f, -0.9085211f, -0.8143131f, -0.7393892f, -0.6781709f, -0.6302362f, -0.5928445f, -0.5636910f, -0.5411961f, -0.5242646f, -0.5077583f, -0.4659258f, -0.3970546f, -0.3046707f, -0.1929928f, -0.0668476f, -0.0f, -0.0f, -0.0f, -0.0f, -0.0f, -0.0f }, {}, { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.3015303f, 1.4659259f, 6.9781060f, -9.0940447f, -3.5390582f, -2.2903500f, -1.6627548f, -1.3065630f, -1.0828403f, -0.9305795f, -0.8213398f, -0.7400936f, -0.6775254f, -0.6248445f, -0.5787917f, -0.5376016f, -0.5f, -0.4650284f, -0.4319343f, -0.4000996f, -0.3689899f, -0.3381170f, -0.3070072f, -0.2751725f, -0.2420785f, -0.2071068f, -0.1695052f, -0.1283151f, -0.0822624f, -0.0295815f } };

    private void imdct(final float[] in, final float[] out, final int block_type) {
        int i, idx6 = 0;
        float in0, in1, in2, in3, in4, in5, in6, in7, in8, in9, in10, in11, in12, in13, in14, in15, in16, in17;
        float out0, out1, out2, out3, out4, out5, out6, out7, out8, out9, out10, out11, out12, out13, out14, out15, out16, out17, tmp;
        if (block_type == 2) {
            out[0] = out[1] = out[2] = out[3] = out[4] = out[5] = out[6] = out[7] = out[8] = out[9] = out[10] = out[11] = out[12] = out[13] = out[14] = out[15] = out[16] = out[17] = out[18] = out[19] = out[20] = out[21] = out[22] = out[23] = out[24] = out[25] = out[26] = out[27] = out[28] = out[29] = out[30] = out[31] = out[32] = out[33] = out[34] = out[35] = 0.0f;
            for (i = 0; i < 3; i++) {
                in[15 + i] += (in[12 + i] += in[9 + i]) + in[6 + i];
                in[9 + i] += (in[6 + i] += in[3 + i]) + in[i];
                in[3 + i] += in[i];
                out1 = (in1 = in[i]) - (in2 = in[12 + i]);
                in3 = in1 + in2 * 0.5f;
                in4 = in[6 + i] * 0.8660254f;
                out0 = in3 + in4;
                out2 = in3 - in4;
                out4 = ((in1 = in[3 + i]) - (in2 = in[15 + i])) * 0.7071068f;
                in3 = in1 + in2 * 0.5f;
                in4 = in[9 + i] * 0.8660254f;
                out5 = (in3 + in4) * 0.5176381f;
                out3 = (in3 - in4) * 1.9318516f;
                tmp = out0;
                out0 += out5;
                out5 = tmp - out5;
                tmp = out1;
                out1 += out4;
                out4 = tmp - out4;
                tmp = out2;
                out2 += out3;
                out3 = tmp - out3;
                in1 = out3 * 0.1072064f;
                out[6 + idx6] += in1;
                out[7 + idx6] += out4 * 0.5f;
                out[8 + idx6] += out5 * 2.3319512f;
                out[9 + idx6] -= out5 * 3.0390580f;
                out[10 + idx6] -= out4 * 1.2071068f;
                out[11 + idx6] -= in1 * 7.5957541f;
                out[12 + idx6] -= out2 * 0.6248445f;
                out[13 + idx6] -= out1 * 0.5f;
                out[14 + idx6] -= out0 * 0.4000996f;
                out[15 + idx6] -= out0 * 0.3070072f;
                out[16 + idx6] -= out1 * 0.2071068f;
                out[17 + idx6] -= out2 * 0.0822623f;
                idx6 += 6;
            }
        } else {
            in[17] += (in[16] += in[15]) + in[14];
            in[15] += (in[14] += in[13]) + in[12];
            in[13] += (in[12] += in[11]) + in[10];
            in[11] += (in[10] += in[9]) + in[8];
            in[9] += (in[8] += in[7]) + in[6];
            in[7] += (in[6] += in[5]) + in[4];
            in[5] += (in[4] += in[3]) + in[2];
            in[3] += (in[2] += in[1]) + in[0];
            in[1] += in[0];
            in0 = in[0] + in[12] * 0.5f;
            in1 = in[0] - in[12];
            in2 = in[8] + in[16] - in[4];
            out4 = in1 + in2;
            in3 = in1 - in2 * 0.5f;
            in4 = (in[10] + in[14] - in[2]) * 0.8660254f;
            out1 = in3 - in4;
            out7 = in3 + in4;
            in5 = (in[4] + in[8]) * 0.9396926f;
            in6 = (in[16] - in[8]) * 0.1736482f;
            in7 = -(in[4] + in[16]) * 0.7660444f;
            in17 = in0 - in5 - in7;
            in8 = in5 + in0 + in6;
            in9 = in0 + in7 - in6;
            in12 = in[6] * 0.8660254f;
            in10 = (in[2] + in[10]) * 0.9848078f;
            in11 = (in[14] - in[10]) * 0.3420201f;
            in13 = in10 + in11 + in12;
            out0 = in8 + in13;
            out8 = in8 - in13;
            in14 = -(in[2] + in[14]) * 0.6427876f;
            in15 = in10 + in14 - in12;
            in16 = in11 - in14 - in12;
            out3 = in9 + in15;
            out5 = in9 - in15;
            out2 = in17 + in16;
            out6 = in17 - in16;
            in0 = in[1] + in[13] * 0.5f;
            in1 = in[1] - in[13];
            in2 = in[9] + in[17] - in[5];
            out13 = (in1 + in2) * 0.7071068f;
            in3 = in1 - in2 * 0.5f;
            in4 = (in[11] + in[15] - in[3]) * 0.8660254f;
            out16 = (in3 - in4) * 0.5176381f;
            out10 = (in3 + in4) * 1.9318517f;
            in5 = (in[5] + in[9]) * 0.9396926f;
            in6 = (in[17] - in[9]) * 0.1736482f;
            in7 = -(in[5] + in[17]) * 0.7660444f;
            in17 = in0 - in5 - in7;
            in8 = in5 + in0 + in6;
            in9 = in0 + in7 - in6;
            in12 = in[7] * 0.8660254f;
            in10 = (in[3] + in[11]) * 0.9848078f;
            in11 = (in[15] - in[11]) * 0.3420201f;
            in13 = in10 + in11 + in12;
            out17 = (in8 + in13) * 0.5019099f;
            out9 = (in8 - in13) * 5.7368566f;
            in14 = -(in[3] + in[15]) * 0.6427876f;
            in15 = in10 + in14 - in12;
            in16 = in11 - in14 - in12;
            out14 = (in9 + in15) * 0.6103873f;
            out12 = (in9 - in15) * 0.8717234f;
            out15 = (in17 + in16) * 0.5516890f;
            out11 = (in17 - in16) * 1.1831008f;
            tmp = out0;
            out0 += out17;
            out17 = tmp - out17;
            tmp = out1;
            out1 += out16;
            out16 = tmp - out16;
            tmp = out2;
            out2 += out15;
            out15 = tmp - out15;
            tmp = out3;
            out3 += out14;
            out14 = tmp - out14;
            tmp = out4;
            out4 += out13;
            out13 = tmp - out13;
            tmp = out5;
            out5 += out12;
            out12 = tmp - out12;
            tmp = out6;
            out6 += out11;
            out11 = tmp - out11;
            tmp = out7;
            out7 += out10;
            out10 = tmp - out10;
            tmp = out8;
            out8 += out9;
            out9 = tmp - out9;
            final float[] winp = floatWinIMDCT[block_type];
            out[0] = out9 * winp[0];
            out[1] = out10 * winp[1];
            out[2] = out11 * winp[2];
            out[3] = out12 * winp[3];
            out[4] = out13 * winp[4];
            out[5] = out14 * winp[5];
            out[6] = out15 * winp[6];
            out[7] = out16 * winp[7];
            out[8] = out17 * winp[8];
            out[9] = out17 * winp[9];
            out[10] = out16 * winp[10];
            out[11] = out15 * winp[11];
            out[12] = out14 * winp[12];
            out[13] = out13 * winp[13];
            out[14] = out12 * winp[14];
            out[15] = out11 * winp[15];
            out[16] = out10 * winp[16];
            out[17] = out9 * winp[17];
            out[18] = out8 * winp[18];
            out[19] = out7 * winp[19];
            out[20] = out6 * winp[20];
            out[21] = out5 * winp[21];
            out[22] = out4 * winp[22];
            out[23] = out3 * winp[23];
            out[24] = out2 * winp[24];
            out[25] = out1 * winp[25];
            out[26] = out0 * winp[26];
            out[27] = out0 * winp[27];
            out[28] = out1 * winp[28];
            out[29] = out2 * winp[29];
            out[30] = out3 * winp[30];
            out[31] = out4 * winp[31];
            out[32] = out5 * winp[32];
            out[33] = out6 * winp[33];
            out[34] = out7 * winp[34];
            out[35] = out8 * winp[35];
        }
    }

    private static float[] floatRawOut;

    private static float[][][] floatPrevBlck;

    private void hybrid(final int ch, final int gr) {
        GRInfo gr_info = (objSI.ch[ch].gr[gr]);
        int sb, ss, bt;
        int max_sb = (17 + rzero_index[ch]) / 18;
        for (sb = 0; sb < max_sb; sb++) {
            bt = ((gr_info.window_switching_flag != 0) && (gr_info.mixed_block_flag != 0) && (sb < 2)) ? 0 : gr_info.block_type;
            imdct(xr[ch][sb], floatRawOut, bt);
            for (ss = 0; ss < 18; ss++) {
                xr[ch][sb][ss] = floatRawOut[ss] + floatPrevBlck[ch][sb][ss];
                floatPrevBlck[ch][sb][ss] = floatRawOut[18 + ss];
            }
        }
        for (; sb < 32; sb++) for (ss = 0; ss < 18; ss++) {
            xr[ch][sb][ss] = floatPrevBlck[ch][sb][ss];
            floatPrevBlck[ch][sb][ss] = 0;
        }
    }

    private static final float[] floatSamples = new float[32];

    public void decodeFrame() throws Exception {
        getSideInfo();
        int nSlots = objHeader.getMainDataSlots();
        int buflen = objInBitStream.getBuffBytes();
        int data_begin = objSI.main_data_begin;
        while (buflen < data_begin) {
            objInBitStream.append(nSlots);
            objHeader.syncFrame();
            nSlots = objHeader.getMainDataSlots();
            getSideInfo();
            buflen = objInBitStream.getBuffBytes();
            data_begin = objSI.main_data_begin;
        }
        int discard = buflen - objInBitStream.getBytePos() - data_begin;
        objInBitStream.skipBytes(discard);
        objInBitStream.append(nSlots);
        int gr, ch, sb, ss;
        for (gr = 0; gr < intMaxGr; gr++) {
            for (ch = 0; ch < intChannels; ch++) {
                if (objHeader.getVersion() == Header.MPEG1) getScaleFactors_1(ch, gr); else getScaleFactors_2(ch, gr);
                huffmanDecoder(ch, gr);
                requantizer(ch, gr, xr[ch]);
            }
            if (boolIntensityStereo) i_stereo(gr);
            if (objHeader.isMSStereo()) ms_stereo();
            for (ch = intFirstChannel; ch <= intLastChannel; ch++) {
                antialias(ch, gr);
                hybrid(ch, gr);
                int rzero_sb = (17 + rzero_index[ch]) / 18;
                for (sb = 1; sb < rzero_sb; sb += 2) for (ss = 1; ss < 18; ss += 2) xr[ch][sb][ss] = -xr[ch][sb][ss];
                for (ss = 0; ss < 18; ss++) {
                    for (sb = 0; sb < 32; sb++) floatSamples[sb] = xr[ch][sb][ss];
                    objFilter.synthesisSubBand(floatSamples, ch);
                }
            }
        }
    }

    public static final class GRInfo {

        public int part2_3_length;

        public int big_values;

        private int global_gain;

        private int scalefac_compress;

        private int window_switching_flag;

        private int block_type;

        private int mixed_block_flag;

        public final int[] table_select = new int[3];

        private final int[] subblock_gain = new int[3];

        private int region0_count;

        private int region1_count;

        private int preflag;

        private int scalefac_scale;

        public int count1table_select;

        public int region1Start;

        public int region2Start;

        public int part2_bits;
    }

    private final class SideInfo {

        private int main_data_begin;

        private Channel[] ch;

        public SideInfo() {
            ch = new Channel[2];
            ch[0] = new Channel();
            ch[1] = new Channel();
        }
    }

    private static final class Channel {

        private int[] scfsi;

        private GRInfo[] gr;

        public Channel() {
            scfsi = new int[4];
            gr = new GRInfo[2];
            gr[0] = new GRInfo();
            gr[1] = new GRInfo();
        }
    }
}
