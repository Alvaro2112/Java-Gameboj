package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) représente une cartouche
 */
public final class Cartridge implements Component {

    private final MBC cartouche;
    private final static int RAM_SIZE = 0X149;
    private final static int RAM_TYPE = 0x147;
    private final static int[] RAM_SIZES = { 0, 2048, 8192, 32768 };

    /**
     * Construit une cartouche en lui attribuant un controleur
     * 
     * @param Controller
     *            donne la cartouche
     */
    private Cartridge(MBC Controller) {

        this.cartouche = Objects.requireNonNull(Controller);

    }

    public void toFile(String fileName) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(
                fileName)) {
            fileOutputStream.write(cartouche.getByteArray());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * crée une cartouche dont la mémoire morte contient les octets du fichier
     * donné
     * 
     * @param romFile
     *            le fichier auquel la cartouche est égale
     * @return la cartouche
     * @throws IOException
     *             si le fichier donné est nul, ou en cas d'entrée sortie
     * @throws IllegalArgumentException
     *             si l'octet de l'index 327 ne contient pas 0
     */

    public static Cartridge ofFile(File romFile) throws IOException {

        // byte memoire[] = new byte[MEMORY_SIZE];
        byte memoire[] = new byte[(int) romFile.length()];

        if (!romFile.exists()) {
            throw new IOException();
        }

        try (InputStream stream = new FileInputStream(romFile)) {
            memoire = stream.readAllBytes();

        } catch (IOException e) {
            e.printStackTrace();
        }

        int mbcType = memoire[RAM_TYPE];
        Rom rom = new Rom(memoire);

        if (mbcType == 1 || mbcType == 2 | mbcType == 3) {

            MBC1 mbc1 = new MBC1(rom, RAM_SIZES[rom.read(RAM_SIZE)]);
            return new Cartridge(mbc1);
        }

        else {
            MBC0 mbc0 = new MBC0(rom);
            return new Cartridge(mbc0);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {

        return cartouche.read(Preconditions.checkBits16(address));
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {

        cartouche.write(Preconditions.checkBits16(address),
                Preconditions.checkBits8(data));

    }
    
    public void setMBCRam(byte[] byteArray) {
        cartouche.setByteArray(byteArray);
    }

    public interface MBC extends Component, Saveable {

    }

    public interface Saveable {
        public byte[] getByteArray();

        public void setByteArray(byte[] byteArray);
    }

}