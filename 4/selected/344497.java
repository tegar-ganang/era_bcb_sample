package mediaframe.mpeg4.audio.AAC;

import java.io.IOException;

/**
 * AACDecoder
 * 
 */
public final class AACDecoder {

    Tns tns = null;

    Config config = null;

    Huffman huffman = null;

    BitStream audio_stream = null;

    byte[] sect = new byte[2 * (Constants.MAXBANDS + 1)];

    short[] factors[] = new short[2][Constants.MAXBANDS];

    Info[] win_seq_info = new Info[Constants.NUM_WIN_SEQ];

    Info[] winmap = new Info[Constants.NUM_WIN_SEQ];

    Info only_long_info = new Info();

    Info eight_short_info = new Info();

    short[] sfbwidth128 = new short[(1 << Constants.LEN_MAX_SFBS)];

    int maxfac = Constants.TEXP;

    Nec_Info nec_info = new Nec_Info();

    MC_Info mc_info = new MC_Info();

    ADIF_Header adif_header;

    ProgConfig prog_config = new ProgConfig();

    private int nsect;

    private int global_gain;

    long bno = 0;

    boolean default_config;

    int current_program;

    float[][] coef = null;

    float[][] data = null;

    float[][] state = null;

    byte[][] group = null;

    int[][] lpflag = null;

    int[][] prstflag = null;

    TNS_frame_info[] tns_frame_info = null;

    PRED_STATUS[][] sp_status = null;

    float[][] prev_quant = null;

    byte[][] mask = null;

    byte[] hasmask = null;

    byte[] wnd = null;

    byte[] max_sfb = null;

    Wnd_Shape[] wnd_shape = null;

    byte[] d_bytes = null;

    Dolby_Adapt dolby_Adapt = new Dolby_Adapt();

    public AACDecoder(BitStream audio_stream) throws IOException {
        super();
        this.audio_stream = audio_stream;
        this.huffman = new Huffman(this);
        this.config = new Config(this);
        this.tns = new Tns(this);
        int i;
        int j;
        coef = new float[Constants.Chans][Constants.LN2];
        data = new float[Constants.Chans][Constants.LN2];
        state = new float[Constants.Chans][Constants.LN];
        group = new byte[Constants.Chans][Constants.NSHORT];
        lpflag = new int[Constants.Chans][Constants.MAXBANDS];
        prstflag = new int[Constants.Chans][Constants.LEN_PRED_RSTGRP + 1];
        tns_frame_info = new TNS_frame_info[Constants.Chans];
        for (i = 0; i < tns_frame_info.length; i++) {
            tns_frame_info[i] = new TNS_frame_info();
        }
        sp_status = new PRED_STATUS[Constants.Chans][Constants.LN2];
        for (i = 0; i < sp_status.length; i++) {
            for (j = 0; j < sp_status[i].length; j++) {
                sp_status[i][j] = new PRED_STATUS();
            }
        }
        prev_quant = new float[Constants.Chans][Constants.LN2];
        mask = new byte[Constants.Winds][Constants.MAXBANDS];
        hasmask = new byte[Constants.Winds];
        wnd = new byte[Constants.Chans];
        max_sfb = new byte[Constants.Chans];
        wnd_shape = new Wnd_Shape[Constants.Chans];
        for (i = 0; i < wnd_shape.length; i++) {
            wnd_shape[i] = new Wnd_Shape();
        }
        d_bytes = new byte[Constants.Avjframe];
        for (i = 0; i < Constants.Chans; i++) {
            for (j = 0; j < Constants.LN; j++) {
                state[i][j] = 0;
            }
        }
        infoinit(Tables.samp_rate_info[mc_info.sampling_rate_idx]);
        adif_header_present = false;
        current_program = -1;
        default_config = true;
        init();
    }

    void infoinit(SR_Info sip) {
        int i, j, k, n, ws;
        short[] sfbands;
        Info ip = only_long_info;
        win_seq_info[Constants.ONLY_LONG_WINDOW] = ip;
        ip.islong = true;
        ip.nsbk = 1;
        ip.bins_per_bk = Constants.LN2;
        for (i = 0; i < ip.nsbk; i++) {
            ip.sfb_per_sbk[i] = sip.nsfb1024;
            ip.sectbits[i] = Constants.LONG_SECT_BITS;
            ip.sbk_sfb_top[i] = sip.SFbands1024;
        }
        ip.sfb_width_128 = null;
        ip.num_groups = 1;
        ip.group_len[0] = 1;
        ip.group_offs[0] = 0;
        ip = eight_short_info;
        win_seq_info[Constants.EIGHT_SHORT_WINDOW] = ip;
        ip.islong = false;
        ip.nsbk = Constants.NSHORT;
        ip.bins_per_bk = Constants.LN2;
        for (i = 0; i < ip.nsbk; i++) {
            ip.sfb_per_sbk[i] = sip.nsfb128;
            ip.sectbits[i] = Constants.SHORT_SECT_BITS;
            ip.sbk_sfb_top[i] = sip.SFbands128;
        }
        ip.sfb_width_128 = sfbwidth128;
        for (i = 0, j = 0, n = sip.nsfb128; i < n; i++) {
            k = sip.SFbands128[i];
            sfbwidth128[i] = (short) (k - j);
            j = k;
        }
        for (ws = 0; ws < Constants.NUM_WIN_SEQ; ws++) {
            if ((ip = win_seq_info[ws]) == null) continue;
            ip.sfb_per_bk = 0;
            k = 0;
            n = 0;
            for (i = 0; i < ip.nsbk; i++) {
                ip.bins_per_sbk[i] = ip.bins_per_bk / ip.nsbk;
                ip.sfb_per_bk += ip.sfb_per_sbk[i];
                sfbands = ip.sbk_sfb_top[i];
                for (j = 0; j < ip.sfb_per_sbk[i]; j++) ip.bk_sfb_top[j + k] = (short) (sfbands[j] + n);
                n += ip.bins_per_sbk[i];
                k += ip.sfb_per_sbk[i];
            }
        }
    }

    /**
	 * Read and decode the data for the next 1024 output samples
	 * return -1 if there was an error.
	 * @throws IOException raises if an I/O error occurs.
	 */
    int huffdecode(int id, MC_Info mip, byte[] win, Wnd_Shape[] wshape, byte[][] group, byte[] hasmask, byte[][] mask, byte[] max_sfb, int[][] lpflag, int[][] prstflag, TNS_frame_info[] tns, float[][] coef) throws IOException {
        int i, tag, ch, widx, first = 0, last = 0;
        boolean common_window;
        Info info = new Info();
        tag = (int) audio_stream.next_bits(Constants.LEN_TAG);
        switch(id) {
            case Constants.ID_SCE:
            case Constants.ID_LFE:
                common_window = false;
                break;
            case Constants.ID_CPE:
                common_window = audio_stream.next_bit();
                break;
            default:
                throw new IOException("Unknown id " + id);
        }
        if ((ch = config.chn_config(id, tag, common_window, mip)) < 0) return -1;
        switch(id) {
            case Constants.ID_SCE:
            case Constants.ID_LFE:
                widx = mip.ch_info[ch].widx;
                first = ch;
                last = ch;
                hasmask[widx] = 0;
                break;
            case Constants.ID_CPE:
                first = ch;
                last = mip.ch_info[ch].paired_ch;
                if (common_window) {
                    widx = mip.ch_info[ch].widx;
                    get_ics_info(widx, win, wshape, group[widx], max_sfb, lpflag[widx], prstflag[widx]);
                    hasmask[widx] = (byte) getmask(winmap[win[widx]], group[widx], max_sfb[widx], mask[widx]);
                } else {
                    hasmask[mip.ch_info[first].widx] = 0;
                    hasmask[mip.ch_info[last].widx] = 0;
                }
                break;
        }
        for (i = first; i <= last; i++) {
            widx = mip.ch_info[i].widx;
            for (int j = 0; j < Constants.LN2; j++) {
                coef[i][j] = 0;
            }
            if (getics(widx, info, common_window, win, wshape, group[widx], max_sfb, lpflag[widx], prstflag[widx], sect, coef[i], factors[i - first], tns[i]) == 0) return -1;
        }
        if ((id == Constants.ID_CPE) && common_window) {
            int j, k, bot, top, table, is_cb, is_sfb, is_sect;
            IS_Info iip = mip.ch_info[last].is_info;
            bot = 0;
            is_sfb = 0;
            is_sect = 0;
            for (i = 0, j = 0; i < nsect; i++) {
                table = sect[j];
                top = sect[j + 1];
                is_cb = ((table == Constants.INTENSITY_HCB) || (table == Constants.INTENSITY_HCB2)) ? 1 : 0;
                if (is_cb > 0) {
                    iip.is_present = true;
                    iip.bot[is_sect] = bot;
                    iip.top[is_sect] = top;
                    iip.sign[is_sect] = (table == Constants.INTENSITY_HCB) ? 1 : -1;
                    is_sect++;
                }
                for (k = bot; k < top; k++) {
                    if (is_cb > 0) {
                        iip.fac[is_sfb] = factors[1][is_sfb];
                    }
                    is_sfb++;
                }
                bot = top;
                j += 2;
            }
            iip.n_is_sect = is_sect;
        }
        return 0;
    }

    void get_ics_info(int widx, byte[] win, Wnd_Shape[] wshape, byte[] group, byte[] max_sfb, int[] lpflag, int[] prstflag) throws IOException {
        int i, j;
        Info info;
        audio_stream.next_bits(Constants.LEN_ICS_RESERV);
        win[widx] = (byte) audio_stream.next_bits(Constants.LEN_WIN_SEQ);
        wshape[widx].this_bk = (byte) audio_stream.next_bits(Constants.LEN_WIN_SH);
        if ((info = winmap[win[widx]]) == null) throw new IOException("bad window code");
        prstflag[0] = 0;
        if (info.islong) {
            max_sfb[widx] = (byte) audio_stream.next_bits(Constants.LEN_MAX_SFBL);
            group[0] = 1;
            if ((lpflag[0] = (int) audio_stream.next_bits(Constants.LEN_PRED_PRES)) > 0) {
                if ((prstflag[0] = (int) audio_stream.next_bits(Constants.LEN_PRED_RST)) > 0) {
                    for (i = 1; i < Constants.LEN_PRED_RSTGRP + 1; i++) {
                        prstflag[i] = (int) audio_stream.next_bits(Constants.LEN_PRED_RST);
                    }
                }
                j = ((max_sfb[widx] < Constants.MAX_PRED_SFB) ? max_sfb[widx] : Constants.MAX_PRED_SFB) + 1;
                for (i = 1; i < j; i++) lpflag[i] = (int) audio_stream.next_bits(Constants.LEN_PRED_ENAB);
                for (; i < Constants.MAX_PRED_SFB + 1; i++) lpflag[i] = 1;
            }
        } else {
            max_sfb[widx] = (byte) audio_stream.next_bits(Constants.LEN_MAX_SFBS);
            getgroup(info, group);
            lpflag[0] = 0;
        }
    }

    /*********************************************************************/
    static void deinterleave(int inptr[], int outptr[], int ngroups, short nsubgroups[], int ncells[], short cellsize[]) {
        int i, j, k, l, intptr_index, outptr_index;
        int start_inptr, start_subgroup_ptr, subgroup_ptr;
        short cell_inc, subgroup_inc;
        outptr_index = intptr_index = start_subgroup_ptr = 0;
        for (i = 0; i < ngroups; i++) {
            cell_inc = 0;
            start_inptr = intptr_index;
            subgroup_inc = 0;
            for (j = 0; j < ncells[i]; j++) {
                subgroup_inc += cellsize[j];
            }
            for (j = 0; j < ncells[i]; j++) {
                subgroup_ptr = start_subgroup_ptr;
                for (k = 0; k < nsubgroups[i]; k++) {
                    outptr_index = subgroup_ptr + cell_inc;
                    for (l = 0; l < cellsize[j]; l++) {
                        outptr[outptr_index++] = inptr[intptr_index++];
                    }
                    subgroup_ptr += subgroup_inc;
                }
                cell_inc += cellsize[j];
            }
            start_subgroup_ptr += (intptr_index - start_inptr);
        }
    }

    static void calc_gsfb_table(Info info, byte[] group) {
        int group_offset;
        int group_idx;
        int offset;
        short group_offset_p;
        int sfb, len;
        if (info.islong) {
            return;
        } else {
            group_offset = 0;
            group_idx = 0;
            do {
                info.group_len[group_idx] = (short) (group[group_idx] - group_offset);
                group_offset = group[group_idx];
                group_idx++;
            } while (group_offset < 8);
            info.num_groups = group_idx;
            group_offset_p = 0;
            offset = 0;
            for (group_idx = 0; group_idx < info.num_groups; group_idx++) {
                len = info.group_len[group_idx];
                for (sfb = 0; sfb < info.sfb_per_sbk[group_idx]; sfb++) {
                    offset += info.sfb_width_128[sfb] * len;
                    info.bk_sfb_top[group_offset_p++] = (short) offset;
                }
            }
        }
    }

    void getgroup(Info info, byte[] group) throws IOException {
        int i, group_index = 0;
        boolean first_short = true;
        for (i = 0; i < info.nsbk; i++) {
            if (info.bins_per_sbk[i] > Constants.SN2) {
                group[group_index++] = (byte) (i + 1);
            } else {
                if (first_short) {
                    first_short = false;
                } else {
                    if (((int) audio_stream.next_bits(1)) == 0) {
                        group[group_index++] = (byte) i;
                    }
                }
            }
        }
        group[group_index] = (byte) i;
    }

    int getmask(Info info, byte[] group, byte max_sfb, byte[] mask) throws IOException {
        int b, i, mp;
        int group_index = 0, mask_index = 0;
        mp = (int) audio_stream.next_bits(Constants.LEN_MASK_PRES);
        if (mp == 0) {
            return 0;
        }
        if (mp == 2) {
            for (b = 0; b < info.nsbk; b = group[group_index++]) for (i = 0; i < info.sfb_per_sbk[b]; i++) mask[mask_index++] = 1;
            return 0;
        }
        for (b = 0; b < info.nsbk; b = group[group_index++]) {
            for (i = 0; i < max_sfb; i++) {
                mask[mask_index] = (byte) audio_stream.next_bits(Constants.LEN_MASK);
                mask_index++;
            }
            for (; i < info.sfb_per_sbk[b]; i++) {
                mask[mask_index] = 0;
                mask_index++;
            }
        }
        return 1;
    }

    void clr_tns(Info info, TNS_frame_info tns_frame_info) {
        int s;
        tns_frame_info.n_subblocks = info.nsbk;
        for (s = 0; s < tns_frame_info.n_subblocks; s++) {
            tns_frame_info.info[s].n_filt = 0;
        }
    }

    static final int neg_mask[] = { 0xfffc, 0xfff8, 0xfff0 };

    static final int sgn_mask[] = { 0x2, 0x4, 0x8 };

    int get_tns(Info info, TNS_frame_info tns_frame_info) throws IOException {
        int f, t, top, res, res2, compress;
        int s;
        int sp, tmp, s_mask, n_mask;
        TNSfilt[] tns_filt;
        TNSinfo tns_info;
        boolean short_flag = !info.islong;
        int tns_filt_index = 0;
        short_flag = (!info.islong);
        tns_frame_info.n_subblocks = info.nsbk;
        for (s = 0; s < tns_frame_info.n_subblocks; s++) {
            tns_info = tns_frame_info.info[s];
            if ((tns_info.n_filt = (int) audio_stream.next_bits(short_flag ? 1 : 2)) == 0) continue;
            tns_info.coef_res = res = (int) audio_stream.next_bits(1) + 3;
            top = info.sfb_per_sbk[s];
            tns_filt = tns_info.filt;
            tns_filt_index = 0;
            for (f = tns_info.n_filt; f > 0; f--) {
                tns_filt[tns_filt_index].stop_band = top;
                top = tns_filt[tns_filt_index].start_band = top - (int) audio_stream.next_bits(short_flag ? 4 : 6);
                tns_filt[tns_filt_index].order = (int) audio_stream.next_bits(short_flag ? 3 : 5);
                if (tns_filt[tns_filt_index].order > 0) {
                    tns_filt[tns_filt_index].direction = (int) audio_stream.next_bits(1);
                    compress = (int) audio_stream.next_bits(1);
                    res2 = res - compress;
                    s_mask = sgn_mask[res2 - 2];
                    n_mask = neg_mask[res2 - 2];
                    sp = 0;
                    for (t = tns_filt[tns_filt_index].order; t > 0; t--) {
                        tmp = (short) audio_stream.next_bits(res2);
                        tns_filt[tns_filt_index].coef[sp++] = (short) (((tmp & s_mask) > 0) ? (tmp | n_mask) : tmp);
                    }
                }
                tns_filt_index++;
            }
        }
        return 1;
    }

    void get_nec_nc(Nec_Info nec_info) throws IOException {
        int i;
        nec_info.number_pulse = (int) audio_stream.next_bits(Constants.LEN_NEC_NPULSE);
        nec_info.pulse_start_sfb = (int) audio_stream.next_bits(Constants.LEN_NEC_ST_SFB);
        for (i = 0; i < nec_info.number_pulse; i++) {
            nec_info.pulse_offset[i] = (int) audio_stream.next_bits(Constants.LEN_NEC_POFF);
            nec_info.pulse_amp[i] = (int) audio_stream.next_bits(Constants.LEN_NEC_PAMP);
        }
    }

    void nec_nc(float[] coef, Nec_Info nec_info) {
        int i, k;
        k = only_long_info.sbk_sfb_top[0][nec_info.pulse_start_sfb];
        for (i = 0; i <= nec_info.number_pulse; i++) {
            k += nec_info.pulse_offset[i];
            if (coef[k] > 0) {
                coef[k] += nec_info.pulse_amp[i];
            } else {
                coef[k] -= nec_info.pulse_amp[i];
            }
        }
    }

    int getics(int widx, Info info, boolean common_window, byte[] win, Wnd_Shape[] wshape, byte[] group, byte[] max_sfb, int[] lpflag, int[] prstflag, byte[] sect, float[] coef, short[] factors, TNS_frame_info tns) throws IOException {
        int i, tot_sfb;
        global_gain = (int) audio_stream.next_bits(Constants.LEN_SCL_PCM);
        if (!common_window) {
            get_ics_info(widx, win, wshape, group, max_sfb, lpflag, prstflag);
        }
        info.copyFields(winmap[win[widx]]);
        if (max_sfb[widx] == 0) {
            tot_sfb = 0;
        } else {
            i = 0;
            tot_sfb = info.sfb_per_sbk[0];
            while (group[i++] < info.nsbk) {
                tot_sfb += info.sfb_per_sbk[0];
            }
        }
        nsect = huffcb(sect, info.sectbits, tot_sfb, info.sfb_per_sbk[0], max_sfb[widx]);
        if ((nsect == 0) && (max_sfb[widx] > 0)) return 0;
        calc_gsfb_table(info, group);
        if (hufffac(info, group, sect, factors) == 0) return 0;
        if (nec_info.pulse_data_present = audio_stream.next_bit()) {
            get_nec_nc(nec_info);
        }
        if (audio_stream.next_bit()) {
            get_tns(info, tns);
        } else {
            clr_tns(info, tns);
        }
        if (audio_stream.next_bit()) {
            throw new IOException("Gain control not implmented");
        }
        return huffspec(info, sect, factors, coef);
    }

    int huffcb(byte[] sect, int[] sectbits, int tot_sfb, int sfb_per_sbk, byte max_sfb) throws IOException {
        int nsect, n, base, bits, len;
        int sect_index = 0;
        bits = sectbits[0];
        len = (1 << bits) - 1;
        nsect = 0;
        for (base = 0; base < tot_sfb && nsect < tot_sfb; ) {
            sect[sect_index++] = (byte) audio_stream.next_bits(Constants.LEN_CB);
            n = (int) audio_stream.next_bits(bits);
            while (n == len && base < tot_sfb) {
                base += len;
                n = (int) audio_stream.next_bits(bits);
            }
            base += n;
            sect[sect_index++] = (byte) base;
            nsect++;
            if ((sect[sect_index - 1] % sfb_per_sbk) == max_sfb) {
                base += (sfb_per_sbk - max_sfb);
                sect[sect_index++] = 0;
                sect[sect_index++] = (byte) base;
                nsect++;
            }
        }
        if (base != tot_sfb || nsect > tot_sfb) {
            return 0;
        }
        return nsect;
    }

    int hufffac(Info info, byte[] group, byte[] sect, short[] factors) throws IOException {
        Hcb hcb;
        int[][] hcw;
        int i, b, bb, t, n, sfb, top, fac, is_pos;
        int[] fac_trans = new int[Constants.MAXBANDS];
        int group_idx = 0;
        int sect_index = 0;
        int factors_index = 0;
        int fac_trans_index = 0;
        for (i = 0; i < Constants.MAXBANDS; i++) {
            factors[i] = 0;
            fac_trans[i] = 0;
        }
        sfb = 0;
        for (i = 0; i < nsect; i++) {
            top = sect[sect_index + 1];
            t = sect[sect_index];
            sect_index += 2;
            for (; sfb < top; sfb++) {
                fac_trans[sfb] = t;
            }
        }
        fac = global_gain;
        is_pos = 0;
        hcb = huffman.book[Constants.BOOKSCL];
        hcw = hcb.hcw;
        bb = 0;
        for (b = 0; b < info.nsbk; ) {
            n = info.sfb_per_sbk[b];
            b = group[group_idx++];
            for (i = 0; i < n; i++) {
                if (fac_trans[fac_trans_index + i] > 0) {
                    if ((info.nsbk == 1) && ((fac_trans[fac_trans_index + i] == Constants.INTENSITY_HCB) || (fac_trans[fac_trans_index + i] == Constants.INTENSITY_HCB2))) {
                        System.out.println(2);
                        t = huffman.decode_huff_cw(hcw);
                        is_pos += t - Constants.MIDFAC;
                        factors[factors_index + i] = (short) is_pos;
                        continue;
                    }
                    t = huffman.decode_huff_cw(hcw);
                    fac += t - Constants.MIDFAC;
                    if ((fac >= 2 * maxfac) || (fac < 0)) {
                        return 0;
                    }
                    factors[factors_index + i] = (short) fac;
                }
            }
            if (!info.islong) {
                for (bb++; bb < b; bb++) {
                    for (i = 0; i < n; i++) {
                        factors[factors_index + i + n] = factors[factors_index + i];
                    }
                    factors_index += n;
                }
            }
            fac_trans_index += n;
            factors_index += n;
        }
        return 1;
    }

    float iquant(int q) {
        return (q >= 0) ? (float) huffman.iq_exp_tbl[q] : (float) (-huffman.iq_exp_tbl[-q]);
    }

    float esc_iquant(int q) {
        if (q > 0) {
            if (q < Constants.MAX_IQ_TBL) {
                return ((float) huffman.iq_exp_tbl[q]);
            } else {
                return (float) (Math.pow(q, 4d / 3d));
            }
        } else {
            q = -q;
            if (q < Constants.MAX_IQ_TBL) {
                return ((float) (-huffman.iq_exp_tbl[q]));
            } else {
                return (float) (-Math.pow(q, 4d / 3d));
            }
        }
    }

    int huffspec(Info info, byte[] sect, short[] factors, float[] coef) throws IOException {
        Hcb hcb;
        int[][] hcw;
        int i, j, k, table, step, temp, stop, bottom, top;
        short[] bands;
        int bands_index = 0;
        int[] quant = new int[Constants.LN2];
        int[] tmp_spec = new int[Constants.LN2];
        int sect_index = 0;
        int quant_index = 0;
        for (i = 0; i < Constants.LN2; i++) {
            quant[i] = 0;
        }
        bands = info.bk_sfb_top;
        bottom = 0;
        k = 0;
        for (i = nsect; i > 0; i--) {
            table = sect[sect_index + 0];
            top = sect[sect_index + 1];
            sect_index += 2;
            if ((table == 0) || (table == Constants.INTENSITY_HCB) || (table == Constants.INTENSITY_HCB2)) {
                bands_index = top;
                k = bands[bands_index - 1];
                bottom = top;
                continue;
            }
            if (table < (Constants.BY4BOOKS + 1)) {
                step = 4;
            } else {
                step = 2;
            }
            hcb = huffman.book[table];
            hcw = hcb.hcw;
            quant_index = k;
            for (j = bottom; j < top; j++) {
                stop = bands[bands_index++];
                while (k < stop) {
                    temp = huffman.decode_huff_cw(hcw);
                    huffman.unpack_idx(quant, quant_index, temp, hcb);
                    if (!hcb.signed_cb) {
                        huffman.get_sign_bits(quant, quant_index, step);
                    }
                    if (table == Constants.ESCBOOK) {
                        quant[quant_index + 0] = getescape(quant[quant_index + 0]);
                        quant[quant_index + 1] = getescape(quant[quant_index + 1]);
                    }
                    quant_index += step;
                    k += step;
                }
            }
            bottom = top;
        }
        if (nec_info.pulse_data_present) {
            nec_nc(coef, nec_info);
        }
        if (!info.islong) {
            deinterleave(quant, tmp_spec, info.num_groups, info.group_len, info.sfb_per_sbk, info.sfb_width_128);
            for (i = 0; i < tmp_spec.length; i++) {
                quant[i] = tmp_spec[i];
            }
        }
        for (i = 0; i < info.bins_per_bk; i++) {
            coef[i] = esc_iquant(quant[i]);
        }
        {
            int sbk, nsbk, sfb, nsfb, fac, top_coef;
            float scale;
            int coef_index = 0;
            i = 0;
            nsbk = info.nsbk;
            for (sbk = 0; sbk < nsbk; sbk++) {
                nsfb = info.sfb_per_sbk[sbk];
                k = 0;
                for (sfb = 0; sfb < nsfb; sfb++) {
                    top_coef = info.sbk_sfb_top[sbk][sfb];
                    fac = factors[i++] - Constants.SF_OFFSET;
                    if ((fac >= 0) && (fac < Constants.TEXP)) {
                        scale = huffman.exptable[fac];
                    } else {
                        if (fac == -Constants.SF_OFFSET) {
                            scale = 0;
                        } else {
                            scale = (float) Math.pow(2.0, 0.25 * fac);
                        }
                    }
                    for (; k < top_coef; k++) {
                        coef[coef_index++] *= scale;
                    }
                }
            }
        }
        return 1;
    }

    /** checked */
    int getescape(int q) throws IOException {
        int i, off, neg;
        if (q < 0) {
            if (q != -16) return q;
            neg = 1;
        } else {
            if (q != +16) return q;
            neg = 0;
        }
        for (i = 4; ; i++) {
            if (!audio_stream.next_bit()) break;
        }
        if (i > 16) {
            off = (int) audio_stream.next_bits(i - 16) << 16;
            off |= audio_stream.next_bits(16);
        } else {
            off = (int) audio_stream.next_bits(i);
        }
        i = off + (1 << i);
        if (neg > 0) {
            i = -i;
        }
        return i;
    }

    /**
	 * if (chan==RIGHT) { 
	 *     do IS decoding for this channel (scale left ch. values with factor(SFr-SFl) )
	 *     reset all lpflags for which IS is on
	 *     pass decoded IS values to predict
	 * }
	 */
    void intensity(MC_Info mip, Info info, int widx, int[] lpflag, int ch, float[][] coef) {
        int left, right, i, k, nsect, sign, bot, top, sfb, ktop;
        float scale;
        Ch_Info cip = mip.ch_info[ch];
        IS_Info iip = mip.ch_info[ch].is_info;
        if (!(cip.cpe && iip.is_present && !cip.ch_is_left)) {
            return;
        }
        left = cip.paired_ch;
        right = ch;
        nsect = iip.n_is_sect;
        for (i = 0; i < nsect; i++) {
            sign = iip.sign[i];
            top = iip.top[i];
            bot = iip.bot[i];
            for (sfb = bot; sfb < top; sfb++) {
                lpflag[1 + sfb] = 0;
                scale = (float) (sign * Math.pow(0.5, 0.25 * (iip.fac[sfb])));
                k = (sfb == 0) ? 0 : info.bk_sfb_top[sfb - 1];
                ktop = info.bk_sfb_top[sfb];
                for (; k < ktop; k++) {
                    coef[right][k] = coef[left][k] * scale;
                }
            }
        }
    }

    void synt(Info info, byte[] group, byte[] mask, float[] right, float[] left) {
        float vrr, vrl;
        short[] band;
        int i, n, nn, b, bb, nband;
        int group_index = 0;
        int right_index = 0;
        int left_index = 0;
        int mask_index = 0;
        bb = 0;
        for (b = 0; b < info.nsbk; ) {
            nband = info.sfb_per_sbk[b];
            band = info.sbk_sfb_top[b];
            b = group[group_index++];
            for (; bb < b; bb++) {
                n = 0;
                for (i = 0; i < nband; i++) {
                    nn = band[i];
                    if (mask[mask_index + i] > 0) {
                        for (; n < nn; n++) {
                            vrr = right[right_index + n];
                            vrl = left[left_index + n];
                            left[left_index + n] = vrr + vrl;
                            right[right_index + n] = vrl - vrr;
                        }
                    }
                    n = nn;
                }
                right_index += info.bins_per_sbk[bb];
                left_index += info.bins_per_sbk[bb];
            }
            mask_index += info.sfb_per_sbk[bb - 1];
        }
    }

    void init() throws IOException {
        predinit();
        winmap[0] = win_seq_info[Constants.ONLY_LONG_WINDOW];
        winmap[1] = win_seq_info[Constants.ONLY_LONG_WINDOW];
        winmap[2] = win_seq_info[Constants.EIGHT_SHORT_WINDOW];
        winmap[3] = win_seq_info[Constants.ONLY_LONG_WINDOW];
    }

    void predinit() throws IOException {
        int i, ch;
        for (ch = 0; ch < Constants.Chans; ch++) {
            for (i = 0; i < Constants.LN2; i++) {
                Monopred.init_pred_stat(sp_status[ch][i], Constants.PRED_ORDER, Constants.PRED_ALPHA, Constants.PRED_A, Constants.PRED_B);
                prev_quant[ch][i] = 0;
            }
        }
    }

    int getdata(byte[] data_bytes) throws IOException {
        boolean byte_align_flag;
        int d_cnt;
        audio_stream.next_bits(Constants.LEN_TAG);
        byte_align_flag = audio_stream.next_bit();
        if ((d_cnt = (int) audio_stream.next_bits(8)) == (1 << 8) - 1) {
            d_cnt += (int) audio_stream.next_bits(8);
        }
        if (byte_align_flag) {
            audio_stream.byteAlign();
        }
        for (int i = 0; i < d_cnt; i++) {
            data_bytes[i] = (byte) audio_stream.next_bits(Constants.LEN_BYTE);
        }
        return 0;
    }

    void getfill() throws IOException {
        int i, cnt;
        if ((cnt = (int) audio_stream.next_bits(Constants.LEN_F_CNT)) == (1 << Constants.LEN_F_CNT) - 1) {
            cnt += (int) audio_stream.next_bits(Constants.LEN_F_ESC) - 1;
        }
        for (i = 0; i < cnt; i++) {
            audio_stream.next_bits(Constants.LEN_BYTE);
        }
    }

    public int decodeFrame(byte[] buf) throws IOException {
        int i, j, ch, wn, ele_id;
        int left, right;
        Info info;
        MC_Info mip = mc_info;
        Ch_Info cip;
        if (startblock() < 0) return -1;
        config.reset_mc_info(mip);
        while ((ele_id = (int) audio_stream.next_bits(Constants.LEN_SE_ID)) != Constants.ID_END) {
            switch(ele_id) {
                case Constants.ID_SCE:
                case Constants.ID_CPE:
                case Constants.ID_LFE:
                    int result_code = huffdecode(ele_id, mip, wnd, wnd_shape, group, hasmask, mask, max_sfb, lpflag, prstflag, tns_frame_info, coef);
                    if (result_code < 0) throw new IOException("huffdecode returned " + result_code);
                    break;
                case Constants.ID_DSE:
                    if (getdata(d_bytes) < 0) throw new IOException("data channel");
                    break;
                case Constants.ID_PCE:
                    break;
                case Constants.ID_FIL:
                    getfill();
                    break;
                case Constants.ID_CCE:
                    System.out.println("Coupling channels isn't supported!");
                    break;
                default:
                    System.out.println("Element not supported: " + ele_id);
                    break;
            }
        }
        config.check_mc_info(mip, (bno == 0 && default_config));
        for (ch = 0; ch < Constants.Chans; ch++) {
            cip = mip.ch_info[ch];
            if ((cip.present) && (cip.cpe) && (cip.ch_is_left)) {
                wn = cip.widx;
                if (hasmask[wn] > 0) {
                    left = ch;
                    right = cip.paired_ch;
                    info = winmap[wnd[wn]];
                    synt(info, group[wn], mask[wn], coef[right], coef[left]);
                }
            }
        }
        for (ch = 0; ch < Constants.Chans; ch++) {
            if (!(mip.ch_info[ch].present)) continue;
            wn = mip.ch_info[ch].widx;
            info = winmap[wnd[wn]];
            intensity(mip, info, wn, lpflag[wn], ch, coef);
            Monopred.predict(info, mip.profile, lpflag[wn], sp_status[ch], prev_quant[ch], coef[ch]);
        }
        for (ch = 0; ch < Constants.Chans; ch++) {
            if (!(mip.ch_info[ch].present)) {
                continue;
            }
            wn = mip.ch_info[ch].widx;
            info = winmap[wnd[wn]];
            left = ch;
            right = left;
            if ((mip.ch_info[ch].cpe) && (mip.ch_info[ch].common_window)) right = mip.ch_info[ch].paired_ch;
            Monopred.predict_reset(info, prstflag[wn], sp_status, prev_quant, left, right);
            for (i = j = 0; i < tns_frame_info[ch].n_subblocks; i++) {
                tns.tns_decode_subblock(coef[ch], j, info.sfb_per_sbk[i], info.sbk_sfb_top[i], info.islong, tns_frame_info[ch].info[i]);
                j += info.bins_per_sbk[i];
            }
            if (Constants.DOLBY_MDCT) {
                dolby_Adapt.freq2time_adapt(wnd[wn], wnd_shape[wn], coef[ch], state[ch], data[ch]);
            }
        }
        if (bno > 1) {
            writeout(data, mip, buf);
        }
        bno++;
        return bno > 2 ? (2048 * mc_info.nch) : 0;
    }

    void writeout(float[][] data, MC_Info mip, byte[] obuf) {
        int i, p_index = 0;
        for (i = 0; i < Constants.Chans; i++) {
            if (!(mip.ch_info[i].present)) continue;
            fmtchan(obuf, p_index, data[i], 2 * mc_info.nch);
            p_index += 2;
        }
    }

    void fmtchan(byte[] p, int p_index, float[] data, int stride) {
        int i, c, data_index = 0;
        float s;
        for (i = 0; i < Constants.LN2; i++) {
            s = data[data_index++];
            if (s < 0) {
                s -= .5;
                if (s < -0x7fff) s = (float) -0x7fff;
            } else {
                s += .5;
                if (s > 0x7fff) s = (float) 0x7fff;
            }
            c = (int) s;
            p[p_index + 1] = (byte) ((c >> 8) & 0xff);
            p[p_index + 0] = (byte) (c & 0xff);
            p_index += stride;
        }
    }

    private boolean adif_header_present = false;

    int startblock() throws IOException {
        if (adif_header_present) {
            if (config.get_adif_header() < 0) return -1;
            adif_header_present = false;
        }
        audio_stream.byteAlign();
        return 1;
    }

    public Huffman getHuffman() {
        return huffman;
    }

    public BitStream getAudio_Stream() {
        return audio_stream;
    }

    public Config getConfig() {
        return config;
    }

    public int getSampleFrequency() {
        return mc_info.sampling_rate;
    }

    public int getChannelCount() {
        return mc_info.nch;
    }

    public String getAudioProfile() {
        switch(prog_config.profile) {
            case Constants.Main_Profile:
                return "Main Profile";
            case Constants.LC_Profile:
                return "LC Profile";
            case Constants.SRS_Profile:
                return "SRC Profile";
            default:
                return "Unknown Profile";
        }
    }
}
