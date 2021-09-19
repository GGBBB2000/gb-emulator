package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

record PowerOnListener(MainGameView view, GameBoy model) implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        final var service = this.model.getService();
        service.scheduleAtFixedRate(this.model::powerOn, 0, 17, TimeUnit.MILLISECONDS);
        this.view.getJMenuBar().getMenu(1).getItem(0).setEnabled(false);
    }
}
