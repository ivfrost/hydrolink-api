package dev.ivfrost.hydro_backend.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.function.BiFunction;

@Service
public class EncoderService {

    public BiFunction<String, String, String> hmacSha256Encoder() {
        return (secretKey, toBeEncoded) -> {
            try {
                if (toBeEncoded == null || toBeEncoded.isEmpty()) {
                    throw new IllegalArgumentException("Input string to be encoded cannot be null or empty");
                }
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] hmac = mac.doFinal(toBeEncoded.getBytes());
                return Base64.getEncoder().encodeToString(hmac);
            } catch (Exception e) {
                throw new HmacEncodingException("Error while encoding string using HMAC-SHA256");
            }
        };
    }
}
