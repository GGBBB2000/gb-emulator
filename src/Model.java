import devices.Bus;
import devices.Cartridge;
import devices.Cpu;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Model {
    Cpu cpu;

    public Model(@NotNull Cartridge cartridge) {
        final var cpuBus = new Bus(cartridge);
        this.cpu = new Cpu(cpuBus);
    }

    public static void main(String[] args) {
        final var cartridge = Model.loadCartridge("./roms/gb-hello-world/helloworld/hello-world.gb");
        //final var headerInfo = Model.loadCartridge("./roms/cpu_instrs/cpu_instrs.gb");
        if (cartridge != null) {
            Model model = new Model(cartridge);
            model.run();
        }
    }

    public static Cartridge loadCartridge(@NotNull final String filePath) {
        byte[] tmpRom;
        try {
            tmpRom = Files.readAllBytes(Path.of(filePath));
        } catch (IOException e) {
            return null;
        }
        var cartridge = new Cartridge();
        cartridge.logo = Arrays.copyOfRange(tmpRom, 0x104, 0x134);
        cartridge.title = new String(Arrays.copyOfRange(tmpRom, 0x134, 0x144));
        cartridge.manufactureCode = Arrays.copyOfRange(tmpRom, 0x13F, 0x143);
        cartridge.cgbFlag = tmpRom[0x143];
        cartridge.newLicenseeCode = Arrays.copyOfRange(tmpRom, 0x144, 0x146);
        cartridge.sgbFlag = tmpRom[0x146];
        cartridge.cartridgeType = tmpRom[0x147];
        cartridge.romSize = tmpRom[0x148];
        cartridge.ramSize = tmpRom[0x149];
        cartridge.destinationCode = tmpRom[0x14A];
        cartridge.oldLicenseeCode = tmpRom[0x14B];
        cartridge.maskRomVersionNumber = tmpRom[0x14C];
        cartridge.headerCheckSum = tmpRom[0x14D];
        cartridge.globalCheckSum = Arrays.copyOfRange(tmpRom, 0x14E, 0x150);
        byte checkSum = 0;
        for (int i = 0x134; i <= 0x14C; i++) {
            checkSum = (byte) (checkSum - tmpRom[i] - 1);
        }
        if (checkSum != cartridge.headerCheckSum) {
            return null;
        }
        cartridge.rom = tmpRom;
        return cartridge;
    }

    public void run() {
        cpu.stepByInst();
    }
}
