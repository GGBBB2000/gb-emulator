package View;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Screen extends JPanel {
    final BufferedImage imageBuffer;

    public Screen(final int width, final int height) {
        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.setPreferredSize(new Dimension(width, height));
        repaint();
    }

    public void setLcdData (byte[] out) {
        final var buffer = (DataBufferByte)this.imageBuffer.getRaster().getDataBuffer();
        for(int i = 0; i < buffer.getSize(); i++) buffer.setElem(i, out[i]);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.imageBuffer, 0, 0, null);
    }
}