package Model;

class Register {
    public int af;
    public int bc;
    public int de;
    public int hl;
    public int sp;
    public int pc;


    Register() {
        af = 0;
        bc = 0;
        de = 0;
        hl = 0;
        sp = 0xFFFE;
        pc = 0x100; // entry point
    }

    public byte getA() { return (byte) ((af & 0xFF00) >> 8); }

    public void setA(byte data) {
        final var upper = data << 8;
        final var lower = this.af & 0xFF;
        this.af = (upper | lower) & 0xFFFF;
    }

    public byte getB() {
        return (byte) ((bc & 0xFF00) >> 8);
    }

    public void setB(byte data) {
        final var upper = data << 8;
        final var lower = this.bc & 0xFF;
        this.bc = (upper | lower) & 0xFFFF;
    }

    public byte getC() {
        return (byte) (bc & 0xFF);
    }

    public void setC(byte data) {
        final var upper = this.bc & 0xFF00;
        this.bc = upper | Byte.toUnsignedInt(data);
    }

    public byte getD() {
        return (byte) ((de & 0xFF00) >> 8);
    }

    public void setD(byte data) {
        final var upper = data << 8;
        final var lower = this.de & 0xFF;
        this.de = (upper | lower) & 0xFFFF;
    }

    public byte getE() {
        return (byte) (de & 0xFF);
    }

    public void setE(byte data) {
        final var upper = this.de & 0xFF00;
        this.de = upper | Byte.toUnsignedInt(data);
    }

    public byte getH() {
        return (byte) ((hl & 0xFF00) >> 8);
    }

    public void setH(byte data) {
        final var upper = data << 8;
        final var lower = this.hl & 0xFF;
        this.hl = (upper | lower) & 0xFFFF;
    }

    public byte getL() {
        return (byte) ((hl) & 0xFF);
    }

    public void setL(byte data) {
        final var upper = this.hl & 0xFF00;
        this.hl = upper | Byte.toUnsignedInt(data);
    }

    public boolean getZ() {
        return ((this.af & 0b1000_0000) >> 7) == 1;
    }

    public void setZ(boolean isZero) {
        if (isZero) {
            this.af |= 0b1000_0000;
        } else {
            this.af &= 0b1111_1111_0111_1111;
        }
    }

    public boolean getN() {
        return ((this.af & 0b0100_0000) >> 6) == 1;
    }

    public void setN(boolean isNegative) {
        if (isNegative) {
            this.af |= 0b0100_0000;
        } else {
            this.af &= 0b1111_1111_1011_1111;
        }
    }

    public boolean getHC() { // half carry flag
        return ((this.af & 0b0010_0000) >> 5) == 1;
    }

    public void setHC(boolean hasHalfCarry) {
        if (hasHalfCarry) {
            this.af |= 0b0010_0000;
        } else {
            this.af &= 0b1111_1111_1101_1111;
        }
    }

    public boolean getFC() { // carry flag
        return ((this.af & 0b0001_0000) >> 4) == 1;
    }

    public void setFC(boolean hasFullCarry) {
        if (hasFullCarry) {
            this.af |= 0b0001_0000;
        } else {
            this.af &= 0b1111_1111_1110_1111;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "pc=" + String.format("0x%04X",pc) +
                ", a=" + String.format("0x%X", getA()) +
                ", bc=" + String.format("0x%X", bc) +
                ", de=" + String.format("0x%X", de) +
                ", hl=" + String.format("0x%X", hl) +
                ", sp=" + String.format("0x%X", sp) +
                ", z=" + getZ() +
                ", n=" + getN() +
                ", h=" + getHC() +
                ", c=" + getFC() +
                '}';
    }
}
