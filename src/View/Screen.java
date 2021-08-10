package View;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

public class Screen extends JPanel {
    final BufferedImage imageBuffer;
    final Graphics2D g2d;

    public Screen(final int width, final int height) {
        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        g2d = this.imageBuffer.createGraphics();
        this.setPreferredSize(new Dimension(width, height));
        repaint();
    }

    public void setLcdData (byte[] out) {
        this.imageBuffer.setData(Raster.createRaster(this.imageBuffer.getSampleModel(), new DataBufferByte(out, out.length), new Point()));
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(this.imageBuffer, 0, 0, null);
    }
}