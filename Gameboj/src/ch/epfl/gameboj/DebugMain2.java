package ch.epfl.gameboj;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdImage;

public final class DebugMain2 {

    private static final int[] COLOR_MAP = new int[] { 0xFF_FF_FF, 0xD3_D3_D3,
            0xA9_A9_A9, 0x00_00_00 };

    public static void main(String[] args) throws IOException {
        
        long startTime = System.nanoTime();

        File romFile = new File("sprite_priority.gb");
        long cycles = (30000000);

        GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));
        gb.runUntil(cycles);

        System.out.println("+--------------------+");
        for (int y = 0; y < 18; ++y) {
            System.out.print("|");
            for (int x = 0; x < 20; ++x) {
                char c = (char) gb.bus().read(0x9800 + 32 * y + x);
                System.out.print(Character.isISOControl(c) ? " " : c);
            }
            System.out.println("|");
        }
        System.out.println("+--------------------+");

        LcdImage li = gb.getLcdController().currentImage();

        BufferedImage i = new BufferedImage(li.getWidth(), li.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < li.getHeight(); ++y)
            for (int x = 0; x < li.getWidth(); ++x) {         
                i.setRGB(x, y, COLOR_MAP[li.getColor(x, y)]);}
        ImageIO.write(i, "png", new File("gb.png"));
        
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        System.out.println(totalTime/Math.pow(10, 9));
    }
    
}