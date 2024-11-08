package Stemmer;

import java.io.*;

public class Stemmer {

    private char[] b;

    private int i, k, i_end, i_beg, j, l;

    private static final int INC = 50;

    private static final char[][] UNICODECHARS = { { 'ሀ', 'ሁ', 'ሂ', 'ሃ', 'ሄ', 'ህ', 'ሆ' }, { 'ለ', 'ሉ', 'ሊ', 'ላ', 'ሌ', 'ል', 'ሎ' }, { 'ሐ', 'ሑ', 'ሒ', 'ሓ', 'ሔ', 'ሕ', 'ሖ' }, { 'መ', 'ሙ', 'ሚ', 'ማ', 'ሜ', 'ም', 'ሞ' }, { 'ሠ', 'ሡ', 'ሢ', 'ሣ', 'ሤ', 'ሥ', 'ሦ' }, { 'ረ', 'ሩ', 'ሪ', 'ራ', 'ሬ', 'ር', 'ሮ' }, { 'ሰ', 'ሱ', 'ሲ', 'ሳ', 'ሴ', 'ስ', 'ሶ' }, { 'ሸ', 'ሹ', 'ሺ', 'ሻ', 'ሼ', 'ሽ', 'ሾ' }, { 'ቀ', 'ቁ', 'ቂ', 'ቃ', 'ቄ', 'ቅ', 'ቆ' }, { 'በ', 'ቡ', 'ቢ', 'ባ', 'ቤ', 'ብ', 'ቦ' }, { 'ቨ', 'ቩ', 'ቪ', 'ቫ', 'ቬ', 'ቭ', 'ቮ' }, { 'ተ', 'ቱ', 'ቲ', 'ታ', 'ቴ', 'ት', 'ቶ' }, { 'ቸ', 'ቹ', 'ቺ', 'ቻ', 'ቼ', 'ች', 'ቾ' }, { 'ኀ', 'ኁ', 'ኂ', 'ኃ', 'ኄ', 'ኅ', 'ኆ' }, { 'ነ', 'ኑ', 'ኒ', 'ና', 'ኔ', 'ን', 'ኖ' }, { 'ኘ', 'ኙ', 'ኚ', 'ኛ', 'ኜ', 'ኝ', 'ኞ' }, { 'አ', 'ኡ', 'ኢ', 'ኣ', 'ኤ', 'እ', 'ኦ' }, { 'ከ', 'ኩ', 'ኪ', 'ካ', 'ኬ', 'ክ', 'ኮ' }, { 'ኸ', 'ኹ', 'ኺ', 'ኻ', 'ኼ', 'ኽ', 'ኾ' }, { 'ወ', 'ዉ', 'ዊ', 'ዋ', 'ዌ', 'ው', 'ዎ' }, { 'ዐ', 'ዑ', 'ዒ', 'ዓ', 'ዔ', 'ዕ', 'ዖ' }, { 'ዘ', 'ዙ', 'ዚ', 'ዛ', 'ዜ', 'ዝ', 'ዞ' }, { 'ዠ', 'ዡ', 'ዢ', 'ዣ', 'ዤ', 'ዥ', 'ዦ' }, { 'የ', 'ዩ', 'ዪ', 'ያ', 'ዬ', 'ይ', 'ዮ' }, { 'ደ', 'ዱ', 'ዲ', 'ዳ', 'ዴ', 'ድ', 'ዶ' }, { 'ዸ', 'ዹ', 'ዺ', 'ዻ', 'ዼ', 'ዽ', 'ዾ' }, { 'ጀ', 'ጁ', 'ጂ', 'ጃ', 'ጄ', 'ጅ', 'ጆ' }, { 'ገ', 'ጉ', 'ጊ', 'ጋ', 'ጌ', 'ግ', 'ጎ' }, { 'ጠ', 'ጡ', 'ጢ', 'ጣ', 'ጤ', 'ጥ', 'ጦ' }, { 'ጨ', 'ጩ', 'ጪ', 'ጫ', 'ጬ', 'ጭ', 'ጮ' }, { 'ፀ', 'ፁ', 'ፂ', 'ፃ', 'ፄ', 'ፅ', 'ፆ' }, { 'ፈ', 'ፉ', 'ፊ', 'ፋ', 'ፌ', 'ፍ', 'ፎ' }, { 'ፐ', 'ፑ', 'ፒ', 'ፓ', 'ፔ', 'ፕ', 'ፖ' } };

    private static char HA = 'ሀ';

    private static char HU = 'ሁ';

    private static char HI = 'ሂ';

    private static char HAA = 'ሃ';

    private static char HEE = 'ሄ';

    private static char HE = 'ህ';

    private static char HO = 'ሆ';

    private static char LA = 'ለ';

    private static char LU = 'ሉ';

    private static char LI = 'ሊ';

    private static char LAA = 'ላ';

    private static char LEE = 'ሌ';

    private static char LE = 'ል';

    private static char LO = 'ሎ';

    private static char HHA = 'ሐ';

    private static char HHU = 'ሑ';

    private static char HHI = 'ሒ';

    private static char HHAA = 'ሓ';

    private static char HHEE = 'ሔ';

    private static char HHE = 'ሕ';

    private static char HHO = 'ሖ';

    private static char MA = 'መ';

    private static char MU = 'ሙ';

    private static char MI = 'ሚ';

    private static char MAA = 'ማ';

    private static char MEE = 'ሜ';

    private static char ME = 'ም';

    private static char MO = 'ሞ';

    private static char SZA = 'ሠ';

    private static char SZU = 'ሡ';

    private static char SZI = 'ሢ';

    private static char SZAA = 'ሣ';

    private static char SZEE = 'ሤ';

    private static char SZE = 'ሥ';

    private static char SZO = 'ሦ';

    private static char RA = 'ረ';

    private static char RU = 'ሩ';

    private static char RI = 'ሪ';

    private static char RAA = 'ራ';

    private static char REE = 'ሬ';

    private static char RE = 'ር';

    private static char RO = 'ሮ';

    private static char SA = 'ሰ';

    private static char SU = 'ሱ';

    private static char SI = 'ሲ';

    private static char SAA = 'ሳ';

    private static char SEE = 'ሴ';

    private static char SE = 'ስ';

    private static char SO = 'ሶ';

    private static char SHA = 'ሸ';

    private static char SHU = 'ሹ';

    private static char SHI = 'ሺ';

    private static char SHAA = 'ሻ';

    private static char SHEE = 'ሼ';

    private static char SHE = 'ሽ';

    private static char SHO = 'ሾ';

    private static char QA = 'ቀ';

    private static char QU = 'ቁ';

    private static char QI = 'ቂ';

    private static char QAA = 'ቃ';

    private static char QEE = 'ቄ';

    private static char QE = 'ቅ';

    private static char QO = 'ቆ';

    private static char BA = 'በ';

    private static char BU = 'ቡ';

    private static char BI = 'ቢ';

    private static char BAA = 'ባ';

    private static char BEE = 'ቤ';

    private static char BE = 'ብ';

    private static char BO = 'ቦ';

    private static char VA = 'ቨ';

    private static char VU = 'ቩ';

    private static char VI = 'ቪ';

    private static char VAA = 'ቫ';

    private static char VEE = 'ቬ';

    private static char VE = 'ቭ';

    private static char VO = 'ቮ';

    private static char TA = 'ተ';

    private static char TU = 'ቱ';

    private static char TI = 'ቲ';

    private static char TAA = 'ታ';

    private static char TEE = 'ቴ';

    private static char TE = 'ት';

    private static char TO = 'ቶ';

    private static char CA = 'ቸ';

    private static char CU = 'ቹ';

    private static char CI = 'ቺ';

    private static char CAA = 'ቻ';

    private static char CEE = 'ቼ';

    private static char CE = 'ች';

    private static char CO = 'ቾ';

    private static char XA = 'ኀ';

    private static char XU = 'ኁ';

    private static char XI = 'ኂ';

    private static char XAA = 'ኃ';

    private static char XEE = 'ኄ';

    private static char XE = 'ኅ';

    private static char XWAA = 'ኋ';

    private static char XO = 'ኆ';

    private static char NA = 'ነ';

    private static char NU = 'ኑ';

    private static char NI = 'ኒ';

    private static char NAA = 'ና';

    private static char NEE = 'ኔ';

    private static char NE = 'ን';

    private static char NO = 'ኖ';

    private static char NYA = 'ኘ';

    private static char NYU = 'ኙ';

    private static char NYI = 'ኚ';

    private static char NYAA = 'ኛ';

    private static char NYEE = 'ኜ';

    private static char NYE = 'ኝ';

    private static char NYO = 'ኞ';

    private static char A = 'አ';

    private static char U = 'ኡ';

    private static char I = 'ኢ';

    private static char AA = 'ኣ';

    private static char EE = 'ኤ';

    private static char E = 'እ';

    private static char O = 'ኦ';

    private static char KA = 'ከ';

    private static char KU = 'ኩ';

    private static char KI = 'ኪ';

    private static char KAA = 'ካ';

    private static char KEE = 'ኬ';

    private static char KE = 'ክ';

    private static char KO = 'ኮ';

    private static char KXA = 'ኸ';

    private static char KXU = 'ኹ';

    private static char KXI = 'ኺ';

    private static char KXAA = 'ኻ';

    private static char KXEE = 'ኼ';

    private static char KXE = 'ኽ';

    private static char KXO = 'ኾ';

    private static char WA = 'ወ';

    private static char WU = 'ዉ';

    private static char WI = 'ዊ';

    private static char WAA = 'ዋ';

    private static char WEE = 'ዌ';

    private static char WE = 'ው';

    private static char WO = 'ዎ';

    private static char A2 = 'ዐ';

    private static char U2 = 'ዑ';

    private static char I2 = 'ዒ';

    private static char AA2 = 'ዓ';

    private static char EE2 = 'ዔ';

    private static char E2 = 'ዕ';

    private static char O2 = 'ዖ';

    private static char ZA = 'ዘ';

    private static char ZU = 'ዙ';

    private static char ZI = 'ዚ';

    private static char ZAA = 'ዛ';

    private static char ZEE = 'ዜ';

    private static char ZE = 'ዝ';

    private static char ZO = 'ዞ';

    private static char ZHA = 'ዠ';

    private static char ZHU = 'ዡ';

    private static char ZHI = 'ዢ';

    private static char ZHAA = 'ዣ';

    private static char ZHEE = 'ዤ';

    private static char ZHE = 'ዥ';

    private static char ZHO = 'ዦ';

    private static char YA = 'የ';

    private static char YU = 'ዩ';

    private static char YI = 'ዪ';

    private static char YAA = 'ያ';

    private static char YEE = 'ዬ';

    private static char YE = 'ይ';

    private static char YO = 'ዮ';

    private static char DA = 'ደ';

    private static char DU = 'ዱ';

    private static char DI = 'ዲ';

    private static char DAA = 'ዳ';

    private static char DEE = 'ዴ';

    private static char DE = 'ድ';

    private static char DO = 'ዶ';

    private static char JA = 'ጀ';

    private static char JU = 'ጁ';

    private static char JI = 'ጂ';

    private static char JAA = 'ጃ';

    private static char JEE = 'ጄ';

    private static char JE = 'ጅ';

    private static char JO = 'ጆ';

    private static char GA = 'ገ';

    private static char GU = 'ጉ';

    private static char GI = 'ጊ';

    private static char GAA = 'ጋ';

    private static char GEE = 'ጌ';

    private static char GE = 'ግ';

    private static char GO = 'ጎ';

    private static char THA = 'ጠ';

    private static char THU = 'ጡ';

    private static char THI = 'ጢ';

    private static char THAA = 'ጣ';

    private static char THEE = 'ጤ';

    private static char THE = 'ጥ';

    private static char THO = 'ጦ';

    private static char CHA = 'ጨ';

    private static char CHU = 'ጩ';

    private static char CHI = 'ጪ';

    private static char CHAA = 'ጫ';

    private static char CHEE = 'ጬ';

    private static char CHE = 'ጭ';

    private static char CHO = 'ጮ';

    private static char TZA = 'ፀ';

    private static char TZU = 'ፁ';

    private static char TZI = 'ፂ';

    private static char TZAA = 'ፃ';

    private static char TZEE = 'ፄ';

    private static char TZE = 'ፅ';

    private static char TZO = 'ፆ';

    private static char FA = 'ፈ';

    private static char FU = 'ፉ';

    private static char FI = 'ፊ';

    private static char FAA = 'ፋ';

    private static char FEE = 'ፌ';

    private static char FE = 'ፍ';

    private static char FO = 'ፎ';

    private static char PA = 'ፐ';

    private static char PU = 'ፑ';

    private static char PI = 'ፒ';

    private static char PAA = 'ፓ';

    private static char PEE = 'ፔ';

    private static char PE = 'ፕ';

    private static char PO = 'ፖ';

    public Stemmer() {
        b = new char[INC];
        i = 0;
        i_end = 0;
        l = 0;
    }

    /**
    * Add a character to the word being stemmed.  When you are finished
    * adding characters, you can call stem(void) to stem the word.
    */
    public void add(char ch) {
        if (i == b.length) {
            char[] new_b = new char[i + INC];
            for (int c = 0; c < i; c++) new_b[c] = b[c];
            b = new_b;
        }
        b[i++] = ch;
    }

    /** Adds wLen characters to the word being stemmed contained in a portion
    * of a char[] array. This is like repeated calls of add(char ch), but
    * faster.
    */
    public void add(char[] w, int wLen) {
        if (i + wLen >= b.length) {
            char[] new_b = new char[i + wLen + INC];
            for (int c = 0; c < i; c++) new_b[c] = b[c];
            b = new_b;
        }
        for (int c = 0; c < wLen; c++) b[i++] = w[c];
    }

    /**
    * After a word has been stemmed, it can be retrieved by toString(),
    * or a reference to the internal buffer can be retrieved by getResultBuffer
    * and getResultLength (which is generally more efficient.)
    */
    public String toString() {
        return new String(b, i_beg, i_end);
    }

    /**
    * Returns the length of the word resulting from the stemming process.
    */
    public int getResultLength() {
        return i_end - i_beg;
    }

    /**
    * Returns a reference to a character buffer containing the results of
    * the stemming process.  You also need to consult getResultLength()
    * to determine the length of the result.
    */
    public char[] getResultBuffer() {
        return b;
    }

    /**
    * Returns a true if a true if the word to be stemmed ends with specified
    * suffix otherwise false.
    */
    public boolean ends(String strsuffix) {
        char[] suffix = strsuffix.toCharArray();
        for (int i = 0; i < suffix.length; i++) {
            if (b[k - suffix.length + i] != suffix[i]) return false;
        }
        return true;
    }

    /**
    * Returns a true if a true if the word to be stemmed begins with specified
    * prefix otherwise false.
    */
    public boolean begins(char[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (b[l + i] != prefix[i]) return false;
        }
        return true;
    }

    public void step1() {
        if (b[k] == NAA) {
            k -= 1;
        }
    }

    public void step2() {
        try {
            if (k > 6 && b[k - 4] == A && b[k - 3] == CE && b[k - 2] == XWAA && b[k - 1] == LA && b[k] == HU) {
                k -= 5;
            } else if (k > 6 && b[k - 4] == A && b[k - 3] == CA && b[k - 2] == WAA && b[k - 1] == LA && b[k] == HU) {
                k -= 5;
            } else if (k > 6 && b[k - 4] == A && b[k - 3] == CA && b[k - 2] == WAA && b[k - 1] == LA && b[k] == SHE) {
                k -= 5;
            } else if (k > 5 && b[k - 3] == CE && b[k - 2] == HU && b[k - 1] == A && b[k] == TE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == WAA && b[k - 2] == CE && b[k - 1] == XWAA && b[k] == LE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == WAA && b[k - 2] == CA && b[k - 1] == WAA && b[k] == LE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == CA && b[k - 2] == WAA && b[k - 1] == LA && b[k] == CE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == A && b[k - 2] == TAA && b[k - 1] == LA && b[k] == HU) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == A && b[k - 2] == TAA && b[k - 1] == LA && b[k] == SHE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == HAA && b[k - 2] == CA && b[k - 1] == WAA && b[k] == LE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == CA && b[k - 2] == WAA && b[k - 1] == LA && b[k] == HE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == SHAA && b[k - 2] == A && b[k - 1] == CA && b[k] == WE) {
                k -= 4;
            } else if (k > 5 && b[k - 3] == SHAA && b[k - 2] == CA && b[k - 1] == WAA && b[k] == LE) {
                k -= 4;
            } else if (k > 3 && b[k - 2] == SHE && b[k - 1] == NYAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHE && b[k - 1] == WAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHAA && b[k - 1] == TAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHE && b[k - 1] == NAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHAA && b[k - 1] == CA && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NYAA && b[k - 1] == LA && b[k] == SHE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == LA && b[k] == SHE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == LA && b[k] == SHE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == XWAA && b[k - 1] == CHA && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == KXA && b[k - 1] == NYAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == KXA && b[k - 1] == WAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == HAA && b[k - 1] == TA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == KXA && b[k - 1] == NAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NYAA && b[k - 1] == LA && b[k] == HE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == LA && b[k] == HE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == TAA && b[k - 1] == LA && b[k] == HE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == LA && b[k] == HE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == XWAA && b[k - 1] == CE && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == XWAA && b[k - 1] == CA && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == HAA && b[k - 1] == LA && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHAA && b[k - 1] == LA && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == LA && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == CAA && b[k - 1] == CE && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == CAA && b[k - 1] == CE && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == CE && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == CA && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == CE && b[k - 1] == HU && b[k] == NYE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == CE && b[k - 1] == HU && b[k] == TE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == CE && b[k - 1] == HU && b[k] == NE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NYA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == HAA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == SHAA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == TAA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == LA && b[k] == CE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NE && b[k - 1] == HAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NE && b[k - 1] == SHAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NA && b[k - 1] == WAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == NAA && b[k - 1] == TAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == NYAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == HAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == SHAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == TAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == TAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == NAA && b[k] == LE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == CE && b[k] == HU) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WAA && b[k - 1] == CE && b[k] == WE) {
                k -= 3;
            } else if (k > 3 && b[k - 2] == WE && b[k - 1] == YAA && b[k] == NE) {
                k -= 3;
            } else if (k > 2 && b[k - 1] == SHE && b[k] == NYE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == SHE && b[k] == WE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == SHAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == SHE && b[k] == NE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NYAA && b[k] == LE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == HAA && b[k] == LE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == SHAA && b[k] == LE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == TAA && b[k] == LE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NAA && b[k] == LE) {
                k -= 2;
            } else if (b[k - 1] == SHAA && b[k] == LE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == HU) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CA && b[k] == HU) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == HU) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == KXA && b[k] == NYE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == KXA && b[k] == WE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == HAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == KXA && b[k] == NE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == HU && b[k] == HE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == HU && b[k] == SHE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == HU && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == XWAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == NYE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == HE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == SHE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == WE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == CE && b[k] == NE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NE && b[k] == HE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NE && b[k] == SHE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NA && b[k] == WE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WE && b[k] == NYE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WE && b[k] == HE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WE && b[k] == SHE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WE && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == WO && b[k] == CE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == YAA && b[k] == NE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == YAA && b[k] == TE) {
                k -= 2;
            } else if (k > 2 && b[k - 1] == NA && b[k] == TE) {
                k -= 2;
            } else if (k > 1 && b[k] == NYE) {
                k -= 1;
            } else if (k > 1 && b[k] == HE) {
                k -= 1;
            } else if (k > 1 && b[k] == SHE) {
                k -= 1;
            } else if (b[k] == WE) {
                k -= 1;
            } else if (b[k] == NE) {
                k -= 1;
            } else if (k > 1 && b[k] == WI) {
                k -= 1;
            } else if (k > 1 && b[k] == CE) {
                k -= 1;
            } else if (k > 1 && b[k] == LE) {
                k -= 1;
            } else if (b[k] == TE) {
                k -= 1;
            } else if (b[k] == ME) {
                k -= 1;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Array exception");
        }
    }

    public void step3() {
        if (b[l] == E && b[l + 1] == NE && b[l + 2] == DE) {
            l += 3;
        } else if (b[l] == E && b[l + 1] == NE && b[l + 2] == DA) {
            l += 3;
        } else if (b[l] == E && b[l + 1] == NE && b[l + 2] == DI) {
            l += 3;
        } else if (b[l] == YA && b[l + 1] == MAA && b[l + 2] == YE) {
            l += 3;
        } else if (b[l] == E && b[l + 1] == NE) {
            l += 2;
        } else if (b[l] == E && b[l + 1] == NA) {
            l += 2;
        } else if (b[l] == E && b[l + 1] == YA) {
            l += 2;
        } else if (b[l] == A && b[l + 1] == LE) {
            l += 2;
        } else if (b[l] == YA && b[l + 1] == MI) {
            l += 2;
        } else if (b[l] == YA && b[l + 1] == ME) {
            l += 2;
        } else if (b[l] == A && b[l + 1] == YE) {
            l += 2;
        } else if (b[l] == A && b[l + 1] == SE) {
            l += 2;
        } else if (b[l] == BA && b[l + 1] == MA) {
            l += 2;
        } else if (b[l] == YA && b[l + 1] == TA) {
            l += 2;
        } else if (b[l] == E) {
            l += 1;
        } else if (b[l] == LE) {
            l += 1;
        } else if (b[l] == TE) {
            l += 1;
        } else if (b[l] == YE) {
            l += 1;
        } else if (b[l] == BE) {
            l += 1;
        } else if (b[l] == BA) {
            l += 1;
        } else if (b[l] == BI) {
            l += 1;
        } else if (b[l] == MA) {
            l += 1;
        } else if (b[l] == KA) {
            l += 1;
        } else if (b[l] == LA) {
            l += 1;
        } else if (b[l] == YAA) {
            l += 1;
        } else if (b[l] == YA) {
            l += 1;
        } else if (b[l] == LE) {
            l += 1;
        }
    }

    public void step4() {
        boolean found;
        for (int w = l; w <= k; w++) {
            found = false;
            for (i = 0; i < UNICODECHARS.length; i++) {
                for (j = 0; j < UNICODECHARS[i].length; j++) {
                    if (b[w] == UNICODECHARS[i][j]) {
                        b[w] = UNICODECHARS[i][5];
                        found = true;
                        break;
                    }
                    if (found) break;
                }
            }
        }
    }

    public void step5() {
        int len = k - l + 1;
        boolean repeated = true;
        for (int i = l; i < k; i++) {
            if (b[i] == b[i + 1]) {
                for (int j = i + 1; j < k; j++) b[j] = b[j + 1];
                k--;
                break;
            }
        }
    }

    public void stem() {
        k = i - 1;
        l = 0;
        if (k > 1) {
            step1();
            step2();
            step3();
            step4();
            step5();
        }
        i_end = k + 1 - l;
        i = 0;
        i_beg = l;
    }

    public String stem(String w) {
        return w;
    }
}
