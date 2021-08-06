package View;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

public class Screen extends Canvas {
    final BufferedImage imageBuffer;
    final Graphics2D g2d;

    public Screen(final int width, final int height) {
        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        g2d = this.imageBuffer.createGraphics();
        byte[][] array = new byte[height][width];
        byte[] out = new byte[height * width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[y * width + x] = (byte) 0x50;
                System.out.println(y * width + x);
            }
        }
        this.imageBuffer.setData(Raster.createRaster(this.imageBuffer.getSampleModel(), new DataBufferByte(out, out.length), new Point()));
        this.setSize(width, height);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(this.imageBuffer, 0, 0, null);
    }
}