package Model;

import jdk.jshell.spi.ExecutionControl;

class Cpu {
    Register register;
    Bus bus;
    boolean imeFlag;

    public Cpu(Bus bus) {
        this.register = new Register();
        this.bus = bus;
        this.imeFlag = false;
    }

    private byte read(final int address) {
        return this.bus.read(address);
    }

    private int readAddress(final int address) {
        final int lowerByte = Byte.toUnsignedInt(this.read(address));
        final int upperByte = Byte.toUnsignedInt(this.read(address + 1));
        return (upperByte << 8) | lowerByte;
    }

    private byte readImmediateN() {
        return this.read(register.pc++);
    }

    private int readImmediateAddr() {
        final int lowerByte = Byte.toUnsignedInt(readImmediateN());
        final int upperByte = Byte.toUnsignedInt(readImmediateN());
        return (upperByte << 8) | lowerByte;
    }

    private void write(final int address, final byte data) {
        this.bus.write(address, data);
    }

    private void writeAddress(final int address, final int data) {
        final byte lowerByte = (byte) (0xFF & data);
        final byte upperByte = (byte) ((0xFF00 & data) >> 8);
        this.write(address, lowerByte);
        this.write(address + 1, upperByte);
    }

    private void set8bitDataByParam(Params param, byte data) {
        switch (param) {
            case A -> this.register.setA(data);
            case B -> this.register.setB(data);
            case C -> this.register.setC(data);
            case INDEXED_C -> this.write(Byte.toUnsignedInt(this.register.getC()) + 0xFF00, data);
            case D -> this.register.setD(data);
            case E -> this.register.setE(data);
            case H -> this.register.setH(data);
            case L -> this.register.setL(data);
            case BC -> this.write(this.register.bc, data); // LD (BC), n
            case DE -> this.write(this.register.de, data); // LD (DE), n
            case HL -> this.write(this.register.hl, data);
            case NN -> this.write(param.getImmediateVal(), data); // LD (nn), n
            case INDEXED_N -> this.write(param.getImmediateVal(), data);
            case SP -> {
                this.write(this.register.sp, data);
                this.register.pc++;
            }
        }
    }

    private void push2Byte(int data) {
        this.register.sp -= 2;
        this.writeAddress(this.register.sp, data);
    }

    private int pop2Byte() {
        final int ret = this.readAddress(this.register.sp);
        this.register.sp += 2;
        return ret;
    }

    private byte get8bitDataByParam(Params param) {
        return switch (param) {
            case A -> this.register.getA();
            case B -> this.register.getB();
            case C -> this.register.getC();
            case INDEXED_C -> this.read(Byte.toUnsignedInt(this.register.getC()) + 0xFF00);
            case D -> this.register.getD();
            case E -> this.register.getE();
            case H -> this.register.getH();
            case L -> this.register.getL();
            case BC -> this.read(this.register.bc);
            case DE -> this.read(this.register.de);
            case HL -> this.read(this.register.hl);
            case N -> (byte) param.getImmediateVal(); //　怪しい
            case NN, INDEXED_N -> this.read(param.getImmediateVal());
            case CC_C -> (this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal(); // read as singed value!
            case CC_NC -> !(this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_Z -> (this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_NZ -> !(this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            default -> throw new IllegalArgumentException(String.format("[%s] Invalid Argument. Use 8bit argument\n", param));
        };
    }

    private void set16bitDataByParam(Params param, int data) {
        switch (param) {
            case AF -> this.register.af = data;
            case BC -> this.register.bc = data; // LD BC, nn
            case DE -> this.register.de = data;
            case HL -> this.register.hl = data;
            case SP -> this.register.sp = data;
            case PC -> this.register.pc = data;
            case NN -> this.writeAddress(param.getImmediateVal(), data);
        }
    }

    private int get16bitDataByParam(Params param) {
        return switch (param) {
            case AF -> this.register.af;
            case BC -> this.register.bc;
            case DE -> this.register.de;
            case HL -> this.register.hl;
            case INDEXED_N, NN -> param.getImmediateVal(); // 0xFFnn
            case INDEXED_SP -> param.getImmediateVal() + this.register.sp;
            default -> throw new IllegalArgumentException(String.format("[%s] Invalid Argument Use 16bit argument\n", param));
        };
    }

    private boolean checkInterrupt() {
        if (this.imeFlag) {
            final var interruptVector = new int[]{
                    0x40, // vblank
                    0x48, // lcd stat
                    0x50, // timer
                    0x58, // serial
                    0x60  // joypad
            };
            final int ie = this.read(0xFFFF); // interrupt enable
            final int irf = this.read(0xFF0F); // interrupt request flag
            for (int i = 0; i < 5; i++) {
                final var flagEnabled = ((ie >>> i) & 0x1) == 1;
                if (flagEnabled) {
                    final int vector = interruptVector[i];
                    final int bitMask = (~(1 << i)) & 0b0001_1111 ; // if i == 2  1 << 2 -> 0b0000_0100 -> (inverse, and)　0b0001_1011
                    this.write(0xFF0F, (byte)(irf & bitMask));
                    this.push2Byte(this.register.pc);
                    this.register.pc = vector & 0x00FF;
                    return true;
                }
            }
        }
        return false;
    }

    private InstructionInfo parse(byte opcode) {
        final var data = Byte.toUnsignedInt(opcode);
        final InstructionInfo info;
        if (data == 0xCB) {
            info = InstructionInfo.getCBPrefixedInstruction(Byte.toUnsignedInt(this.readImmediateN()));
        } else {
            info = InstructionInfo.getInstruction(data);
        }

        switch (info.from()) {
            case N, INDEXED_N -> info.from().setImmediateVal(Byte.toUnsignedInt(readImmediateN()));
            case NN -> info.from().setImmediateVal(readImmediateAddr());
        }
        switch (info.to()) {
            case N, INDEXED_N, INDEXED_SP -> info.to().setImmediateVal(Byte.toUnsignedInt(readImmediateN()));
            case NN -> info.to().setImmediateVal(readImmediateAddr());
        }
        return info;
    }

    public int stepByInst() {
        if (this.checkInterrupt()) {
            return 4 * 5; // interrupt takes 5 machine cycle
        }
        final var op = readImmediateN();
        final var instInfo = parse(op);
        try {
            execInstruction(instInfo);
        } catch (ExecutionControl.NotImplementedException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        //System.out.printf("%-20s %s IME=%b\n", instInfo, this.register.toString(), this.imeFlag);
        return instInfo.cycle();
    }

    private void execInstruction(final InstructionInfo instInfo) throws ExecutionControl.NotImplementedException {
        switch (instInfo.instruction()) {
            case LD -> { // 8bit LD
                final byte data = this.get8bitDataByParam(instInfo.from());
                this.set8bitDataByParam(instInfo.to(), data);
            }
            case LDI -> {
                final byte data = this.get8bitDataByParam(instInfo.from());
                this.set8bitDataByParam(instInfo.to(), data);
                this.register.hl++;
            }
            case LDD -> {
                final byte data = this.get8bitDataByParam(instInfo.from());
                this.set8bitDataByParam(instInfo.to(), data);
                this.register.hl--;
            }
            case WLD -> { // 16bit LD
                final int data = this.get16bitDataByParam(instInfo.from());
                this.set16bitDataByParam(instInfo.to(), data);
            }
            case PUSH -> {
                final int data = this.get16bitDataByParam(instInfo.from());
                this.push2Byte(data);
            }
            case POP -> {
                final int data = this.pop2Byte();
                this.set16bitDataByParam(instInfo.to(), data);
            }
            case ADD -> { // 8bit ALU
                final int from = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final int to = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.to()));
                final int result = from + to;
                this.set8bitDataByParam(instInfo.to(), (byte)result);
                this.register.setZ((result & 0xFF) == 0);
                this.register.setN(false);
                this.register.setHC((((from & 0xF) + (to & 0xF)) & 0xF) == 0);
                this.register.setFC((result >>> 8) != 0);
            }
            //case ADC -> {
            //}
            //case SUB -> {}
            //case SBC -> {}
            case AND -> {
                final byte from = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(this.register.getA() & from);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(false);
            }
            case OR -> {
                final byte from = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(this.register.getA() | from);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(false);
            }
            case XOR -> {
                final byte from = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(this.register.getA() ^ from);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(false);
            }
            case CP -> {
                // compare "from"  and "to"(A register) as Unsigned value. execute to - from but not write result to "A" register
                final int from = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final int to = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.to())); // argument must be "A" register
                final var result = to - from;
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((to & 0xF) < (from & 0xF));
                this.register.setFC(to < from);
            }
            case INC -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(data + 1);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC((((data & 0xF) + 1) & 0x10) == 0);
            }
            case DEC -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(data - 1);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((data & 0xF) > 1);
            }
            // case ADD(16bit) -> {} //16bit Arithmetic
            case WINC -> { // 16bit INC
                final var data = this.get16bitDataByParam(instInfo.from());
                final int result = data + 1;
                this.set16bitDataByParam(instInfo.to(), result);
            }
            case WDEC -> {
                final var data = this.get16bitDataByParam(instInfo.from());
                final int result = data - 1;
                this.set16bitDataByParam(instInfo.to(), result);
            }
            // case SWAP -> {} // Miscellaneous
            // case DAA -> {}
            // case CPL -> {}
            // case CCF -> {}
            // case SCF -> {}
            case NOP -> {}
            case HALT -> {}
            // case STOP -> {}
            case DI -> this.imeFlag = false; // disable interrupt IME <- false
            case EI -> this.imeFlag = true; // Interrupts are enabled after instruction after EI is executed
            // case RLCA -> {}// Rotates & Shifts
            // case RLA -> {}
            // case RRCA -> {}
            // case RRA -> {}
            // case RLC -> {}
            // case RL -> {}
            // case RRC -> {}
            // case RR -> {}
            // case SLA -> {}
            // case SRA -> {}
            // case SRL -> {}
            // case BIT -> {} // Bit Opcodes
            // case SET -> {}
            // case RES -> {}
            case JP -> {
                final var jumpAdder = this.get16bitDataByParam(instInfo.from());
                this.register.pc = switch (instInfo.to()) {
                    case CC_C -> (this.register.getFC()) ? jumpAdder : 0;
                    case CC_NC -> (!this.register.getFC()) ? jumpAdder : 0;
                    case CC_Z -> (this.register.getZ()) ? jumpAdder : 0;
                    case CC_NZ -> (!this.register.getZ()) ? jumpAdder : 0;
                    default -> jumpAdder;
                };
            }
            case JR -> {
                final var n = this.get8bitDataByParam(instInfo.from());
                this.register.pc += switch (instInfo.to()) {
                    case CC_C -> (this.register.getFC()) ? n : 0;
                    case CC_NC -> (!this.register.getFC()) ? n : 0;
                    case CC_Z -> (this.register.getZ()) ? n : 0;
                    case CC_NZ -> (!this.register.getZ()) ? n : 0;
                    default -> n;
                };
            }
             case CALL -> { // Calls
                int address = this.get16bitDataByParam(instInfo.from());
                switch (instInfo.to()) {
                    case NONE -> this.push2Byte(this.register.pc);
                    case CC_C -> {
                        if (this.register.getFC()) {
                            this.push2Byte(this.register.pc);
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_NC -> {
                        if (!this.register.getFC()) {
                            this.push2Byte(this.register.pc);
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_Z -> {
                        if (this.register.getZ()) {
                            this.push2Byte(this.register.pc);
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_NZ -> {
                        if (!this.register.getZ()) {
                            this.push2Byte(this.register.pc);
                        } else {
                            address = this.register.pc;
                        }
                    }
                    default -> throw new IllegalArgumentException(String.format("CALL: [%s] Illegal argument!\n", instInfo.to()));
                }
                this.register.pc = address;
             }
            // case RST -> {}
             case RET -> { // Returns
                 int address = this.register.pc;
                 switch (instInfo.to()) {
                     case NONE -> address = this.pop2Byte();
                     case CC_C -> {
                         if (this.register.getFC()) {
                             address = this.pop2Byte();
                         }
                     }
                     case CC_NC -> {
                         if (!this.register.getFC()) {
                             address = this.pop2Byte();
                         }
                     }
                     case CC_Z -> {
                         if (this.register.getZ()) {
                             address = this.pop2Byte();
                         }
                     }
                     case CC_NZ -> {
                         if (!this.register.getZ()) {
                             address = this.pop2Byte();
                         }
                     }
                     default -> throw new IllegalArgumentException(String.format("CALL: [%s] Illegal argument!\n", instInfo.to()));
                 }
                 this.register.pc = address;
             }

            case RETI -> {
                this.register.pc = this.pop2Byte();
                this.imeFlag = true;
            }
            default -> throw new ExecutionControl.NotImplementedException(String.format("[%s]: not implemented or Illegal instruction!\n", instInfo));
        }
    }
}