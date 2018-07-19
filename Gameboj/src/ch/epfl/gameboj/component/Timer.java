package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Timer implements Component, Clocked {

    private int regTIMA = 0;
    private int regDIV = 0;
    private int regTMA = 0;
    private int regTAC = 0;
    private int writeMinAddress = 0;
    private int writeMaxAddress = 65535;
    private int writeMinData = 0;
    private int writeMaxData = 255;

    Bus bus;
    Cpu timer;

    public Timer(Cpu cpu) {

        Preconditions.checkNull(cpu);
        timer = cpu;

    }

    @Override

    public void cycle(long cycle) {

        boolean s0 = state();
        regDIV = Bits.clip(16, regDIV + 4);
        incIfChange(s0);

    }

    @Override
    public int read(int address) {

        int valueAtAddress;

        switch (Preconditions.checkBits16(address)) {

        case AddressMap.REG_DIV:

            valueAtAddress = Bits.extract(regDIV, 8, 8);
            break;

        case AddressMap.REG_TIMA:

            valueAtAddress = regTIMA;
            break;

        case AddressMap.REG_TMA:

            valueAtAddress = regTMA;
            break;

        case AddressMap.REG_TAC:

            valueAtAddress = regTAC;
            break;

        default:

            valueAtAddress = NO_DATA;
            break;

        }

        return valueAtAddress;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */

    @Override
    public void write(int address, int data) {

        boolean s0 = state();

        if (address < writeMinAddress || address > writeMaxAddress
                || data < writeMinData || data > writeMaxData) {

            throw new IllegalArgumentException();

        }
        switch (Preconditions.checkBits16(address)) {

        case AddressMap.REG_DIV:

            Preconditions.checkBits8(data);
            regDIV = 0;
            incIfChange(s0);
            break;

        case AddressMap.REG_TIMA:

            regTIMA = Preconditions.checkBits8(data);
            break;

        case AddressMap.REG_TMA:

            regTMA = Preconditions.checkBits8(data);
            break;

        case AddressMap.REG_TAC:

            regTAC = Preconditions.checkBits8(data);
            incIfChange(s0);
            break;
        }
    }

    private boolean state() {

        int divBitIndex = 0;

        switch (Bits.clip(2, regTAC)) {

        case 0:

            divBitIndex = 9;

            break;

        case 1:

            divBitIndex = 3;

            break;

        case 2:

            divBitIndex = 5;

            break;

        case 3:

            divBitIndex = 7;

            break;
        }

        return Bits.test(regTAC, 2) && Bits.test(regDIV, divBitIndex);
    }

    private void incIfChange(boolean previousState) {

        if (previousState & !(state())) {

            if (read(AddressMap.REG_TIMA) == 0xFF) {

                timer.requestInterrupt(Interrupt.TIMER);

                write(AddressMap.REG_TIMA, read(AddressMap.REG_TMA));

            } else {

                write(AddressMap.REG_TIMA, read(AddressMap.REG_TIMA) + 1);
            }

        }
    }
}
