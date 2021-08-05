package View;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MainGameView extends JFrame {
    JPanel jPanel;
    JMenuBar menuBar;

    public MainGameView() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.jPanel = new JPanel();
        this.jPanel.setSize(160, 144);
        this.jPanel.setBackground(Color.BLACK);

        this.menuBar = this.createMenuBar();

        this.setJMenuBar(this.menuBar);
        this.setContentPane(this.jPanel);
        this.setSize(200, 200);
        this.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        final var jMenuBar = new JMenuBar();
        final var file = new JMenu("Files");

        final var load = new JMenuItem("load");
        file.add(load);
        jMenuBar.add(file);
        return jMenuBar;
    }
}
