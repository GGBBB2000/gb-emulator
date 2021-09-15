package Model.MBCs;

public class Mbc1 extends Cartridge {
    byte[] ram;
    int romBankNum;
    int ramBank;
    int secondaryBankNum;
    boolean ramIsEnable;
    BankingMode bankingMode;

    enum BankingMode {
        SIMPLE,
        ADVANCED,
    }

    public Mbc1(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
        final var ramSize = this.getRamSize(cartridgeInfo.ramSize());
        if (ramSize > 0) {
            this.ram = new byte[ramSize];
        }
        this.romBankNum = 1;
        this.ramBank = 0;
        this.ramIsEnable = false;
        this.secondaryBankNum = 0;
        this.bankingMode = BankingMode.SIMPLE;
    }

    @Override
    public byte read(final int address) {
        final var rom = this.cartridgeInfo.rom();
        final var ram = this.ram;
        if (address < 0x4000) {
            return rom[address];
        } else if (address < 0x8000) {
            return rom[address + 16 * 1024 * (this.romBankNum - 1)];
        } else if (0xA000 <= address && address < 0xC000) {
            return ram[address + 8 * 1024 * this.ramBank - 0xA000];
        }
        return 0;
    }

    @Override
    public void write(int address, byte data) {
        if (address < 0x2000) { // RAM Enable
            this.ramIsEnable = data == 0xA; // 00h: disable 0Ah: enable
        } else if (address < 0x4000) {
            this.romBankNum = data & 0b11111;
        } else if (address < 0x6000) { // ram bank or upper large rom bank select
            this.ramBank = data & 0b11;
        } else if (address < 0x8000) { //
            if ((data & 1) == 1) {
                this.bankingMode = BankingMode.ADVANCED;
            } else {
                this.bankingMode = BankingMode.SIMPLE;
            }
        } else if (this.ramIsEnable && 0xA000 <= address && address < 0xC000) {
            this.ram[address + 8 * 1024 * this.ramBank - 0xA000] = data;
        }
    }
}
