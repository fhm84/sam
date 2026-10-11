package de.halbmann.sam.classification.boundary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import de.halbmann.sam.core.exception.ValidationException;
import dev.langchain4j.data.image.Image;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClassificationServiceTest {

    private final SheetAnalyzer analyzer = mock(SheetAnalyzer.class);
    private final ClassificationService service = new ClassificationService(analyzer, new SimpleMeterRegistry(), false);

    @Test
    void shouldSendOnlyCroppedImageToAnalyzer() throws IOException {
        BufferedImage page = new BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(page, "jpg", jpeg);

        service.analyzeImage(new ByteArrayInputStream(jpeg.toByteArray()));

        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(analyzer).analyze(captor.capture());
        BufferedImage sent = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(captor.getValue().base64Data())));
        assertThat(captor.getValue().mimeType(), is("image/png"));
        assertThat(sent.getWidth(), is(1000));
        assertThat("only top 20% + bottom 15% of the page are sent", sent.getHeight(), is(280 + 210));
    }

    @Test
    void shouldRejectUndecodableImageWithoutCallingAnalyzer() {
        byte[] heicLike = "not decodable by ImageIO".getBytes();

        assertThrows(ValidationException.class, () -> service.analyzeImage(new ByteArrayInputStream(heicLike)));
        verify(analyzer, never()).analyze(any(Image.class));
    }
}
