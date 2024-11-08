package edu.georgetown.nnj.data.formats;

import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.data.layout.NNJDataLayout464ii;
import edu.georgetown.nnj.data.NNJAbstractDataSource;
import edu.georgetown.nnj.data.NNJDataFileReader;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.NNJDataMask;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.data.NNJDataWindow;
import edu.georgetown.nnj.data.NNJDataFileWriter;
import java.io.*;
import static edu.georgetown.nnj.util.NNJUtilNumberConversion.byteArrayToIntArray16Rev;

/** Encapsulates the data for NounouJ. Internal representations
 *and calculations will utilize integers, since toInteger arithmetic is generally much
 *faster than floating-point arithmetic, and the data is not of exceedingly
 *high resolution or dynamic range to begin with. The internal representation
 *is scaled up by a certain factor (DATA_EXTRA_BITS) from the original int(16-bit)
 *values. This should be taken into account when implementing any operation
 *on these values-- the values are very large, and are therefore prone to
 *overflow, so they may need to be cast to long(64-bit).<p>
 *
 *All data sent along the data processing stream (via interface NNJDataSource)
 *is either sent by value or sent as a protective copy. Therefore, the data
 *within this class is impervious to outside modification by downstream members
 *of the processing stream.
 */
public final class NNJDataSourceDa extends NNJAbstractDataSource implements NNJDataFileReader, NNJDataFileWriter {

    /** The raw data from a .da file is scaled up by multiplying the short (16-bit)
     * value with DATA_EXTRA_BITS into an (int) value.
     * Therefore, the values should fall between [-32768*DATA_EXTRA_BITS, 
     * 32768*DATA_EXTRA_BITS], or equivalently [+/- DATA_SCALE].<p>
     *
     * When scaling, the resulting values fed to each drawing method are assumed to
     * fall between [+/-DATA_SCALE], and values larger than this may exceed the
     * window boundaries.<p>
     *
     *Besides the speed of toInteger arithmetic, this data format is used in order
     *to use the old (and faster) java.awt.Graphics functions, which
     *take only integers. The new Graphics2D functions taking doubles/floats was tested,
     *but it was much slower as of JDK 1.5 on a fast Wintel notebook 
     *with dedicated graphics board.<p>
     *
     *This optimization may cause minor round of errors with repeated filtering,
     *etc., but also remember that the original data is only int16 to begin with.
     *This optimization may become unnecessary at some point in the future,
     *if Graphics2D functions are accelerated to draw floating point quickly.*/
    private int[][] data = { { 0, 0 }, { 0, 0 } };

    /**To be used in static read methods, to access layout information.*/
    private static NNJDataLayout464ii DATA_LAYOUT = NNJDataLayout464ii.INSTANCE;

    /**Remember to generate each time this class is constructed, or data changed.*/
    private NNJDataWindow dataWin;

    /**Remember to generate each time this class is constructed, or data changed.*/
    private NNJDataMask dataMask = new NNJDataMask();

    private boolean dataLoaded = false;

    private int frameCount;

    public static int[] STD_DA_HEADER = { 1, 1, 100, 26, 4096, 0, 0, 22616, 22616, 22616, 22616, 22616, 22616, 12593, 13878, 14906, 12593, 14649, 14906, 13621, 12336, 12593, 12336, 19789, 24929, 31097, 12336, 13878, 8224, 8224, 27499, 29812, 25186, 29298, 12336, 13878, 12336, 13621, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -9999, 0, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 20046, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 20046, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 0, 0, 472, 0, 0, 0, 0, 0, -9999, 0, 0, 0, 472, 18504, 0, 0, 0, 0, 0, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 0, 0, 0, 0, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 8224, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 17, 32, 33, 48, 49, 2, 3, 18, 19, 34, 35, 50, 51, 4, 5, 20, 21, 36, 37, 52, 53, 6, 7, 22, 23, 38, 39, 54, 55, 8, 9, 24, 25, 40, 41, 56, 57, 10, 11, 26, 27, 42, 43, 58, 59, 12, 13, 28, 29, 44, 45, 60, 61, 14, 15, 30, 31, 46, 47, 62, 63, 64, 65, 80, 81, 96, 97, 112, 113, 66, 67, 82, 83, 98, 99, 114, 115, 68, 69, 84, 85, 100, 101, 116, 117, 70, 71, 86, 87, 102, 103, 118, 119, 72, 73, 88, 89, 104, 105, 120, 121, 74, 75, 90, 91, 106, 107, 122, 123, 76, 77, 92, 93, 108, 109, 124, 125, 78, 79, 94, 95, 110, 111, 126, 127, 128, 129, 144, 145, 160, 161, 176, 177, 130, 131, 146, 147, 162, 163, 178, 179, 132, 133, 148, 149, 164, 165, 180, 181, 134, 135, 150, 151, 166, 167, 182, 183, 136, 137, 152, 153, 168, 169, 184, 185, 138, 139, 154, 155, 170, 171, 186, 187, 140, 141, 156, 157, 172, 173, 188, 189, 142, 143, 158, 159, 174, 175, 190, 191, 192, 193, 208, 209, 224, 225, 240, 241, 194, 195, 210, 211, 226, 227, 242, 243, 196, 197, 212, 213, 228, 229, 244, 245, 198, 199, 214, 215, 230, 231, 246, 247, 200, 201, 216, 217, 232, 233, 248, 249, 202, 203, 218, 219, 234, 235, 250, 251, 204, 205, 220, 221, 236, 237, 252, 253, 206, 207, 222, 223, 238, 239, 254, 255, 256, 257, 272, 273, 288, 289, 304, 305, 258, 259, 274, 275, 290, 291, 306, 307, 260, 261, 276, 277, 292, 293, 308, 309, 262, 263, 278, 279, 294, 295, 310, 311, 264, 265, 280, 281, 296, 297, 312, 313, 266, 267, 282, 283, 298, 299, 314, 315, 268, 269, 284, 285, 300, 301, 316, 317, 270, 271, 286, 287, 302, 303, 318, 319, 320, 321, 336, 337, 352, 353, 368, 369, 322, 323, 338, 339, 354, 355, 370, 371, 324, 325, 340, 341, 356, 357, 372, 373, 326, 327, 342, 343, 358, 359, 374, 375, 328, 329, 344, 345, 360, 361, 376, 377, 330, 331, 346, 347, 362, 363, 378, 379, 332, 333, 348, 349, 364, 365, 380, 381, 334, 335, 350, 351, 366, 367, 382, 383, 384, 385, 400, 401, 416, 417, 432, 433, 386, 387, 402, 403, 418, 419, 434, 435, 388, 389, 404, 405, 420, 421, 436, 437, 390, 391, 406, 407, 422, 423, 438, 439, 392, 393, 408, 409, 424, 425, 440, 441, 394, 395, 410, 411, 426, 427, 442, 443, 396, 397, 412, 413, 428, 429, 444, 445, 398, 399, 414, 415, 430, 431, 446, 447, 448, 449, 464, 465, 480, 481, 496, 497, 450, 451, 466, 467, 482, 483, 498, 499, 452, 453, 468, 469, 484, 485, 500, 501, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13621, 19789, 8224, 8224, 8224, 8224, 16705, 17219, 0, 0, 0 };

    /**This is the header information which was in the original &#46;da file.*/
    private int[] dataHeadDA = K.copy(STD_DA_HEADER);

    /**This constant provides the multiplication factor by which
     *original neuroplex data short (16-bit) is multiplied to give
     *internal representation in int (32-bit).
     */
    private static int DATA_EXTRA_BITS = 1024;

    /**The Neuroplex da file is scaled such that multiplying this value
     * to the original toInteger data values will give mV.*/
    private static double DA_SCALE_TO_MV = 0.305175781250000d;

    private static double ABS_GAIN = ((double) DATA_EXTRA_BITS) / DA_SCALE_TO_MV;

    private static String ABS_VAL_UNIT = "mV";

    private static double SAMPLING_RATE = 1600d;

    @Override
    public int getDataExtraBits() {
        return DATA_EXTRA_BITS;
    }

    @Override
    public double getAbsoluteGain() {
        return ABS_GAIN;
    }

    @Override
    public String getAbsoluteUnit() {
        return ABS_VAL_UNIT;
    }

    @Override
    public double getSamplingRate() {
        return SAMPLING_RATE;
    }

    @Override
    public int getTotalFrameCount() {
        return frameCount;
    }

    private NNJDataSourceDa() {
    }

    public NNJDataSourceDa(File file) throws IOException {
        loadFile(file);
    }

    /**Opens data at the path/filename "string".*/
    public NNJDataSourceDa(String file) throws IOException {
        loadFile(file);
    }

    private NNJDataSourceDa(File file, int detToRead) throws IOException {
        constructorCommon(file, detToRead);
    }

    private void constructorCommon(File file, int detToRead) throws IOException {
        try {
            this.openDA(file, detToRead);
        } catch (IOException e) {
            throw e;
        }
        if (detToRead == -1) {
        }
    }

    @Override
    public int readDataPointImpl(int ch, int fr) {
        if (dataLoaded) {
            return this.data[ch][fr];
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int ch, int start, int end, int decimationFactor) {
        if (dataLoaded) {
            return super.readDataTraceSegmentImpl(ch, start, end, decimationFactor);
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int ch, int start, int end) {
        if (dataLoaded) {
            if (start == 0) {
                if (end == this.frameCount - 1) {
                    return K.copy(this.data[ch]);
                } else {
                    return K.copy(this.data[ch], end + 1);
                }
            } else {
                return K.copy(this.data[ch], start, end);
            }
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public NNJDataLayout getDataLayout() {
        return DATA_LAYOUT;
    }

    @Override
    public NNJDataWindow getDataWindow() {
        if (this.dataWin == null) {
            throw new NullPointerException("DataWindow requested before initialization!");
        }
        return this.dataWin;
    }

    @Override
    public NNJDataMask getDataMask() {
        if (this.dataMask == null) {
            throw new NullPointerException("DataMask requested before initialization!");
        }
        return this.dataMask;
    }

    /** Convenience method for rapid I/O without returning object. */
    public static double[][] readFile(File file) {
        NNJDataSourceDa data;
        double[][] tempret = null;
        try {
            data = new NNJDataSourceDa(file, -2);
            tempret = K.toDouble(data.readData());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        tempret = K.multiply(tempret, DA_SCALE_TO_MV);
        return tempret;
    }

    /** Convenience method for rapid I/O without returning object. */
    public static double[][] readFile(String fileStr) {
        double[][] tempret = null;
        File file = new File(fileStr);
        tempret = readFile(file);
        return tempret;
    }

    /** Convenience method for rapid I/O of a single trace from muliple files. */
    public static double[] readFileTrace(File file, int channel) {
        NNJDataSourceDa data = null;
        double[] tempret = null;
        try {
            data = new NNJDataSourceDa(file, channel);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        tempret = K.toDouble(data.readDataTrace(channel));
        tempret = K.multiply(tempret, DA_SCALE_TO_MV);
        return tempret;
    }

    /** Convenience method for rapid I/O of a single trace from muliple files. */
    public static double[] readFileTrace(String fileStr, int channel) {
        double[] tempret = null;
        File file = new File(fileStr);
        tempret = readFileTrace(file, channel);
        return tempret;
    }

    /**Loads the specified file into the data object*/
    @Override
    public void loadFile(String file) throws IOException {
        File tempFile = new File(file);
        loadFile(tempFile);
    }

    /**Loads the specified file into the data object*/
    @Override
    public void loadFile(File file) throws IOException {
        this.openDA(file, -1);
    }

    /**@param detToRead setting this to -1 gives the default initialization action.
     * Setting to -2 skips certain unnecessary initialization steps,
     * for calls from the static convenience methods. Setting to a valid detector
     * value skips the same steps as -2, and additionally, only reads the trace
     * for detector det.
     */
    private void openDA(File file, int detToRead) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        byte[] byteBuff;
        byteBuff = new byte[2560 * 2];
        fin.read(byteBuff);
        dataHeadDA = byteArrayToIntArray16Rev(byteBuff);
        this.frameCount = dataHeadDA[5 - 1];
        if (frameCount < 0) {
            frameCount = -frameCount * 1024;
        }
        this.dataWin = new NNJDataWindow(this);
        this.dataMask = new NNJDataMask();
        byteBuff = new byte[frameCount * 2];
        if (detToRead == -1) {
            this.data = new int[DATA_LAYOUT.getChannelCount()][];
            for (int det = 0; det < data.length; det++) {
                fin.read(byteBuff);
                this.data[det] = byteArrayToIntArray16Rev(byteBuff);
                this.data[det] = K.multiply(this.data[det], DATA_EXTRA_BITS);
            }
            this.dataLoaded = true;
        } else if (detToRead == -2) {
            this.data = new int[DATA_LAYOUT.getChannelCount()][];
            for (int det = 0; det < data.length; det++) {
                fin.read(byteBuff);
                this.data[det] = byteArrayToIntArray16Rev(byteBuff);
                this.data[det] = K.multiply(this.data[det], DATA_EXTRA_BITS);
            }
        } else if (DATA_LAYOUT.isValidDetector(detToRead)) {
            fin.skip((long) frameCount * (long) detToRead);
            fin.read(byteBuff);
            this.data[detToRead] = byteArrayToIntArray16Rev(byteBuff);
            this.data[detToRead] = K.multiply(this.data[detToRead], DATA_EXTRA_BITS);
        }
        fin.close();
    }

    @Override
    public boolean supportsFileType(String extension) {
        return (extension.equalsIgnoreCase(".da") || extension.equalsIgnoreCase("da"));
    }

    @Override
    public void writeData(int[][] newData) {
        if (this.data.length != newData.length || this.data[0].length != newData[0].length) {
            throw new IllegalArgumentException("Input array must be equal in dimensions to original!");
        } else {
            this.data = newData;
        }
    }

    @Override
    public void writeFile(String fileName, NNJDataSource source) throws IOException {
        File file = new File(fileName);
        FileOutputStream fout = new FileOutputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        DataOutputStream dout = new DataOutputStream(bout);
        for (int k = 0; k < 2560; k++) {
            dout.writeShort(Short.reverseBytes((short) dataHeadDA[k]));
        }
        for (int det = 0; det < this.getDataLayout().getChannelCount(); det++) for (int frame = 0; frame < this.getTotalFrameCount(); frame++) {
            dout.writeShort(Short.reverseBytes((short) ((double) source.readDataPoint(det, frame) / (double) source.getDataExtraBits())));
        }
        dout.close();
        bout.close();
        fout.close();
    }
}
