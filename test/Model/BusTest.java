package Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusTest {
    final VRam vRam = new VRam();
    final WRam wRam = new WRam();
    final Ppu ppu = new Ppu(new VRam(), new Lcd(0, 0), this.interruptRegister);
    final InterruptRegister interruptRegister = new InterruptRegister();
    final Bus bus = new Bus(vRam, wRam, ppu, interruptRegister);

    @Test
    void vramIOTest() {
        for (int addr = 0x8000; addr < 0xA000; addr++) {
            final byte val = (byte)((addr + 1) % 128);
            bus.write(addr, val);
            assertEquals(val, bus.read(addr));
        }
    }
    //TODO: impl extWramTest

    @Test
    void wramIOTest() {
        for (int addr = 0xC000; addr < 0xE000; addr++) {
            final byte val = (byte)((addr + 2) % 128);
            bus.write(addr, val);
            if (addr < 0xDE00) { // read echo wram test
                assertEquals(val, bus.read(addr));
                //System.out.printf("0x%x 0x%x\n", addr, addr + 0x2000);
                assertEquals(val, bus.read(addr + 0x2000));
            } else {
                //System.out.printf("0x%x\n", addr);
                assertEquals(val, bus.read(addr));
            }
        }
    }
}