package Model;

class WRam {
    final byte[][] ram;
    int bank; // bank number: 1~7

    public WRam() {
        this.ram = new byte[8][8 * 1024]; // 8KiB * 8 (0 is a fixed bank)
        this.bank = 1;
    }

    public byte read(final int address) {
        if (address < 0x2000) {
            return this.ram[0][address];
        } else {
            return this.ram[bank][address - 0x2000];
        }
    }

    public void write(final int address, byte data) {
        if (address < 0x2000) {
            this.ram[0][address] = data;
        } else {
            this.ram[this.bank][address - 0x2000] = data;
        }
    }
}
