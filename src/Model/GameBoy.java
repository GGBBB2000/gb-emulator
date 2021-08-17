package Model;

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GameBoy {
    PropertyChangeSupport pcs;
    ScheduledExecutorService service;
    Cpu cpu;
    Bus bus;
    Ppu ppu;
    VRam vRam;
    WRam wRam;
    Cartridge cartridge;
    Lcd lcd;

    public GameBoy() {
        this.lcd = new Lcd(160, 144);
        this.vRam = new VRam();
        this.wRam = new WRam();
        this.ppu = new Ppu(vRam, this.lcd);
        this.bus = new Bus(this.vRam, this.wRam, this.ppu);
        this.cpu = new Cpu(this.bus);
        this.service = Executors.newSingleThreadScheduledExecutor();
        this.pcs = new PropertyChangeSupport(this);
    }

    public ScheduledExecutorService getService() {
        return service;
    }

    public byte[] getLcd() { return this.lcd.getData(); }

    public void addLcdListener(PropertyChangeListener propertyChangeListener) {
        this.lcd.addPropertyChangeListener(propertyChangeListener);
    }

    public void removeLcdListener(PropertyChangeListener propertyChangeListener) {
        this.lcd.removePropertyChangeListener(propertyChangeListener);
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
        final var cart = new Cartridge();
        cart.logo = Arrays.copyOfRange(tmpRom, 0x104, 0x134);
        cart.title = new String(Arrays.copyOfRange(tmpRom, 0x134, 0x144));
        cart.manufactureCode = Arrays.copyOfRange(tmpRom, 0x13F, 0x143);
        cart.cgbFlag = tmpRom[0x143];
        cart.newLicenseeCode = Arrays.copyOfRange(tmpRom, 0x144, 0x146);
        cart.sgbFlag = tmpRom[0x146];
        cart.cartridgeType = tmpRom[0x147];
        cart.romSize = tmpRom[0x148];
        cart.ramSize = tmpRom[0x149];
        cart.destinationCode = tmpRom[0x14A];
        cart.oldLicenseeCode = tmpRom[0x14B];
        cart.maskRomVersionNumber = tmpRom[0x14C];
        cart.headerCheckSum = tmpRom[0x14D];
        cart.globalCheckSum = Arrays.copyOfRange(tmpRom, 0x14E, 0x150);
        byte checkSum = 0;
        for (int i = 0x134; i <= 0x14C; i++) {
            checkSum = (byte) (checkSum - tmpRom[i] - 1);
        }
        if (checkSum != cart.headerCheckSum) {
            pcs.firePropertyChange("loadFailed", false, true);
        }
        cart.rom = tmpRom;
        this.bus.connectCartridge(cart);
        this.cartridge = cart;
        pcs.firePropertyChange("success", false, true);
    }

    public void powerOn() {
        if (this.cartridge != null) {
            this.run();
        }
    }

    int count = 0;
    public void run() {
        int cycleSum = 0;
        while (cycleSum < 70224) {
            final int cycle = cpu.stepByInst();
            ppu.run(cycle);
            cycleSum += cycle;
        }
        if (count++ % 59 == 0) {
            System.out.println(count / 60);
        }
        count %= 600;
    }
}