package View;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class Screen extends JPanel {
    final BufferedImage imageBuffer;
    final Dimension scaleInfo;

    public Screen(final int width, final int height) {
        this.imageBuffer = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        this.scaleInfo = new Dimension(width, height);
        this.setPreferredSize(scaleInfo);
        repaint();
    }

    public void setLcdData(byte[] out) {
        final var buffer = (DataBufferByte) this.imageBuffer.getRaster().getDataBuffer();
        for (int i = 0; i < buffer.getSize(); i++) buffer.setElem(i, out[i]);
        repaint();
    }

    public void scaleImage(final int scale) {
        this.scaleInfo.setSize(this.imageBuffer.getWidth() * scale, this.imageBuffer.getHeight() * scale);
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.imageBuffer.getScaledInstance(this.scaleInfo.width, this.scaleInfo.height, Image.SCALE_DEFAULT), 0, 0, null);
        this.setSize(this.scaleInfo);
    }
}