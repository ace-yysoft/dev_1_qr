package com.example.qrgenerator.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import com.example.qrgenerator.model.Product;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.BasicStroke;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeService {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 200;
    private static final String LOGO_PATH = "classpath:static/logo.png";
    private static final int LOGO_SIZE = 40;
    
    private final SecurityService securityService;
    
    public Product generateQRCode(Product product) {
        return generateQRCode(product, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    public Product generateQRCode(Product product, int width, int height) {
        try {
            LocalDateTime timestamp = LocalDateTime.now();
            String hash = securityService.generateHash(product.getCode(), timestamp);
            String qrContent = securityService.generateSecureQRContent(
                product.getCode(), timestamp, hash);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                width,
                height
            );
            
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            addLogo(qrImage);
            
            qrImage = applyStyle(qrImage);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            String qrCode = Base64.getEncoder().encodeToString(baos.toByteArray());
            
            product.setQrCode(qrCode);
            product.setQrCodeText(qrContent);
            return product;
            
        } catch (Exception e) {
            throw new RuntimeException("QR 코드 생성 실패", e);
        }
    }
    
    private void addLogo(BufferedImage qrImage) {
        try {
            Resource resource = new ClassPathResource(LOGO_PATH);
            BufferedImage logo = ImageIO.read(resource.getInputStream());
            
            BufferedImage scaledLogo = new BufferedImage(
                LOGO_SIZE, LOGO_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaledLogo.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(logo, 0, 0, LOGO_SIZE, LOGO_SIZE, null);
            g2d.dispose();
            
            int x = (qrImage.getWidth() - LOGO_SIZE) / 2;
            int y = (qrImage.getHeight() - LOGO_SIZE) / 2;
            Graphics2D qrGraphics = qrImage.createGraphics();
            qrGraphics.drawImage(scaledLogo, x, y, null);
            qrGraphics.dispose();
        } catch (Exception e) {
            log.warn("로고 추가 실패", e);
        }
    }
    
    private BufferedImage applyStyle(BufferedImage qrImage) {
        int padding = 20;
        int newWidth = qrImage.getWidth() + (padding * 2);
        int newHeight = qrImage.getHeight() + (padding * 2);
        
        BufferedImage styledImage = new BufferedImage(
            newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d = styledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);
        
        g2d.drawImage(qrImage, padding, padding, null);
        
        g2d.setColor(new Color(26, 35, 126));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(padding/2, padding/2, 
            newWidth - padding, newHeight - padding);
        
        g2d.dispose();
        return styledImage;
    }
} 