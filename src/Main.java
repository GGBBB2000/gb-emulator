import Controller.MainGameController;
import Model.GameBoy;
import View.MainGameView;

public class Main {
    public static void main(String[] args) {
        final var view = new MainGameView();
        final var model = new GameBoy();
        final var mainController = new MainGameController(view, model);
    }
}
