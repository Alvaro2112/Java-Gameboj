package ch.epfl.gameboj.gui;

import java.awt.image.BufferedImage;

import java.io.InputStream;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192)
 *
 *         Sert a créer une méthode qui attribue une couleur aux bits
 */
public final class ImageConverter {

    private static final int[] COLOR_MAP_BASE = new int[] { 0xFF_FF_FF_FF,
            0xFF_D3_D3_D3, 0xFF_A9_A9_A9, 0xFF_00_00_00 };
    private static final int[] COLOR_MAP_OCEAN = new int[] { 0xFF_FF_FF_FF,
            0xFF_00_FF_FF, 0xFF_A9_FF_FF, 0xFF_00_00_00 };
    private static final int[] COLOR_MAP_ORIGINAL = new int[] { 0xFF_9E_FD_A9,
            0xFF_00_60_10, 0xFF_16_B8_4E, 0xFF_00_00_00 };
    private static final int[] COLOR_MAP_APOCALYPSE = new int[] { 0xFF_9E_FD_A9,
            0xFF_00_60_10, 0xFF_16_B8_4E, 0xFF_99_00_00 };

    private static int color = 0;

    private ImageConverter() {
    }

    /**
     * 
     * Donne une couleur aux bits
     * 
     * @param imagelcd
     *            donne les bits de l'image
     * @return une Image avec 4 couleurs
     */
    public static void changeColor(int col) {
        Preconditions.checkArgument(col < 5);
        color = col;
    }

    public static Image convert(LcdImage imagelcd) {
        Preconditions.checkArgument(
                imagelcd.getHeight() == LcdController.LCD_HEIGHT);
        Preconditions
                .checkArgument(imagelcd.getWidth() == LcdController.LCD_WIDTH);

        WritableImage wImage = new WritableImage(LcdController.LCD_WIDTH,
                LcdController.LCD_HEIGHT);
        PixelWriter pixWriter = wImage.getPixelWriter();

        if (color == 1) {
            for (int y = 0; y < 144; ++y) {
                for (int x = 0; x < 160; ++x) {
                    pixWriter.setArgb(x, y,
                            COLOR_MAP_OCEAN[imagelcd.getColor(x, y)]);
                }
            }

        } else if (color == 2) {
            for (int y = 0; y < 144; ++y) {
                for (int x = 0; x < 160; ++x) {
                    pixWriter.setArgb(x, y,
                            COLOR_MAP_ORIGINAL[imagelcd.getColor(x, y)]);
                }
            }

        } else if (color == 3) {
            for (int y = 0; y < 144; ++y) {
                for (int x = 0; x < 160; ++x) {
                    pixWriter.setArgb(x, y,
                            COLOR_MAP_APOCALYPSE[imagelcd.getColor(x, y)]);
                }
            }

        } else {
            for (int y = 0; y < 144; ++y) {
                for (int x = 0; x < 160; ++x) {

                    pixWriter.setArgb(x, y,
                            COLOR_MAP_BASE[imagelcd.getColor(x, y)]);
                }
            }

        }

        return (Image) wImage;

    }

}