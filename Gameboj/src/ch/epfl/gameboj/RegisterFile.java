package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Représente un banc de registre 8 bits
 */
public final class RegisterFile<E extends Register> {

    private int[] banc8;

    /**
     * construit un banc de registres 8 bits dont la taille est celle du tableau
     * donnée
     * 
     * @param allRegs
     *            donne la taille du banc
     */
    public RegisterFile(E[] allRegs) {

        banc8 = new int[allRegs.length];
    }

    /**
     * 
     * @param reg
     *            donne le registre dont on tire la valeur
     * @return la valeur contenue dans le registre
     */
    public int get(E reg) {

        int register = banc8[reg.index()];

        return register;
    }

    /**
     * Change la valeur contenue dans le registre par celle de newValue
     * 
     * @param reg
     *            registre dont le contenu est à changer
     * @param newValue
     *            valeur à mettre dans le registre
     * @throws IllegalArgumentException
     *             si newValue n'est pas une valeur 8bits
     */
    public void set(E reg, int newValue) {

        banc8[reg.index()] = Preconditions.checkBits8(newValue);
    }

    /**
     * teste le bit donné dans le registre
     * 
     * @param reg
     *            donne le registre ou on va tester le bit
     * @param b
     *            donne le bit à tester
     * @return true si le bit testé vaut 1, sinon 0.
     */
    public boolean testBit(E reg, Bit b) {

        return Bits.test(banc8[reg.index()], b.index());
    }

    /**
     * Change la valeur du bit donné dans le registre donné en fonction de
     * newvalue
     * 
     * @param reg
     *            registre qui contient la valeur à changer
     * @param bit
     *            bit à changer
     * @param newValue
     *            si c'est vrai alors le Bit vaudra 1 sinon 0
     */
    public void setBit(E reg, Bit bit, boolean newValue) {

        set(reg, Bits.set(get(reg), bit.index(), newValue));
    }

}
