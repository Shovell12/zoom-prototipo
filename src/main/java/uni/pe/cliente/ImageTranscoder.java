package uni.pe.cliente;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Base64;

/** Conversiones entre BufferedImage y cadenas Base64 JPEG compartidas por toda la capa de media. */
final class ImageTranscoder {

    private ImageTranscoder() {}

    /** Codifica una imagen como JPEG en Base64. */
    static String toBase64Jpg(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /** Decodifica un Base64 a BufferedImage; retorna null si el string es nulo o el contenido inválido. */
    static BufferedImage fromBase64(String base64) throws IOException {
        if (base64 == null) return null;
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }
}
