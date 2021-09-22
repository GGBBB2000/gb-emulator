package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

record PowerOnListener(MainGameView view, GameBoy model) implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        final var service = this.model.getService();
        this.model.powerOn();
        this.view.getJMenuBar().getMenu(1).getItem(0).setEnabled(false);
    }
}
