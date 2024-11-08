package org.axsl.fontR;

/**
 * <p>Class containing font-related utility methods.</p>
 * <p>References to CSS2 are to the Cascading Style Sheet standard, version 2.0,
 * found at:</p>
 * <p><code>http://www.w3.org/TR/REC-CSS2/</code></p>
 * <p>References to XSL-FO refer to the eXtensible Stylesheet Language
 * Formatting Objects standard, version 1.0, found at:</p>
 * <p><code>http://www.w3.org/TR/2001/REC-xsl-20011015/</code></p>
 */
public final class FontUtility {

    /** String value for invalid input. */
    public static final String INPUT_INVALID = "invalid";

    /** String value for input "normal". */
    public static final String INPUT_NORMAL = "normal";

    /** String value for input "italic". */
    public static final String INPUT_ITALIC = "italic";

    /** String value for input "oblique". */
    public static final String INPUT_OBLIQUE = "oblique";

    /** String value for input "backslant". */
    public static final String INPUT_BACKSLANT = "backslant";

    /** String value for input "small-caps". */
    public static final String INPUT_SMALL_CAPS = "small-caps";

    /** String value for input "ultra-condensed". */
    public static final String INPUT_ULTRA_CONDENSED = "ultra-condensed";

    /** String value for input "extra-condensed". */
    public static final String INPUT_EXTRA_CONDENSED = "extra-condensed";

    /** String value for input "condensed". */
    public static final String INPUT_CONDENSED = "condensed";

    /** String value for input "semi-condensed". */
    public static final String INPUT_SEMI_CONDENSED = "semi-condensed";

    /** String value for input "semi-expanded". */
    public static final String INPUT_SEMI_EXPANDED = "semi-expanded";

    /** String value for input "expanded". */
    public static final String INPUT_EXPANDED = "expanded";

    /** String value for input "extra-expanded". */
    public static final String INPUT_EXTRA_EXPANDED = "extra-expanded";

    /** String value for input "ultra-expanded". */
    public static final String INPUT_ULTRA_EXPANDED = "ultra-expanded";

    /**
     * Private Constructor. This class should never be instantiated.
     */
    private FontUtility() {
    }

    /**
     * Converts XSL-FO String input for font-selection-strategy into a value
     * expected by {@link FontConsumer#selectFontXSL(int, String[], int, int,
     * int, int, int, int)}.
     * @param input The XSL-FO-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * @return One of {@link Font#FONT_SELECTION_CBC} or
     * {@link Font#FONT_SELECTION_AUTO}.
     * For invalid input, -1.
     */
    public static byte foFontSelectionStrategy(String input, final boolean lowerCaseOnly) {
        if (input == null) {
            return -1;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals("character-by-character")) {
            return Font.FONT_SELECTION_CBC;
        }
        if (input.equals("auto")) {
            return Font.FONT_SELECTION_AUTO;
        }
        return -1;
    }

    /**
     * Converts a CSS2-style String input for font-style into a value expected
     * by {@link FontConsumer#selectFontCSS(String[], int, int, int, int, int,
     * int)}.
     * @param input The CSS2-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether CSS is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of {@link Font#FONT_STYLE_NORMAL},
     * {@link Font#FONT_STYLE_ITALIC}, {@link Font#FONT_STYLE_OBLIQUE},
     * or {@link Font#FONT_STYLE_BACKSLANT}.
     * For invalid input, -1.
     * @see FontConsumer#selectFontCSS(String[], int, int, int, int, int, int)
     */
    public static byte cssFontStyle(String input, final boolean lowerCaseOnly) {
        if (input == null) {
            return -1;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals(FontUtility.INPUT_NORMAL)) {
            return Font.FONT_STYLE_NORMAL;
        }
        if (input.equals(FontUtility.INPUT_ITALIC)) {
            return Font.FONT_STYLE_ITALIC;
        }
        if (input.equals(FontUtility.INPUT_OBLIQUE)) {
            return Font.FONT_STYLE_OBLIQUE;
        }
        return -1;
    }

    /**
     * Same as {@link #cssFontStyle(String, boolean)}, except input is an
     * XSL-FO-style String.
     * @param input The XSL-FO-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether XSL-FO is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of {@link Font#FONT_STYLE_NORMAL},
     * {@link Font#FONT_STYLE_ITALIC}, {@link Font#FONT_STYLE_OBLIQUE},
     * or {@link Font#FONT_STYLE_BACKSLANT}.
     * For invalid input, -1.
     * @see #cssFontStyle(String, boolean)
     */
    public static byte foFontStyle(String input, final boolean lowerCaseOnly) {
        final byte returnValue = cssFontStyle(input, lowerCaseOnly);
        if (returnValue > -1) {
            return returnValue;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals(FontUtility.INPUT_BACKSLANT)) {
            return Font.FONT_STYLE_BACKSLANT;
        }
        return -1;
    }

    /**
     * Converts a CSS2-style String input for font-weight into a value expected
     * by {@link FontConsumer#selectFontCSS(String[], int, int, int, int, int,
     * int)}.
     * @param input The CSS2-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether CSS is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of
     * {@link Font#FONT_WEIGHT_100}, {@link Font#FONT_WEIGHT_200},
     * {@link Font#FONT_WEIGHT_300}, {@link Font#FONT_WEIGHT_400},
     * {@link Font#FONT_WEIGHT_500}, {@link Font#FONT_WEIGHT_600},
     * {@link Font#FONT_WEIGHT_700}, {@link Font#FONT_WEIGHT_800},
     * {@link Font#FONT_WEIGHT_900}, {@link Font#FONT_WEIGHT_NORMAL},
     * and {@link Font#FONT_WEIGHT_BOLD}.
     * For invalid input, -1.
     * @see FontConsumer#selectFontCSS(String[], int, int, int, int, int, int)
     */
    public static short cssFontWeight(String input, final boolean lowerCaseOnly) {
        if (input == null) {
            return -1;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals(FontUtility.INPUT_NORMAL)) {
            return Font.FONT_WEIGHT_NORMAL;
        }
        if (input.equals("bold")) {
            return Font.FONT_WEIGHT_BOLD;
        }
        if (input.equals("100")) {
            return Font.FONT_WEIGHT_100;
        }
        if (input.equals("200")) {
            return Font.FONT_WEIGHT_200;
        }
        if (input.equals("300")) {
            return Font.FONT_WEIGHT_300;
        }
        if (input.equals("400")) {
            return Font.FONT_WEIGHT_400;
        }
        if (input.equals("500")) {
            return Font.FONT_WEIGHT_500;
        }
        if (input.equals("600")) {
            return Font.FONT_WEIGHT_600;
        }
        if (input.equals("700")) {
            return Font.FONT_WEIGHT_700;
        }
        if (input.equals("800")) {
            return Font.FONT_WEIGHT_800;
        }
        if (input.equals("900")) {
            return Font.FONT_WEIGHT_900;
        }
        return -1;
    }

    /**
     * Same as {@link #cssFontWeight(String, boolean)}, except input is an
     * XSL-FO-style String.
     * @param input The XSL-FO-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether XSL-FO is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of
     * {@link Font#FONT_WEIGHT_100}, {@link Font#FONT_WEIGHT_200},
     * {@link Font#FONT_WEIGHT_300}, {@link Font#FONT_WEIGHT_400},
     * {@link Font#FONT_WEIGHT_500}, {@link Font#FONT_WEIGHT_600},
     * {@link Font#FONT_WEIGHT_700}, {@link Font#FONT_WEIGHT_800},
     * {@link Font#FONT_WEIGHT_900}, {@link Font#FONT_WEIGHT_NORMAL},
     * and {@link Font#FONT_WEIGHT_BOLD}.
     * For invalid input, -1.
     * @see #cssFontWeight(String, boolean)
     */
    public static short foFontWeight(final String input, final boolean lowerCaseOnly) {
        return cssFontWeight(input, lowerCaseOnly);
    }

    /**
     * Converts a CSS2-style String input for font-variant into a value expected
     * by {@link FontConsumer#selectFontCSS(String[], int, int, int, int, int,
     * int)}.
     * @param input The CSS2-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether CSS is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of {@link Font#FONT_VARIANT_NORMAL} or
     * {@link Font#FONT_VARIANT_SMALL_CAPS}.
     * For invalid input, -1.
     * @see FontConsumer#selectFontCSS(String[], int, int, int, int, int, int)
     */
    public static byte cssFontVariant(String input, final boolean lowerCaseOnly) {
        if (input == null) {
            return -1;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals(FontUtility.INPUT_NORMAL)) {
            return Font.FONT_VARIANT_NORMAL;
        }
        if (input.equals(FontUtility.INPUT_SMALL_CAPS)) {
            return Font.FONT_VARIANT_SMALL_CAPS;
        }
        return -1;
    }

    /**
     * Same as {@link #cssFontVariant(String, boolean)}, except input is an
     * XSL-FO-style String.
     * @param input The XSL-FO-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether XSL-FO is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of {@link Font#FONT_VARIANT_NORMAL} or
     * {@link Font#FONT_VARIANT_SMALL_CAPS}.
     * For invalid input, -1.
     * @see #cssFontVariant(String, boolean)
     */
    public static byte foFontVariant(final String input, final boolean lowerCaseOnly) {
        return cssFontVariant(input, lowerCaseOnly);
    }

    /**
     * Converts a CSS2-style String input for font-stretch into a value expected
     * by {@link FontConsumer#selectFontCSS(String[], int, int, int, int, int,
     * int)}.
     * @param input The CSS2-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether CSS is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of
     * {@link Font#FONT_STRETCH_ULTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_EXTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_CONDENSED},
     * {@link Font#FONT_STRETCH_SEMI_CONDENSED},
     * {@link Font#FONT_STRETCH_NORMAL},
     * {@link Font#FONT_STRETCH_SEMI_EXPANDED},
     * {@link Font#FONT_STRETCH_EXPANDED},
     * {@link Font#FONT_STRETCH_EXTRA_EXPANDED},
     * {@link Font#FONT_STRETCH_ULTRA_EXPANDED}.
     * For invalid input, -1.
     * @see FontConsumer#selectFontCSS(String[], int, int, int, int, int, int)
     */
    public static byte cssFontStretch(String input, final boolean lowerCaseOnly) {
        if (input == null) {
            return -1;
        }
        if (!lowerCaseOnly) {
            input = input.toLowerCase();
        }
        if (input.equals(FontUtility.INPUT_NORMAL)) {
            return Font.FONT_STRETCH_NORMAL;
        }
        if (input.equals(FontUtility.INPUT_ULTRA_CONDENSED)) {
            return Font.FONT_STRETCH_ULTRA_CONDENSED;
        }
        if (input.equals(FontUtility.INPUT_EXTRA_CONDENSED)) {
            return Font.FONT_STRETCH_EXTRA_CONDENSED;
        }
        if (input.equals(FontUtility.INPUT_CONDENSED)) {
            return Font.FONT_STRETCH_CONDENSED;
        }
        if (input.equals(FontUtility.INPUT_SEMI_CONDENSED)) {
            return Font.FONT_STRETCH_SEMI_CONDENSED;
        }
        if (input.equals(FontUtility.INPUT_SEMI_EXPANDED)) {
            return Font.FONT_STRETCH_SEMI_EXPANDED;
        }
        if (input.equals(FontUtility.INPUT_EXPANDED)) {
            return Font.FONT_STRETCH_EXPANDED;
        }
        if (input.equals(FontUtility.INPUT_EXTRA_EXPANDED)) {
            return Font.FONT_STRETCH_EXTRA_EXPANDED;
        }
        if (input.equals(FontUtility.INPUT_ULTRA_EXPANDED)) {
            return Font.FONT_STRETCH_ULTRA_EXPANDED;
        }
        return -1;
    }

    /**
     * Same as {@link #cssFontStretch(String, boolean)}, except input is an
     * XSL-FO-style String.
     * @param input The XSL-FO-style String to be converted.
     * @param lowerCaseOnly Set to true to insist that all input values be
     * lowercase.
     * There is some ambiguity about whether XSL-FO is case-sensitive or not. If
     * it is, all input should be lower case.
     * @return For valid input, one of
     * {@link Font#FONT_STRETCH_ULTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_EXTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_CONDENSED},
     * {@link Font#FONT_STRETCH_SEMI_CONDENSED},
     * {@link Font#FONT_STRETCH_NORMAL},
     * {@link Font#FONT_STRETCH_SEMI_EXPANDED},
     * {@link Font#FONT_STRETCH_EXPANDED},
     * {@link Font#FONT_STRETCH_EXTRA_EXPANDED},
     * {@link Font#FONT_STRETCH_ULTRA_EXPANDED}.
     * For invalid input, -1.
     * @see #cssFontStretch(String, boolean)
     */
    public static byte foFontStretch(final String input, final boolean lowerCaseOnly) {
        return cssFontStretch(input, lowerCaseOnly);
    }

    /**
     * Converts a CSS2-style String input for font-family into a value expected
     * by {@link FontConsumer#selectFontCSS(String[], int, int, int, int, int,
     * int)}.
     * @param input The CSS2-style font-family String to be converted.
     * This is a comma-delimited String containing one or more potential
     * font-family items to be selected. Consult the CSS2 standard for
     * details.
     * @return A String array with the parsed font-family items in it, or null
     * if none were found.
     * @see FontConsumer#selectFontCSS(String[], int, int, int, int, int, int)
     */
    public static String[] cssFontFamily(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim();
        final char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch(chars[i]) {
                case '\t':
                case '\n':
                case '\f':
                case '\r':
                    {
                        chars[i] = ' ';
                    }
            }
        }
        int elementCount = 0;
        int charCount = 0;
        boolean insideDoubleQuotes = false;
        boolean insideSingleQuotes = false;
        for (int i = 0; i < chars.length; i++) {
            final char currentChar = chars[i];
            switch(currentChar) {
                case '\"':
                    {
                        if (insideSingleQuotes) {
                            return null;
                        }
                        insideDoubleQuotes = !insideDoubleQuotes;
                        charCount++;
                        break;
                    }
                case '\'':
                    {
                        if (insideDoubleQuotes) {
                            return null;
                        }
                        insideSingleQuotes = !insideSingleQuotes;
                        charCount++;
                        break;
                    }
                case ',':
                    {
                        if (insideDoubleQuotes || insideSingleQuotes) {
                            charCount++;
                            break;
                        }
                        if (charCount > 0) {
                            elementCount++;
                            charCount = 0;
                        }
                        break;
                    }
                case ' ':
                    {
                        break;
                    }
                default:
                    {
                        charCount++;
                        break;
                    }
            }
        }
        if (charCount > 0) {
            elementCount++;
        }
        if (elementCount < 1) {
            return null;
        }
        if (insideDoubleQuotes || insideSingleQuotes) {
            return null;
        }
        final String[] returnArray = new String[elementCount];
        elementCount = 0;
        charCount = 0;
        boolean insideQuotes = false;
        int firstChar = -1;
        for (int i = 0; i < chars.length; i++) {
            final char currentChar = chars[i];
            switch(currentChar) {
                case '\"':
                case '\'':
                    {
                        insideQuotes = !insideQuotes;
                        break;
                    }
                case ',':
                    {
                        if (insideQuotes) {
                            charCount++;
                        } else if (charCount > 0) {
                            returnArray[elementCount] = extractText(chars, firstChar, charCount);
                            firstChar = -1;
                            charCount = 0;
                            elementCount++;
                        }
                        break;
                    }
                default:
                    {
                        charCount++;
                        if (firstChar < 0) {
                            firstChar = i;
                        }
                        break;
                    }
            }
        }
        if (charCount > 0) {
            returnArray[elementCount] = extractText(chars, firstChar, charCount);
        }
        return returnArray;
    }

    /**
     * Normalizes whitespace for a portion of a char array.
     * @param chars The char array to be normalized.
     * @param firstChar The index to the first char to be examined.
     * @param charCount The number of chars to be examined.
     * @return The normalized String.
     */
    private static String extractText(final char[] chars, final int firstChar, int charCount) {
        final int maxIndex = Math.min(chars.length, firstChar + charCount);
        boolean anyChanges = true;
        while (anyChanges) {
            anyChanges = false;
            for (int i = firstChar; i < maxIndex - 1; i++) {
                if (chars[i] == ' ' && chars[i + 1] == ' ') {
                    for (int j = i + 1; j < maxIndex - 1; j++) {
                        chars[j] = chars[j + 1];
                    }
                    charCount--;
                    anyChanges = true;
                }
            }
        }
        final String returnValue = String.copyValueOf(chars, firstChar, charCount);
        return returnValue.trim();
    }

    /**
     * Same as {@link #cssFontFamily(String)}, except input is an
     * XSL-FO-style String.
     * @param input The XSL-FO font-family String to be converted.
     * This is a comma-delimited String containing one or more potential
     * font-family items to be selected. Consult the XSL-FO standard for
     * details.
     * @return A String array with the parsed font-family items in it, or null
     * if none were found.
     * @see #cssFontFamily(String)
     */
    public static String[] foFontFamily(final String input) {
        return cssFontFamily(input);
    }

    /**
     * Returns a String description for a numeric font-style value, suitable
     * for using in user messages.
     * @param fontStyle One of {@link Font#FONT_STYLE_NORMAL},
     * {@link Font#FONT_STYLE_ITALIC}, {@link Font#FONT_STYLE_OBLIQUE},
     * or {@link Font#FONT_STYLE_BACKSLANT}.
     * @return A String description for fontStyle
     */
    public static String fontStyleName(final int fontStyle) {
        switch(fontStyle) {
            case Font.FONT_STYLE_NORMAL:
                {
                    return FontUtility.INPUT_NORMAL;
                }
            case Font.FONT_STYLE_ITALIC:
                {
                    return FontUtility.INPUT_ITALIC;
                }
            case Font.FONT_STYLE_OBLIQUE:
                {
                    return FontUtility.INPUT_OBLIQUE;
                }
            case Font.FONT_STYLE_BACKSLANT:
                {
                    return FontUtility.INPUT_BACKSLANT;
                }
            default:
                {
                    return FontUtility.INPUT_INVALID;
                }
        }
    }

    /**
     * Returns a String description for a numeric font-weight value, suitable
     * for using in user messages.
     * @param fontWeight One of
     * {@link Font#FONT_WEIGHT_100},
     * {@link Font#FONT_WEIGHT_200},
     * {@link Font#FONT_WEIGHT_300},
     * {@link Font#FONT_WEIGHT_400},
     * {@link Font#FONT_WEIGHT_500},
     * {@link Font#FONT_WEIGHT_600},
     * {@link Font#FONT_WEIGHT_700},
     * {@link Font#FONT_WEIGHT_800}, or
     * {@link Font#FONT_WEIGHT_900}.
     * @return A String description for fontWeight
     */
    public static String fontWeightName(final int fontWeight) {
        switch(fontWeight) {
            case Font.FONT_WEIGHT_100:
            case Font.FONT_WEIGHT_200:
            case Font.FONT_WEIGHT_300:
            case Font.FONT_WEIGHT_400:
            case Font.FONT_WEIGHT_500:
            case Font.FONT_WEIGHT_600:
            case Font.FONT_WEIGHT_700:
            case Font.FONT_WEIGHT_800:
            case Font.FONT_WEIGHT_900:
                {
                    return Integer.toString(fontWeight);
                }
            default:
                {
                    return FontUtility.INPUT_INVALID;
                }
        }
    }

    /**
     * Returns a String description for a numeric font-weight value, suitable
     * for using in user messages.
     * @param fontVariant One of {@link Font#FONT_VARIANT_NORMAL} or
     * {@link Font#FONT_VARIANT_SMALL_CAPS}.
     * @return A String description for fontWeight
     */
    public static String fontVariantName(final int fontVariant) {
        switch(fontVariant) {
            case Font.FONT_VARIANT_NORMAL:
                {
                    return FontUtility.INPUT_NORMAL;
                }
            case Font.FONT_VARIANT_SMALL_CAPS:
                {
                    return FontUtility.INPUT_SMALL_CAPS;
                }
            default:
                {
                    return FontUtility.INPUT_INVALID;
                }
        }
    }

    /**
     * Returns a String description for a numeric font-weight value, suitable
     * for using in user messages.
     * @param fontStretch One of
     * {@link Font#FONT_STRETCH_ULTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_EXTRA_CONDENSED},
     * {@link Font#FONT_STRETCH_CONDENSED},
     * {@link Font#FONT_STRETCH_SEMI_CONDENSED},
     * {@link Font#FONT_STRETCH_NORMAL},
     * {@link Font#FONT_STRETCH_SEMI_EXPANDED},
     * {@link Font#FONT_STRETCH_EXPANDED},
     * {@link Font#FONT_STRETCH_EXTRA_EXPANDED}, or
     * {@link Font#FONT_STRETCH_ULTRA_EXPANDED}.
     * @return A String description for fontStretch
     */
    public static String fontStretchName(final int fontStretch) {
        switch(fontStretch) {
            case Font.FONT_STRETCH_ULTRA_CONDENSED:
                {
                    return FontUtility.INPUT_ULTRA_CONDENSED;
                }
            case Font.FONT_STRETCH_EXTRA_CONDENSED:
                {
                    return FontUtility.INPUT_EXTRA_CONDENSED;
                }
            case Font.FONT_STRETCH_CONDENSED:
                {
                    return FontUtility.INPUT_CONDENSED;
                }
            case Font.FONT_STRETCH_SEMI_CONDENSED:
                {
                    return FontUtility.INPUT_SEMI_CONDENSED;
                }
            case Font.FONT_STRETCH_NORMAL:
                {
                    return FontUtility.INPUT_NORMAL;
                }
            case Font.FONT_STRETCH_SEMI_EXPANDED:
                {
                    return FontUtility.INPUT_SEMI_EXPANDED;
                }
            case Font.FONT_STRETCH_EXPANDED:
                {
                    return FontUtility.INPUT_EXPANDED;
                }
            case Font.FONT_STRETCH_EXTRA_EXPANDED:
                {
                    return FontUtility.INPUT_EXTRA_EXPANDED;
                }
            case Font.FONT_STRETCH_ULTRA_EXPANDED:
                {
                    return FontUtility.INPUT_ULTRA_EXPANDED;
                }
            default:
                {
                    return FontUtility.INPUT_INVALID;
                }
        }
    }
}
