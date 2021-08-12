package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class LcdController implements PropertyChangeListener {
    final MainGameView view;
    final GameBoy model;

    public LcdController(MainGameView view, GameBoy model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final var lcdData = model.getLcd();
        final var screen = view.getLcd();
        screen.setLcdData(lcdData);
    }
}
