package Controller;

import Model.GameBoy;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

record FileLoader(JFrame view, GameBoy model) implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        final var chooser = new JFileChooser();
        final int selected = chooser.showOpenDialog(view);
        switch (selected) {
            case JFileChooser.APPROVE_OPTION -> {
                final var filePath = chooser.getSelectedFile().getAbsolutePath();
                model.loadCartridge(filePath);
            }
            case JFileChooser.CANCEL_OPTION -> System.out.println("キャンセル");
            // TODO send error message if some error occurred
        }
    }
}
