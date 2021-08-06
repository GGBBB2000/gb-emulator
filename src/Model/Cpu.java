package Model;

import jdk.jshell.spi.ExecutionControl;

public class Cpu {
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

    private int read2Byte(final int address) {
        final int upperByte = this.read(address);
        final int lowerByte = this.read(address - 1);
        return (upperByte << 8) | lowerByte;
    }

    private byte readImmediateN() {
        return this.read(register.pc++);
    }

    private int readImmediateAddr() {
        final byte lowerByte = readImmediateN();
        final byte upperByte = readImmediateN();
        return (upperByte << 8) | lowerByte;
    }

    private void write(final int address, final byte data) {
        this.bus.write(address, data);
    }

    private void write2Byte(final int address, final int data) {
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
            case SP -> {
                this.write(this.register.sp, data);
                this.register.pc++;
            }
        }
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
            case NN, INDEXED_N-> this.read(param.getImmediateVal());
            case CC_C -> (this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal(); // read as singed value!
            case CC_NC -> !(this.register.getHC()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_Z -> (this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            case CC_NZ -> !(this.register.getZ()) ? (byte) 0 : (byte) param.getImmediateVal();
            default -> throw new IllegalArgumentException(String.format("[%s] Invalid Argument. Use 8bit argument\n", param));
        };
    }

    private void push1Byte(int data) {
        this.write2Byte(this.register.sp, data);
        this.register.sp -= 2;
    }

    private int pop1Byte() {
        final var ret = this.read2Byte(this.register.sp);
        this.register.sp -= 2;
        return ret;
    }

    private void set16bitDataByParam(Params param, int data) {
        switch (param) {
            case AF -> this.register.af = data;
            case BC -> this.register.bc = data; // LD BC, nn
            case DE -> this.register.de = data;
            case HL -> this.register.hl = data;
            case SP -> this.register.sp = data;
            case PC -> this.register.pc = data;
            case NN -> this.write2Byte(param.getImmediateVal(), data);
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

    public int stepByInst() {
//        while (this.register.pc < 0x150) {
            final var op = readImmediateN();
            final var instInfo = parse(op);
            try {
                execInstruction(instInfo);
            } catch (ExecutionControl.NotImplementedException | IllegalArgumentException e) {
                e.printStackTrace();
                //break;
            }
            System.out.printf("%-20s %s IME=%b\n", instInfo, this.register.toString(), this.imeFlag);
       // }
        return 0;
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

    private void execInstruction(final InstructionInfo instInfo) throws ExecutionControl.NotImplementedException {
        switch (instInfo.instruction()) {
            case LD -> { // 8bit LD
                final byte data = this.get8bitDataByParam(instInfo.from());
                this.set8bitDataByParam(instInfo.to(), data);
            }
            case WLD -> { // 16bit LD
                final int data = this.get16bitDataByParam(instInfo.from());
                this.set16bitDataByParam(instInfo.to(), data);
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
            case JP -> {
                if (instInfo.to() != Params.PC
                        && (instInfo.to() == Params.CC_C ||
                        instInfo.to() == Params.CC_Z ||
                        instInfo.to() == Params.CC_NC ||
                        instInfo.to() == Params.CC_NZ)) { // conditional jump
                    final var n = this.get8bitDataByParam(instInfo.from());
                    this.register.pc += n;
                } else if (instInfo.to() == Params.PC) {
                    final int jumpAddr = this.get16bitDataByParam(instInfo.from());
                    this.set16bitDataByParam(instInfo.to(), jumpAddr);
                } else {
                    throw new IllegalArgumentException(String.format("[%s]: Illegal parameter!\n", instInfo.to()));
                }

            }
            case PUSH -> {
                final int data = this.get16bitDataByParam(instInfo.from());
                this.push1Byte(data);
            }
            case POP -> {
                final int data = this.pop1Byte();
                this.set16bitDataByParam(instInfo.to(), data);
            }
            case CP -> {
                final byte data = this.get8bitDataByParam(instInfo.from());
                final var result = this.register.getA() - data;
                this.register.setZ(result == 0);
                this.register.setN(true);
                this.register.setHC((this.register.getA() & 0xF) < (data & 0xF));
                this.register.setFC(this.register.getA() < data);
            }
            case JR -> {
                final var n = this.get8bitDataByParam(instInfo.from());
                this.register.pc += n;
            }
            case DI -> this.imeFlag = false; // disable interrupt IME <- false
            default -> throw new ExecutionControl.NotImplementedException(String.format("[%s]: not implemented or Illegal instruction!\n", instInfo));
        }
    }
}