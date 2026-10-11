package de.halbmann.sam.classification.controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import lombok.experimental.UtilityClass;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
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
@UtilityClass
public class DocumentUtils {

    static final int dpi = 90; // DPI for rendering the PDF

    // Upper bound for uploaded images (photos/scans) — roughly what the PDF path produces for A4 at 90 dpi
    static final int MAX_IMAGE_WIDTH = 1024;

    /**
     * Extracts plain text from the first page of a PDF using PDFTextStripper.
     * Returns an empty string for scanned/image-only PDFs (no embedded text).
     */
    public static String extractText(InputStream pdfStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            return stripper.getText(document);
        }
    }

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

    /**
     * Prepares an uploaded image (photo/scan) for the vision model the same way {@link #pdfToImage}
     * does for PDFs: downscales it to at most {@link #MAX_IMAGE_WIDTH} pixels wide and keeps only the
     * top and bottom strips, so the full page is never sent to the LLM.
     *
     * @throws IOException if the image format cannot be decoded
     */
    public static void imageToCroppedPng(InputStream imageStream, OutputStream outputStream) throws IOException {
        BufferedImage image = ImageIO.read(imageStream);
        if (image == null) {
            throw new IOException("Unsupported image format");
        }

        BufferedImage combined = cropImage(downscale(image, MAX_IMAGE_WIDTH));

        ImageIOUtil.writeImage(combined, "png", outputStream, dpi);
    }

    static BufferedImage downscale(BufferedImage image, int maxWidth) {
        if (image.getWidth() <= maxWidth) {
            return image;
        }
        int height = Math.max(1, (int) Math.round(image.getHeight() * (double) maxWidth / image.getWidth()));
        BufferedImage scaled = new BufferedImage(maxWidth, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, maxWidth, height, null);
        g.dispose();
        return scaled;
    }

    static BufferedImage cropImage(BufferedImage bim) {
        return cropImage(bim, BufferedImage.TYPE_BYTE_GRAY);
    }

    static BufferedImage cropImage(BufferedImage bim, int imageType) {
        int percentageTop = 20;
        // calculate the height for the top part of the image
        int topHeight = (int) (bim.getHeight() / 100.0 * percentageTop);
        BufferedImage topBim = bim.getSubimage(0, 0, bim.getWidth(), topHeight);

        int percentageBottom = 15;
        // calculate the height for the bottom part of the image
        int bottomHeight = (int) (bim.getHeight() / 100.0 * percentageBottom);
        BufferedImage bottomBim = bim.getSubimage(0, bim.getHeight() - bottomHeight, bim.getWidth(), bottomHeight);

        // determine the width and height for the combined image
        int width = Math.max(topBim.getWidth(), bottomBim.getWidth());
        int height = topBim.getHeight() + bottomBim.getHeight();

        // create a new buffered image with the determined width and height
        BufferedImage combined = new BufferedImage(width, height, imageType);

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
