package ch.epfl.gameboj;

import java.awt.AWTException;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192)
 *
 *         Classe qui lance tout ce qui faut pour faire fonctionner la gameboj,
 *         en attachant les différents composants à un bus commun
 * 
 */
public class GameBoy {
    private Bus bus = new Bus();
    private Ram workRAM;
    private Cpu cpu;
    private RamController workControl;
    private RamController workCopy;
    private BootRomController controller;
    private long cycles = 0;
    private Timer timer;
    private LcdController lcdControl;
    private Joypad joyPad;
    private Cartridge loadedCartridge;
    private BootRomController bootRomController;
    private static final long CYCLES_PER_SECOND = (long) Math.pow(2, 20);
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND
            * Math.pow(10, -9);

    /**
     * Construit une gameboy
     * 
     * @param cartridge
     *            représente la cartouche
     * @throws NullPointerException
     *             si la cartouche est nulle
     */
    public GameBoy(Cartridge cartridge) {

        Preconditions.checkNull(cartridge);

        cpu = new Cpu();
        timer = new Timer(cpu);
        controller = new BootRomController(cartridge);
        workRAM = new Ram(AddressMap.WORK_RAM_SIZE);

        workControl = new RamController(workRAM, AddressMap.WORK_RAM_START,
                AddressMap.WORK_RAM_END);
        workCopy = new RamController(workRAM, AddressMap.ECHO_RAM_START,
                AddressMap.ECHO_RAM_END);
        lcdControl = new LcdController(cpu);
        joyPad = new Joypad(cpu);

        workCopy.attachTo(bus);
        controller.attachTo(bus);
        timer.attachTo(bus);
        cpu.attachTo(bus);
        lcdControl.attachTo(bus);
        workControl.attachTo(bus);
        joyPad.attachTo(bus);

    }

    public GameBoy(Cartridge cartridge, String saveFileName)
            throws LineUnavailableException {
        loadedCartridge = cartridge;

        bootRomController = new BootRomController(
                Objects.requireNonNull(cartridge));
        try (FileInputStream fis = new FileInputStream(saveFileName)) {
            bootRomController.setCartridgeRam(fis.readAllBytes());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Preconditions.checkNull(cartridge);

        cpu = new Cpu();
        timer = new Timer(cpu);
        controller = new BootRomController(cartridge);
        workRAM = new Ram(AddressMap.WORK_RAM_SIZE);

        workControl = new RamController(workRAM, AddressMap.WORK_RAM_START,
                AddressMap.WORK_RAM_END);
        workCopy = new RamController(workRAM, AddressMap.ECHO_RAM_START,
                AddressMap.ECHO_RAM_END);
        lcdControl = new LcdController(cpu);
        joyPad = new Joypad(cpu);

        workCopy.attachTo(bus);
        controller.attachTo(bus);
        timer.attachTo(bus);
        cpu.attachTo(bus);
        lcdControl.attachTo(bus);
        workControl.attachTo(bus);
        joyPad.attachTo(bus);

    }

    /**
     * Retourne le bus
     * 
     * @return le bus
     */
    public Bus bus() {
        return bus;
    }

    /**
     * Retourne le cpu
     * 
     * @return le cpu
     */
    public Cpu cpu() {
        return cpu;
    }

    /**
     * retourne le nombre de cycle effectué
     * 
     * @return cycles
     */
    public long cycles() {

        return cycles;

    }

    /**
     * Simule le fonctionnement de la gameboy, en incrémentant les cycles
     * 
     * @param cycle
     *            le nombre de cycle que la gameboy doit effectuer
     * @throws IllegalArgumentException
     *             si le cycle actuel de la gameboy est plus grand que le nombre
     *             de cycle que la gameboy doit faire
     */
    public void runUntil(long cycle) {

        Preconditions.checkArgument(cycles() <= cycle);

        while (cycles() < cycle) {

            timer.cycle(cycles);
            lcdControl.cycle(cycles);
            cpu.cycle(cycles);
            ++cycles;
        }
    }

    /**
     * Retourne le timer
     * 
     * @return timer
     */
    public Timer getTimer() {
        return timer;
    }

    public LcdController getLcdController() {
        return lcdControl;
    }

    public Joypad getJoyPad() {

        return joyPad;

    }

    public void saveAsPng()
            throws HeadlessException, AWTException, IOException {
        BufferedImage image = new Robot().createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ImageIO.write(image, "png", new File("/screenshot.png"));
    }

    public Cartridge getCartridge() {
        return loadedCartridge;
    }

}