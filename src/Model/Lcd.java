package Model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

class Lcd {
    byte[] lcd;
    PropertyChangeSupport pcs;
    boolean isEnabled;

    public Lcd(final int width, final int height) {
        this.lcd = new byte[width * height];
        this.pcs = new PropertyChangeSupport(this);
        this.isEnabled = true;
    }

    int pixelCounter = 0;
    public void draw(byte data) {
        isEnabled = true;
        lcd[pixelCounter] = data;
        pixelCounter++;
        if (pixelCounter == this.lcd.length) {
            this.pcs.firePropertyChange("LCD_DRAW", false, true);
            this.pixelCounter = 0;
        }
    }

    public void reset() {
        Arrays.fill(this.lcd, (byte) 255);
        this.pcs.firePropertyChange("LCD_DRAW", false, this.isEnabled);
        this.pixelCounter = 0;
        this.isEnabled = false;
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
