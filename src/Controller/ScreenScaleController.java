package Controller;

import View.MainGameView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public record ScreenScaleController(MainGameView view, int scale) implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        final var screen = view.getLcd();
        screen.scaleImage(this.scale);
        view.setScale(this.scale);
    }
}
