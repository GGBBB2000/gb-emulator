package View;

import javax.swing.*;
import java.awt.*;

public class MainGameView extends JFrame {
    final JPanel jPanel;
    final JMenuBar menuBar;
    final Screen lcd;
    final Dimension scaleInfo;

    public MainGameView() {
        this.scaleInfo = new Dimension();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.jPanel = new JPanel();
        this.jPanel.setSize(160, 144);
        this.lcd = new Screen(160, 144);

        this.menuBar = this.createMenuBar();
        this.setJMenuBar(this.menuBar);

        this.jPanel.add(this.lcd);

        this.setContentPane(this.jPanel);
        this.setSize(200, 220);
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

        final var view = new JMenu("View");
        for (int i = 1; i <= 5; i++) {
            final var menuStr = String.format("%d x %d", i, i);
            view.add(menuStr);
        }
        jMenuBar.add(view);

        return jMenuBar;
    }

    public Screen getLcd() {
        return lcd;
    }

    public void setScale(final int scale) {
        final var insets = this.getInsets();
        final int menuHeight = this.menuBar.getHeight();
        final var scaledWidth = 160 * scale + insets.left + insets.right + 20;
        final var scaledHeight = 144 * scale + insets.bottom + insets.top + menuHeight + 20;
        this.scaleInfo.setSize(scaledWidth, scaledHeight);
        this.setSize(this.scaleInfo);
        repaint();
    }
}
