package Model;

public final class Cartridge {
    public byte[] logo; // nintendo logo
    public String title;
    public byte[] manufactureCode;
    public byte cgbFlag;
    public byte[] newLicenseeCode;
    public byte sgbFlag;
    public byte cartridgeType;
    public long romSize;
    public long ramSize;
    public byte destinationCode;
    public byte oldLicenseeCode;
    public byte maskRomVersionNumber;
    public byte headerCheckSum;
    public byte[] globalCheckSum;
    public byte[] rom;

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();
        message.append("headerInfo { logo: ");
        for (var i: logo) {
            message.append(String.format("%X", i));
        }
        message.append(", ")
                .append("title: ").append(title).append(", ")
                .append("newLicenseeCode: ")
                .append(String.format("%X%X, ", newLicenseeCode[0], newLicenseeCode[1]))
                .append("sgbFlag: ").append(sgbFlag).append(", ")
                .append("cartridgeType: ").append(cartridgeType).append(", ")
                .append("romSize: ").append((2 << romSize) * 32 * 1024).append(", ")
                .append("ramSize: ").append(ramSize).append(", ")
                .append("destinationCode: ").append(destinationCode).append(", ")
                .append("oldLicenseeCode: ").append(oldLicenseeCode).append(", ")
                .append("maskRomVersionNumber: ").append(maskRomVersionNumber).append(", ")
                .append("headerCheckSum: ").append(String.format("%X, ", headerCheckSum))
                .append("globalCheckSum: ").append(String.format("%X%X }", globalCheckSum[0], globalCheckSum[1]));
        return message.toString();
    }
}
