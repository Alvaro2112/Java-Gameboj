package ch.epfl.gameboj.component.lcd;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Arrays;
import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;

/**
 * 
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Représente le controlleur de la GameBoy,
 */
public final class LcdController implements Clocked, Component {

    private final Cpu cpu;
    private Bus bus;
    private LcdImage currentImage;
    private final Ram videoRam;
    private final Ram spriteRam;

    // *Addresses*
    private final int ADDRESS_LCDC = 0xFF40;
    private final int ADDRESS_STAT = 0xFF41;
    private final int ADDRESS_SCY = 0xFF42;
    private final int ADDRESS_SCX = 0xFF43;
    private final int ADDRESS_LY = 0xFF44;
    private final int ADDRESS_LYC = 0xFF45;
    private final int ADDRESS_DMA = 0xFF46;
    private final int ADDRESS_BGP = 0xFF47;
    private final int ADDRESS_OBP0 = 0xFF48;
    private final int ADDRESS_OBP1 = 0xFF49;
    private final int ADDRESS_WY = 0xFF4A;
    private final int ADDRESS_WX = 0xFF4B;
    private final int TILE_ADDRESS_OVERFLOW = 128;

    // *Copy*
    private boolean dmaChanged = false;
    private int cyclesSinceCopy = 0;

    // *Cycles*
    private final int LINE_CYCLES = 114;
    private long nextNonIdleCycle = 0;
    private long lcdOnCycle = 0;
    private int cyclesSinceLine = 0;
    private final int IMAGE_CYCLES = 17556;
    private LcdImage.Builder nextImageBuilder;

    // *Sprites*
    private final int SPRITES_X_OFFSET = 8;
    private final int SPRITES_Y_OFFSET = 16;
    private final int MAX_SPRITES_IN_LINE = 10;
    private final int MAX_SPRITES_IN_MEMORY = 40;
    private final int TILE_SIZE = 8;

    // *Image*
    private final int BACKGROUND_WIDTH = 256;
    private final int TILES_IN_LINE = 32;
    private final int WX_X_OFFSET = 7;
    private int winY = 0;
    public static final int LCD_HEIGHT = 144;
    public static final int LCD_WIDTH = 160;

    final static private BitVector NULL_VECTOR = new BitVector(LCD_WIDTH,
            false);

    final static private LcdImageLine NULL_LINE = new LcdImageLine(NULL_VECTOR,
            NULL_VECTOR, NULL_VECTOR);

    /**
     * Represente les registres utilises dans le LcdControleur
     */
    private enum Regs implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    private RegisterFile<Register> regs = new RegisterFile<>(Regs.values());

    /**
     * Cree un LcdController a partir dun cpu
     * 
     * @param cpu
     * @throws NullPointerException
     *             si le cpu passé en argument est null
     */
    public LcdController(Cpu cpu) {

        this.cpu = Objects.requireNonNull(cpu);
        this.videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        this.spriteRam = new Ram(AddressMap.OAM_RAM_SIZE);

    }

    /**
     * 
     * @return L'Image actuelle du controlleur
     */
    public LcdImage currentImage() {

        if (currentImage == null) {

            List<LcdImageLine> list = new ArrayList<>(LCD_HEIGHT);

            for (int i = 0; i < LCD_HEIGHT; i++)
                list.add(NULL_LINE);

            return new LcdImage(LCD_WIDTH, LCD_HEIGHT, list);

        } else

            return currentImage;
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int address)
     */
    @Override
    public int read(int address) {

        Preconditions.checkBits16(address);

        if (address >= AddressMap.VIDEO_RAM_START
                && address < AddressMap.VIDEO_RAM_END)

            return videoRam.read(address - AddressMap.VIDEO_RAM_START);

        else if (address >= AddressMap.REGS_LCDC_START
                && address < AddressMap.REGS_LCDC_END) {

            if (address == ADDRESS_STAT)
                return regs.get(Regs.STAT);

            return regRead(address);
        }

        else if (address >= AddressMap.OAM_START & address < AddressMap.OAM_END)
            return spriteRam.read(address - AddressMap.OAM_START);

        return NO_DATA;
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int address, int data)
     */
    @Override
    public void write(int address, int data) {

        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        if (address >= AddressMap.VIDEO_RAM_START
                && address < AddressMap.VIDEO_RAM_END)
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);

        else if (address >= AddressMap.REGS_LCDC_START
                && address < AddressMap.REGS_LCDC_END)

            switch (address) {

            case ADDRESS_LCDC:
                regWrite(ADDRESS_LCDC, data);
                break;

            case ADDRESS_STAT:
                regWrite(ADDRESS_STAT, data);
                break;

            case ADDRESS_LY:
                break;

            case ADDRESS_LYC:
                regWrite(ADDRESS_LYC, data);
                break;

            default:
                regWrite(address, data);
                break;
            }

        else if (address >= AddressMap.OAM_START & address < AddressMap.OAM_END)
            spriteRam.write(address - AddressMap.OAM_START, data);
    }

    /**
     * Change le mode du Controlleur
     * 
     * @param mode
     * @throws IllegalArgumentException
     *             si le mode passé en argument nest pas compris entre 0 et 3
     */
    private void changeMode(int mode) {

        Preconditions.checkArgument(mode >= 0 && mode <= 3);
        switch (mode) {

        case 0:
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 0, false));
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 1, false));
            if (Bits.test(regs.get(Regs.STAT), 3))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            break;

        case 1:
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 0, true));
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 1, false));
            cpu.requestInterrupt(Interrupt.VBLANK);

            if (Bits.test(regs.get(Regs.STAT), 4))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            cpu.requestInterrupt(Interrupt.VBLANK);
            break;

        case 2:
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 0, false));
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 1, true));

            if (Bits.test(regs.get(Regs.STAT), 5))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            break;

        case 3:
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 0, true));
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 1, true));
            break;
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.cpu.Cpu#cycle(long cycle)
     */
    @Override
    public void cycle(long cycle) {

        if (nextNonIdleCycle == Long.MAX_VALUE
                && Bits.test(read(ADDRESS_LCDC), 7)) {
            lcdOnCycle = cycle;
            nextNonIdleCycle = 0;
        }

        if (cycle - lcdOnCycle == nextNonIdleCycle)
            reallyCycle(cycle);

        if (dmaChanged && cyclesSinceCopy < 160) {
            write(cyclesSinceCopy + AddressMap.OAM_START,
                    bus.read(((regs.get(Regs.DMA)) << 8) + cyclesSinceCopy));

            cyclesSinceCopy++;

            if (cyclesSinceCopy == 160) {
                dmaChanged = false;
                cyclesSinceCopy = 0;
            }
        }
    }

    /**
     * Fait ce qu'il ya a faire a un cycle donne
     * 
     * @param cycle,
     *            le cycle actuelle
     * @throws IllegalArgumentException
     *             si le cycle passé en argument est negatif
     */
    public void reallyCycle(long cycle) {
        if (cycle < 0)
            throw new IllegalArgumentException();

        int frameCycle = (int) ((cycle - lcdOnCycle)) % IMAGE_CYCLES;
        cyclesSinceLine = (int) ((cycle - lcdOnCycle) % LINE_CYCLES);

        int atLine = frameCycle / LINE_CYCLES;

        if (atLine < 144) {
            if (frameCycle == 0) {
                winY = 0;
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
            }
            switch (cyclesSinceLine) {

            case 20:
                nextImageBuilder.setLine(atLine, computeLine(atLine));
                nextNonIdleCycle += 43;
                changeMode(3);
                break;

            case 63:
                nextNonIdleCycle += 51;
                changeMode(0);
                break;

            case 0:
                changeLy(atLine);
                nextNonIdleCycle += 20;
                changeMode(2);
                break;
            }

        } else {
            if (atLine == 144) {
                currentImage = nextImageBuilder.build();
                changeMode(1);
            }
            nextNonIdleCycle += LINE_CYCLES;
            changeLy(atLine);
        }

    }

    /**
     * Calcul la ligne a etre aficher au moment de l'appel de la fonction a
     * lindex passé en argument
     * 
     * @param lineIndex
     * @return la ligne a etre afficher
     * @throws IndexOutOfBoundsException
     *             si l'index passé en argument deppase la hauteur de l'écran
     */
    private LcdImageLine computeLine(int lineIndex) {
        Objects.checkIndex(lineIndex, LCD_HEIGHT);

        LcdImageLine.Builder lineBuild = new LcdImageLine.Builder(
                BACKGROUND_WIDTH);
        int line = (lineIndex + regs.get(Regs.SCY)) % BACKGROUND_WIDTH;
        int tileLine = (int) line >>> 3;
        int lineInTile = (int) line % (TILE_SIZE);
        LcdImageLine spriteBelow = NULL_LINE;
        LcdImageLine spriteFront = NULL_LINE;
        LcdImageLine screenLine = NULL_LINE;
        LcdImageLine winLine = NULL_LINE;
        LcdImageLine lineReturned = NULL_LINE;

        if (Bits.test(regs.get(Regs.LCDC), 1)) {
            spriteBelow = spriteLine(spritesIntersectingLine(), false);
            spriteFront = (spriteLine(spritesIntersectingLine(), true));
        }

        lineReturned = lineReturned.below(spriteBelow);

        if (Bits.test(regs.get(Regs.LCDC), 0)) {
            for (int a = 0; a < TILES_IN_LINE; ++a) {

                int lsb, msb, tileAddress = 0;
                int TILE_SOURCE = Bits.test(regs.get(Regs.LCDC), 4) ? 1 : 0;
                int BG_DISPLAY_DATA = Bits.test(regs.get(Regs.LCDC), 3) ? 1 : 0;

                tileAddress = this
                        .read(AddressMap.BG_DISPLAY_DATA[BG_DISPLAY_DATA]
                                + (tileLine << 5) + a);

                if (!Bits.test(regs.get(Regs.LCDC), 4))
                    tileAddress = (tileAddress < TILE_ADDRESS_OVERFLOW)
                            ? tileAddress + TILE_ADDRESS_OVERFLOW
                            : tileAddress - TILE_ADDRESS_OVERFLOW;

                lsb = this.read(AddressMap.TILE_SOURCE[TILE_SOURCE]
                        + (tileAddress << 4) + (lineInTile << 1));

                msb = this.read(AddressMap.TILE_SOURCE[TILE_SOURCE]
                        + (tileAddress << 4) + (lineInTile << 1) + 1);

                lineBuild.setBytes(a, Bits.reverse8(msb), Bits.reverse8(lsb));

            }

            screenLine = lineBuild.build();
            screenLine = screenLine.extract(regs.get(Regs.SCX), LCD_WIDTH);
            screenLine = screenLine.mapColors(regs.get(Regs.BGP));
            BitVector or = spriteBelow.opacity().not();
            lineReturned = lineReturned.below(screenLine,
                    (screenLine.opacity().or(or)));
        }

        int wxTranslated = regs.get(Regs.WX) - WX_X_OFFSET;
        wxTranslated = wxTranslated < 0 ? 0 : wxTranslated;

        if (lineIndex >= regs.get(Regs.WY) && Bits.test(regs.get(Regs.LCDC), 5)
                && wxTranslated < 160) {

            winLine = computeWin();
            lineReturned = lineReturned.join(winLine, wxTranslated);
        }

        lineReturned = lineReturned.below(spriteFront);
        return lineReturned;
    }

    /**
     * Calcul la ligne de la fenetre a etre afficher au moment de lappel de la
     * fonction
     * 
     * @return la ligne de la fenetre a etre afficher
     */
    private LcdImageLine computeWin() {

        int wxTranslated = regs.get(Regs.WX) - WX_X_OFFSET;
        int tileLineWin = (int) winY >>> 3;
        int lineInTileWin = (int) winY % (TILE_SIZE);
        LcdImageLine.Builder winLineBuilder = new LcdImageLine.Builder(256);
        LcdImageLine winLine;

        for (int a = 0; a < TILES_IN_LINE; ++a) {

            int lsb, msb, tileAddress = 0;
            int TILE_SOURCE = Bits.test(regs.get(Regs.LCDC), 4) ? 1 : 0;
            int BG_DISPLAY_DATA = Bits.test(regs.get(Regs.LCDC), 6) ? 1 : 0;

            tileAddress = this.read(AddressMap.BG_DISPLAY_DATA[BG_DISPLAY_DATA]
                    + (tileLineWin << 5) + a);

            if (!Bits.test(regs.get(Regs.LCDC), 4))
                tileAddress = (tileAddress < TILE_ADDRESS_OVERFLOW)
                        ? tileAddress + TILE_ADDRESS_OVERFLOW
                        : tileAddress - TILE_ADDRESS_OVERFLOW;

            lsb = this.read(AddressMap.TILE_SOURCE[TILE_SOURCE]
                    + (tileAddress << 4) + (lineInTileWin << 1));

            msb = this.read(AddressMap.TILE_SOURCE[TILE_SOURCE]
                    + (tileAddress << 4) + (lineInTileWin << 1) + 1);

            winLineBuilder.setBytes(a, Bits.reverse8(msb), Bits.reverse8(lsb));
        }

        ++winY;
        winLine = winLineBuilder.build();
        winLine = winLine.shift(wxTranslated);
        winLine = winLine.extract(0, LCD_WIDTH);
        winLine = winLine.mapColors(regs.get(Regs.BGP));
        return winLine;
    }

    /**
     * Calcul lindex des sprites qui aparaissent a la ligne au moment de lappel
     * de la fonction
     * 
     * @return un tableau contenant les sprites trouves(Maximum 10) ordonne
     *         selon leur position en X
     */
    public int[] spritesIntersectingLine() {

        int index = 0, spritesFound = 0;
        int[] indexes = new int[MAX_SPRITES_IN_LINE];

        while (spritesFound < MAX_SPRITES_IN_LINE
                & index < MAX_SPRITES_IN_MEMORY) {

            if (bus.read(AddressMap.OAM_START + (index << 2))
                    - SPRITES_Y_OFFSET <= regs.get(Regs.LY)
                    && bus.read(AddressMap.OAM_START + (index << 2))
                            + getHeight()
                            - SPRITES_Y_OFFSET > regs.get(Regs.LY)) {

                indexes[spritesFound] = Bits.make16(
                        bus.read(AddressMap.OAM_START + (index << 2) + 1),
                        index);
                spritesFound++;
            }
            index++;
        }

        Arrays.sort(indexes, 0, spritesFound);
        int[] finall = new int[spritesFound];

        for (int i = 0; i < spritesFound; i++)
            finall[i] = Bits.clip(8, indexes[i]);

        return finall;
    }

    /**
     * Calcul la ligne contenant les sprites
     * 
     * @param spriteIndex
     *            tableau qui contient les index des sprites presant a ligne
     *            actuelle
     * @param isInFront
     *            est a true si on veut les sprites qui sont devant le plan et
     *            est a faux si lon veut les sprites du background
     * @return la lignes de sprites
     */
    public LcdImageLine spriteLine(int[] spriteIndex, boolean isInFront) {

        List<LcdImageLine> spriteLines = new ArrayList<>();
        LcdImageLine line = NULL_LINE;

        for (int i = 0; i < spriteIndex.length; i++) {

            int lsb, msb = 0;
            LcdImageLine.Builder spriteBuilder = new LcdImageLine.Builder(
                    LCD_WIDTH);
            LcdImageLine spriteLine;
            int spriteIndexx = spriteIndex[i];
            int spriteXPosition = bus
                    .read(AddressMap.OAM_START + (spriteIndexx << 2) + 1)
                    - SPRITES_X_OFFSET;
            boolean spritePalette = Bits.test(
                    bus.read(AddressMap.OAM_START + (spriteIndexx << 2) + 3),
                    4);
            boolean hFlip = Bits.test(
                    bus.read(AddressMap.OAM_START + (spriteIndexx << 2) + 3),
                    5);
            boolean vFlip = Bits.test(
                    bus.read(AddressMap.OAM_START + (spriteIndexx << 2) + 3),
                    6);
            int lineInTile = (regs.get(Regs.LY)
                    - (bus.read(AddressMap.OAM_START + (spriteIndexx << 2))))
                    & (getHeight() - 1);
            int tileAddress = bus
                    .read(AddressMap.OAM_START + (spriteIndexx << 2) + 2);

            if (vFlip)
                lineInTile = getHeight() - 1 - lineInTile;

            lsb = this.read(AddressMap.TILE_SOURCE[1] + (tileAddress << 4)
                    + (lineInTile << 1));

            msb = this.read(AddressMap.TILE_SOURCE[1] + (tileAddress << 4)
                    + (lineInTile << 1) + 1);

            if (hFlip) {

                msb = Bits.reverse8(msb);
                lsb = Bits.reverse8(lsb);
            }

            spriteLine = spriteBuilder
                    .setBytes(0, Bits.reverse8(msb), Bits.reverse8(lsb))
                    .build();

            if (spritePalette)
                spriteLine = spriteLine.mapColors(regs.get(Regs.OBP1));

            else
                spriteLine = spriteLine.mapColors(regs.get(Regs.OBP0));

            if ((isInFront && !Bits.test(
                    bus.read(AddressMap.OAM_START + (spriteIndexx << 2) + 3),
                    7))
                    || (!isInFront & Bits.test(bus.read(
                            AddressMap.OAM_START + (spriteIndexx << 2) + 3),
                            7))) {

                spriteLine = spriteLine.shift(spriteXPosition);
                spriteLines.add(spriteLine);
            }
        }

        LcdImageLine[] planDeSprite = new LcdImageLine[spriteLines.size()];

        spriteLines.toArray(planDeSprite);

        for (int i = planDeSprite.length - 1; i >= 0; --i)

            line = line.below(planDeSprite[i]);

        return line;

    }

    /**
     * Calcul la hauter du sprite a une ligne donne
     * 
     * @return la hauteur du sprite
     */
    private int getHeight() {

        return 8 * (Bits.test(regs.get(Regs.LCDC), 2) ? 2 : 1);
    }

    /**
     * Regarde si LY et LYC ont la meme valeur
     * 
     * @return vrai si ils ont la meme valeur et faux si ils lont pas
     */
    private boolean checkLyLyc() {

        return regs.get(Regs.LY) == regs.get(Regs.LYC);
    }

    /**
     * Change LY et lance les interruption necessaires
     * 
     * @param data
     * @throws IllegalArgumentException
     *             si la valeur passé en argument n'est pas de 8 bits
     */
    private void changeLy(int data) {

        Preconditions.checkBits8(data);
        regs.set(Regs.LY, data);

        if (checkLyLyc()) {
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 2, true));

            if (Bits.test(regs.get(Regs.STAT), 6))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        }

        else
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 2, false));
    }

    /**
     * Change LYC et lance les interruptions necessaires
     * 
     * @param data
     * @throws IllegalArgumentException
     *             si la valeur passe en argument ne vaus pas 8 bits
     */
    private void changeLyc(int data) {

        Preconditions.checkBits8(data);
        regs.set(Regs.LYC, data);
        if (checkLyLyc()) {
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 2, true));

            if (Bits.test(regs.get(Regs.STAT), 6))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        } else
            regs.set(Regs.STAT, Bits.set(regs.get(Regs.STAT), 2, false));
    }

    /**
     * Lis a ladress donne dans un registre
     * 
     * @param address,
     *            laddres ou il faut lire
     * @return la valeur a ladress
     */
    private int regRead(int address) {

        int atReg = 0;
        switch (address) {

        case ADDRESS_LCDC:
            atReg = regs.get(Regs.LCDC);
            break;

        case ADDRESS_STAT:
            atReg = regs.get(Regs.STAT);
            break;

        case ADDRESS_LY:
            atReg = regs.get(Regs.LY);
            break;

        case ADDRESS_LYC:
            atReg = regs.get(Regs.LYC);
            break;

        case ADDRESS_SCY:
            atReg = regs.get(Regs.SCY);
            break;

        case ADDRESS_SCX:
            atReg = regs.get(Regs.SCX);
            break;

        case ADDRESS_DMA:
            atReg = regs.get(Regs.DMA);
            break;

        case ADDRESS_BGP:
            atReg = regs.get(Regs.BGP);
            break;

        case ADDRESS_OBP0:
            atReg = regs.get(Regs.OBP0);
            break;

        case ADDRESS_OBP1:
            atReg = regs.get(Regs.OBP1);
            break;

        case ADDRESS_WY:
            atReg = regs.get(Regs.WY);
            break;

        case ADDRESS_WX:
            atReg = regs.get(Regs.WX);
            break;
        }
        return atReg;
    }

    /**
     * ecris dans le registre a ladresse donne
     * 
     * @param address,
     *            ladres dans la quelle il faut ecrire
     * @param data,Ce
     *            quil faut ecrire a ladresse
     * @throws IllegalArgumentException
     *             si la data passé en argument ne fait pas 8 bits ou si la data
     *             ne fait pas 16 bits
     */
    private void regWrite(int address, int data) {
        Preconditions.checkBits8(data);
        Preconditions.checkBits16(data);
        switch (address) {
        case ADDRESS_LCDC:
            regs.set(Regs.LCDC, data);
            if (Bits.test(regs.get(Regs.LCDC), 7) == false) {
                changeMode(0);
                changeLy(0);
                nextNonIdleCycle = Long.MAX_VALUE;
            }
            break;

        case ADDRESS_STAT:
            data = (data & 0b11111000) | (regRead(ADDRESS_STAT) & 0b00000111); // !!!
            regs.set(Regs.STAT, data);
            if (!Bits.test(regs.get(Regs.STAT), 7))
                changeMode(0);
            break;

        case ADDRESS_LY:
            changeLy(data);
            break;

        case ADDRESS_LYC:
            changeLyc(data);
            break;

        case ADDRESS_SCY:
            regs.set(Regs.SCY, data);
            break;

        case ADDRESS_SCX:
            regs.set(Regs.SCX, data);
            break;

        case ADDRESS_DMA:
            dmaChanged = true;
            regs.set(Regs.DMA, data);
            break;

        case ADDRESS_BGP:
            regs.set(Regs.BGP, data);
            break;

        case ADDRESS_OBP0:
            regs.set(Regs.OBP0, data);
            break;

        case ADDRESS_OBP1:
            regs.set(Regs.OBP1, data);
            break;

        case ADDRESS_WY:
            regs.set(Regs.WY, data);
            break;

        case ADDRESS_WX:
            regs.set(Regs.WX, data);
            break;
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#attachTo(Bus bus)
     */
    @Override
    public void attachTo(Bus bus) {

        this.bus = bus;
        bus.attach(this);
    }
}
