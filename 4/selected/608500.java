package de.sciss.eisenkraut.io;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import de.sciss.app.AbstractApplication;
import de.sciss.app.AbstractCompoundEdit;
import de.sciss.eisenkraut.gui.WaveformView;
import de.sciss.eisenkraut.math.ConstQ;
import de.sciss.eisenkraut.math.FastLog;
import de.sciss.eisenkraut.math.MathUtil;
import de.sciss.eisenkraut.util.PrefsUtil;
import de.sciss.io.AudioFile;
import de.sciss.io.CacheManager;
import de.sciss.io.IOUtil;
import de.sciss.io.Span;
import de.sciss.util.MutableInt;

/**
 * 	Sonagram trail with automatic handling of subsampled versions.
 * 	This class is dedicated to the memory of Niklas Werner, a
 * 	former fellow student of mine. The sonagram colour table is
 * 	taken from his work sonasound (http://sonasound.sourceforge.net/).
 * 	<P>
 * 	Performance tests with the new SlidingDFT class were quite
 * 	disappointing. Even in the worst scenario, fftSize=32768 and stepSize=128,
 * 	the plain FFT approach is still 7.5 times faster than the
 * 	sliding DFT variant... 
 * 
 *	@version	0.70, 28-Jun-08
 *	@author		Hanns Holger Rutz
 *
 *	@todo		editing (addAllDep)
 *	@todo		delay compensation (fftSize data seems to be missing,
 *				the necessary pre-delay seems to be fftSize / 2)
 *	@todo		cache management
 *	@todo		cost-calculation of potential sliding DFT
 *	@todo		there's an efficient multi-channel FFT?
 *	@todo		the AsyncEvent should have Span info so DocumentFrame
 *				doesn't always need to repaint
 */
public class DecimatedSonaTrail extends DecimatedTrail {

    private static final int UPDATE_PERIOD = 4000;

    protected final Decimator decimator;

    protected int fftSize;

    protected final int stepSize;

    protected final ConstQ constQ;

    protected final int numKernels;

    protected final float[] filterBuf;

    private static final int[] colors = { 0x000000, 0x050101, 0x090203, 0x0E0304, 0x120406, 0x160507, 0x1A0608, 0x1D0609, 0x20070A, 0x23080B, 0x25080C, 0x27090D, 0x290A0D, 0x2B0A0E, 0x2D0B0F, 0x2E0B0F, 0x300C10, 0x310C10, 0x320C10, 0x320D11, 0x330D11, 0x340D11, 0x340D11, 0x350E12, 0x350E12, 0x360E12, 0x360E12, 0x360E12, 0x360F13, 0x370F13, 0x370F13, 0x370F13, 0x381014, 0x381014, 0x381014, 0x391014, 0x391015, 0x3A1115, 0x3A1115, 0x3B1115, 0x3B1116, 0x3C1216, 0x3D1216, 0x3D1217, 0x3E1217, 0x3E1317, 0x3F1317, 0x3F1317, 0x401418, 0x401418, 0x401418, 0x401418, 0x411518, 0x411518, 0x411518, 0x411517, 0x421617, 0x421617, 0x421617, 0x421617, 0x421717, 0x431717, 0x431717, 0x431717, 0x441818, 0x441818, 0x441818, 0x451818, 0x451818, 0x461919, 0x461919, 0x471919, 0x471919, 0x481A1A, 0x481A1A, 0x491A1A, 0x4A1A1B, 0x4A1B1B, 0x4B1B1B, 0x4B1B1B, 0x4C1C1C, 0x4C1C1C, 0x4C1C1C, 0x4D1C1C, 0x4D1D1C, 0x4D1D1C, 0x4D1D1C, 0x4E1D1C, 0x4E1E1C, 0x4E1E1C, 0x4E1E1C, 0x4F1E1B, 0x4F1F1B, 0x4F1F1B, 0x4F1F1B, 0x4F1F1B, 0x50201C, 0x50201C, 0x50201C, 0x50201C, 0x50201C, 0x51211C, 0x51211C, 0x51211D, 0x51211D, 0x52221D, 0x52221E, 0x52221E, 0x52221E, 0x53231F, 0x53231F, 0x53231F, 0x542420, 0x542420, 0x542420, 0x542420, 0x552521, 0x552521, 0x552521, 0x552522, 0x562622, 0x562622, 0x562622, 0x562623, 0x572723, 0x572723, 0x572723, 0x572723, 0x582824, 0x582824, 0x582824, 0x582824, 0x582824, 0x592924, 0x592924, 0x592925, 0x592925, 0x5A2A25, 0x5A2A25, 0x5A2A25, 0x5B2A26, 0x5B2B26, 0x5B2B27, 0x5B2B27, 0x5C2C28, 0x5C2C28, 0x5C2C29, 0x5C2C29, 0x5C2D2A, 0x5D2D2B, 0x5D2D2C, 0x5D2D2C, 0x5D2E2D, 0x5E2E2E, 0x5E2E2F, 0x5E2E30, 0x5E2F30, 0x5F2F31, 0x5F2F32, 0x5F2F33, 0x603034, 0x603034, 0x603035, 0x603035, 0x613036, 0x613136, 0x613137, 0x613137, 0x623138, 0x623238, 0x623239, 0x623239, 0x63323A, 0x63333A, 0x63333B, 0x63333B, 0x64343C, 0x64343C, 0x64343C, 0x64343D, 0x64353D, 0x65353E, 0x65353E, 0x65353F, 0x65363F, 0x653640, 0x663641, 0x663641, 0x663742, 0x673742, 0x673743, 0x673743, 0x683844, 0x683844, 0x683844, 0x693845, 0x693845, 0x6A3946, 0x6A3946, 0x6B3947, 0x6B3947, 0x6C3A48, 0x6C3A48, 0x6D3A49, 0x6D3A49, 0x6E3B4A, 0x6E3B4A, 0x6F3B4B, 0x703C4C, 0x703C4C, 0x713C4D, 0x713C4E, 0x723D4E, 0x723D4F, 0x733D50, 0x733E51, 0x743E52, 0x743E52, 0x753E53, 0x753F54, 0x763F55, 0x763F55, 0x773F56, 0x773F57, 0x784058, 0x784058, 0x784059, 0x784059, 0x79405A, 0x79405A, 0x79405B, 0x79405B, 0x7A405B, 0x7A405C, 0x7A405C, 0x7A405D, 0x7A405D, 0x7B405E, 0x7B405E, 0x7B405F, 0x7C4060, 0x7C3F60, 0x7C3F61, 0x7D3F62, 0x7D3F62, 0x7D3F63, 0x7E3F64, 0x7E4065, 0x7F4066, 0x7F4067, 0x804067, 0x804068, 0x814069, 0x81406A, 0x82406A, 0x82406B, 0x83406C, 0x833F6C, 0x833F6C, 0x843F6D, 0x843F6D, 0x853F6D, 0x853F6E, 0x863F6E, 0x863F6E, 0x873F6E, 0x873F6E, 0x883F6E, 0x883F6F, 0x893F6F, 0x893F6F, 0x8A3F6F, 0x8B4070, 0x8B4070, 0x8C4070, 0x8C4070, 0x8D4071, 0x8D4171, 0x8E4172, 0x8E4172, 0x8F4173, 0x8F4273, 0x904274, 0x904274, 0x914375, 0x914376, 0x924376, 0x924377, 0x934478, 0x934478, 0x934479, 0x944479, 0x94447A, 0x94447B, 0x94447B, 0x95447C, 0x95447D, 0x95447D, 0x95447E, 0x95447F, 0x964480, 0x964480, 0x964481, 0x964482, 0x974483, 0x974383, 0x974384, 0x974385, 0x974386, 0x984386, 0x984387, 0x984388, 0x984389, 0x99438A, 0x99438A, 0x99438B, 0x99438C, 0x9A438D, 0x9A438D, 0x9A438E, 0x9B448F, 0x9B448F, 0x9B4490, 0x9B4490, 0x9C4491, 0x9C4491, 0x9C4492, 0x9C4492, 0x9D4493, 0x9D4493, 0x9D4493, 0x9D4494, 0x9E4494, 0x9E4495, 0x9E4495, 0x9E4496, 0x9F4497, 0x9F4397, 0x9F4398, 0x9F4398, 0xA04399, 0xA0439A, 0xA0439B, 0xA0439B, 0xA1439C, 0xA1439D, 0xA1439E, 0xA1439F, 0xA2439F, 0xA243A0, 0xA243A1, 0xA243A2, 0xA344A3, 0xA344A3, 0xA344A4, 0xA344A5, 0xA344A6, 0xA444A6, 0xA445A7, 0xA445A8, 0xA445A9, 0xA545A9, 0xA546AA, 0xA546AB, 0xA546AB, 0xA647AC, 0xA647AD, 0xA647AE, 0xA748AF, 0xA748AF, 0xA748B0, 0xA748B1, 0xA849B2, 0xA849B2, 0xA849B3, 0xA949B4, 0xA94AB5, 0xA94AB6, 0xA94AB6, 0xAA4AB7, 0xAA4BB8, 0xAA4BB8, 0xAA4BB9, 0xAA4BBA, 0xAB4CBB, 0xAB4CBB, 0xAB4CBC, 0xAB4CBC, 0xAB4CBD, 0xAB4DBD, 0xAB4DBE, 0xAB4DBE, 0xAB4DBF, 0xAB4EBF, 0xAB4EC0, 0xAB4EC0, 0xAB4EC1, 0xAB4FC1, 0xAB4FC2, 0xAB4FC2, 0xAB50C3, 0xAA50C3, 0xAA50C3, 0xAA50C4, 0xAA51C4, 0xAA51C5, 0xAA51C5, 0xAA51C6, 0xAA52C6, 0xAA52C7, 0xAA52C7, 0xAA52C8, 0xAA53C8, 0xAA53C9, 0xAA53C9, 0xAA53CA, 0xAB54CB, 0xAB54CB, 0xAB54CC, 0xAB54CC, 0xAB54CD, 0xAC55CD, 0xAC55CE, 0xAC55CE, 0xAC55CF, 0xAD56CF, 0xAD56D0, 0xAD56D0, 0xAE56D1, 0xAE57D1, 0xAE57D2, 0xAE57D2, 0xAF58D3, 0xAF58D3, 0xAF58D3, 0xAF58D4, 0xAF59D4, 0xAF59D4, 0xAF59D5, 0xAF59D5, 0xAF5AD5, 0xAF5AD5, 0xAF5AD6, 0xAF5AD6, 0xAF5BD6, 0xAF5BD6, 0xAF5BD6, 0xAF5BD6, 0xAF5CD7, 0xAE5CD7, 0xAE5CD7, 0xAE5CD7, 0xAD5CD7, 0xAD5DD7, 0xAD5DD6, 0xAD5DD6, 0xAC5DD6, 0xAC5ED6, 0xAC5ED6, 0xAB5ED6, 0xAB5ED6, 0xAB5FD6, 0xAB5FD6, 0xAB5FD6, 0xAB60D7, 0xAA60D7, 0xAA60D7, 0xAA60D7, 0xAA61D7, 0xAA61D7, 0xAA61D8, 0xAA61D8, 0xAB62D8, 0xAB62D8, 0xAB62D9, 0xAB62D9, 0xAB63D9, 0xAB63DA, 0xAB63DA, 0xAB63DA, 0xAB64DB, 0xAA64DB, 0xAA64DB, 0xAA64DB, 0xAA64DC, 0xA965DC, 0xA965DC, 0xA965DC, 0xA965DC, 0xA865DD, 0xA866DD, 0xA866DD, 0xA766DD, 0xA766DE, 0xA767DE, 0xA767DE, 0xA768DF, 0xA668DF, 0xA668DF, 0xA669DF, 0xA669E0, 0xA66AE0, 0xA66AE0, 0xA66BE1, 0xA66BE1, 0xA66CE1, 0xA66CE1, 0xA76DE2, 0xA76DE2, 0xA76EE2, 0xA76EE2, 0xA76FE2, 0xA770E3, 0xA670E3, 0xA671E3, 0xA671E3, 0xA672E3, 0xA672E3, 0xA573E2, 0xA573E2, 0xA574E2, 0xA574E2, 0xA475E2, 0xA475E2, 0xA476E2, 0xA376E2, 0xA377E2, 0xA377E2, 0xA378E3, 0xA278E3, 0xA279E3, 0xA279E3, 0xA17AE3, 0xA17AE4, 0xA17BE4, 0xA17BE4, 0xA07CE5, 0xA07CE5, 0xA07DE5, 0xA07DE5, 0x9F7EE6, 0x9F7EE6, 0x9F7FE6, 0x9F7FE6, 0x9F80E7, 0x9E80E7, 0x9E80E7, 0x9E81E7, 0x9E81E7, 0x9D82E7, 0x9D82E7, 0x9D83E7, 0x9D83E6, 0x9C83E6, 0x9C84E6, 0x9C84E6, 0x9C85E6, 0x9B85E6, 0x9B86E6, 0x9B86E6, 0x9B87E7, 0x9A87E7, 0x9A87E7, 0x9A88E7, 0x9988E7, 0x9989E8, 0x9989E8, 0x988AE8, 0x988AE9, 0x988BE9, 0x988BE9, 0x978CEA, 0x978CEA, 0x978DEA, 0x978DEA, 0x978EEA, 0x978FEB, 0x968FEB, 0x9690EA, 0x9690EA, 0x9691EA, 0x9691EA, 0x9692EA, 0x9692EA, 0x9693E9, 0x9693E9, 0x9694E9, 0x9694E8, 0x9695E8, 0x9695E8, 0x9696E7, 0x9696E7, 0x9797E7, 0x9697E6, 0x9697E6, 0x9698E6, 0x9698E5, 0x9699E5, 0x9699E5, 0x969AE5, 0x969AE4, 0x969BE4, 0x969BE4, 0x969CE4, 0x969CE3, 0x969DE3, 0x969DE3, 0x969EE3, 0x979FE3, 0x979FE2, 0x97A0E2, 0x97A0E2, 0x97A1E1, 0x98A1E1, 0x98A2E1, 0x98A2E1, 0x98A3E0, 0x99A3E0, 0x99A4E0, 0x99A4E0, 0x9AA5DF, 0x9AA5DF, 0x9AA6DF, 0x9AA6DF, 0x9BA7DF, 0x9BA7DE, 0x9BA7DE, 0x9BA7DE, 0x9BA8DE, 0x9BA8DE, 0x9BA8DD, 0x9BA8DD, 0x9BA8DD, 0x9BA9DD, 0x9BA9DC, 0x9BA9DC, 0x9BA9DC, 0x9BAADC, 0x9BAADB, 0x9BAADB, 0x9BABDB, 0x9AABDA, 0x9AABDA, 0x9AACD9, 0x9AACD9, 0x99ADD8, 0x99ADD8, 0x99AED7, 0x99AED7, 0x98AFD6, 0x98AFD5, 0x98B0D5, 0x98B0D4, 0x97B1D4, 0x97B1D3, 0x97B2D3, 0x97B3D3, 0x96B3D2, 0x96B4D2, 0x96B4D1, 0x95B5D1, 0x95B5D1, 0x95B6D1, 0x95B6D0, 0x94B7D0, 0x94B7D0, 0x94B7D0, 0x94B8CF, 0x93B8CF, 0x93B9CF, 0x93B9CF, 0x93BACF, 0x93BBCF, 0x92BBCE, 0x92BCCE, 0x92BCCE, 0x91BDCE, 0x91BDCD, 0x91BECD, 0x91BECD, 0x90BFCD, 0x90BFCC, 0x90C0CC, 0x90C0CC, 0x8FC1CC, 0x8FC1CB, 0x8FC2CB, 0x8FC2CB, 0x8FC3CB, 0x8EC3CA, 0x8EC3CA, 0x8EC4CA, 0x8EC4C9, 0x8DC4C9, 0x8DC4C9, 0x8DC5C9, 0x8DC5C8, 0x8CC5C8, 0x8CC5C8, 0x8CC5C8, 0x8CC6C7, 0x8BC6C7, 0x8BC6C7, 0x8BC6C7, 0x8BC7C7, 0x8AC7C6, 0x8AC7C6, 0x8AC7C6, 0x89C7C5, 0x89C8C5, 0x89C8C5, 0x88C8C5, 0x88C8C4, 0x88C9C4, 0x88C9C4, 0x87C9C4, 0x87C9C3, 0x87CAC3, 0x87CAC3, 0x87CAC3, 0x87CBC3, 0x86CBC2, 0x86CBC2, 0x86CBC2, 0x86CCC2, 0x86CCC1, 0x86CCC1, 0x87CCC1, 0x87CDC1, 0x87CDC0, 0x87CDC0, 0x87CDC0, 0x87CEC0, 0x87CEBF, 0x87CEBF, 0x87CEBF, 0x87CFBF, 0x86CFBE, 0x86CFBE, 0x86CFBE, 0x86CFBD, 0x85D0BD, 0x85D0BD, 0x85D0BC, 0x85D0BC, 0x84D0BC, 0x84D1BC, 0x84D1BB, 0x83D1BB, 0x83D2BB, 0x83D2BB, 0x83D2BB, 0x83D3BB, 0x82D3BA, 0x82D3BA, 0x82D4BA, 0x82D4BA, 0x82D5BA, 0x82D5BA, 0x82D6BA, 0x82D6BA, 0x82D7B9, 0x82D7B9, 0x82D8B9, 0x82D8B9, 0x82D9B8, 0x82D9B8, 0x82DAB7, 0x83DBB7, 0x83DBB6, 0x83DCB5, 0x83DCB4, 0x83DDB3, 0x83DDB2, 0x84DEB1, 0x84DEB0, 0x84DFAF, 0x84DFAE, 0x84E0AD, 0x85E0AB, 0x85E1AA, 0x85E1A9, 0x86E2A8, 0x86E2A7, 0x87E3A7, 0x87E3A6, 0x87E3A5, 0x88E3A4, 0x88E4A4, 0x89E4A3, 0x89E4A3, 0x8AE5A2, 0x8AE5A2, 0x8BE5A1, 0x8BE5A1, 0x8CE5A0, 0x8CE6A0, 0x8DE6A0, 0x8DE69F, 0x8EE69F, 0x8FE79F, 0x8FE79E, 0x90E79E, 0x90E79D, 0x91E79D, 0x91E89C, 0x92E89C, 0x92E89B, 0x93E89B, 0x93E99A, 0x94E99A, 0x94E999, 0x94E999, 0x95EA98, 0x95EA98, 0x96EA97, 0x97EB97, 0x97EB96, 0x98EB95, 0x98EB95, 0x99EC94, 0x99EC94, 0x9AEC93, 0x9AEC93, 0x9BED92, 0x9BED92, 0x9CED91, 0x9CED91, 0x9DEE90, 0x9DEE90, 0x9EEE8F, 0x9EEE8F, 0x9FEF8F, 0x9FEF8E, 0x9FEF8E, 0xA0EF8D, 0xA0F08D, 0xA1F08C, 0xA1F08C, 0xA1F08B, 0xA2F18B, 0xA2F18A, 0xA2F18A, 0xA3F189, 0xA3F288, 0xA3F288, 0xA4F287, 0xA4F287, 0xA5F387, 0xA5F386, 0xA5F386, 0xA6F385, 0xA6F385, 0xA6F484, 0xA7F484, 0xA7F483, 0xA7F483, 0xA8F582, 0xA8F582, 0xA9F581, 0xA9F581, 0xA9F681, 0xAAF680, 0xAAF680, 0xABF780, 0xABF77F, 0xABF77F, 0xACF77F, 0xACF87E, 0xADF87E, 0xADF87E, 0xADF97E, 0xAEF97D, 0xAEF97D, 0xAFF97D, 0xAFFA7D, 0xB0FA7C, 0xB1FA7C, 0xB1FA7C, 0xB2FA7C, 0xB3FB7C, 0xB3FB7B, 0xB4FB7B, 0xB5FB7B, 0xB5FB7A, 0xB6FB7A, 0xB7FB7A, 0xB8FB7A, 0xB9FB79, 0xB9FB79, 0xBAFB79, 0xBBFB79, 0xBCFB78, 0xBCFB78, 0xBDFB78, 0xBEFB78, 0xBFFB78, 0xBFFA77, 0xC0FA77, 0xC0FA77, 0xC1FA77, 0xC1FA77, 0xC2FA77, 0xC2FA77, 0xC3FA77, 0xC3FA77, 0xC4FA77, 0xC4FA77, 0xC4FA77, 0xC5FA77, 0xC5FA77, 0xC6FA77, 0xC7FB78, 0xC7FB78, 0xC8FB78, 0xC8FB78, 0xC9FB78, 0xCAFB79, 0xCAFB79, 0xCBFB79, 0xCCFB7A, 0xCDFB7A, 0xCEFB7A, 0xCEFB7A, 0xCFFB7B, 0xD0FB7B, 0xD1FB7B, 0xD2FB7B, 0xD3FB7C, 0xD3FA7C, 0xD4FA7C, 0xD5FA7C, 0xD6FA7C, 0xD7FA7C, 0xD7FA7C, 0xD8FA7C, 0xD9FA7D, 0xDAFA7D, 0xDAFA7D, 0xDBFA7D, 0xDCFA7E, 0xDDFA7E, 0xDDFA7E, 0xDEFA7F, 0xDFFB80, 0xDFFB80, 0xE0FB81, 0xE0FB82, 0xE1FB82, 0xE1FB83, 0xE2FB84, 0xE2FB85, 0xE3FB86, 0xE3FB87, 0xE4FB88, 0xE4FB89, 0xE5FB8A, 0xE5FB8B, 0xE6FB8C, 0xE6FB8E, 0xE7FB8F, 0xE7FA8F, 0xE8FA90, 0xE8FA91, 0xE9FA92, 0xE9FA93, 0xEAFA94, 0xEAFA95, 0xEBFA95, 0xEBFA96, 0xECFA97, 0xECFA97, 0xEDFA98, 0xEDFA99, 0xEEFB99, 0xEEFB9A, 0xEFFB9B, 0xEFFA9B, 0xEFFA9C, 0xF0FA9C, 0xF0FA9D, 0xF0FA9D, 0xF0FA9E, 0xF1FA9E, 0xF1FA9F, 0xF1FA9F, 0xF1FAA0, 0xF2FAA0, 0xF2FAA1, 0xF2FAA1, 0xF2FAA2, 0xF2FAA2, 0xF3FBA3, 0xF3FBA3, 0xF3FBA3, 0xF3FBA4, 0xF3FBA5, 0xF4FBA5, 0xF4FBA6, 0xF4FBA6, 0xF4FBA7, 0xF5FBA8, 0xF5FBA8, 0xF5FBA9, 0xF5FBAA, 0xF6FBAB, 0xF6FBAC, 0xF6FBAD, 0xF7FBAF, 0xF7FAB0, 0xF7FAB1, 0xF7FAB3, 0xF8FAB4, 0xF8FAB6, 0xF8FAB7, 0xF9FAB9, 0xF9FABA, 0xF9FABC, 0xF9FABE, 0xFAFABF, 0xFAFAC1, 0xFAFAC2, 0xFAFAC4, 0xFAFAC5, 0xFBFBC7, 0xFBFBC8, 0xFBFBC9, 0xFBFBCA, 0xFBFBCB, 0xFBFBCC, 0xFBFBCE, 0xFBFBCF, 0xFBFBD0, 0xFBFBD1, 0xFBFBD2, 0xFBFBD3, 0xFBFBD5, 0xFBFBD6, 0xFBFBD7, 0xFBFBD9, 0xFBFBDB, 0xFAFADC, 0xFAFADE, 0xFAFAE0, 0xFAFAE2, 0xFAFAE4, 0xFAFAE6, 0xFAFAE8, 0xFAFAEA, 0xFAFAED, 0xFAFAEF, 0xFAFAF1, 0xFAFAF3, 0xFAFAF5, 0xFAFAF7, 0xFAFAF9, 0xFBFBFB, 0xFBFBFC, 0xFBFBFE, 0xFBFBFF, 0xFBFBFF, 0xFBFBFF, 0xFBFBFF, 0xFBFBFF, 0xFBFBFF, 0xFCFCFF, 0xFCFCFF, 0xFCFCFF, 0xFCFCFF, 0xFDFDFF, 0xFDFDFF, 0xFDFDFF, 0xFEFEFE };

    private static FastLog log10 = null;

    public DecimatedSonaTrail(AudioTrail fullScale, int model) throws IOException {
        super();
        switch(model) {
            case MODEL_SONA:
                decimator = new SonaDecimator();
                break;
            default:
                throw new IllegalArgumentException("Model " + model);
        }
        this.fullScale = fullScale;
        this.model = model;
        constQ = new ConstQ();
        final Preferences cqPrefs = AbstractApplication.getApplication().getUserPrefs().node(PrefsUtil.NODE_VIEW).node(PrefsUtil.NODE_SONAGRAM);
        constQ.readPrefs(cqPrefs);
        constQ.setSampleRate(fullScale.getRate());
        System.out.println("Creating ConstQ Kernels...");
        constQ.createKernels();
        numKernels = constQ.getNumKernels();
        filterBuf = new float[numKernels];
        fftSize = constQ.getFFTSize();
        modelChannels = constQ.getNumKernels();
        fullChannels = fullScale.getChannelNum();
        decimChannels = fullChannels * modelChannels;
        System.out.println("...done.");
        stepSize = Math.max(64, Math.min(fftSize, MathUtil.nextPowerOfTwo((int) (constQ.getMaxTimeRes() / 1000 * fullScale.getRate() / Math.sqrt(2)))));
        int decimKorr, j;
        for (decimKorr = 1, j = stepSize; j > 2; decimKorr++, j >>= 1) ;
        final int decimations[] = { decimKorr };
        SUBNUM = decimations.length;
        this.decimHelps = new DecimationHelp[SUBNUM];
        for (int i = 0; i < SUBNUM; i++) {
            this.decimHelps[i] = new DecimationHelp(fullScale.getRate(), decimations[i]);
        }
        MAXSHIFT = decimations[0];
        MAXCOARSE = 1 << MAXSHIFT;
        MAXMASK = -MAXCOARSE;
        MAXCEILADD = MAXCOARSE - 1;
        if (log10 == null) {
            log10 = new FastLog(10, 11);
        }
        tmpBufSize = fftSize;
        tmpBufSize2 = 1 << decimations[0];
        for (int i = 1; i < SUBNUM; i++) {
            tmpBufSize2 = Math.max(tmpBufSize2, 1 << (decimations[i] - decimations[i - 1]));
        }
        setRate(fullScale.getRate());
        fullScale.addDependant(this);
        addAllDepAsync();
    }

    /**
	 * @synchronization must be called in the event thread
	 */
    public void drawWaveform(DecimationInfo info, WaveformView view, Graphics2D g2) {
        final int imgW = tmpBufSize2;
        final int maxLen = imgW << info.shift;
        final BufferedImage bufImg = new BufferedImage(imgW, modelChannels, BufferedImage.TYPE_INT_RGB);
        final WritableRaster raster = bufImg.getRaster();
        final int[] data = new int[imgW * modelChannels];
        final int dataStartOff = imgW * (modelChannels - 1);
        final Shape clipOrig = g2.getClip();
        long start = info.span.start;
        long totalLength = info.getTotalLength();
        Span chunkSpan;
        long fullLen;
        int chunkLen;
        float scaleX, ampLog;
        Rectangle r;
        final float pixScale = 10720 / (view.getAmpLogMax() - view.getAmpLogMin());
        final float pixOff = -view.getAmpLogMin() / 10;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        try {
            drawBusyList.clear();
            synchronized (bufSync) {
                createBuffers();
                long screenOffX = 0;
                while (totalLength > 0) {
                    fullLen = Math.min(maxLen, totalLength);
                    chunkLen = (int) (decimHelps[info.idx].fullrateToSubsample(fullLen));
                    fullLen = (long) chunkLen << info.shift;
                    chunkSpan = new Span(start, start + fullLen);
                    if (readFrames(info.idx, tmpBuf2, 0, drawBusyList, chunkSpan, null)) {
                        for (int ch = 0, tmpChReset = 0; ch < fullChannels; ch++, tmpChReset += modelChannels) {
                            r = view.rectForChannel(ch);
                            scaleX = (float) r.width / info.getTotalLength();
                            for (int x = 0, off = 0; x < chunkLen; x++) {
                                for (int y = 0, off2 = x + dataStartOff, tmpCh = tmpChReset; y < modelChannels; y++, tmpCh++, off2 -= imgW, off++) {
                                    ampLog = log10.calc(Math.max(1.0e-9f, tmpBuf2[tmpCh][x]));
                                    data[off2] = colors[Math.max(0, Math.min(1072, (int) ((ampLog + pixOff) * pixScale)))];
                                }
                            }
                            raster.setDataElements(0, 0, imgW, modelChannels, data);
                            g2.drawImage(bufImg, r.x + (int) (screenOffX * scaleX + 0.5f), r.y, r.x + (int) ((screenOffX + fullLen) * scaleX + 0.5f), r.y + r.height, 0, 0, chunkLen, modelChannels, view);
                        }
                    }
                    start += fullLen;
                    totalLength -= fullLen;
                    screenOffX += fullLen;
                }
            }
            for (int ch = 0; ch < fullChannels; ch++) {
                r = view.rectForChannel(ch);
                g2.clipRect(r.x, r.y, r.width, r.height);
                if (!drawBusyList.isEmpty()) {
                    g2.setPaint(pntBusy);
                    for (int i = 0; i < drawBusyList.size(); i++) {
                        chunkSpan = (Span) drawBusyList.get(i);
                        scaleX = (float) r.width / info.getTotalLength();
                        g2.fillRect((int) ((chunkSpan.start - info.span.start) * scaleX) + r.x, r.y, (int) (chunkSpan.getLength() * scaleX), r.height);
                    }
                }
                g2.setClip(clipOrig);
            }
        } catch (IOException e1) {
            System.err.println(e1);
        }
        bufImg.flush();
    }

    /**
	 * Determines which subsampled version is suitable for a given display range
	 * (the most RAM and CPU economic while maining optimal display resolution).
	 * For a given time span, the lowest resolution is chosen which will produce
	 * at least <code>minLen</code> frames.
	 * 
	 * @param tag the time span the caller is interested in
	 * @param minLen the minimum number of sampled points wanted.
	 * @return an information object describing the best subsample of the track
	 *         editor. note that info.sublength will be smaller than minLen if
	 *         tag.getLength() was smaller than minLen (in this case the
	 *         fullrate version is used).
	 */
    public DecimationInfo getBestSubsample(Span tag, int minLen) {
        final DecimationInfo info;
        final boolean fromPCM;
        final long fullLength = tag.getLength();
        long subLength, n;
        int idx, inlineDecim;
        subLength = fullLength;
        n = decimHelps[0].fullrateToSubsample(fullLength);
        subLength = n;
        idx = 0;
        switch(model) {
            case MODEL_SONA:
                inlineDecim = 1;
                break;
            default:
                assert false : model;
                inlineDecim = 1;
        }
        subLength /= inlineDecim;
        fromPCM = idx == -1;
        info = new DecimationInfo(tag, subLength, fullChannels, idx, fromPCM ? 0 : decimHelps[idx].shift, inlineDecim, model);
        return info;
    }

    private boolean readFrames(int sub, float[][] data, int dataOffset, List busyList, Span readSpan, AbstractCompoundEdit ce) throws IOException {
        int idx = editIndexOf(readSpan.start, true, ce);
        if (idx < 0) idx = -(idx + 2);
        final long startR = decimHelps[sub].roundAdd - readSpan.start;
        final List coll = editGetCollByStart(ce);
        final MutableInt readyLen = new MutableInt(0);
        final MutableInt busyLen = new MutableInt(0);
        boolean someReady = false;
        DecimatedStake stake;
        int chunkLen, discrepancy;
        Span subSpan;
        int readOffset, nextOffset = dataOffset;
        int len = (int) (readSpan.getLength() >> decimHelps[sub].shift);
        while ((len > 0) && (idx < coll.size())) {
            stake = (DecimatedStake) coll.get(idx);
            subSpan = new Span(Math.max(stake.getSpan().start, readSpan.start), Math.min(stake.getSpan().stop, readSpan.stop));
            stake.readFrames(sub, data, nextOffset, subSpan, readyLen, busyLen);
            chunkLen = readyLen.value() + busyLen.value();
            readOffset = nextOffset + readyLen.value();
            nextOffset = (int) ((subSpan.stop + startR) >> decimHelps[sub].shift) + dataOffset;
            discrepancy = nextOffset - readOffset;
            len -= readyLen.value() + discrepancy;
            if (readyLen.value() > 0) someReady = true;
            if (busyLen.value() == 0) {
                if (discrepancy > 0) {
                    if (readOffset > 0) {
                        for (int i = readOffset, k = readOffset - 1; i < nextOffset; i++) {
                            for (int j = 0; j < data.length; j++) {
                                data[j][i] = data[j][k];
                            }
                        }
                    }
                }
            } else {
                final Span busySpan = new Span(subSpan.stop - (subSpan.getLength() * busyLen.value() / chunkLen), subSpan.stop);
                final int busyLastIdx = busyList.size() - 1;
                if (busyLastIdx >= 0) {
                    final Span busySpan2 = (Span) busyList.get(busyLastIdx);
                    if (busySpan.touches(busySpan2)) {
                        busyList.set(busyLastIdx, busySpan.union(busySpan2));
                    } else {
                        busyList.add(busySpan);
                    }
                } else {
                    busyList.add(busySpan);
                }
                for (int i = Math.max(0, readOffset); i < nextOffset; i++) {
                    for (int j = 0; j < data.length; j++) {
                        data[j][i] = 0f;
                    }
                }
            }
            idx++;
        }
        return someReady;
    }

    public float getMinFreq() {
        return constQ.getMinFreq();
    }

    public float getMaxFreq() {
        return constQ.getMaxFreq();
    }

    public void debugDump() {
        for (int i = 0; i < getNumStakes(); i++) {
            ((DecimatedStake) get(i, true)).debugDump();
        }
    }

    private void addAllDepAsync() throws IOException {
        if (threadAsync != null) throw new IllegalStateException();
        final List stakes = fullScale.getAll(true);
        if (stakes.isEmpty()) return;
        final DecimatedStake das;
        final Span union = fullScale.getSpan();
        final Span extSpan;
        final long fullrateStop, fullrateLen;
        final int numFullBuf;
        final AbstractCompoundEdit ce = null;
        final Object source = null;
        synchronized (fileSync) {
            das = allocAsync(union);
        }
        extSpan = das.getSpan();
        fullrateStop = Math.min(extSpan.getStop(), fullScale.editGetSpan(ce).stop);
        fullrateLen = fullrateStop - extSpan.getStart();
        numFullBuf = (int) ((fullrateLen - fftSize + stepSize + stepSize - 1) / stepSize);
        synchronized (bufSync) {
            createBuffers();
        }
        editClear(source, das.getSpan(), ce);
        editAdd(source, das, ce);
        threadAsync = new Thread(new Runnable() {

            public void run() {
                final int pri = Thread.currentThread().getPriority();
                Thread.currentThread().setPriority(pri - 2);
                long pos;
                long framesWrittenCache = 0;
                Span tag2;
                int len;
                long time;
                int inBufOff = 0, nextLen = fftSize >> 1;
                long nextTime = System.currentTimeMillis() + UPDATE_PERIOD;
                pos = extSpan.getStart();
                try {
                    for (int i = 0; (i < numFullBuf) && keepAsyncRunning; i++) {
                        synchronized (bufSync) {
                            len = (int) Math.min(nextLen, fullrateStop - pos);
                            tag2 = new Span(pos, pos + len);
                            fullScale.readFrames(tmpBuf, fftSize - nextLen, tag2, null);
                            if (len < nextLen) {
                                for (int ch = 0; ch < fullChannels; ch++) {
                                    for (int j = (inBufOff + len) % tmpBufSize, k = nextLen - len; k >= 0; k--) {
                                        tmpBuf[ch][j] = 0f;
                                        if (++j == tmpBufSize) j = 0;
                                    }
                                }
                            }
                            decimator.decimatePCM(tmpBuf, tmpBuf2, 0, 1, 1);
                            das.continueWrite(0, tmpBuf2, 0, 1);
                            framesWrittenCache++;
                            pos += nextLen;
                            inBufOff = (inBufOff + nextLen) % tmpBufSize;
                            nextLen = stepSize;
                            for (int ch = 0; ch < fullChannels; ch++) {
                                System.arraycopy(tmpBuf[ch], nextLen, tmpBuf[ch], 0, fftSize - nextLen);
                            }
                        }
                        time = System.currentTimeMillis();
                        if (time >= nextTime) {
                            nextTime = time + UPDATE_PERIOD;
                            if (asyncManager != null) {
                                asyncManager.dispatchEvent(new AsyncEvent(DecimatedSonaTrail.this, AsyncEvent.UPDATE, time, DecimatedSonaTrail.this));
                            }
                        }
                    }
                    if (keepAsyncRunning) {
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                } finally {
                    if (asyncManager != null) {
                        asyncManager.dispatchEvent(new AsyncEvent(DecimatedSonaTrail.this, AsyncEvent.FINISHED, System.currentTimeMillis(), DecimatedSonaTrail.this));
                    }
                    synchronized (threadAsync) {
                        threadAsync.notifyAll();
                    }
                }
            }
        });
        keepAsyncRunning = true;
        threadAsync.start();
    }

    protected void addAllDep(Object source, List stakes, AbstractCompoundEdit ce, Span union) throws IOException {
        if (DEBUG) System.err.println("addAllDep " + union.toString());
        final DecimatedStake das;
        final Span extSpan;
        final long fullrateStop, fullrateLen;
        final int numFullBuf;
        final double progWeight;
        int inBufOff = 0, nextLen = fftSize >> 1;
        long pos;
        long framesWritten = 0;
        Span tag2;
        int len;
        synchronized (fileSync) {
            das = alloc(union);
        }
        extSpan = das.getSpan();
        pos = extSpan.start;
        fullrateStop = Math.min(extSpan.stop, fullScale.editGetSpan(ce).stop);
        fullrateLen = fullrateStop - extSpan.start;
        progWeight = 1.0 / fullrateLen;
        numFullBuf = (int) ((fullrateLen - fftSize + stepSize + stepSize - 1) / stepSize);
        synchronized (bufSync) {
            flushProgression();
            createBuffers();
            for (int i = 0; i < numFullBuf; i++) {
                len = (int) Math.min(nextLen, fullrateStop - pos);
                tag2 = new Span(pos, pos + len);
                fullScale.readFrames(tmpBuf, fftSize - nextLen, tag2, null);
                if (len < nextLen) {
                    for (int ch = 0; ch < fullChannels; ch++) {
                        for (int j = (inBufOff + len) % tmpBufSize, k = nextLen - len; k >= 0; k--) {
                            tmpBuf[ch][j] = 0f;
                            if (++j == tmpBufSize) j = 0;
                        }
                    }
                }
                decimator.decimatePCM(tmpBuf, tmpBuf2, 0, 1, 1);
                das.continueWrite(0, tmpBuf2, 0, 1);
                pos += nextLen;
                inBufOff = (inBufOff + nextLen) % tmpBufSize;
                nextLen = stepSize;
                for (int ch = 0; ch < fullChannels; ch++) {
                    System.arraycopy(tmpBuf[ch], nextLen, tmpBuf[ch], 0, fftSize - nextLen);
                }
                setProgression(framesWritten, progWeight);
            }
        }
        editClear(source, das.getSpan(), ce);
        editAdd(source, das, ce);
    }

    protected DecimatedStake allocAsync(Span span) throws IOException {
        if (!Thread.holdsLock(fileSync)) throw new IllegalMonitorStateException();
        final long floorStart = span.start / stepSize * stepSize;
        final long ceilStop = (span.stop + stepSize - 1) / stepSize * stepSize;
        final Span extSpan = (floorStart == span.start) && (ceilStop == span.stop) ? span : new Span(floorStart, ceilStop);
        final Span[] fileSpans = new Span[SUBNUM];
        final Span[] biasedSpans = new Span[SUBNUM];
        long fileStart;
        long fileStop;
        if (tempFAsync == null) {
            tempFAsync = createTempFiles();
        }
        synchronized (tempFAsync) {
            for (int i = 0; i < SUBNUM; i++) {
                fileStart = tempFAsync[i].getFrameNum();
                fileStop = fileStart + (extSpan.getLength() >> decimHelps[i].shift);
                tempFAsync[i].setFrameNum(fileStop);
                fileSpans[i] = new Span(fileStart, fileStop);
                biasedSpans[i] = extSpan;
            }
        }
        return new DecimatedStake(extSpan, tempFAsync, fileSpans, biasedSpans, decimHelps);
    }

    protected File[] createCacheFileNames() {
        final AudioFile[] audioFiles = fullScale.getAudioFiles();
        if ((audioFiles.length == 0) || (audioFiles[0] == null)) return null;
        final CacheManager cm = PrefCacheManager.getInstance();
        if (!cm.isActive()) return null;
        final File[] f = new File[audioFiles.length];
        for (int i = 0; i < f.length; i++) {
            f[i] = cm.createCacheFileName(IOUtil.setFileSuffix(audioFiles[i].getFile(), "fft"));
        }
        return f;
    }

    protected void subsampleWrite(float[][] inBuf, float[][] outBuf, DecimatedStake das, int len, AudioStake cacheAS, long cacheOff) throws IOException {
        int decim;
        if (SUBNUM < 1) return;
        decim = decimHelps[0].shift;
        assert len % fftSize == 0;
        len = (len / stepSize * modelChannels) >> decim;
        if (inBuf != null) {
            decimator.decimatePCM(inBuf, outBuf, 0, len, 1 << decim);
            das.continueWrite(0, outBuf, 0, len);
            if (cacheAS != null) {
                cacheAS.writeFrames(outBuf, 0, new Span(cacheOff, cacheOff + len));
            }
        }
        subsampleWrite2(outBuf, das, len);
    }

    private void subsampleWrite2(float[][] buf, DecimatedStake das, int len) throws IOException {
        int decim;
        for (int i = 1; i < SUBNUM; i++) {
            decim = decimHelps[i].shift - decimHelps[i - 1].shift;
            len >>= decim;
            decimator.decimate(buf, buf, 0, len, 1 << decim);
            das.continueWrite(i, buf, 0, len);
        }
    }

    private abstract class Decimator {

        protected Decimator() {
        }

        protected abstract void decimate(float[][] inBuf, float[][] outBuf, int outOff, int len, int decim);

        protected abstract void decimatePCM(float[][] inBuf, float[][] outBuf, int outOff, int len, int decim);

        protected abstract int draw(DecimationInfo info, int ch, int[][] peakPolyX, int[][] peakPolyY, int[][] rmsPolyX, int[][] rmsPolyY, int decimLen, Rectangle r, float deltaYN, int off);
    }

    private class SonaDecimator extends Decimator {

        protected SonaDecimator() {
        }

        protected void decimate(float[][] inBuf, float[][] outBuf, int outOff, int len, int decim) {
            int stop, j, k, m, ch;
            float f1;
            float[] inBufCh, outBufCh;
            for (ch = 0; ch < fullChannels; ch++) {
                inBufCh = inBuf[ch];
                outBufCh = outBuf[ch];
                for (j = outOff, stop = outOff + len, k = 0; j < stop; j++) {
                    f1 = inBufCh[k];
                    for (m = k + decim, k++; k < m; k++) {
                        f1 += inBufCh[k];
                    }
                    outBufCh[j] = f1 / decim;
                }
            }
        }

        protected void decimatePCM(float[][] inBuf, float[][] outBuf, int outOff, int len, int decim) {
            final float w = 1.0f / decim;
            for (int inOff = 0, stop = outOff + len, decimCnt = 0; outOff < stop; inOff += stepSize) {
                for (int ch = 0, outChanOff = 0; ch < fullChannels; ch++) {
                    constQ.transform(inBuf[ch], inOff, fftSize, filterBuf, 0);
                    if (decimCnt == 0) {
                        for (int i = 0; i < numKernels; i++) {
                            outBuf[outChanOff++][outOff] = filterBuf[i] * w;
                        }
                    } else {
                        for (int i = 0; i < numKernels; i++) {
                            outBuf[outChanOff++][outOff] += filterBuf[i] * w;
                        }
                    }
                }
                decimCnt = (decimCnt + 1) % decim;
                if (decimCnt == 0) outOff++;
            }
        }

        protected int draw(DecimationInfo info, int ch, int[][] peakPolyX, int[][] peakPolyY, int[][] rmsPolyX, int[][] rmsPolyY, int decimLen, Rectangle r, float deltaYN, int off) {
            int ch2;
            float[] sPeakP, sPeakN, sRMSP;
            float offX, scaleX, scaleY;
            ch2 = ch * 3;
            sPeakP = tmpBuf[ch2++];
            sPeakN = tmpBuf[ch2++];
            sRMSP = tmpBuf[ch2];
            scaleX = 4 * r.width / (float) (info.sublength - 1);
            scaleY = r.height * deltaYN;
            offX = scaleX * off;
            return drawFullWavePeakRMS(sPeakP, sPeakN, sRMSP, decimLen, peakPolyX[ch], peakPolyY[ch], rmsPolyX[ch], rmsPolyY[ch], off, offX, scaleX, scaleY);
        }

        private int drawFullWavePeakRMS(float[] sPeakP, float[] sPeakN, float[] sRMS, int len, int[] peakPolyX, int[] peakPolyY, int[] rmsPolyX, int[] rmsPolyY, int off, float offX, float scaleX, float scaleY) {
            int x;
            float peakP, peakN, rms;
            for (int i = 0, k = peakPolyX.length - 1 - off; i < len; i++, off++, k--) {
                x = (int) (i * scaleX + offX);
                peakPolyX[off] = x;
                peakPolyX[k] = x;
                rmsPolyX[off] = x;
                rmsPolyX[k] = x;
                peakP = sPeakP[i];
                peakN = sPeakN[i];
                peakPolyY[off] = (int) (peakP * scaleY) + 2;
                peakPolyY[k] = (int) (peakN * scaleY) - 2;
                rms = (float) Math.sqrt(sRMS[i]);
                rmsPolyY[off] = (int) (Math.min(peakP, rms) * scaleY);
                rmsPolyY[k] = (int) (Math.max(peakN, -rms) * scaleY);
            }
            return off;
        }
    }
}
