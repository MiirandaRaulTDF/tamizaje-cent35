package cent35.controllers;

import cent35.dtos.ConsultaPublicaDTO;
import cent35.repositories.ResultadosTamizajeRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/api/consulta-publica")
public class ConsultaPublicaController {
    private final ResultadosTamizajeRepository resultados;

    public ConsultaPublicaController(ResultadosTamizajeRepository resultados) {
        this.resultados = resultados;
    }

    @GetMapping("/{token}")
    public ResponseEntity<ConsultaPublicaDTO> consultar(@PathVariable String token) {
        return resultados.findByTokenConsulta(token)
                .map(resultado -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(new ConsultaPublicaDTO(resultado)))
                .orElse(ResponseEntity.notFound().<ConsultaPublicaDTO>build());
    }

    @GetMapping(value = "/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String token, @RequestParam String baseUrl) {
        if (!resultados.findByTokenConsulta(token).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        String target = construirUrlConsulta(baseUrl, token);
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new MultiFormatWriter().encode(target, BarcodeFormat.QR_CODE, 360, 360, hints);
            BufferedImage image = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 360; y++) {
                for (int x = 0; x < 360; x++) image.setRGB(x, y, matrix.get(x, y) ? 0x111111 : 0xFFFFFF);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.noStore())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(output.toByteArray());
        } catch (WriterException | IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el QR");
        }
    }

    private String construirUrlConsulta(String baseUrl, String token) {
        try {
            URI uri = URI.create(baseUrl);
            String path = uri.getPath() == null ? "" : uri.getPath();
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || !path.endsWith("/resultado.html")) {
                throw new IllegalArgumentException("URL inválida");
            }
            return uri.toASCIIString() + "?token=" + token;
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dirección para el QR no es válida");
        }
    }
}
