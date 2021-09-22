package Controller;

import Model.GameBoy;
import View.MainGameView;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

public class FileDropController extends TransferHandler {
    final GameBoy model;
    final MainGameView view;
    static final DataFlavor fileFlavor = DataFlavor.javaFileListFlavor;

    FileDropController(MainGameView view, GameBoy model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        return support.isDataFlavorSupported(fileFlavor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) return false;

        List<File> data;
        try {
            var t = support.getTransferable();
            data = (List<File>) t.getTransferData(fileFlavor);
        } catch (Exception e) {
            return false;
        }

        if (data.size() != 1) return false;
        model.loadCartridge(data.get(0).getAbsolutePath());
        final var info = model.getCartridgeInfo();
        this.view.setTitle(info.title());
        model.powerOn();
        return true;
    }
}
