package Model;

interface IODevice {
    byte read(final int address);
    void write(final int address, final byte data);
}
