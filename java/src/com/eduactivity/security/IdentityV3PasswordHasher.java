package com.eduactivity.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class IdentityV3PasswordHasher {
    // Defaults similar to ASP.NET Identity v3
    private static final int PRF_HMACSHA256 = 1; // maps to PBKDF2WithHmacSHA256
    private static final int ITERATIONS = 10000;
    private static final int SALT_LEN = 16;
    private static final int SUBKEY_LEN = 32; // bytes

    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_LEN];
            new SecureRandom().nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, SUBKEY_LEN * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] subkey = skf.generateSecret(spec).getEncoded();

            int totalLen = 1 + 4 + 4 + 4 + salt.length + subkey.length;
            byte[] out = new byte[totalLen];
            int pos = 0;
            out[pos++] = 0x01; // format marker
            writeInt(out, pos, PRF_HMACSHA256); pos += 4;
            writeInt(out, pos, ITERATIONS); pos += 4;
            writeInt(out, pos, salt.length); pos += 4;
            System.arraycopy(salt, 0, out, pos, salt.length); pos += salt.length;
            System.arraycopy(subkey, 0, out, pos, subkey.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    private static void writeInt(byte[] arr, int offset, int value) {
        arr[offset] = (byte)((value >>> 24) & 0xFF);
        arr[offset+1] = (byte)((value >>> 16) & 0xFF);
        arr[offset+2] = (byte)((value >>> 8) & 0xFF);
        arr[offset+3] = (byte)(value & 0xFF);
    }
}

