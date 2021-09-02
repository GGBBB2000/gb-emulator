package Model.MBCs;

class RomOnly extends Cartridge {

    /*
    32KiB ROM only
     */
    public RomOnly(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
    }

    @Override
    public byte read(final int address) {
        return this.cartridgeInfo.rom()[address];
    }

    @Override
    public void write(final int address, byte data) {

    }
}
