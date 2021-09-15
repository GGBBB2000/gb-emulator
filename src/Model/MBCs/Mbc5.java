package Model.MBCs;

public class Mbc5 extends Cartridge {

    int romBank;
    int ramBank;
    //final byte[][] ram;

    public Mbc5(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
        this.romBank = 1;
        this.ramBank = 0;
    }

    @Override
    public byte read(final int address) {
        byte data = 0;
        final var rom = this.cartridgeInfo.rom();
        if (address < 0x4000) {
            data = rom[address];
        } else if (address < 0x8000) {
            data = rom[address + (this.romBank - 1) * 16 * 1024];
        } else if (address < 0xC000) {
            // return ram
        }
        return data;
    }

    @Override
    public void write(final int address, final byte data) {
        if (address < 0x2000) { // ram enable

        } else if (address < 0x3000) { // 8 least significant bits of ROM bank number
            this.romBank = Byte.toUnsignedInt(data);
        } else if (address < 0x4000) { // 9bit of ROM bank number
            if (data > 0) {
                this.romBank |= 0b1_0000_0000;
            } else {
                this.romBank &= 0xFF;
            }
        }
    }
}
