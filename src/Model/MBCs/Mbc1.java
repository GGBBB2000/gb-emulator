package Model.MBCs;

public class Mbc1 extends Cartridge {
    byte[][] ram;
    int romBankNum;
    int secondaryBankNum;
    boolean ramIsEnable;

    public Mbc1(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
        if (cartridgeInfo.ramSize() > 0) {
            // TODO: make ram bank
        }
        this.romBankNum = 1;
        this.ramIsEnable = false;
        this.secondaryBankNum = 0;
    }

    @Override
    public byte read(final int address) {
        final var rom = this.cartridgeInfo.rom();
        if (address < 0x4000) {
            return rom[address];
        } else if (address < 0x8000) {
            return rom[address + 16 * 1024 * (this.romBankNum - 1)];
        } else if (0xA000 <= address && address < 0xC000) {
        }
        return 0;
    }

    @Override
    public void write(int address, byte data) {
        if (address < 0x2000) { // RAM Enable
            this.ramIsEnable = data == 0; // 00h: disable 0Ah: enable
        } else if (address < 0x4000) {
            this.romBankNum = data & 0b11111;
        }
    }
}
