package ch.epfl.gameboj.component.memory;

import java.util.Arrays;

import ch.epfl.gameboj.Preconditions;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) represente une mémoire morte de la
 *         gameboy avec un tableau
 */
public final class Rom {
    
    private final byte[] rom;

    /**
     * construit une mémoire morte dont le contenu et la taille sont ceux du
     * tableau d'octets donné en argument
     * 
     * @param donne
     *            la taille et le contenu de la mémoire
     * 
     * @throws NullPointerException()
     *             si l'argument est nul
     */
    public Rom(byte[] data) {
        Preconditions.checkNull(data);
        this.rom = Arrays.copyOf(data, data.length);

    }

    /**
     * retourne la longueur du tableau de la mémoire morte
     * 
     * @return la taille du tableau
     */
    public int size() {
        return rom.length;

    }

    /**
     * qui retourne l'octet se trouvant à l'index donné, sous la forme d'une
     * valeur comprise entre 0 et FF16
     * 
     * @return l'octet se trouvant à l'index donné
     * 
     * @throws IndexOutOfBondsException
     *             si l'index n'est pas une valeur représentable par 8bits
     */
    public int read(int index) {
        if (index >= 0 && index < size()) {

            return Byte.toUnsignedInt(rom[index]);

        } else {

            throw new IndexOutOfBoundsException("ear");
        }
    }
}
