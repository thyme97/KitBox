package com.kitbox.qrcode;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrCodeServiceTest {

    @Test
    void encodeDecodeRoundtrip() throws Exception {
        String content = "https://example.com/kitbox?code=加密测试123";
        BufferedImage image = QrCodeService.encode(content, 320, "M", 2);
        assertEquals(320, image.getWidth());
        assertEquals(content, QrCodeService.decode(image));
    }

    @Test
    void allErrorCorrectionLevels() throws Exception {
        String content = "KitBox 二维码 H 级纠错";
        for (String level : new String[]{"L", "M", "Q", "H"}) {
            BufferedImage image = QrCodeService.encode(content, 256, level, 2);
            assertEquals(content, QrCodeService.decode(image), "纠错级别 " + level + " 往返失败");
        }
    }

    @Test
    void emptyContentRejected() {
        assertThrows(IllegalArgumentException.class, () -> QrCodeService.encode("", 320, "M", 2));
    }

    @Test
    void decodeImageWithoutQrFails() {
        BufferedImage plain = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        assertThrows(IllegalArgumentException.class, () -> QrCodeService.decode(plain));
    }
}
