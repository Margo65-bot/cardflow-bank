package com.example.bankcards.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * JPA-конвертер для шифрования и дешифрования номера банковской карты.
 *
 * <p>Использует алгоритм {@code AES} с режимом {@code ECB} и дополнением {@code PKCS5Padding}.
 * Номер карты автоматически шифруется при сохранении в БД и расшифровывается при чтении.</p>
 *
 * <p>Ключ шифрования берётся из {@code application.yml} (свойство {@code app.encryption.key}).
 * <b>Важно:</b> ключ должен храниться в секрете. При смене ключа все ранее сохранённые
 * номера карт станут нечитаемыми.</p>
 *
 * <p>Применяется через аннотацию {@code @Convert} на поле сущности {@link com.example.bankcards.entity.Card#cardNumber}.</p>
 *
 * @see com.example.bankcards.entity.Card
 */
@Component
@Converter
public class CardNumberEncryptionConverter implements AttributeConverter<String, String> {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /** Секретный ключ шифрования. Внедряется из конфигурации через сеттер */
    private static String SECRET_KEY;

    /**
     * Внедряет секретный ключ из конфигурации приложения.
     *
     * <p>Метод статический, так как JPA требует конструктор без аргументов,
     * а {@code @Value} не работает со статическими полями напрямую.</p>
     *
     * @param secretKey секретный ключ из {@code app.encryption.key}
     */
    @Value("${app.encryption.key}")
    public void setSecretKey(String secretKey) {
        SECRET_KEY = secretKey;
    }

    /**
     * Шифрует номер карты перед сохранением в БД.
     *
     * @param attribute оригинальный номер карты (16 цифр)
     * @return зашифрованный номер в формате Base64 или {@code null} если входной параметр null
     * @throws RuntimeException если произошла ошибка шифрования
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM));
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования номера карты", e);
        }
    }

    /**
     * Расшифровывает номер карты при чтении из БД.
     *
     * @param dbData зашифрованный номер карты в формате Base64
     * @return оригинальный номер карты или {@code null} если входной параметр null
     * @throws RuntimeException если произошла ошибка дешифрования (например, неверный ключ)
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM));
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка дешифрования номера карты", e);
        }
    }
}
