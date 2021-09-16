package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public record JoyPadInputListener(MainGameView view, GameBoy model) implements KeyListener {
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
