package com.skhabibulla.hrassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts optional user-supplied AI credential using the hardware-backed Keystore where available. */
final class SecretStore {
    private static final String ALIAS = "HR_HABIB_AI_KEY";
    private static final String PREF = "hr_secure";

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return kg.generateKey();
    }

    static void save(Context ctx, String secret) throws Exception {
        SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (secret == null || secret.isEmpty()) {
            p.edit().remove("iv").remove("key").apply();
            return;
        }
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        byte[] ciphertext = c.doFinal(secret.getBytes(StandardCharsets.UTF_8));
        p.edit()
                .putString("iv", Base64.encodeToString(c.getIV(), Base64.NO_WRAP))
                .putString("key", Base64.encodeToString(ciphertext, Base64.NO_WRAP)).apply();
    }

    static String read(Context ctx) throws Exception {
        SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String iv = p.getString("iv", "");
        String encrypted = p.getString("key", "");
        if (iv.isEmpty() || encrypted.isEmpty()) return "";
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
}
