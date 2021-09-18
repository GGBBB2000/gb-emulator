package Controller;

import Model.GameBoy;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

public class FileDropController extends TransferHandler {
    final GameBoy model;

    static final DataFlavor fileFlavor = DataFlavor.javaFileListFlavor;

    FileDropController(GameBoy model) { this.model = model; }

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
            data = (List<File>)t.getTransferData(fileFlavor);
        } catch (Exception e){
            return false;
        }

        if (data.size() != 1) return false;

        model.loadCartridge(data.get(0).getAbsolutePath());
        return true;
    }
}
