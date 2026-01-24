package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.exception.HmacEncodingException;
import java.util.Base64;
import java.util.function.BiFunction;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class EncoderService {

  public BiFunction<String, String, String> hmacSha512Encoder() {
    return (secretKey, toBeEncoded) -> {
      try {
        if (toBeEncoded == null || toBeEncoded.isEmpty()) {
          throw new IllegalArgumentException("Input string to be encoded cannot be null or empty");
        }
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hmac = mac.doFinal(toBeEncoded.getBytes());
        return Base64.getEncoder().encodeToString(hmac);
      } catch (Exception e) {
        throw new HmacEncodingException("Error while encoding string using HMAC-SHA512");
      }
    };
  }

}
