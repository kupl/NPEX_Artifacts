/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.render.pcl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.CountingOutputStream;

import org.apache.xmlgraphics.util.UnitConv;

import org.apache.fop.fonts.Typeface;
import org.apache.fop.render.pcl.fonts.PCLFontReader;
import org.apache.fop.render.pcl.fonts.PCLSoftFontManager;
import org.apache.fop.util.bitmap.BitmapImageUtil;
import org.apache.fop.util.bitmap.DitherUtil;

/**
 * This class provides methods for generating PCL print files.
 */
public class PCLGenerator {

    private static final String US_ASCII = "US-ASCII";

    private static final String ISO_8859_1 = "ISO-8859-1";

    /** The ESC (escape) character */
    public static final char ESC = '\033';

    /** A list of all supported resolutions in PCL (values in dpi) */
    public static final int[] PCL_RESOLUTIONS = new int[] {75, 100, 150, 200, 300, 600};

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private final DecimalFormat df2 = new DecimalFormat("0.##", symbols);
    private final DecimalFormat df4 = new DecimalFormat("0.####", symbols);

    private final CountingOutputStream out;
    protected Map<Typeface, PCLFontReader> fontReaderMap = new HashMap<Typeface, PCLFontReader>();
    protected Map<PCLSoftFontManager, Map<Typeface, Long>> fontManagerMap
            = new LinkedHashMap<PCLSoftFontManager, Map<Typeface, Long>>();

    private boolean currentSourceTransparency = true;
    private boolean currentPatternTransparency = true;

    private int maxBitmapResolution = PCL_RESOLUTIONS[PCL_RESOLUTIONS.length - 1];
    private float ditheringQuality = 0.5f;

    /**
     * true: Standard PCL shades are used (poor quality). false: user-defined pattern are used
     * to create custom dither patterns for better grayscale quality.
     */
    private static final boolean USE_PCL_SHADES = false;

    /**
     * Main constructor.
     * @param out the OutputStream to write the PCL stream to
     */
    public PCLGenerator(OutputStream out) {
        this.out = new CountingOutputStream(out);
    }

    /**
     * Main constructor.
     * @param out the OutputStream to write the PCL stream to
     * @param maxResolution the maximum resolution to encode bitmap images at
     */
    public PCLGenerator(OutputStream out, int maxResolution) {
        this(out);
        boolean found = false;
        for (int pclResolutions : PCL_RESOLUTIONS) {
            if (pclResolutions == maxResolution) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Illegal value for maximum resolution!");
        }
        this.maxBitmapResolution = maxResolution;
    }

    public void addFont(PCLSoftFontManager sfManager, Typeface font) {
        if (!fontManagerMap.containsKey(sfManager)) {
            fontManagerMap.put(sfManager, new LinkedHashMap<Typeface, Long>());
        }
        Map<Typeface, Long> fonts = fontManagerMap.get(sfManager);
        if (!fonts.containsKey(font)) {
            fonts.put(font, out.getByteCount());
        }
    }

    /** @return the OutputStream that this generator writes to */
    public OutputStream getOutputStream() {
        return this.out;
    }

    /**
     * Returns the currently active text encoding.
     * @return the text encoding
     */
    public String getTextEncoding() {
        return ISO_8859_1;
    }

    /** @return the maximum resolution to encode bitmap images at */
    public int getMaximumBitmapResolution() {
        return this.maxBitmapResolution;
    }

    /**
     * Writes a PCL escape command to the output stream.
     * @param cmd the command (without the ESCAPE character)
     * @throws IOException In case of an I/O error
     */
    public void writeCommand(String cmd) throws IOException {
        out.write(27); //ESC
        out.write(cmd.getBytes(US_ASCII));
    }

    /**
     * Writes raw text (in ISO-8859-1 encoding) to the output stream.
     * @param s the text
     * @throws IOException In case of an I/O error
     */
    public void writeText(String s) throws IOException {
        out.write(s.getBytes(ISO_8859_1));
    }

    /**
     * Writes raw bytes to the output stream
     * @param bytes The bytes
     * @throws IOException In case of an I/O error
     */
    public void writeBytes(byte[] bytes) throws IOException {
        out.write(bytes);
    }

    /**
     * Formats a double value with two decimal positions for PCL output.
     *
     * @param value value to format
     * @return the formatted value
     */
    public final String formatDouble2(double value) {
        return df2.format(value);
    }

    /**
     * Formats a double value with four decimal positions for PCL output.
     *
     * @param value value to format
     * @return the formatted value
     */
    public final String formatDouble4(double value) {
        return df4.format(value);
    }

    /**
     * Sends the universal end of language command (UEL).
     * @throws IOException In case of an I/O error
     */
    public void universalEndOfLanguage() throws IOException {
        writeCommand("%-12345X");
    }

    /**
     * Resets the printer and restores the user default environment.
     * @throws IOException In case of an I/O error
     */
    public void resetPrinter() throws IOException {
        writeCommand("E");
    }

    /**
     * Sends the job separation command.
     * @throws IOException In case of an I/O error
     */
    public void separateJobs() throws IOException {
        writeCommand("&l1T");
    }

    /**
     * Sends the form feed character.
     * @throws IOException In case of an I/O error
     */
    public void formFeed() throws IOException {
        out.write(12); //=OC ("FF", Form feed)
    }

    /**
     * Sets the unit of measure.
     * @param value the resolution value (units per inch)
     * @throws IOException In case of an I/O error
     */
    public void setUnitOfMeasure(int value) throws IOException {
        writeCommand("&u" + value + "D");
    }

    /**
     * Sets the raster graphics resolution
     * @param value the resolution value (units per inch)
     * @throws IOException In case of an I/O error
     */
    public void setRasterGraphicsResolution(int value) throws IOException {
        writeCommand("*t" + value + "R");
    }

    /**
     * Selects the page size.
     * @param selector the integer representing the page size
     * @throws IOException In case of an I/O error
     */
    public void selectPageSize(int selector) throws IOException {
        writeCommand("&l" + selector + "A");
    }

    /**
     * Selects the paper source. The parameter is usually printer-specific. Usually, "1" is the
     * default tray, "2" is the manual paper feed, "3" is the manual envelope feed, "4" is the
     * "lower" tray and "7" is "auto-select". Consult the technical reference for your printer
     * for all available values.
     * @param selector the integer representing the paper source/tray
     * @throws IOException In case of an I/O error
     */
    public void selectPaperSource(int selector) throws IOException {
        writeCommand("&l" + selector + "H");
    }

    /**
     * Selects the output bin. The parameter is usually printer-specific. Usually, "1" is the
     * default output bin (upper bin) and "2" is the lower (rear) output bin. Some printers
     * may support additional output bins. Consult the technical reference for your printer
     * for all available values.
     * @param selector the integer representing the output bin
     * @throws IOException In case of an I/O error
     */
    public void selectOutputBin(int selector) throws IOException {
        writeCommand("&l" + selector + "G");
    }

    /**
     * Selects the duplexing mode for the page.
     * The parameter is usually printer-specific.
     * "0" means Simplex,
     * "1" means Duplex, Long-Edge Binding,
     * "2" means Duplex, Short-Edge Binding.
     * @param selector the integer representing the duplexing mode of the page
     * @throws IOException In case of an I/O error
     */
    public void selectDuplexMode(int selector) throws IOException {
        writeCommand("&l" + selector + "S");
    }

    /**
     * Clears the horizontal margins.
     * @throws IOException In case of an I/O error
     */
    public void clearHorizontalMargins() throws IOException {
        writeCommand("9");
    }

    /**
     * The Top Margin command designates the number of lines between
     * the top of the logical page and the top of the text area.
     * @param numberOfLines the number of lines (See PCL specification for details)
     * @throws IOException In case of an I/O error
     */
    public void setTopMargin(int numberOfLines) throws IOException {
        writeCommand("&l" + numberOfLines + "E");
    }

    /**
     * The Text Length command can be used to define the bottom border. See the PCL specification
     * for details.
     * @param numberOfLines the number of lines
     * @throws IOException In case of an I/O error
     */
    public void setTextLength(int numberOfLines) throws IOException {
        writeCommand("&l" + numberOfLines + "F");
    }

    /**
     * Sets the Vertical Motion Index (VMI).
     * @param value the VMI value
     * @throws IOException In case of an I/O error
     */
    public void setVMI(double value) throws IOException {
        writeCommand("&l" + formatDouble4(value) + "C");
    }

    /**
     * Sets the cursor to a new absolute coordinate.
     * @param x the X coordinate (in millipoints)
     * @param y the Y coordinate (in millipoints)
     * @throws IOException In case of an I/O error
     */
    public void setCursorPos(double x, double y) throws IOException {
        if (x < 0) {
            //A negative x value will result in a relative movement so go to "0" first.
            //But this will most probably have no effect anyway since you can't paint to the left
            //of the logical page
            writeCommand("&a0h" + formatDouble2(x / 100) + "h" + formatDouble2(y / 100) + "V");
        } else {
            writeCommand("&a" + formatDouble2(x / 100) + "h" + formatDouble2(y / 100) + "V");
        }
    }

    /**
     * Pushes the current cursor position on a stack (stack size: max 20 entries)
     * @throws IOException In case of an I/O error
     */
    public void pushCursorPos() throws IOException {
        writeCommand("&f0S");
    }

    /**
     * Pops the current cursor position from the stack.
     * @throws IOException In case of an I/O error
     */
    public void popCursorPos() throws IOException {
        writeCommand("&f1S");
    }

    /**
     * Changes the current print direction while maintaining the current cursor position.
     * @param rotate the rotation angle (counterclockwise), one of 0, 90, 180 and 270.
     * @throws IOException In case of an I/O error
     */
    public void changePrintDirection(int rotate) throws IOException {
        writeCommand("&a" + rotate + "P");
    }

    /**
     * Enters the HP GL/2 mode.
     * @param restorePreviousHPGL2Cursor true if the previous HP GL/2 pen position should be
     *                                   restored, false if the current position is maintained
     * @throws IOException In case of an I/O error
     */
    public void enterHPGL2Mode(boolean restorePreviousHPGL2Cursor) throws IOException {
        if (restorePreviousHPGL2Cursor) {
            writeCommand("%0B");
        } else {
            writeCommand("%1B");
        }
    }

    /**
     * Enters the PCL mode.
     * @param restorePreviousPCLCursor true if the previous PCL cursor position should be restored,
     *                                 false if the current position is maintained
     * @throws IOException In case of an I/O error
     */
    public void enterPCLMode(boolean restorePreviousPCLCursor) throws IOException {
        if (restorePreviousPCLCursor) {
            writeCommand("%0A");
        } else {
            writeCommand("%1A");
        }
    }

    /**
     * Generate a filled rectangle at the current cursor position.
     *
     * @param w the width in millipoints
     * @param h the height in millipoints
     * @param col the fill color
     * @throws IOException In case of an I/O error
     */
    protected void fillRect(int w, int h, Color col, boolean colorEnabled) throws IOException {
        if ((w == 0) || (h == 0)) {
            return;
        }
        if (h < 0) {
            h *= -1;
        } else {
            //y += h;
        }
        setPatternTransparencyMode(false);
        if (USE_PCL_SHADES
                || Color.black.equals(col)
                || Color.white.equals(col)) {
            writeCommand("*c" + formatDouble4(w / 100.0) + "h"
                              + formatDouble4(h / 100.0) + "V");
            int lineshade = convertToPCLShade(col);
            writeCommand("*c" + lineshade + "G");
            writeCommand("*c2P"); //Shaded fill
        } else {
            if (colorEnabled) {
                selectColor(col);
                writeCommand("*c" + formatDouble4(w / 100.0) + "h"
                        + formatDouble4(h / 100.0) + "V");
                writeCommand("*c0P"); //Solid fill
            } else {
                defineGrayscalePattern(col, 32, DitherUtil.DITHER_MATRIX_4X4);

                writeCommand("*c" + formatDouble4(w / 100.0) + "h"
                                  + formatDouble4(h / 100.0) + "V");
                writeCommand("*c32G");
                writeCommand("*c4P"); //User-defined pattern
            }
        }
        // Reset pattern transparency mode.
        setPatternTransparencyMode(true);
    }

    /**
     * Generates a user-defined pattern for a dithering pattern matching the grayscale value
     * of the color given.
     * @param col the color to create the pattern for
     * @param patternID the pattern ID to use
     * @param ditherMatrixSize the size of the Bayer dither matrix to use (4 or 8 supported)
     * @throws IOException In case of an I/O error
     */
    public void defineGrayscalePattern(Color col, int patternID, int ditherMatrixSize)
            throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(baout);
        data.writeByte(0); //Format
        data.writeByte(0); //Continuation
        data.writeByte(1); //Pixel Encoding
        data.writeByte(0); //Reserved
        data.writeShort(8); //Width in Pixels
        data.writeShort(8); //Height in Pixels
        //data.writeShort(600); //X Resolution (didn't manage to get that to work)
        //data.writeShort(600); //Y Resolution
        int gray255 = convertToGray(col.getRed(), col.getGreen(), col.getBlue());

        byte[] pattern;
        if (ditherMatrixSize == 8) {
            pattern = DitherUtil.getBayerDither(DitherUtil.DITHER_MATRIX_8X8, gray255, false);
        } else {
            //Since a 4x4 pattern did not work, the 4x4 pattern is applied 4 times to an
            //8x8 pattern. Maybe this could be changed to use an 8x8 bayer dither pattern
            //instead of the 4x4 one.
            pattern = DitherUtil.getBayerDither(DitherUtil.DITHER_MATRIX_4X4, gray255, true);
        }
        data.write(pattern);
        if ((baout.size() % 2) > 0) {
            baout.write(0);
        }
        writeCommand("*c" + patternID + "G");
        writeCommand("*c" + baout.size() + "W");
        baout.writeTo(this.out);
        IOUtils.closeQuietly(data);
        IOUtils.closeQuietly(baout);
        writeCommand("*c4Q"); //temporary pattern
    }

    /**
     * Sets the source transparency mode.
     * @param transparent true if transparent, false for opaque
     * @throws IOException In case of an I/O error
     */
    public void setSourceTransparencyMode(boolean transparent) throws IOException {
        setTransparencyMode(transparent, currentPatternTransparency);
    }

    /**
     * Sets the pattern transparency mode.
     * @param transparent true if transparent, false for opaque
     * @throws IOException In case of an I/O error
     */
    public void setPatternTransparencyMode(boolean transparent) throws IOException {
        setTransparencyMode(currentSourceTransparency, transparent);
    }

    /**
     * Sets the transparency modes.
     * @param source source transparency: true if transparent, false for opaque
     * @param pattern pattern transparency: true if transparent, false for opaque
     * @throws IOException In case of an I/O error
     */
    public void setTransparencyMode(boolean source, boolean pattern) throws IOException {
        if (source != currentSourceTransparency && pattern != currentPatternTransparency) {
            writeCommand("*v" + (source ? '0' : '1') + "n" + (pattern ? '0' : '1') + "O");
        } else if (source != currentSourceTransparency) {
            writeCommand("*v" + (source ? '0' : '1') + "N");
        } else if (pattern != currentPatternTransparency) {
            writeCommand("*v" + (pattern ? '0' : '1') + "O");
        }
        this.currentSourceTransparency = source;
        this.currentPatternTransparency = pattern;
    }

    /**
     * Convert an RGB color value to a grayscale from 0 to 100.
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @return the gray value
     */
    public final int convertToGray(int r, int g, int b) {
        return BitmapImageUtil.convertToGray(r, g, b);
    }

    /**
     * Convert a Color value to a PCL shade value (0-100).
     * @param col the color
     * @return the PCL shade value (100=black)
     */
    public final int convertToPCLShade(Color col) {
        float gray = convertToGray(col.getRed(), col.getGreen(), col.getBlue()) / 255f;
        return (int)(100 - (gray * 100f));
    }

    /**
     * Selects the current grayscale color (the given color is converted to grayscales).
     * @param col the color
     * @throws IOException In case of an I/O error
     */
    public void selectGrayscale(Color col) throws IOException {
        if (Color.black.equals(col)) {
            selectCurrentPattern(0, 0); //black
        } else if (Color.white.equals(col)) {
            selectCurrentPattern(0, 1); //white
        } else {
            if (USE_PCL_SHADES) {
                selectCurrentPattern(convertToPCLShade(col), 2);
            } else {
                defineGrayscalePattern(col, 32, DitherUtil.DITHER_MATRIX_4X4);
                selectCurrentPattern(32, 4);
            }
        }
    }

    public void selectColor(Color col) throws IOException {
        writeCommand("*v6W");
        writeBytes(new byte[]{0, 1, 1, 8, 8, 8});
        writeCommand(String.format("*v%da%db%dc0I", col.getRed(), col.getGreen(), col.getBlue()));
        writeCommand("*v0S");
    }

    /**
     * Select the current pattern
     * @param patternID the pattern ID (&lt;ESC&gt;*c#G command)
     * @param pattern the pattern type (&lt;ESC&gt;*v#T command)
     * @throws IOException In case of an I/O error
     */
    public void selectCurrentPattern(int patternID, int pattern) throws IOException {
        if (pattern > 1) {
            writeCommand("*c" + patternID + "G");
        }
        writeCommand("*v" + pattern + "T");
    }

    /**
     * Sets the dithering quality used when encoding gray or color images. If not explicitely
     * set a medium setting (0.5f) is used.
     * @param quality a quality setting between 0.0f (worst/fastest) and 1.0f (best/slowest)
     */
    public void setDitheringQuality(float quality) {
        quality = Math.min(Math.max(0f, quality), 1.0f);
        this.ditheringQuality = quality;
    }

    /**
     * Returns the dithering quality used when encoding gray or color images.
     * @return the quality setting between 0.0f (worst/fastest) and 1.0f (best/slowest)
     */
    public float getDitheringQuality() {
        return this.ditheringQuality;
    }

    /**
     * Indicates whether an image is a monochrome (b/w) image.
     * @param img the image
     * @return true if it's a monochrome image
     */
    public static boolean isMonochromeImage(RenderedImage img) {
        return BitmapImageUtil.isMonochromeImage(img);
    }

    /**
     * Indicates whether an image is a grayscale image.
     * @param img the image
     * @return true if it's a grayscale image
     */
    public static boolean isGrayscaleImage(RenderedImage img) {
        return BitmapImageUtil.isGrayscaleImage(img);
    }

    private static int jaiAvailable = -1; //no synchronization necessary, not critical

    /**
     * Indicates whether JAI is available. JAI has shown to be reliable when dithering a
     * grayscale or color image to monochrome bitmaps (1-bit).
     * @return true if JAI is available
     */
    public static boolean isJAIAvailable() {
        if (jaiAvailable < 0) {
            try {
                String clName = "javax.media.jai.JAI";
                Class.forName(clName);
                jaiAvailable = 1;
            } catch (ClassNotFoundException cnfe) {
                jaiAvailable = 0;
            }
        }
        return (jaiAvailable > 0);
    }

    private int calculatePCLResolution(int resolution) {
        return calculatePCLResolution(resolution, false);
    }

    /**
     * Calculates the ideal PCL resolution for a given resolution.
     * @param resolution the input resolution
     * @param increased true if you want to go to a higher resolution, for example if you
     *                  convert grayscale or color images to monochrome images so dithering has
     *                  a chance to generate better quality.
     * @return the resulting PCL resolution (one of 75, 100, 150, 200, 300, 600)
     */
    private int calculatePCLResolution(int resolution, boolean increased) {
        int choice = -1;
        for (int i = PCL_RESOLUTIONS.length - 2; i >= 0; i--) {
            if (resolution > PCL_RESOLUTIONS[i]) {
                int idx = i + 1;
                if (idx < PCL_RESOLUTIONS.length - 2) {
                    idx += increased ? 2 : 0;
                } else if (idx < PCL_RESOLUTIONS.length - 1) {
                    idx += increased ? 1 : 0;
                }
                choice = idx;
                break;
                //return PCL_RESOLUTIONS[idx];
            }
        }
        if (choice < 0) {
            choice = (increased ? 2 : 0);
        }
        while (choice > 0 && PCL_RESOLUTIONS[choice] > getMaximumBitmapResolution()) {
            choice--;
        }
        return PCL_RESOLUTIONS[choice];
    }

    private boolean isValidPCLResolution(int resolution) {
        return resolution == calculatePCLResolution(resolution);
    }

    //Threshold table to convert an alpha channel (8-bit) into a clip mask (1-bit)
    private static final byte[] THRESHOLD_TABLE = new byte[256];
    static { // Initialize the arrays
        for (int i = 0; i < 256; i++) {
            THRESHOLD_TABLE[i] = (byte) ((i < 240) ? 255 : 0);
        }
    }

    /* not used
    private RenderedImage getMask(RenderedImage img, Dimension targetDim) {
        ColorModel cm = img.getColorModel();
        if (cm.hasAlpha()) {
            BufferedImage alpha = new BufferedImage(img.getWidth(), img.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY);
            Raster raster = img.getData();
            GraphicsUtil.copyBand(raster, cm.getNumColorComponents(), alpha.getRaster(), 0);

            BufferedImageOp op1 = new LookupOp(new ByteLookupTable(0, THRESHOLD_TABLE), null);
            BufferedImage alphat = op1.filter(alpha, null);

            BufferedImage mask;
            if (true) {
                mask = new BufferedImage(targetDim.width, targetDim.height,
                        BufferedImage.TYPE_BYTE_BINARY);
            } else {
                byte[] arr = {(byte)0, (byte)0xff};
                ColorModel colorModel = new IndexColorModel(1, 2, arr, arr, arr);
                WritableRaster wraster = Raster.createPackedRaster(DataBuffer.TYPE_BYTE,
                                                   targetDim.width, targetDim.height, 1, 1, null);
                mask = new BufferedImage(colorModel, wraster, false, null);
            }

            Graphics2D g2d = mask.createGraphics();
            try {
                AffineTransform at = new AffineTransform();
                double sx = targetDim.getWidth() / img.getWidth();
                double sy = targetDim.getHeight() / img.getHeight();
                at.scale(sx, sy);
                g2d.drawRenderedImage(alphat, at);
            } finally {
                g2d.dispose();
            }
            return mask;
        } else {
            return null;
        }
    }
    */

    /**
     * Paint a bitmap at the current cursor position. The bitmap is converted to a monochrome
     * (1-bit) bitmap image.
     * @param img the bitmap image
     * @param targetDim the target Dimention (in mpt)
     * @param sourceTransparency true if the background should not be erased
     * @throws IOException In case of an I/O error
     */
    public void paintBitmap(RenderedImage img, Dimension targetDim, boolean sourceTransparency,
                            PCLRenderingUtil pclUtil) throws IOException {
        final boolean printerSupportsColor = pclUtil.isColorEnabled();
        boolean monochrome = isMonochromeImage(img);
        double targetHResolution = img.getWidth() / UnitConv.mpt2in(targetDim.width);
        double targetVResolution = img.getHeight() / UnitConv.mpt2in(targetDim.height);
        double targetResolution = Math.max(targetHResolution, targetVResolution);
        int resolution = (int)Math.round(targetResolution);
        int effResolution = calculatePCLResolution(resolution, !(printerSupportsColor && !monochrome));
        Dimension orgDim = new Dimension(img.getWidth(), img.getHeight());
        Dimension effDim;
        if (targetResolution == effResolution) {
            effDim = orgDim; //avoid scaling side-effects
        } else {
            effDim = new Dimension(
                    (int)Math.ceil(UnitConv.mpt2px(targetDim.width, effResolution)),
                    (int)Math.ceil(UnitConv.mpt2px(targetDim.height, effResolution)));
        }
        boolean scaled = !orgDim.equals(effDim);
        if (!monochrome) {
            if (printerSupportsColor) {
                RenderedImage effImg = img;
                if (scaled) {
                    effImg = BitmapImageUtil.convertTosRGB(img, effDim);
                }
                selectCurrentPattern(0, 0); //Solid black
                renderImageAsColor(effImg, effResolution);
            } else {
                //Transparency mask disabled. Doesn't work reliably
                /*
                final boolean transparencyDisabled = true;
                RenderedImage mask = (transparencyDisabled ? null : getMask(img, effDim));
                if (mask != null) {
                    pushCursorPos();
                    selectCurrentPattern(0, 1); //Solid white
                    setTransparencyMode(true, true);
                    paintMonochromeBitmap(mask, effResolution);
                    popCursorPos();
                }
                */

                RenderedImage red = BitmapImageUtil.convertToMonochrome(
                        img, effDim, this.ditheringQuality);
                selectCurrentPattern(0, 0); //Solid black
                setTransparencyMode(sourceTransparency /*|| mask != null*/, true);
                paintMonochromeBitmap(red, effResolution);
            }
        } else {
            RenderedImage effImg = img;
            if (scaled) {
                effImg = BitmapImageUtil.convertToMonochrome(img, effDim);
            }
            setSourceTransparencyMode(sourceTransparency);
            selectCurrentPattern(0, 0); //Solid black
            paintMonochromeBitmap(effImg, effResolution);
        }
    }

    private int toGray(int rgb) {
        // see http://www.jguru.com/faq/view.jsp?EID=221919
        double greyVal = 0.072169d * (rgb & 0xff);
        rgb >>= 8;
        greyVal += 0.715160d * (rgb & 0xff);
        rgb >>= 8;
        greyVal += 0.212671d * (rgb & 0xff);
        return (int)greyVal;
    }

    private void renderImageAsColor(RenderedImage imgOrg, int dpi) throws IOException {
        BufferedImage img = new BufferedImage(imgOrg.getWidth(), imgOrg.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imgOrg.getWidth(), imgOrg.getHeight());
        g.drawImage((Image) imgOrg, 0, 0, null);

        if (!isValidPCLResolution(dpi)) {
            throw new IllegalArgumentException("Invalid PCL resolution: " + dpi);
        }
        int w = img.getWidth();
        ColorModel cm = img.getColorModel();
        if (cm instanceof DirectColorModel) {
            writeCommand("*v6W");           // ImagingMode
            out.write(new byte[]{0, 3, 0, 8, 8, 8});
        } else {
            IndexColorModel icm = (IndexColorModel)cm;
            writeCommand("*v6W");     // ImagingMode
            out.write(new byte[]{0, 1, (byte)icm.getMapSize(), 8, 8, 8});

            byte[] reds = new byte[256];
            byte[] greens = new byte[256];
            byte[] blues = new byte[256];

            icm.getReds(reds);
            icm.getGreens(greens);
            icm.getBlues(blues);
            for (int i = 0; i < icm.getMapSize(); i++) {
                writeCommand("*v" + (reds[i] & 0xFF) + "A");    //ColorComponentOne
                writeCommand("*v" + (greens[i] & 0xFF) + "B");  //ColorComponentTwo
                writeCommand("*v" + (blues[i] & 0xFF) + "C");   //ColorComponentThree
                writeCommand("*v" + i + "I");          //AssignColorIndex
            }
        }
        setRasterGraphicsResolution(dpi);
        writeCommand("*r0f" + img.getHeight() + "t" + (w) + "S");
        writeCommand("*r1A");

        Raster raster = img.getData();

        ColorEncoder encoder = new ColorEncoder(img);
        // Transfer graphics data
        if (cm.getTransferType() == DataBuffer.TYPE_BYTE) {
            DataBufferByte dataBuffer = (DataBufferByte)raster.getDataBuffer();
             if (img.getSampleModel() instanceof MultiPixelPackedSampleModel && dataBuffer.getNumBanks() == 1) {
                byte[] buf = dataBuffer.getData();
                MultiPixelPackedSampleModel sampleModel = (MultiPixelPackedSampleModel)img.getSampleModel();
                int scanlineStride = sampleModel.getScanlineStride();
                int idx = 0;
                for (int y = 0, maxy = img.getHeight(); y < maxy; y++) {
                    for (int x = 0; x < scanlineStride; x++) {
                        encoder.add8Bits(buf[idx]);
                        idx++;
                    }
                    encoder.endLine();
                }
             } else {
                 throw new IOException("Unsupported image");
             }
        } else if (cm.getTransferType() == DataBuffer.TYPE_INT) {
             DataBufferInt dataBuffer = (DataBufferInt)raster.getDataBuffer();
             if (img.getSampleModel() instanceof SinglePixelPackedSampleModel && dataBuffer.getNumBanks() == 1) {
                int[] buf = dataBuffer.getData();
                SinglePixelPackedSampleModel sampleModel = (SinglePixelPackedSampleModel)img.getSampleModel();
                int scanlineStride = sampleModel.getScanlineStride();
                int idx = 0;
                for (int y = 0, maxy = img.getHeight(); y < maxy; y++) {
                    for (int x = 0; x < scanlineStride; x++) {
                        encoder.add8Bits((byte)(buf[idx] >> 16));
                        encoder.add8Bits((byte)(buf[idx] >> 8));
                        encoder.add8Bits((byte)(buf[idx] >> 0));
                        idx++;
                    }
                    encoder.endLine();
                }
             } else {
                 throw new IOException("Unsupported image");
             }
        } else {
            throw new IOException("Unsupported image");
        }
        // End raster graphics
        writeCommand("*rB");
    }
    /**
     * Paint a bitmap at the current cursor position. The bitmap must be a monochrome
     * (1-bit) bitmap image.
     * @param img the bitmap image (must be 1-bit b/w)
     * @param resolution the resolution of the image (must be a PCL resolution)
     * @throws IOException In case of an I/O error
     */
    public void paintMonochromeBitmap(RenderedImage img, int resolution) throws IOException {
        if (!isValidPCLResolution(resolution)) {
            throw new IllegalArgumentException("Invalid PCL resolution: " + resolution);
        }
        boolean monochrome = isMonochromeImage(img);
        if (!monochrome) {
            throw new IllegalArgumentException("img must be a monochrome image");
        }

        setRasterGraphicsResolution(resolution);
        writeCommand("*r0f" + img.getHeight() + "t" + img.getWidth() + "s1A");
        Raster raster = img.getData();

        Encoder encoder = new Encoder(img);
        // Transfer graphics data
        int imgw = img.getWidth();
        IndexColorModel cm = (IndexColorModel)img.getColorModel();
        if (cm.getTransferType() == DataBuffer.TYPE_BYTE) {
            DataBufferByte dataBuffer = (DataBufferByte)raster.getDataBuffer();
            MultiPixelPackedSampleModel packedSampleModel = new MultiPixelPackedSampleModel(
                    DataBuffer.TYPE_BYTE, img.getWidth(), img.getHeight(), 1);
            if (img.getSampleModel().equals(packedSampleModel)
                    && dataBuffer.getNumBanks() == 1) {
                //Optimized packed encoding
                byte[] buf = dataBuffer.getData();
                int scanlineStride = packedSampleModel.getScanlineStride();
                int idx = 0;
                int c0 = toGray(cm.getRGB(0));
                int c1 = toGray(cm.getRGB(1));
                boolean zeroIsWhite = c0 > c1;
                for (int y = 0, maxy = img.getHeight(); y < maxy; y++) {
                    for (int x = 0, maxx = scanlineStride; x < maxx; x++) {
                        if (zeroIsWhite) {
                            encoder.add8Bits(buf[idx]);
                        } else {
                            encoder.add8Bits((byte)~buf[idx]);
                        }
                        idx++;
                    }
                    encoder.endLine();
                }
            } else {
                //Optimized non-packed encoding
                for (int y = 0, maxy = img.getHeight(); y < maxy; y++) {
                    byte[] line = (byte[])raster.getDataElements(0, y, imgw, 1, null);
                    for (int x = 0, maxx = imgw; x < maxx; x++) {
                        encoder.addBit(line[x] == 0);
                    }
                    encoder.endLine();
                }
            }
        } else {
            //Safe but slow fallback
            for (int y = 0, maxy = img.getHeight(); y < maxy; y++) {
                for (int x = 0, maxx = imgw; x < maxx; x++) {
                    int sample = raster.getSample(x, y, 0);
                    encoder.addBit(sample == 0);
                }
                encoder.endLine();
            }
        }

        // End raster graphics
        writeCommand("*rB");
    }

    private class Encoder {

        private int imgw;
        private int bytewidth;
        private byte[] rle; //compressed (RLE)
        private byte[] uncompressed; //uncompressed
        private int lastcount = -1;
        private byte lastbyte;
        private int rlewidth;
        private byte ib; //current image bits
        private int x;
        private boolean zeroRow = true;

        public Encoder(RenderedImage img) {
            imgw = img.getWidth();
            bytewidth = (imgw / 8);
            if ((imgw % 8) != 0) {
                bytewidth++;
            }
            rle = new byte[bytewidth * 2];
            uncompressed = new byte[bytewidth];
        }

        public void addBit(boolean bit) {
            //Set image bit for black
            if (bit) {
                ib |= 1;
            }

            //RLE encoding
            if ((x % 8) == 7 || ((x + 1) == imgw)) {
                finishedByte();
            } else {
                ib <<= 1;
            }
            x++;
        }

        public void add8Bits(byte b) {
            ib = b;
            finishedByte();
            x += 8;
        }

        private void finishedByte() {
            if (rlewidth < bytewidth) {
                if (lastcount >= 0) {
                    if (ib == lastbyte) {
                        lastcount++;
                    } else {
                        rle[rlewidth++] = (byte)(lastcount & 0xFF);
                        rle[rlewidth++] = lastbyte;
                        lastbyte = ib;
                        lastcount = 0;
                    }
                } else {
                    lastbyte = ib;
                    lastcount = 0;
                }
                if (lastcount == 255 || ((x + 1) == imgw)) {
                    rle[rlewidth++] = (byte)(lastcount & 0xFF);
                    rle[rlewidth++] = lastbyte;
                    lastbyte = 0;
                    lastcount = -1;
                }
            }
            uncompressed[x / 8] = ib;
            if (ib != 0) {
                zeroRow = false;
            }
            ib = 0;
        }

        public void endLine() throws IOException {
            if (zeroRow && PCLGenerator.this.currentSourceTransparency) {
                writeCommand("*b1Y");
            } else if (rlewidth < bytewidth) {
                writeCommand("*b1m" + rlewidth + "W");
                out.write(rle, 0, rlewidth);
            } else {
                writeCommand("*b0m" + bytewidth + "W");
                out.write(uncompressed);
            }
            lastcount = -1;
            rlewidth = 0;
            ib = 0;
            x = 0;
            zeroRow = true;
        }


    }

    private class ColorEncoder {
        private int imgw;
        private int bytewidth;
        private byte ib; //current image bits

        private int currentIndex;
        private int len;
        private int shiftBit = 0x80;
        private int whiteLines;
        final byte[] zeros;
        final byte[] buff1;
        final byte[] buff2;
        final byte[] encodedRun;
        final byte[] encodedTagged;
        final byte[] encodedDelta;
        byte[] seed;
        byte[] current;
        int compression;
        int seedLen;

        public ColorEncoder(RenderedImage img) {
            imgw = img.getWidth();
            bytewidth = imgw * 3 + 1;

            zeros         = new byte[bytewidth];
            buff1         = new byte[bytewidth];
            buff2         = new byte[bytewidth];
            encodedRun    = new byte[bytewidth];
            encodedTagged = new byte[bytewidth];
            encodedDelta  = new byte[bytewidth];

            seed    = buff1;
            current = buff2;

            seedLen = 0;
            compression = (-1);
            System.arraycopy(zeros, 0, seed, 0, zeros.length);

        }

        private int runCompression(byte[] buff, int len) {
            int bytes = 0;

            try {
                for (int i = 0; i < len;) {
                    int sameCount;
                    byte seed = current[i++];

                    for (sameCount = 1; i < len && current[i] == seed; i++) {
                        sameCount++;
                    }

                    for (; sameCount > 256; sameCount -= 256) {
                        buff[bytes++] = (byte)255;
                        buff[bytes++] = seed;
                    }
                    if (sameCount > 0) {
                        buff[bytes++] = (byte)(sameCount - 1);
                        buff[bytes++] = seed;
                    }

                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return len + 1;
            }
            return bytes;
        }

        private int deltaCompression(byte[] seed, byte[] buff, int len) {
            int bytes = 0;

            try {
                for (int i = 0; i < len;) {
                    int sameCount;
                    int diffCount;

                    for (sameCount = 0; i < len && current[i] == seed[i]; i++) {
                        sameCount++;
                    }
                    for (diffCount = 0; i < len && current[i] != seed[i]; i++) {
                        diffCount++;
                    }

                    for (; diffCount != 0;) {
                        int diffToWrite = (diffCount > 8) ?  8 : diffCount;
                        int sameToWrite = (sameCount > 31) ? 31 : sameCount;

                        buff[bytes++] = (byte)(((diffToWrite - 1) << 5) | sameToWrite);
                        sameCount -= sameToWrite;
                        if (sameToWrite == 31) {
                            for (; sameCount >= 255; sameCount -= 255) {
                                buff[bytes++] = (byte)255;
                            }
                            buff[bytes++] = (byte)sameCount;
                            sameCount = 0;
                        }

                        System.arraycopy(current, i - diffCount, buff, bytes, diffToWrite);
                        bytes += diffToWrite;

                        diffCount -= diffToWrite;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return len + 1;
            }
            return bytes;
        }

        private int tiffCompression(byte[] encodedTagged, int len) {
            int literalCount = 0;
            int bytes = 0;

            try {
                for (int from = 0; from < len;) {
                    int repeatLength;
                    int repeatValue = current[from];

                    for (repeatLength = 1; repeatLength < 128
                            && from + repeatLength < len
                            && current[from + repeatLength] == repeatValue;) {
                        repeatLength++;
                    }

                    if (literalCount == 128 || (repeatLength > 2 && literalCount > 0)) {
                        encodedTagged[bytes++] = (byte)(literalCount - 1);
                        System.arraycopy(current, from - literalCount, encodedTagged, bytes, literalCount);
                        bytes += literalCount;
                        literalCount = 0;
                    }
                    if (repeatLength > 2) {
                        encodedTagged[bytes++] = (byte)(1 - repeatLength);
                        encodedTagged[bytes++] = current[from];
                        from += repeatLength;
                    } else {
                        literalCount++;
                        from++;
                    }
                }
                if (literalCount > 0) {
                    encodedTagged[bytes++] = (byte)(literalCount - 1);
                    System.arraycopy(current, (3 * len) - literalCount, encodedTagged, bytes, literalCount);
                    bytes += literalCount;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return len + 1;
            }
            return bytes;
        }

        public void addBit(boolean bit) {
            //Set image bit for black
            if (bit) {
                ib |= shiftBit;
            }
            shiftBit >>= 1;
            if (shiftBit == 0) {
                add8Bits(ib);
                shiftBit = 0x80;
                ib = 0;
            }
        }

        public void add8Bits(byte b) {
            current[currentIndex++] = b;
            if (b != 0) {
                len = currentIndex;
            }
        }

        public void endLine() throws IOException {
            if (len == 0) {
                whiteLines++;
            } else {
                if (whiteLines > 0) {
                    writeCommand("*b" + whiteLines + "Y");
                    whiteLines = 0;
                }

                int unencodedCount = len;
                int runCount = runCompression(encodedRun, len);
                int tiffCount = tiffCompression(encodedTagged, len);
                int deltaCount = deltaCompression(seed, encodedDelta, Math.max(len, seedLen));

                int bestCount = Math.min(unencodedCount, Math.min(runCount, Math.min(tiffCount, deltaCount)));
                int bestCompression;

                if (bestCount == unencodedCount) {
                    bestCompression = 0;
                } else if (bestCount == runCount) {
                    bestCompression = 1;
                } else if (bestCount == tiffCount) {
                    bestCompression = 2;
                } else {
                    bestCompression = 3;
                }

                if (compression != bestCompression) {
                    compression = bestCompression;
                    writeCommand("*b" + compression + "M");
                }

                if (bestCompression == 0) {
                    writeCommand("*b" + unencodedCount + "W");
                    out.write(current, 0, unencodedCount);
                } else if (bestCompression == 1) {
                    writeCommand("*b" + runCount + "W");
                    out.write(encodedRun, 0, runCount);
                } else if (bestCompression == 2) {
                    writeCommand("*b" + tiffCount + "W");
                    out.write(encodedTagged, 0, tiffCount);
                } else if (bestCompression == 3) {
                    writeCommand("*b" + deltaCount + "W");
                    out.write(encodedDelta, 0, deltaCount);
                }

                if (current == buff1) {
                    seed    = buff1;
                    current = buff2;
                } else {
                    seed    = buff2;
                    current = buff1;
                }
                seedLen = len;
            }
            shiftBit = 0x80;
            ib = 0;
            len = 0;
            currentIndex = 0;
        }
    }

}
