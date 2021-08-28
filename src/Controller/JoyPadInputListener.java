package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class JoyPadInputListener implements KeyListener {
    final MainGameView view;
    final GameBoy model;

    JoyPadInputListener(MainGameView view, GameBoy model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        final var keyCode = e.getKeyCode();
        this.model.setKeyState(keyCode, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        final var keyCode = e.getKeyCode();
        this.model.setKeyState(keyCode, false);
    }
}
