package Model.MBCs;

class RomOnly extends Cartridge {
    @Override
    public byte read(int address) {
        return this.rom[address];
    }

    @Override
    public void write(int address, byte data) {

    }
}
