package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

record LcdController(MainGameView view, GameBoy model) implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final var lcdData = model.getLcd();
        final var screen = view.getLcd();
        screen.setLcdData(lcdData);
    }
}
