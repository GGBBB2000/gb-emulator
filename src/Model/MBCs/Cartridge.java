package Model.MBCs;

import Model.IODevice;

public abstract class Cartridge implements IODevice {
    CartridgeInfo cartridgeInfo;

    public Cartridge(final CartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
    }

    @Override
    abstract public byte read(final int address);

    @Override
    abstract public void write(int address, byte data);

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
                .append("romSize: ").append((2 << info.romSize()) * 32 * 1024).append(", ")
                .append("ramSize: ").append(info.ramSize()).append(", ")
                .append("destinationCode: ").append(info.destinationCode()).append(", ")
                .append("oldLicenseeCode: ").append(info.oldLicenseeCode()).append(", ")
                .append("maskRomVersionNumber: ").append(info.maskRomVersionNumber()).append(", ")
                .append("headerCheckSum: ").append(String.format("%X, ", info.headerCheckSum()))
                .append("globalCheckSum: ").append(String.format("%X%X }", info.globalCheckSum()[0], info.globalCheckSum()[1]));
        return message.toString();
    }

    public static Cartridge getRom(final CartridgeInfo cartridgeInfo) {
        final var bankNum = Byte.toUnsignedInt(cartridgeInfo.cartridgeType());
        return switch (bankNum) {
            case 0x0 -> new RomOnly(cartridgeInfo);
            case 0x1 -> new Mbc1(cartridgeInfo);
            default -> throw new IllegalStateException("Illegal rom bank(or not implemented) : bank number[" + bankNum + "]");
        };
    }
}

