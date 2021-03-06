package Model;

import jdk.jshell.spi.ExecutionControl;

class Cpu implements IODevice {
    final Register register;
    final Bus bus;
    boolean imeFlag;
    boolean isHalt;
    boolean isStopped;

    public Cpu(Bus bus) {
        this.register = new Register();
        this.bus = bus;
        this.imeFlag = false;
        this.isHalt = false;
        this.isStopped = false;
    }

    public void reset() {
        this.register.reset();
        this.imeFlag = false;
        this.isHalt = false;
        this.isStopped = false;
    }

    public void resume() {
        this.isStopped = false;
    }

    @Override
    public byte read(final int address) {
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

    @Override
    public void write(final int address, final byte data) {
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
            case N -> (byte) param.getImmediateVal(); //????????????
            case NN, INDEXED_N -> this.read(param.getImmediateVal());
            case BIT0 -> (byte)0;
            case BIT1 -> (byte)1;
            case BIT2 -> (byte)2;
            case BIT3 -> (byte)3;
            case BIT4 -> (byte)4;
            case BIT5 -> (byte)5;
            case BIT6 -> (byte)6;
            case BIT7 -> (byte)7;
            case CC_C -> (this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal(); // read as singed value!
            case CC_NC -> !(this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_Z -> (this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_NZ -> !(this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            default -> throw new IllegalArgumentException(String.format("[%s] Invalid Argument. Use 8bit argument\n", param));
        };
    }

    private void set16bitDataByParam(Params param, int data) {
        switch (param) {
            case AF -> this.register.af = data & 0xFFF0; // lower 4 bits of f register are always 0
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
            case SP -> this.register.sp;
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
                final var flagRequested = ((irf >>> i) & 0x1) == 1;
                if (flagEnabled && flagRequested) {
                    final int vector = interruptVector[i];
                    final int bitMask = (~(1 << i)) & 0b0001_1111 ; // if i == 2  1 << 2 -> 0b0000_0100 -> (inverse, and)???0b0001_1011
                    this.write(0xFF0F, (byte)(irf & bitMask));
                    this.push2Byte(this.register.pc);
                    this.register.pc = vector & 0x00FF;
                    this.imeFlag = false;
                    this.isHalt = false;
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
        if (this.isStopped) {
            return 0;
        }
        if (this.isHalt) {
            final var hasInterrupt = read(0xFF0F) != 0;
            if (hasInterrupt) {
                this.isHalt = false;
            }
            return 4; // same as NOP
        }
        final var op = readImmediateN();
        final var instInfo = parse(op);
        int cycle = 0;
        try {
            cycle = execInstruction(instInfo) + instInfo.cycle();
        } catch (ExecutionControl.NotImplementedException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        //System.out.printf("%-20s %s IME=%b\n", instInfo, this.register.toString(), this.imeFlag);
        return cycle;
    }

    private int execInstruction(final InstructionInfo instInfo) throws ExecutionControl.NotImplementedException {
        int additionalCycle = 0;
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
            case LDHL -> {
                final int n = this.get8bitDataByParam(instInfo.from()); // read n as signed value
                final int sp = this.get16bitDataByParam(instInfo.to());
                final int result = sp + n;
                this.register.hl = result & 0xFFFF;
                this.register.setZ(false);
                this.register.setN(false);
                this.register.setHC((sp & 0xF) + (n & 0xF) > 0xF);
                this.register.setFC((sp & 0xFF) + (n & 0xFF) > 0xFF);
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
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ((result & 0xFF) == 0);
                this.register.setN(false);
                this.register.setHC((from & 0xF) + (to & 0xF) > 0xF);
                this.register.setFC(result > 0xFF);
            }
            case ADC -> {
                final int from = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final int to = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.to()));
                final int carry = (this.register.getFC()) ? 1 : 0;
                final int result = from + to + carry;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ((result & 0xFF) == 0);
                this.register.setN(false);
                this.register.setHC((from & 0xF) + (to & 0xF) + carry > 0xF);
                this.register.setFC(result > 0xFF);
            }
            case SUB -> {
                final var from = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from())); // n
                final var to = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.to())); // reg.a
                final var result = (byte) (to - from); // reg.a - to
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((to & 0xF) < (from & 0xF));
                this.register.setFC(to < from); // set if no borrow

            }
            case SBC -> {
                final var from = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var to = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.to()));
                final var carry = (this.register.getFC()) ? 1 : 0;
                final byte result = (byte) (to - (from + carry));
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((to & 0xF) < (from & 0xF) + carry);
                this.register.setFC(to < from + carry); // set if no borrow
            }
            case AND -> {
                final byte from = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte) (this.register.getA() & from);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(true);
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
                this.register.setHC(((data & 0xF) + 1) > 0xF);
            }
            case DEC -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final byte result = (byte)(data - 1);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((data & 0xF) < 1);
            }
            case WADD -> { //16bit Arithmetic
                switch (instInfo.to()) {
                    case HL -> {
                        final var from = this.get16bitDataByParam(instInfo.from());
                        final var to = this.get16bitDataByParam(instInfo.to());
                        final var result = (from + to) & 0xFFFF;
                        this.set16bitDataByParam(instInfo.to(), result);
                        this.register.setHC((from & 0x0FFF) + (to & 0x0FFF) > 0x0FFF); // carry from bit 11
                        this.register.setFC(from + to > 0xFFFF); // carry from bit 15
                    }
                    case SP -> {
                        final byte from = this.get8bitDataByParam(instInfo.from()); // read N as signed value
                        final var to = this.get16bitDataByParam(instInfo.to());
                        final var result = from + to;
                        this.set16bitDataByParam(instInfo.to(), result);
                        this.register.setZ(false);
                        this.register.setHC((from & 0xF) + (to & 0xF) > 0xF); // carry from bit 3 ?
                        this.register.setFC((from & 0xFF) + (to & 0xFF) > 0xFF); // carry from bit 15
                    }
                    default -> throw new IllegalArgumentException(String.format("WADD: do not use [%s] as TO argument\n", instInfo.to()));
                }
                this.register.setN(false);
            }
            case WINC -> { // 16bit INC
                final var data = this.get16bitDataByParam(instInfo.from());
                final int result = (data + 1) & 0xFFFF;
                this.set16bitDataByParam(instInfo.to(), result);
            }
            case WDEC -> {
                final var data = this.get16bitDataByParam(instInfo.from());
                final int result = (data - 1) & 0xFFFF;
                this.set16bitDataByParam(instInfo.to(), result);
            }
            case SWAP -> { // Miscellaneous
                final var data = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var upper = (data & 0xF0) >> 4;
                final var lower = data & 0x0F;
                final var result = (byte) ((lower << 4) | upper);
                this.set8bitDataByParam(instInfo.to(), result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(false);
            }
            case DAA -> {
                var hexData = Byte.toUnsignedInt(this.register.getA());
                if (this.register.getN()) {
                    if (this.register.getFC()) {
                        hexData -= 0x60;
                    }
                    if (this.register.getHC()) {
                        hexData -= 0x06;
                    }
                } else {
                    if (hexData > 0x99 || this.register.getFC()) {
                        hexData += 0x60;
                        this.register.setFC(true);
                    }
                    final var lowerNibble = hexData & 0xF;
                    if (lowerNibble > 0x9 || this.register.getHC()) {
                        hexData += 0x6;
                    }
                }
                this.register.setA((byte) hexData);
                this.register.setZ(this.register.getA() == 0);
                this.register.setHC(false);

            }
            case CPL -> {
                final var result = (byte) ((~this.register.getA()) & 0xFF);
                this.register.setA(result);
                this.register.setN(true);
                this.register.setHC(true);
            }
            case CCF -> { // complement carry flag
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(!this.register.getFC());
            }
            case SCF -> { // set carry flag
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(true);
            }
            case NOP -> {
            }
            case HALT -> {
                this.isHalt = true;
            }
            case STOP -> {
                //isStopped = true;
            }
            case DI -> this.imeFlag = false; // disable interrupt IME <- false
            case EI -> this.imeFlag = true; // Interrupts are enabled after instruction after EI is executed
            case RLCA -> { // Rotates & Shifts
                final var data = Byte.toUnsignedInt(this.register.getA());
                final var bit7 = (data & 0b1000_0000) >>> 7;
                final var result = (data << 1) | bit7;
                this.register.setA((byte) result);
                this.register.setZ(false);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(bit7 == 1);
            }
            case RLA -> {
                final var data = Byte.toUnsignedInt(this.register.getA());
                final var bit7 = (data & 0b1000_0000) >>> 7;
                final var carryBit = (this.register.getFC()) ? 1 : 0;
                final var result = (data << 1) | carryBit;
                this.register.setA((byte) result);
                this.register.setZ(false);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(bit7 == 1);
            }
            case RRCA -> {
                final var data = Byte.toUnsignedInt(this.register.getA());
                final var lowBit = data & 1;
                final var result = (lowBit << 7) | (data >>> 1);
                this.register.setA((byte) result);
                this.register.setZ(false);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case RRA -> {
                final var data = Byte.toUnsignedInt(this.register.getA());
                final var lowBit = data & 1;
                final var carryBit = (this.register.getFC()) ? 1 : 0;
                final var result = (carryBit << 7) | (data >>> 1);
                this.register.setA((byte) result);
                this.register.setZ(false);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case RLC -> {
                final var data = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var bit7 = (data & 0b1000_0000) >>> 7;
                final var result = (data << 1) | bit7;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ((byte) result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(bit7 == 1);
            }
            case RL -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final var bit7 = (data & 0b1000_0000) >>> 7;
                final var carryBit = (this.register.getFC()) ? 1 : 0;
                final var result = (data << 1) | carryBit;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ((byte) result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(bit7 == 1);
            }
            case RRC -> {
                final var data = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var lowBit = data & 1;
                final var result = (lowBit << 7) | data >>> 1;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case RR -> {
                final var data = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var lowBit = data & 1;
                final var carryBit = (this.register.getFC()) ? 1 : 0;
                final var result = (carryBit << 7) | data >>> 1;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case SLA -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final var signBit = (data & 0b1000_0000) >>> 7;
                var result = data << 1;

                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ((result & 0xFF) == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(signBit == 1);
            }
            case SRA -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final var lowBit = data & 1;
                final var result = data >> 1; // shift arithmetic
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case SRL -> {
                final var data = Byte.toUnsignedInt(this.get8bitDataByParam(instInfo.from()));
                final var lowBit = data & 1;
                final var result = data >>> 1;
                this.set8bitDataByParam(instInfo.to(), (byte) result);
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(false);
                this.register.setFC(lowBit == 1);
            }
            case BIT -> { // Bit Opcodes
                final var bitIndex = this.get8bitDataByParam(instInfo.to());
                final var bitMask = 1 << bitIndex;
                final byte data = this.get8bitDataByParam(instInfo.from());
                final var result = data & bitMask;
                this.register.setZ(result == 0);
                this.register.setN(false);
                this.register.setHC(true);
            }
            case SET -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final var bitIndex = this.get8bitDataByParam(instInfo.to());
                final var bitMask = 1 << bitIndex;
                final var result = data | bitMask;
                this.set8bitDataByParam(instInfo.from(), (byte) result);
            }
            case RES -> {
                final var data = this.get8bitDataByParam(instInfo.from());
                final var bitIndex = this.get8bitDataByParam(instInfo.to());
                final var bitMask = (~(1 << bitIndex)) & 0xFF;
                final var result = data & bitMask;
                this.set8bitDataByParam(instInfo.from(), (byte) result);
            }
            case JP -> {
                final var jumpAdder = this.get16bitDataByParam(instInfo.from());
                switch (instInfo.to()) {
                    case CC_C -> {
                        this.register.pc = (this.register.getFC()) ? jumpAdder : this.register.pc;
                        additionalCycle = 4;
                    }
                    case CC_NC -> {
                        this.register.pc = (!this.register.getFC()) ? jumpAdder : this.register.pc;
                        additionalCycle = 4;
                    }
                    case CC_Z -> {
                        this.register.pc = (this.register.getZ()) ? jumpAdder : this.register.pc;
                        additionalCycle = 4;
                    }
                    case CC_NZ -> {
                        this.register.pc = (!this.register.getZ()) ? jumpAdder : this.register.pc;
                        additionalCycle = 4;
                    }
                    default -> this.register.pc = jumpAdder;
                }
            }
            case JR -> {
                final var n = this.get8bitDataByParam(instInfo.from());
                switch (instInfo.to()) {
                    case CC_C -> {
                        this.register.pc += (this.register.getFC()) ? n : 0;
                        additionalCycle += (this.register.getFC()) ? 4 : 0;
                    }
                    case CC_NC -> {
                        this.register.pc += (!this.register.getFC()) ? n : 0;
                        additionalCycle += (!this.register.getFC()) ? 4 : 0;
                    }
                    case CC_Z -> {
                        this.register.pc += (this.register.getZ()) ? n : 0;
                        additionalCycle += (this.register.getZ()) ? 4 : 0;
                    }
                    case CC_NZ -> {
                        this.register.pc += (!this.register.getZ()) ? n : 0;
                        additionalCycle += (!this.register.getZ()) ? 4 : 0;
                    }
                    default -> this.register.pc += n;
                }
            }
             case CALL -> { // Calls
                int address = this.get16bitDataByParam(instInfo.from());
                switch (instInfo.to()) {
                    case NONE -> this.push2Byte(this.register.pc);
                    case CC_C -> {
                        if (this.register.getFC()) {
                            this.push2Byte(this.register.pc);
                            additionalCycle += 12;
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_NC -> {
                        if (!this.register.getFC()) {
                            this.push2Byte(this.register.pc);
                            additionalCycle = 12;
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_Z -> {
                        if (this.register.getZ()) {
                            this.push2Byte(this.register.pc);
                            additionalCycle = 12;
                        } else {
                            address = this.register.pc;
                        }
                    }
                    case CC_NZ -> {
                        if (!this.register.getZ()) {
                            this.push2Byte(this.register.pc);
                            additionalCycle = 12;
                        } else {
                            address = this.register.pc;
                        }
                    }
                    default -> throw new IllegalArgumentException(String.format("CALL: [%s] Illegal argument!\n", instInfo.to()));
                }
                 this.register.pc = address;
             }
            case RST -> {
                final var jumpAdder = switch (instInfo.op()) {
                    case 0xC7 -> 0;
                    case 0xCF -> 0x8;
                    case 0xD7 -> 0x10;
                    case 0xDF -> 0x18;
                    case 0xE7 -> 0x20;
                    case 0xEF -> 0x28;
                    case 0xF7 -> 0x30;
                    case 0xFF -> 0x38;
                    default -> throw new IllegalStateException("RST invalid opcode[" + instInfo.op() + "]");
                };
                this.push2Byte(this.register.pc);
                this.set16bitDataByParam(instInfo.to(), jumpAdder);
            }
            case RET -> { // Returns
                int address = this.register.pc;
                switch (instInfo.to()) {
                    case NONE -> address = this.pop2Byte();
                    case CC_C -> {
                        if (this.register.getFC()) {
                            address = this.pop2Byte();
                            additionalCycle = 12;
                        }
                    }
                    case CC_NC -> {
                         if (!this.register.getFC()) {
                             address = this.pop2Byte();
                             additionalCycle = 12;
                         }
                     }
                     case CC_Z -> {
                         if (this.register.getZ()) {
                             address = this.pop2Byte();
                             additionalCycle = 12;
                         }
                     }
                     case CC_NZ -> {
                         if (!this.register.getZ()) {
                             address = this.pop2Byte();
                             additionalCycle = 12;
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
            default -> throw new ExecutionControl.NotImplementedException(String.format("[%s @ 0x%4X]: not implemented or Illegal instruction!\n", instInfo, this.register.pc));
        }
        return additionalCycle;
    }
}