package View;

import javax.swing.*;

public class MainGameView extends JFrame {
    JPanel jPanel;
    JMenuBar menuBar;
    Screen lcd;

    public MainGameView() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.jPanel = new JPanel();
        this.jPanel.setSize(160, 144);
        this.lcd = new Screen(160, 144);

        this.menuBar = this.createMenuBar();
        this.setJMenuBar(this.menuBar);

        this.jPanel.add(this.lcd);

        this.setContentPane(this.jPanel);
        this.setSize(250, 250);
        this.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        final var jMenuBar = new JMenuBar();
        final var file = new JMenu("Files");
        final var load = new JMenuItem("load");
        file.add(load);
        jMenuBar.add(file);

        final var machine = new JMenu("Machine");
        final var powerOn = new JMenuItem("Power on");
        powerOn.setEnabled(false);
        machine.add(powerOn);
        jMenuBar.add(machine);

        return jMenuBar;
    }

    public Screen getLcd() {
        return lcd;
    }
}
