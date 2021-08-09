package Model;

class VRam {
    private final byte[][] ram;
    private int bank;

    public VRam() {
        this.ram = new byte[2][16 * 1024]; // 16KiB * 2
        this.bank = 0;
    }

    public byte read(final int address) {
        return this.ram[this.bank][address];
    }

    public void write(final int address, byte data) {
        this.ram[bank][address] = data;
    }
}
