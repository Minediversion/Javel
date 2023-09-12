package com.minediversion.javel;

import com.intellij.openapi.wm.ToolWindow;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class credEncryption {
    public static final Path cred = Path.of(System.getenv("APPDATA") + "\\JetBrains\\JutgePlugin\\crd");

    public static void createCredFile(Credentials credentials, ToolWindow toolWindow){
        try {
            if (!Files.exists(cred)) {
                byte[] iv = generateIv();
                SecretKey secretKey = getKey(credentials.pass, toolWindow);
                if(secretKey == null) return;
                Files.createFile(cred);
                String crd = Base64.getEncoder().encodeToString(iv) + "\n" +
                        encrypt(credentials.usr.getText(), secretKey, iv) + "\n" +
                        encrypt(String.valueOf(credentials.pass.getPassword()), secretKey, iv);
                Files.write(cred, crd.getBytes());
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static Credentials getCredentials(JPasswordField pass, ToolWindow toolWindow){
        try {
            List<String> list = Files.readAllLines(cred);
            byte[] iv = Base64.getDecoder().decode(list.get(0));
            SecretKey secretKey = getKey(pass, toolWindow);
            if(secretKey == null) return null;
            return new Credentials(decrypt(list.get(1), secretKey, iv), decrypt(list.get(2), secretKey, iv));
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private static String encrypt(String input, SecretKey key, byte[] iv){
       try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] cipherText = cipher.doFinal(input.getBytes());
            return Base64.getEncoder()
                    .encodeToString(cipherText);
        }catch (NoSuchPaddingException | NoSuchAlgorithmException |
                InvalidAlgorithmParameterException | InvalidKeyException |
                BadPaddingException | IllegalBlockSizeException e){
            throw new RuntimeException(e);
        }
    }

    private static String decrypt(String cipherText, SecretKey key, byte[] iv){
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plainText = cipher.doFinal(Base64.getDecoder()
                    .decode(cipherText));
            return new String(plainText);
        }catch (NoSuchPaddingException | NoSuchAlgorithmException |
        InvalidAlgorithmParameterException | InvalidKeyException |
        BadPaddingException | IllegalBlockSizeException e){
            throw new RuntimeException(e);
        }
    }

    private static SecretKey genKey(JPasswordField pass, byte[] salt){
        try{
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(pass.getPassword(), salt, 65536, 256);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    private static SecretKey getKey(JPasswordField pass, ToolWindow toolWindow){
        try {
            String jks = System.getenv("APPDATA") + "\\JetBrains\\JutgePlugin\\crd.jceks";
            String credAlias = "jutge-cred";
            if(Files.exists(Path.of(jks))) {
                KeyStore keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(new FileInputStream(jks), pass.getPassword());
                return (SecretKey) keyStore.getKey(credAlias, pass.getPassword());
            }else{
                Files.createDirectory(Path.of(System.getenv("APPDATA") + "\\JetBrains\\JutgePlugin\\"));
                Files.createFile(Path.of(jks));
                KeyStore keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(null, pass.getPassword());
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(salt);
                SecretKey secretKey = genKey(pass, salt);
                KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
                KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(pass.getPassword());
                keyStore.setEntry(credAlias, secretKeyEntry, password);
                keyStore.store(new FileOutputStream(jks), pass.getPassword());
                return secretKey;
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }catch (UnrecoverableKeyException e){
            JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Incorrect Password");
            return null;
        }catch (IOException e){
            if(e.getMessage().contains("password")){
                JutgeNotificationHandler.notifyError(toolWindow.getProject(), "Incorrect Password");
                return null;
            }else throw new RuntimeException(e);
        }
    }

    private static byte[] generateIv(){
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static class Credentials {
        JTextField usr;
        JPasswordField pass;

        public Credentials(JTextField usr, JPasswordField pass){
            this.usr = usr;
            this.pass = pass;
        }

        public Credentials(String usr, String pass) {
            this.usr = new JTextField();
            this.usr.setText(usr);
            this.pass = new JPasswordField();
            this.pass.setText(pass);
        }
    }
}
