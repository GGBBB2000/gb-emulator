package Controller;

import Model.GameBoy;
import View.MainGameView;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public record MainGameController(MainGameView view, GameBoy model) implements PropertyChangeListener {
    public MainGameController {
        final var menuBar = view.getJMenuBar();
        menuBar.getMenu(0).getItem(0).addActionListener(new FileLoader(view, model));
        menuBar.getMenu(1).getItem(0).addActionListener(new PowerOnListener(view, model));
        for (int i = 0; i < 5; i++) {
            final var item = menuBar.getMenu(2).getItem(i);
            item.addActionListener(new ScreenScaleController(view, i + 1));
        }
        view.addKeyListener(new JoyPadInputListener(view, model));
        view.setTransferHandler(new FileDropController(model));
        model.addPropertyChangeListener(this);
        model.addLcdListener(new LcdController(view, model));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final var p = evt.getPropertyName();
        if (p.equals("loadFiled")) {
            JOptionPane.showMessageDialog(this.view, "ファイルの読み込み中にエラーが発生しました", "loading error", JOptionPane.ERROR_MESSAGE);
        } else if (p.equals("success")) {
            // index 1 ...[Machine]
            this.view.getJMenuBar().getMenu(1).getItem(0).setEnabled(true);
        }
    }
}
