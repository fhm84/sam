package de.halbmann.sam.classification.controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

/**
 * Utility class for handling PDF documents and converting them to images.
 *
 * <p>This class provides methods to convert a PDF document to an image and crop images. The main
 * method, {@code pdfToImage}, converts the first page of a PDF document to a PNG image and writes
 * it to an output stream. It also includes a commented-out section for converting all pages of a
 * PDF document to images.
 *
 * <p>The {@code cropImage} methods are used to crop a specified percentage of the top and bottom
 * parts of an image and combine them into a new image.
 */
public class DocumentUtils {

  static final int dpi = 90; // DPI for rendering the PDF

  public static void pdfToImage(InputStream pdfFile, OutputStream outputStream) throws IOException {
    // Load the PDF document (incl. auto-close)
    try (PDDocument document = Loader.loadPDF(pdfFile.readAllBytes())) {
      PDFRenderer pdfRenderer = new PDFRenderer(document);
      // in general, we only need the first page to get the sheets metadata like title, publisher,
      // arranger, composer, ...
      BufferedImage bim = pdfRenderer.renderImageWithDPI(0, dpi, ImageType.BINARY);

      BufferedImage combined = cropImage(bim);

      ImageIOUtil.writeImage(combined, "png", outputStream, dpi);
    }
  }

  static BufferedImage cropImage(BufferedImage bim) {
    int percentageTop = 20;
    // calculate the height for the top part of the image
    int topHeight = (int) ((bim.getHeight() / 100.0) * percentageTop);
    BufferedImage topBim = bim.getSubimage(0, 0, bim.getWidth(), topHeight);

    int percentageBottom = 15;
    // calculate the height for the bottom part of the image
    int bottomHeight = (int) ((bim.getHeight() / 100.0) * percentageBottom);
    BufferedImage bottomBim =
        bim.getSubimage(0, bim.getHeight() - bottomHeight, bim.getWidth(), bottomHeight);

    // determine the width and height for the combined image
    int width = Math.max(topBim.getWidth(), bottomBim.getWidth());
    int height = topBim.getHeight() + bottomBim.getHeight();

    // create a new buffered image with the determined width and height
    BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

    // get the graphics context of the combined image
    Graphics g = combined.getGraphics();

    // draw the images onto the combined image
    int y = 0;
    g.drawImage(topBim, 0, y, null);
    y += topBim.getHeight();
    g.drawImage(bottomBim, 0, y, null);

    // dispose the graphics context
    g.dispose();
    return combined;
  }
}
