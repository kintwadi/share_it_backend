package com.vicinity24.api.core.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TwoFactorService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;
    private final EncryptionService encryptionService;

    public TwoFactorService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
        this.secretGenerator = new DefaultSecretGenerator();
        this.qrGenerator = new ZxingPngQrGenerator();
        this.codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String generateQrCodeImageUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Vicinity24")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        byte[] imageData;
        try {
            imageData = qrGenerator.generate(data);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Error generating QR code");
        }

        return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }

    public boolean verify(String secret, String code) {
        try {
            // Decrypt the secret before verifying
            String decryptedSecret = encryptionService.decrypt(secret);
            return codeVerifier.isValidCode(decryptedSecret, code);
        } catch (Exception e) {
            // If decryption fails, return false
            return false;
        }
    }
}
