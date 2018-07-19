
package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.component.cpu.Alu;
import ch.epfl.gameboj.component.cpu.Alu.Flag;
import ch.epfl.gameboj.component.cpu.Alu.RotDir;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) représente le processeur de la gameboy
 */

public final class Cpu implements Component, Clocked {
    private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(
            Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(
            Opcode.Kind.PREFIXED);

    private Bus bus;
    private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

    private long nextNonIdleCycle;
    private int PC = 0;
    private int SP = 0;
    private int prefixed = 203;
    private boolean IME = false;
    private int IE = 0;
    private int IF = 0;

    public enum Reg implements Register {
        A, F, B, C, D, E, H, L
    };

    public enum Interrupt implements Bit {
        VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
    };

    private enum Reg16 implements Register {

        AF(Reg.A, Reg.F), BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L),;

        private final Reg r1, r2;

        private Reg16(Reg a, Reg b) {

            this.r1 = a;
            this.r2 = b;
        }
    };

    private enum FlagSrc {

        V0, V1, ALU, CPU
    };

    private RegisterFile<Register> reg8 = new RegisterFile<>(Reg.values());

    public int[] _testGetPcSpAFBCDEHL() {

        int Regs[] = new int[10];

        Regs[0] = PC;
        Regs[1] = SP;
        Regs[2] = reg8.get(Reg.A);
        Regs[3] = reg8.get(Reg.F);
        Regs[4] = reg8.get(Reg.B);
        Regs[5] = reg8.get(Reg.C);
        Regs[6] = reg8.get(Reg.D);
        Regs[7] = reg8.get(Reg.E);
        Regs[8] = reg8.get(Reg.H);
        Regs[9] = reg8.get(Reg.L);

        return Regs;

    }

    public Cpu() {

    }

    private static Opcode[] buildOpcodeTable(Opcode.Kind opKind) {

        Opcode[] opcodeTable = new Opcode[256];

        for (Opcode o : Opcode.values()) {

            if (o.kind == opKind)

                opcodeTable[o.encoding] = o;

        }

        return opcodeTable;

    }

    @Override
    /**
     * fait que le processeur évolue en lui faisaint éxectuer toutes les
     * opérations qu'il doit exécuter durant le cycle donné.
     * 
     * @param cycle
     *            donne le cycle
     * 
     */
    public void cycle(long cycle) {
        if ((nextNonIdleCycle == Long.MAX_VALUE) && testIeIf()) {

            nextNonIdleCycle = cycle;
            reallyCycle();

        } else if (this.nextNonIdleCycle == cycle) {
            reallyCycle();

        }

        else {
        }

    }

    public void reallyCycle() {
        if (IME && testIeIf()) {

            IME = false;
            int i = 31 - Integer
                    .numberOfLeadingZeros(Integer.lowestOneBit(IF & IE));
            IF = Bits.set(IF, i, false);
            push16(PC); // i
            PC = 0x40 + (8 * i);
            nextNonIdleCycle = nextNonIdleCycle + 5;

        } else {

            int suivant = bus.read(PC);

            Opcode OPsuivant;

            if (suivant == prefixed) {

                suivant = bus.read(PC + 1);
                OPsuivant = PREFIXED_OPCODE_TABLE[suivant];

            } else {

                OPsuivant = DIRECT_OPCODE_TABLE[suivant];

            }

            dispatch(OPsuivant);
        }
    }

    /**
     * Lit l'opcode et éxécute l'instruction correspondante
     * 
     * @param opcode
     *            donne l'opcode de l'instruction
     */
    private void dispatch(Opcode opcode) {

        int nextPC = PC + opcode.totalBytes;
        boolean instructionDone = false;

        switch (opcode.family) {
        case NOP: {

        }

            break;

        case LD_R8_HLR: {

            reg8.set(extractReg(opcode, 3), read8AtHl());
            ;
        }

            break;

        case LD_A_HLRU: {

            int a = read8AtHl();

            reg8.set(Reg.A, a);

            setReg16(Reg16.HL, reg16(Reg16.HL) + extractHlIncrement(opcode));
            ;
        }

            break;

        case LD_A_N8R: {

            reg8.set(Reg.A, read8(AddressMap.REGS_START + read8AfterOpcode()));
            ;
        }

            break;

        case LD_A_CR: {

            reg8.set(Reg.A, read8(AddressMap.REGS_START + reg8.get(Reg.C)));
            ;
        }

            break;

        case LD_A_N16R: {

            reg8.set(Reg.A, read8(read16AfterOpcode()));
            ;
        }

            break;

        case LD_A_BCR: {

            reg8.set(Reg.A, read8(reg16(Reg16.BC)));
            ;
        }

            break;

        case LD_A_DER: {

            reg8.set(Reg.A, read8(reg16(Reg16.DE)));
        }

            break;

        case LD_R8_N8: {

            reg8.set(extractReg(opcode, 3), read8AfterOpcode());
            ;
        }

            break;

        case LD_R16SP_N16: {

            setReg16SP(extractReg16(opcode), read16AfterOpcode());
            ;
        }

            break;

        case POP_R16: {

            setReg16(extractReg16(opcode), pop16());
            ;
        }

            break;

        case LD_HLR_R8: {

            write8AtHl(reg8.get(extractReg(opcode, 0)));
            ;
        }

            break;

        case LD_HLRU_A: {

            int hlvalue = reg16(Reg16.HL);

            write8AtHl(reg8.get(Reg.A));

            setReg16(Reg16.HL,

                    Bits.clip(16, hlvalue + extractHlIncrement(opcode)));
            ;
        }

            break;

        case LD_N8R_A: {

            write8(AddressMap.REGS_START + read8AfterOpcode(), reg8.get(Reg.A));
            ;
        }

            break;

        case LD_CR_A: {

            write8(AddressMap.REGS_START + reg8.get(Reg.C), reg8.get(Reg.A));
            ;
        }

            break;

        case LD_N16R_A: {

            write8(read16AfterOpcode(), reg8.get(Reg.A));
            ;
        }

            break;

        case LD_BCR_A: {

            write8(reg16(Reg16.BC), reg8.get(Reg.A));
            ;
        }

            break;

        case LD_DER_A: {

            write8(reg16(Reg16.DE), reg8.get(Reg.A));
            ;
        }

            break;

        case LD_HLR_N8: {

            write8AtHl(read8AfterOpcode());
            ;
        }

            break;

        case LD_N16R_SP: {

            write16(read16AfterOpcode(), SP);
            ;
        }

            break;

        case LD_R8_R8: {

            reg8.set(extractReg(opcode, 3), reg8.get(extractReg(opcode, 0)));
            ;
        }

            break;

        case LD_SP_HL: {

            SP = reg16(Reg16.HL);
            ;
        }

            break;

        case PUSH_R16: {

            push16(reg16(extractReg16(opcode)));
            ;
        }

            break;

        case ADD_A_R8: {

            int add = Alu.add(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0)),
                    carry1(opcode));

            setRegFlags(Reg.A, add);
            ;
        }
            break;
        case ADD_A_N8: {
            int add = Alu.add(reg8.get(Reg.A), read8AfterOpcode(),
                    carry1(opcode));
            setRegFlags(Reg.A, add);
            ;
        }
            break;
        case ADD_A_HLR: {

            int add = Alu.add(reg8.get(Reg.A), read8AtHl(), carry1(opcode));
            setRegFlags(Reg.A, add);
            ;

        }
            break;
        case INC_R8: {
            int add = Alu.add(reg8.get(extractReg(opcode, 3)), 1);
            reg8.set(extractReg(opcode, 3), Alu.unpackValue(add));
            combineAluFlags(add, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.CPU);
            ;
        }
            break;
        case INC_HLR: {

            int add = Alu.add(read8AtHl(), 1);
            write8AtHl(Alu.unpackValue(add));
            combineAluFlags(add, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.CPU);
            ;
        }
            break;
        case INC_R16SP: {
            ;

            if (extractReg16(opcode) == Reg16.AF) {
                setReg16SP(extractReg16(opcode), Bits.clip(16, SP + 1));
            } else {
                setReg16SP(extractReg16(opcode),
                        Bits.clip(16, reg16(extractReg16(opcode)) + 1));
            }

        }
            break;
        case ADD_HL_R16SP: {
            Reg16 r = extractReg16(opcode);
            int regValue = r == Reg16.AF ? SP : reg16(r);
            int sum = Alu.add16H(reg16(Reg16.HL), regValue);
            setReg16SP(Reg16.HL, Alu.unpackValue(sum));
            combineAluFlags(sum, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.ALU);
        }
            break;
        case LD_HLSP_S8: {
            int sum = Alu.add16L(SP,
                    Bits.clip(16, Bits.signExtend8(read8AfterOpcode())));
            if (Bits.test(opcode.encoding, 4)) {
                setReg16(Reg16.HL, Alu.unpackValue(sum));
            } else {
                SP = Alu.unpackValue(sum);
            }
            combineAluFlags(sum, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU,
                    FlagSrc.ALU);
        }
            break;

        // Subtract
        case SUB_A_R8: {
            int sub = Alu.sub(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0)),
                    carry1(opcode));
            setRegFlags(Reg.A, sub);
        }
            break;
        case SUB_A_N8: {
            int sub = Alu.sub(reg8.get(Reg.A), read8AfterOpcode(),
                    carry1(opcode));
            setRegFlags(Reg.A, sub);
        }
            break;
        case SUB_A_HLR: {
            int sub = Alu.sub(reg8.get(Reg.A), read8AtHl(), carry1(opcode));
            setRegFlags(Reg.A, sub);
        }
            break;
        case DEC_R8: {
            ;
            int sub = Alu.sub(reg8.get(extractReg(opcode, 3)), 1);
            reg8.set(extractReg(opcode, 3), Alu.unpackValue(sub));
            combineAluFlags(sub, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU,
                    FlagSrc.CPU);
        }
            break;
        case DEC_HLR: {
            ;
            int sub = Alu.sub(read8AtHl(), 1);
            write8AtHl(Alu.unpackValue(sub));
            combineAluFlags(sub, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU,
                    FlagSrc.CPU);
        }
            break;
        case CP_A_R8: {
            int sub = Alu.sub(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0)));
            setFlags(sub);

        }
            break;
        case CP_A_N8: {
            ;
            int sub = Alu.sub(reg8.get(Reg.A), read8AfterOpcode());
            setFlags(sub);
        }
            break;
        case CP_A_HLR: {
            int sub = Alu.sub(reg8.get(Reg.A), read8AtHl());
            setFlags(sub);
        }
            break;
        case DEC_R16SP: {
            Reg16 r = extractReg16(opcode);
            int regValue = r == Reg16.AF ? SP : reg16(r);
            setReg16SP(r, Bits.clip(16, regValue - 1));
        }
            break;

        // And, or, xor, complement
        case AND_A_N8: {
            setRegFlags(Reg.A, Alu.and(reg8.get(Reg.A), read8AfterOpcode()));
        }
            break;
        case AND_A_R8: {
            setRegFlags(Reg.A,
                    Alu.and(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0))));
        }
            break;
        case AND_A_HLR: {
            setRegFlags(Reg.A, Alu.and(reg8.get(Reg.A), read8AtHl()));
        }
            break;
        case OR_A_R8: {
            setRegFlags(Reg.A,
                    Alu.or(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0))));
        }
            break;
        case OR_A_N8: {
            setRegFlags(Reg.A, Alu.or(reg8.get(Reg.A), read8AfterOpcode()));
        }
            break;
        case OR_A_HLR: {
            setRegFlags(Reg.A, Alu.or(reg8.get(Reg.A), read8AtHl()));
        }
            break;
        case XOR_A_R8: {
            setRegFlags(Reg.A,
                    Alu.xor(reg8.get(Reg.A), reg8.get(extractReg(opcode, 0))));
        }
            break;
        case XOR_A_N8: {
            setRegFlags(Reg.A, Alu.xor(reg8.get(Reg.A), read8AfterOpcode()));
        }
            break;
        case XOR_A_HLR: {
            setRegFlags(Reg.A, Alu.xor(reg8.get(Reg.A), read8AtHl()));
        }
            break;
        case CPL: {
            int cpl = Bits.complement8(reg8.get(Reg.A));
            reg8.set(Reg.A, cpl);
            combineAluFlags(cpl, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1,
                    FlagSrc.CPU);
        }
            break;

        // Rotate, shift
        case ROTCA: {
            int rot = Alu.rotate(extractDir(opcode), reg8.get(Reg.A));
            setRegFromAlu(Reg.A, rot);
            combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0,
                    FlagSrc.ALU);
        }
            break;
        case ROTA: {
            int rot = Alu.rotate(extractDir(opcode), reg8.get(Reg.A),
                    reg8.testBit(Reg.F, Flag.C));
            setRegFromAlu(Reg.A, rot);
            combineAluFlags(rot, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0,
                    FlagSrc.ALU);
        }
            break;
        case ROTC_R8: {
            Reg r = extractReg(opcode, 0);
            int rot = Alu.rotate(extractDir(opcode), reg8.get(r));
            setRegFlags(r, rot);
        }
            break;
        case ROT_R8: {
            Reg r = extractReg(opcode, 0);
            int rot = Alu.rotate(extractDir(opcode), reg8.get(r),
                    reg8.testBit(Reg.F, Flag.C));
            setRegFlags(r, rot);
        }
            break;
        case ROTC_HLR: {
            int rot = Alu.rotate(extractDir(opcode), read8AtHl());
            write8AtHlAndSetFlags(rot);
        }
            break;
        case ROT_HLR: {
            int rot = Alu.rotate(extractDir(opcode), read8AtHl(),
                    reg8.testBit(Reg.F, Flag.C));
            write8AtHlAndSetFlags(rot);
        }
            break;
        case SWAP_R8: {
            Reg r = extractReg(opcode, 0);
            int swap = Alu.swap(reg8.get(r));
            setRegFlags(r, swap);
        }
            break;
        case SWAP_HLR: {
            int swap = Alu.swap(read8AtHl());
            write8AtHlAndSetFlags(swap);
        }
            break;
        case SLA_R8: {
            Reg r = extractReg(opcode, 0);
            int shiftL = Alu.shiftLeft(reg8.get(r));
            setRegFlags(r, shiftL);
        }
            break;
        case SRA_R8: {
            Reg r = extractReg(opcode, 0);
            int shiftR = Alu.shiftRightA(reg8.get(r));
            setRegFlags(r, shiftR);
        }
            break;
        case SRL_R8: {
            Reg r = extractReg(opcode, 0);
            int shiftR = Alu.shiftRightL(reg8.get(r));
            setRegFlags(r, shiftR);
        }
            break;
        case SLA_HLR: {
            int shiftL = Alu.shiftLeft(read8AtHl());
            write8AtHlAndSetFlags(shiftL);
        }
            break;
        case SRA_HLR: {
            int shiftR = Alu.shiftRightA(read8AtHl());
            write8AtHlAndSetFlags(shiftR);
        }
            break;
        case SRL_HLR: {
            int shiftR = Alu.shiftRightL(read8AtHl());
            write8AtHlAndSetFlags(shiftR);
        }
            break;

        // Bit test and set
        case BIT_U3_R8: {
            int test = Alu.testBit(reg8.get(extractReg(opcode, 0)),
                    extract(opcode));
            combineAluFlags(test, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1,
                    FlagSrc.CPU);
        }
            break;
        case BIT_U3_HLR: {
            int test = Alu.testBit(read8AtHl(), extract(opcode));
            combineAluFlags(test, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1,
                    FlagSrc.CPU);
        }
            break;
        case CHG_U3_R8: {
            Reg r = extractReg(opcode, 0);
            if (!Bits.test(opcode.encoding, 6)) {
                reg8.set(r,
                        reg8.get(r) & Bits.complement8(1 << extract(opcode)));
            } else {
                reg8.set(r, reg8.get(r) | (1 << extract(opcode)));
            }
        }
            break;
        case CHG_U3_HLR: {
            if (!Bits.test(opcode.encoding, 6)) {
                write8AtHl(
                        read8AtHl() & Bits.complement8(1 << extract(opcode)));
            } else {
                write8AtHl(read8AtHl() | (1 << extract(opcode)));
            }
        }
            break;

        // Misc. ALU
        case DAA: {
            ;
            int adjust = Alu.bcdAdjust(reg8.get(Reg.A),
                    reg8.testBit(Reg.F, Flag.N), reg8.testBit(Reg.F, Flag.H),
                    reg8.testBit(Reg.F, Flag.C));
            setRegFromAlu(Reg.A, adjust);
            combineAluFlags(adjust, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0,
                    FlagSrc.ALU);
        }
            break;
        case SCCF: {
            ;
            if (Bits.test(opcode.encoding, 3)) {

                combineAluFlags(
                        Alu.maskZNHC(false, false, false, carry2(opcode)),
                        FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
            }

            else {
                combineAluFlags(
                        Alu.maskZNHC(false, false, false, carry2(opcode)),
                        FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V1);
            }

        }

            break;

        // Jumps
        case JP_HL: {
            nextPC = reg16(Reg16.HL);
        }
            break;
        case JP_N16: {
            nextPC = read16AfterOpcode();
        }
            break;
        case JP_CC_N16: {
            if (extractCC(opcode)) {
                nextPC = read16AfterOpcode();
                instructionDone = true;
            }
        }
            break;
        case JR_E8: {
            nextPC += Bits.signExtend8(read8AfterOpcode());
        }
            break;
        case JR_CC_E8: {
            if (extractCC(opcode)) {
                nextPC += Bits.signExtend8(read8AfterOpcode());
                instructionDone = true;
            }
        }
            break;

        // Calls and returns
        case CALL_N16: {
            push16(nextPC);
            nextPC = read16AfterOpcode();
        }
            break;
        case CALL_CC_N16: {
            if (extractCC(opcode)) {
                push16(nextPC);
                nextPC = read16AfterOpcode();
                instructionDone = true;
            }
        }
            break;
        case RST_U3: {
            push16(nextPC);
            nextPC = 8 * Bits.extract(opcode.encoding, 3, 3);
        }
            break;
        case RET: {
            nextPC = pop16();
        }
            break;
        case RET_CC: {
            ;
            if (extractCC(opcode)) {
                nextPC = pop16();
                instructionDone = true;
            }
        }
            break;

        // Interrupts
        case EDI: {
            ;
            if ((Bits.test(opcode.encoding, 3))) {
                IME = true;

            } else {
                IME = false;

            }
        }
            break;
        case RETI: {
            IME = true;
            nextPC = pop16();
        }
            break;

        // Misc control
        case HALT: {
            nextNonIdleCycle = Long.MAX_VALUE;
        }
            break;
        case STOP:
            throw new Error("STOP is not implemented");
        }

        PC = nextPC;
        nextNonIdleCycle += opcode.cycles
                + (instructionDone ? opcode.additionalCycles : 0);
    }

    /**
     * attache le cpu au bus donné
     * 
     * @param bus
     *            donne le bus à attacher au processeur
     */
    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
    }

    /**
     * lit depuis le bus la valeur à l'addresse donnée
     * 
     * @param address
     *            addresse dont on va lire la valeur
     * @return la valeur qui est à l'addresse donnée depuis le bus
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits
     */
    private int read8(int address) {
        return bus.read(address);

    }

    /**
     * lit depuis le bus la valeur 8 bits à l'adresse contenue dans la paire de
     * registres HL
     * 
     * @return la valeur qui est à l'addresse contenue dans le registre HL
     *         depuis le bus
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits
     * 
     */

    private int read8AtHl() {
        return read8(reg16(Reg16.HL));
    }

    /**
     * qui lit depuis le bus la valeur 8 bits à l'adresse suivant celle contenue
     * dans le compteur de programme
     * 
     * @return la valeur lue depuis le bus la valeur 8 bits suivant celle
     *         contenue dans le compteur de programme
     * 
     */

    private int read8AfterOpcode() {
        assert PC < 0xFFFF;

        return read8(PC + 1);
    }

    /**
     * lit depuis le bus la valeur contenue à l'addresse donnée et à l'addresse
     * donnée +1
     * 
     * @param address
     *            donne l'addresse ou on doit prendre les valeurs
     * @return la valeur lue depuis le bus la valeur contenue à l'addresse
     *         donnée et à l'addresse donnée +1
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits
     */
    private int read16(int address) {
        assert address < 0xFFFF;

        return (Bits.make16(read8(address + 1), read8(address)));
    }

    /**
     * lit depuis le bus la valeur 16 bits contenue à l'addresse suivant celle
     * contenue dans le compteur de programme
     * 
     * @return la valeur lue depuis le bus la valeur 16 bits contenue à
     *         l'addresse suivant celle contenue dans le compteur de programme
     * 
     */
    private int read16AfterOpcode() {

        int a = PC + 1;
        int b = read16(a);

        return b;
    }

    /**
     * écrit sur le bus, à l'addresse donnée la valeur 8 bits donnée
     * 
     * @param address
     *            donne l'addresse ou on va écrire la valeur donnée
     * @param v
     *            donne la valeur à écrire ans l'addresse donnée du bus
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits
     */
    private void write8(int address, int v) {
        bus.write(address, v);
    }

    /**
     * écrit sur le bus, à l'addresse donnée la valeur 16 bits donnée
     * 
     * @param address
     *            donne l'addresse ou on va écrire la valeur donnée
     * @param v
     *            donne la valeur à écrire ans l'addresse donnée du bus
     * @throws IllegalArgumentException
     *             si l'adresse n'est pas une valeur 16 bits ou si la donnée
     *             n'est pas une valeur 8 bits
     */
    private void write16(int address, int v) {
        assert address < 0xFFFF;
        bus.write(Bits.clip(16, address + 1), Bits.extract(v, 8, 8));
        bus.write(address, Bits.clip(8, v));
    }

    /**
     * écrit sur le bus la valeur donnée àl'addresse contenue dans le registre
     * Hl
     * 
     * @param v
     *            valeur 8 bits à écrire dans le bus.
     * @throws IllegalArgumentException
     *             si la valeur donnée n'est pas une valeur 8 bits
     */
    private void write8AtHl(int v) {
        write8(reg16(Reg16.HL), v);
    }

    /**
     * décrémente l'adresse contenue dans le pointeur de pile (registre SP) de 2
     * unités, puis écrit à cette nouvelle adresse la valeur 16 bits donnée,
     * 
     * @param v
     *            valeur donnée à écrire à l'addresse contenue dans le registre
     *            SP
     */
    private void push16(int v) {

        SP = Bits.clip(16, SP -= 2);
        write16(SP, v);

    }

    /**
     * qui lit depuis le bus et retourne la valeur 16 bits à l'adresse contenue
     * dans le registre SP puis l'incrémente de 2 unités.
     * 
     * @return la valeur 16 bits à l'addresse contenue dans le registre SP.
     */

    private int pop16() {

        int a = read16(SP);

        SP = Bits.clip(16, SP += 2);

        return a;
    }

    /**
     * donne la valeur contenue dans la paire de registre donnée
     * 
     * @param r
     *            registre de 16 bits
     * @return la valeur de 16 bits faite des valeurs contenues dans la paire
     *         des registres
     */
    private int reg16(Reg16 r) {

        int a = reg8.get(r.r1);
        int b = reg8.get(r.r2);

        return Bits.make16(a, b);
    }

    /**
     * modifie la valeur contenue dans la paire de registres donnée, en faisant
     * attention de mettre à 0 les bits de poids faible si la paire en question
     * est AF
     * 
     * @param r
     *            paire de registre dont on va changer le contenu
     * @param newV
     *            valeur qu'on va mettre dans le registre
     */
    private void setReg16(Reg16 r, int newV) {
        if (r == Reg16.AF) {
            int c = newV & 0b11110000;
            reg8.set(r.r2, c);
            int b = Bits.extract(newV, 8, 8);
            reg8.set(r.r1, b);
        }

        else {

            int a = Bits.clip(8, newV);
            int b = Bits.extract(newV, 8, 8);
            reg8.set(r.r1, b);
            reg8.set(r.r2, a);

        }

    }

    /**
     * modifie la valeur contenue dans la paire de registres donnée, si le
     * registre donné est AF c'est le registre SP qui est changé
     * 
     * @param r
     *            registre dont on change le contenu
     * @param newV
     *            valeur à mettre dans le registre
     */
    private void setReg16SP(Reg16 r, int newV) {
        if (r == Reg16.AF) {
            SP = newV;
        } else {
            reg8.set(r.r2, Bits.clip(8, newV));
            reg8.set(r.r1, Bits.extract(newV, 8, 8));

        }
    }

    /**
     * teste le 4ieme bit de l'opcode pour voir si la rotation se fait à gauche
     * ou a droite
     * 
     * @param opcode
     *            donne l'instruction à éxécuter
     * @return RotDir.RIGHT si le 4ieme bit vaut 1 sinon RotDir.LEFT
     */
    private RotDir extractDir(Opcode opcode) {

        if (Bits.test(opcode.encoding, 3)) {
            return RotDir.RIGHT;
        } else {
            return RotDir.LEFT;
        }
    }

    /**
     * qui extrait et retourne l'identité d'un registre 8 bits de l'encodage de
     * l'opcode donné, à partir du bit d'index donné,
     * 
     * @param opcode
     *            donne l'opcode dont on tirera le registre
     * @param startBit
     *            donne le premier bit, où le registre à tirer est dans l'opcode
     * @return le registre contenu dans les 3 bits de l'opcode donné qui indique
     *         le registre
     */
    private Reg extractReg(Opcode opcode, int startBit) {

        Reg a = null;

        switch (Bits.extract(opcode.encoding, startBit, 3)) {
        case 0:
            a = Reg.B;
            break;
        case 1:
            a = Reg.C;
            break;
        case 2:
            a = Reg.D;
            break;
        case 3:
            a = Reg.E;
            break;
        case 4:
            a = Reg.H;
            break;
        case 5:
            a = Reg.L;
            break;
        case 6:
            ;
            break;
        case 7:
            a = Reg.A;
            break;
        }
        return a;
    }

    /**
     * donne le registre donné dans la paire de bits qui indique les registres
     * dans l'opcode donné
     * 
     * @param opcode
     *            donne l'instruction ou les registres sont indiqués
     * @return le registre donné dans la paire de bits qui indique les registres
     *         dans l'opcode donné
     */
    private Reg16 extractReg16(Opcode opcode) {
        Reg16 a = null;

        switch (Bits.extract(opcode.encoding, 4, 2)) {
        case 0:
            a = Reg16.BC;
            break;
        case 1:
            a = Reg16.DE;
            break;
        case 2:
            a = Reg16.HL;
            break;
        case 3:
            a = Reg16.AF;
            break;
        }
        return a;

    }

    /**
     * qui retourne -1 ou +1 en fonction du bit d'index 4 de l'opcode
     * 
     * @param opcode
     *            donne l'instruction ou le bit est à tester,
     * @return 1 ou -1 en fonction du 4ieme bit, s'il vaut 1 retourne -1 sinon 1
     */
    private int extractHlIncrement(Opcode opcode) {
        int a;
        if (Bits.test(opcode.encoding, 4)) {
            a = -1;
        } else {
            a = 1;
        }

        return a;
    }

    public int extract(Opcode opcode) {
        return Bits.extract(opcode.encoding, 3, 3);

    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        if (Preconditions.checkBits16(address) == AddressMap.REG_IE) {

            IE = Preconditions.checkBits16(data);

        } else if (address == AddressMap.REG_IF) {

            IF = Preconditions.checkBits16(data);
        }

        else if (address >= AddressMap.HIGH_RAM_START
                && address < AddressMap.HIGH_RAM_END) {

            highRam.write(address - AddressMap.HIGH_RAM_START,
                    Preconditions.checkBits16(data));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {
        int a = NO_DATA;
        if (Preconditions.checkBits16(address) == AddressMap.REG_IE) {

            a = IE;

        } else if (address == AddressMap.REG_IF) {

            a = IF;
        }

        else if (address >= AddressMap.HIGH_RAM_START
                && address < AddressMap.HIGH_RAM_END) {

            a = highRam.read(address - AddressMap.HIGH_RAM_START);
        }

        return a;

    }

    /**
     * extrait une valeur depuis la valeur donnée et la stocke dans le registre
     * donnée
     * 
     * @param r
     *            donne le registre à changer
     * @param vf
     *            donne la valeur dont on extrait une autre valeur en enlevant
     *            les fanions
     */
    public void setRegFromAlu(Reg r, int vf) {
        reg8.set(r, Alu.unpackValue(vf));
    }

    /**
     * met dans le registre F les fanions qui sont dans la valeur donnée
     * 
     * @param valueFlags
     *            valeur dont on extrait les fanions
     */
    public void setFlags(int valueFlags) {
        reg8.set(Reg.F, Alu.unpackFlags(valueFlags));
    }

    /**
     * Regroupe les deux fonctions du dessus
     * 
     * @param r
     *            donne le registe ou on met la valeur extraite depuis la valeur
     *            donnée
     * @param vf
     *            donne la valeur depuis laquelle on extrait les fanions et les
     *            bits de poids forts
     */
    public void setRegFlags(Reg r, int vf) {
        setRegFromAlu(r, vf);
        setFlags(vf);
    }

    /**
     * écrit dans le registres HL la valeur donnée et met dans le registe F les
     * fanions qui sont dans la valeur donnée.
     * 
     * @param vf
     *            donne la valeur depuis laquelle on extrait les fanions et les
     *            bits de poids forts
     */
    public void write8AtHlAndSetFlags(int vf) {
        write8AtHl(Alu.unpackValue(vf));
        setFlags(vf);
    }

    /**
     * combine les fanions stockés dans le registre F de la valeur donnée avec
     * ceux contenus dans la paire vf et stocke le resultat dans le registre F
     * 
     * @param vf
     *            valeur utilisée pour combiner ses fanions aux autres valeurs
     *            données
     * @param z
     *            valeur utilisée pour determiner le fanion Z
     * @param n
     *            valeur utilisée pour determiner le fanion N
     * @param h
     *            valeur utilisée pour determiner le fanion H
     * @param c
     *            valeur utilisée pour determiner le fanion C
     */
    public void combineAluFlags(int vf, FlagSrc z, FlagSrc n, FlagSrc h,
            FlagSrc c) {
        switch (c) {
        case V0: {
            reg8.setBit(Reg.F, Flag.values()[4], false);
        }
            break;
        case V1: {
            reg8.setBit(Reg.F, Flag.values()[4], true);
        }
            break;
        case ALU: {
            reg8.setBit(Reg.F, Flag.values()[4],
                    Bits.test(Alu.unpackFlags(vf), 4));
        }
            break;
        case CPU: {
            reg8.setBit(Reg.F, Flag.values()[4], Bits.test(reg8.get(Reg.F), 4));
        }
            break;
        }
        switch (h) {
        case V0: {
            reg8.setBit(Reg.F, Flag.values()[5], false);
        }
            break;
        case V1: {
            reg8.setBit(Reg.F, Flag.values()[5], true);
        }
            break;
        case ALU: {
            reg8.setBit(Reg.F, Flag.values()[5],
                    Bits.test(Alu.unpackFlags(vf), 5));
        }
            break;
        case CPU: {
            reg8.setBit(Reg.F, Flag.values()[5], Bits.test(reg8.get(Reg.F), 5));
        }
            break;
        }
        switch (n) {
        case V0: {
            reg8.setBit(Reg.F, Flag.values()[6], false);
        }
            break;
        case V1: {
            reg8.setBit(Reg.F, Flag.values()[6], true);
        }
            break;
        case ALU: {
            reg8.setBit(Reg.F, Flag.values()[6],
                    Bits.test(Alu.unpackFlags(vf), 6));
        }
            break;
        case CPU: {
            reg8.setBit(Reg.F, Flag.values()[6], Bits.test(reg8.get(Reg.F), 6));
        }
            break;
        }
        switch (z) {
        case V0: {
            reg8.setBit(Reg.F, Flag.values()[7], false);
        }
            break;
        case V1: {
            reg8.setBit(Reg.F, Flag.values()[7], true);
        }
            break;
        case ALU: {
            reg8.setBit(Reg.F, Flag.values()[7],
                    Bits.test(Alu.unpackFlags(vf), 7));
        }
            break;
        case CPU: {
            reg8.setBit(Reg.F, Flag.values()[7], Bits.test(reg8.get(Reg.F), 7));
        }
            break;
        }

        reg8.set(Reg.F, reg8.get(Reg.F));

    }

    public boolean extractCC(Opcode opcode) {
        int a = Bits.extract(opcode.encoding, 3, 2);
        boolean Return = true;
        switch (a) {
        case 0: {
            Return = !reg8.testBit(Reg.F, Flag.Z);

        }
            break;
        case 1: {
            Return = reg8.testBit(Reg.F, Flag.Z);

        }
            break;
        case 2: {
            Return = !reg8.testBit(Reg.F, Flag.C);

        }
            break;
        case 3: {
            Return = reg8.testBit(Reg.F, Flag.C);
        }
            break;

        }

        return Return;

    }

    /**
     * donne le carry de l'instruction à éxécuter
     * 
     * @param opcode
     *            donne une des valeurs qui influe sur le carry
     * @return 1 si le fanion C est égal à 1 et si le 4ieme bit de l'opcode est
     *         égal à1 sinon 0
     */
    public boolean carry1(Opcode opcode) {

        boolean c = Bits.test(reg8.get(Reg.F), 4);
        boolean b = Bits.test(opcode.encoding, 3);
        return (c & b);

    }

    /**
     * donne l'inverse de la focntion carry1
     * 
     * @param opcode
     *            donne une des valeurs qui influe sur le carry
     * @return 0 si le fanion C est égal à 1 et si le 4ieme bit de l'opcode est
     *         égal à1 sinon 1
     */
    public boolean carry2(Opcode opcode) {
        return !carry1(opcode);
    }

    private boolean testIeIf() {

        return (IF & IE) != 0;

    }

    private boolean getIME() {
        return IME;

    }

    private void setF(int a) {
        Preconditions.checkBits8(a);
        reg8.set(Reg.F, a);

    }

    private void setIE(int a) {
        IE = a;
    }

    private void setIF(int a) {
        IF = a;
    }

    public void requestInterrupt(Interrupt i) {

        IF = Bits.set(IF, i.index(), true);
    }

}