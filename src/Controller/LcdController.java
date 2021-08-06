package Controller;

import Model.GameBoy;
import View.MainGameView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class LcdController implements PropertyChangeListener {
    public LcdController(MainGameView view, GameBoy model) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }

    /*@Override
    public void propertyChange(PropertyChangeEvent evt) {
        // final var data = model.getLcd();
        ////this.imageBuffer.setData(Raster.createRaster(this.imageBuffer.getSampleModel(), new DataBufferByte(out, out.length), new Point()));
        repaint();
    }*/

}
