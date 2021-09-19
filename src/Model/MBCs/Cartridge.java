package Model.MBCs;

import Model.IODevice;

public abstract class Cartridge implements IODevice {
    final CartridgeInfo cartridgeInfo;

    public Cartridge(final CartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
    }

    @Override
    abstract public byte read(final int address);

    @Override
    abstract public void write(int address, byte data);

    int getRamSize(long ramNumber) {
        int num = (int) (ramNumber & 0b111);
        return switch (num) {
            case 0x2 -> 8 * 1024;
            case 0x3 -> 32 * 1024;
            case 0x4 -> 128 * 1024;
            case 0x5 -> 64 * 1024;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        final var info = this.cartridgeInfo;
        StringBuilder message = new StringBuilder();
        message.append("headerInfo { logo: ");
        for (var i : info.logo()) {
            message.append(String.format("%X", i));
        }
        message.append(", ")
                .append("title: ").append(info.title()).append(", ")
                .append("newLicenseeCode: ")
                .append(String.format("%X%X, ", info.newLicenseeCode()[0], info.newLicenseeCode()[1]))
                .append("sgbFlag: ").append(info.sgbFlag()).append(", ")
                .append("cartridgeType: ").append(info.cartridgeType()).append(", ")
                .append("romSize: ").append((1 << info.romSize()) * 32 * 1024).append(", ")
                .append("ramSize: ").append(info.ramSize()).append(", ")
                .append("destinationCode: ").append(info.destinationCode()).append(", ")
                .append("oldLicenseeCode: ").append(info.oldLicenseeCode()).append(", ")
                .append("maskRomVersionNumber: ").append(info.maskRomVersionNumber()).append(", ")
                .append("headerCheckSum: ").append(String.format("%X, ", info.headerCheckSum()))
                .append("globalCheckSum: ").append(String.format("%X%X }", info.globalCheckSum()[0], info.globalCheckSum()[1]));
        return message.toString();
    }

    public CartridgeInfo getCartridgeInfo() {
        return cartridgeInfo;
    }

    public static Cartridge getRom(final CartridgeInfo cartridgeInfo) {
        final var bankNum = Byte.toUnsignedInt(cartridgeInfo.cartridgeType());
        return switch (bankNum) {
            case 0x0 -> new RomOnly(cartridgeInfo);
            case 0x1, 0x2, 0x3 -> new Mbc1(cartridgeInfo);
            case 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E -> new Mbc5(cartridgeInfo);
            default -> throw new IllegalStateException("Illegal rom bank(or not implemented) : bank number[" + bankNum + "]");
        };
    }
}

