package devices;

public class Cpu {
    Register register;

    public Cpu() {
        register = new Register();
    }

    public int stepByInst() {
        return 0;
    }
    private byte fetch() {
        return 0;
    }
 }