package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Représente le joypad
 */
public final class Joypad implements Component {

    private final Cpu cpu;

    private int P1 = 0;
    private int firstLine = 0;
    private int secondLine = 0;

    /**
     * Enum qui représente toutes les touches de la game boy
     *
     */
    public enum Key {
        RIGHT, LEFT, UP, DOWN, A, B, SELECT, START
    }

    /**
     * ENum utilisé pour les différents bits du registe P1
     *
     */
    private enum KeyBoardState implements Bit {
        COL0, COL1, COL2, COL3, LINE0, LINE1, UNUSED_6, UNUSED_7
    }

    private static final int LINE_LENGTH = 4;

    /**
     * Construit un joypad
     * 
     * @param cpu
     *            le cpu qui est utilisé par le joypad
     * @throws NullPointerException
     *             si le cpu passé en argument est nul
     */
    public Joypad(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
    }

    /**
     * Change la ligne d'état du joypad lors que une touche est pressée ou
     * relachée
     * 
     * @param k
     *            la touche du joypad
     * @param bitSet
     *            true si la touche est pressée, false si relachée
     */
    private void lineChange(Key k, boolean bitSet) {

        if (k.ordinal() < LINE_LENGTH)
            firstLine = Bits.set(firstLine, k.ordinal(), bitSet);
        else
            secondLine = Bits.set(secondLine, k.ordinal() % LINE_LENGTH,
                    bitSet);
    }

    /**
     * Utilisé quand on appuie sur une touche donnée.
     * 
     * @param k
     *            la touche sur laquelle on appuie
     */
    public void keyPressed(Key k) {

        lineChange(k, true);
        changeP1();
    }

    /**
     * Utilisé quand on relache sur une touche donnée.
     * 
     * @param k
     *            la touche sur laquelle on lache
     */
    public void keyReleased(Key k) {

        lineChange(k, false);
        changeP1();
    }

    private void changeP1() {
        int previousP1 = P1;
        P1 &= 0b1111_0000;

        if (Bits.test(P1, KeyBoardState.LINE0))
            P1 |= firstLine;

        if (Bits.test(P1, KeyBoardState.LINE1))
            P1 |= secondLine;

        if (P1 > previousP1)
            cpu.requestInterrupt(Interrupt.JOYPAD);
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int address)
     */
    @Override
    public int read(int address) {

        Preconditions.checkBits16(address);

        int returnn = address == AddressMap.REG_P1 ? Bits.complement8(P1)
                : NO_DATA;

        return returnn;
    }

    /**
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        if (Preconditions.checkBits16(address) == AddressMap.REG_P1) {

            P1 = (P1 & 0b1100_1111)
                    | (Bits.complement8(Preconditions.checkBits8(data))
                            & 0b0011_0000);
            changeP1();
        }
    }
}