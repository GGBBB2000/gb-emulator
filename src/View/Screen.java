package View;

import java.awt.*;
import java.awt.image.BufferedImage;

class Screen extends Canvas {
    final BufferedImage imageBuffer;
    final Graphics2D g2d;

    public Screen(final int width, final int height) {
        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        g2d = this.imageBuffer.createGraphics();
        this.setSize(width, height);
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(this.imageBuffer, 0, 0, null);
    }
}