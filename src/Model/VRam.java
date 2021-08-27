package Model;

class VRam implements IODevice {
    private final byte[][] ram;
    private final int bank;

    public VRam() {
        this.ram = new byte[2][16 * 1024]; // 16KiB * 2
        this.bank = 0;
    }

    @Override
    public byte read(final int address) {
        return this.ram[this.bank][address];
    }

    @Override
    public void write(final int address, byte data) {
        this.ram[bank][address] = data;
    }
}
