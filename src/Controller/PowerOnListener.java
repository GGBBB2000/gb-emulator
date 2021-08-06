package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

public class PowerOnListener implements ActionListener {
    GameBoy model;
    MainGameView view;

    public PowerOnListener (MainGameView view, GameBoy model) {
        this.view = view;
        this.model = model;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        final var service = this.model.getService();
        service.scheduleAtFixedRate(() -> {
            this.model.powerOn();
        }, 0, 500,TimeUnit.MILLISECONDS);
        this.view.getJMenuBar().getMenu(1).getItem(0).setEnabled(false);
    }
}
