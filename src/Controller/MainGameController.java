package Controller;

import Controller.FileLoader;
import Model.GameBoy;
import View.MainGameView;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MainGameController implements PropertyChangeListener  {
    GameBoy model;
    MainGameView view;
    public MainGameController(MainGameView view, GameBoy model) {
        this.model = model;
        this.view = view;
        final var menuBar = view.getJMenuBar();
        menuBar.getMenu(0).getItem(0).addActionListener(new FileLoader(view, model));
        this.model.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final var p =  evt.getPropertyName();
        if (p.equals("loadFiled")) {
            JOptionPane.showMessageDialog(this.view, "ファイルの読み込み中にエラーが発生しました", "loading error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
