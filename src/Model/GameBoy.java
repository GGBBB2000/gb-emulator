package Model;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class GameBoy {

    PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    Cpu cpu;
    Bus bus;
    Cartridge cartridge;

    public GameBoy() {
        this.bus = new Bus();
        this.cpu = new Cpu(this.bus);
    }
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        pcs.removePropertyChangeListener(propertyChangeListener);
    }

    public void loadCartridge(@NotNull final String filePath) {
        byte[] tmpRom;
        try {
            tmpRom = Files.readAllBytes(Path.of(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            pcs.firePropertyChange("loadFailed", false, true);
            return;
        }
        final var cartridge = new Cartridge();
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
            pcs.firePropertyChange("loadFailed", false, true);
        }
        cartridge.rom = tmpRom;
        this.cartridge = cartridge;
        pcs.firePropertyChange("success", false, true);
    }

    public void powerOn() {
        if (this.cartridge != null) {
            this.bus.mapRom(this.cartridge.rom);
            this.run();
        }
    }

    public void run() {
        cpu.stepByInst();
    }
}
