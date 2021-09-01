package Model.MBCs;

public record CartridgeInfo(byte[] logo, String title, byte[] manufactureCode, byte cgbFlag, byte[] newLicenseeCode,
                            byte sgbFlag, byte cartridgeType, long romSize, long ramSize, byte destinationCode,
                            byte oldLicenseeCode, byte maskRomVersionNumber, byte headerCheckSum, byte[] globalCheckSum,
                            byte[] rom) {
}
