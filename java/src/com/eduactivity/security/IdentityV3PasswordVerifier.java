package com.eduactivity.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class IdentityV3PasswordVerifier {
    public static boolean verify(String hashed, String password) {
        if (hashed == null || hashed.isEmpty()) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(hashed);
            if (decoded.length < 13) return false;
            if (decoded[0] != 0x01) return false; // format marker v3

            int prf = readInt(decoded, 1);
            int iter = readInt(decoded, 5);
            int saltLen = readInt(decoded, 9);
            if (saltLen <= 0 || 13 + saltLen > decoded.length) return false;
            byte[] salt = new byte[saltLen];
            System.arraycopy(decoded, 13, salt, 0, saltLen);
            int subkeyLen = decoded.length - 13 - saltLen;
            if (subkeyLen <= 0) return false;
            byte[] expectedSubkey = new byte[subkeyLen];
            System.arraycopy(decoded, 13 + saltLen, expectedSubkey, 0, subkeyLen);

            String alg = prfToAlg(prf);
            if (alg == null) return false;
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iter, subkeyLen * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(alg);
            byte[] actualSubkey = skf.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(expectedSubkey, actualSubkey);
        } catch (Exception e) {
            return false;
        }
    }

    private static String prfToAlg(int prf) {
        // .NET KeyDerivationPrf: HMACSHA1=0, HMACSHA256=1, HMACSHA512=2
        switch (prf) {
            case 0: return "PBKDF2WithHmacSHA1";
            case 1: return "PBKDF2WithHmacSHA256";
            case 2: return "PBKDF2WithHmacSHA512";
            default: return null;
        }
    }

    private static int readInt(byte[] arr, int offset) {
        // network byte order (big-endian)
        return (arr[offset] & 0xFF) << 24 |
               (arr[offset+1] & 0xFF) << 16 |
               (arr[offset+2] & 0xFF) << 8 |
               (arr[offset+3] & 0xFF);
    }
}

