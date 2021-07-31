package devices;

public class Cpu {
}

class Register {
    public byte a = 0;
    public int bc = 0;
    public int de = 0;
    public int hl = 0;
    public int pc = 0x100; // entry point
    public boolean z = false;
    public boolean c = false; // carry
    public boolean n = false; // negative
    public boolean h = false; // half carry

    public byte getB() {
        return (byte) ((bc & 0xFF00) >> 8);
    }

    public void setB(byte data) {
        final var upper = data << 8;
        final var lower = this.bc & 0xFF;
        this.bc = upper | lower;
    }

    public byte getC() {
        return (byte) (bc & 0xFF);
    }

    public void setC(byte data) {
        final var upper = this.bc & 0xFF00;
        this.bc = upper | data;
    }

    public byte getD() {
        return (byte) ((de & 0xFF00) >> 8);
    }

    public void setD(byte data) {
        final var upper = data << 8;
        final var lower = this.de & 0xFF;
        this.de = upper | lower;    }

    public byte getE() {
        return (byte) (de & 0xFF);
    }

    public void setE(byte data) {
        final var upper = this.de & 0xFF00;
        this.de = upper | data;
    }

    public byte getH() {
        return (byte) ((hl & 0xFF00) >> 8);
    }

    public void setH(byte data) {
        final var upper = data << 8;
        final var lower = this.hl & 0xFF;
        this.hl = upper | lower;
    }

    public byte getL() {
        return (byte) ((hl) & 0xFF);
    }

    public void setL(byte data) {
        final var upper = this.hl & 0xFF00;
        this.hl = upper | data;
    }

    @Override
    public String toString() {
        return "Register{" +
                "pc=" + String.format("0x%04X",pc) +
                ", a=" + String.format("0x%X", a) +
                ", bc=" + String.format("0x%X", bc) +
                ", de=" + String.format("0x%X", de) +
                ", hl=" + String.format("0x%X", hl) +
                ", z=" + z +
                ", c=" + c +
                ", n=" + n +
                ", h=" + h +
                '}';
    }
}