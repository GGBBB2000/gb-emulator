package Model;

class VRam {
    private final byte[] ram;

    public VRam() {
        this.ram = new byte[16 * 1024]; // 16KiB
    }

    public byte read(final int address) {
        return this.ram[address];
    }

    public void write(final int address, byte data) {
        this.ram[address] = data;
    }
}
