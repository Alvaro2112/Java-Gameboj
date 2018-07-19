package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*représente une  mémoire vive la gameboy par un tableau
*/

public class Ram {
    private byte[] ram;

    /**
     * construit une ram dont la taille est donné en argument
     * 
     * @param donne la taille de la ram
     * 
     */
    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);

        ram = new byte[size];

    }

    /**
     * retourne la longueur du tableau de la mémoire vive
     * 
     * @return la taille du tableau
     */

    public int size() {
        return ram.length;
    }

    /**
     * qui retourne l'octet se trouvant à l'index donné, sous la forme d'une
     * valeur comprise entre 0 et FF16
     * 
     * @return l'octet se trouvant à l'index donné sous la forme d'un int
     * 
     * @throws IndexOutOfBondsException si l'index donné n'est pas une valeur
     * comprise entre 0 et la taille du tableau
     * 
     */

    public int read(int index) {
        if (index >= 0 && index <= size()) {

            return Byte.toUnsignedInt(ram[index]);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * modifie le contenu de la mémoire à l'index donné pour qu'il soit égal à
     * la valeur donnée 
     * 
     * @param index donne l'index de la valeur a changer
     * 
     * @param value donne la valeur qu'on met dans le tableau à l'index donné
     * 
     * @throws IndexOutOfBondsException si l'index donné n'est pas une valeur
     * comprise entre 0 et la taille du tableau
     */
    public void write(int index, int value) {
        if (index >= 0 && index <= size()) {
            

            ram[index] = (byte) (Preconditions.checkBits8(value));
        }

        else {
            throw new IndexOutOfBoundsException();
        }
    }

}
