package com.zhen.plugin.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESDecryptFn {
    private final byte[] k;
    private final byte[] t;
    AESDecryptFn(byte[] k, byte[] t) {
        this.k = k;
        this.t = t;
    }

    public byte[] run() {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(k, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return cipher.doFinal(t);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
