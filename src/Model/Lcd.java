package Model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Random;

class Lcd {
    byte[] lcd;
    PropertyChangeSupport pcs;

    public Lcd(final int width, final int height) {
        this.lcd = new byte[width * height];
        this.pcs = new PropertyChangeSupport(this);
    }

    int pixelCounter = 0;
    public void draw(byte data) {
        lcd[pixelCounter] = (byte)(new Random()).nextInt();
        pixelCounter++;
        if (pixelCounter == this.lcd.length) {
            this.pcs.firePropertyChange("LCD_DRAW", false, true);
            this.pixelCounter = 0;
        }
    }

    public byte[] getData() {
        return this.lcd;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        pcs.removePropertyChangeListener(propertyChangeListener);
    }
}
