package org.fao.waicent.util;

/** * <P> * This class implements simple sorting algorithms, for various classes etc. * <P> * <BR><BR> Author: Alan Williamson <A HREF="MAILTO:alan@n-ary.com">alan@n-ary.com</A> * <BR> Date:   December 1997 * * @version		1.0 * @author		Alan Willamson (alan@n-ary.com) * @since		JDK1.1.4 */
public class sort extends java.lang.Object {

    private static String Cal[] = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

    /**     * <P>     * Sorts an array of Strings into asending order     * <P>     * @param _data         The array that is to be sorted.     *     */
    public static void ascending(String _data[]) {
        for (int i = _data.length; --i >= 0; ) {
            boolean swapped = false;
            for (int j = 0; j < i; j++) {
                if (_data[j].compareTo(_data[j + 1]) > 0) {
                    String T = _data[j];
                    _data[j] = _data[j + 1];
                    _data[j + 1] = T;
                    swapped = true;
                }
            }
            if (!swapped) {
                return;
            }
        }
    }

    /**     * <P>     * Sorts an array of Strings into asending order, looking for special calendar     * variables.  If the months are found then the data is sorted in order of months.     * <P>     * @param _data         The array that is to be sorted.     *     */
    public static void calendar(String _data[]) {
        for (int y = 0; y < Cal.length; y++) {
            if (_data[0].indexOf(Cal[y]) == 0) {
                for (int x = 0; x < Cal.length; x++) {
                    _data[x] = Cal[x];
                }
                return;
            }
        }
        ascending(_data);
    }
}
