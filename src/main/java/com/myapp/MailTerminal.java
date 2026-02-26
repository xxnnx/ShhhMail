package com.myapp;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class MailTerminal{
    public static void main(String[] args){
        String user = "";
        String password = "";
        String CRYPTO_KEY = "";

        try {
            File configFile = new File("./config.txt");
            if (!configFile.exists()) {
                System.out.println("Файл не найден");
                return;
            }
            List<String> lines = Files.readAllLines(configFile.toPath());
            user = lines.get(1).trim();
            password = lines.get(3).trim();
            CRYPTO_KEY = lines.get(5).trim();
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            return;
        }

        String host = "imap.mail.ru"; 

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(host, user, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();

            clearScreen();

            System.out.println("Добро пожаловать!\n");
            
            int total = messages.length;
            int countToShow = 5;

            System.out.println("--- ПОСЛЕДНИЕ ПИСЬМА ---");
            //for (int i = 0; i < countToShow; i++) {
                int i = 0;
                int index = total - 1 - i;
                String subject = messages[index].getSubject();
                //System.out.println("[" + (i + 1) + "] " + messages[index].getSubject());

            if (subject != null && subject.contains("[ENCRYPTED]")) {
                System.out.println("\033[31m[" + (i + 1) + "] [!] " + subject + "\033[0m");
            } else {
            }
            //}

            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();

            if (choice > 0 && choice <= countToShow){
                Message selected = messages[total - choice];
                String rawText = getTextFromMessage(selected);

                System.out.println("\n==================================");
                System.out.println("ОТ: " + selected.getFrom()[0]);
                System.out.println("ТЕМА: " + selected.getSubject());
                System.out.println("ТЕКСТ ПИСЬМА:");
                System.out.println("----------------------------------");
                System.out.println(getTextFromMessage(selected));
                System.out.println("==================================");

                System.out.println("1) Расшифровать текст");
                System.out.println("2) Ответить");

                int subchoice = scanner.nextInt();
                scanner.nextLine();

                if (subchoice == 1){
                    byte[] encryptedBytes = null; 
                    try {
                            encryptedBytes = Base64.getDecoder().decode(rawText.trim());

                            SecretKeySpec secretkey = new SecretKeySpec(CRYPTO_KEY.getBytes(), "AES");
                            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                            cipher.init(Cipher.DECRYPT_MODE, secretkey);

                            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

                            System.out.println("\n--- РАСШИФРОВАННЫЙ ТЕКСТ ---");
                            System.out.println(new String(decryptedBytes, "UTF-8"));
                            System.out.println("---------------------------");
                        
                    } catch (IllegalArgumentException e1){ 
                        System.out.println("\n[!] Не удалось расшифровать ключом из конфига.");
                        System.out.println("Возможно, отправитель использовал другой ключ.");
                        System.out.println("Хотите ввести другой ключ? (y/n)]");
                        String answer = scanner.nextLine();
                        if (answer.equalsIgnoreCase("y")){
                            System.out.println("16-значный ключ:");
                            String otherkey = scanner.nextLine();
                            if (otherkey.length() == 16){
                                try {
                                    SecretKeySpec manualSkey = new SecretKeySpec(otherkey.getBytes("UTF-8"), "AES");
                                    Cipher manualCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                                    manualCipher.init(Cipher.DECRYPT_MODE, manualSkey);
                                    byte[] manualDecrypted = manualCipher.doFinal(encryptedBytes);

                                    System.out.println("\n--- РАСШИФРОВАННО ВРУЧНУЮ ---");
                                    System.out.println(new String(manualDecrypted, "UTF-8"));
                                    System.out.println("-----------------------------");
                                } catch (Exception ex) {
                                    System.out.println("[!] Ошибка: Ручной ключ тоже не подошел. Текст остается секретом.");
                                }
                            }

                        } else { System.out.println("[!] Ошибка: Нужно ровно 16 символов!"); } 
                    }
                } else if (subchoice == 2){
                    System.out.print("Введите текст ответа: ");
                    String replyText = scanner.nextLine();

                    String recipient = selected.getFrom()[0].toString();
                    sendReply(recipient, selected.getSubject(), replyText, session, user, password, CRYPTO_KEY);
                    System.out.println("[...] Шифруем ключом из конфига и отправляем...");
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    private static String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) return bodyPart.getContent().toString();
            }
        }
        return "[Письмо содержит HTML или вложения, просмотр простого текста недоступен]";
    }
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void sendReply(String to, String subject, String body, Session session, String user, String password, String key){
        try{
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(body.getBytes("UTF-8"));
            String secureBody = Base64.getEncoder().encodeToString(encryptedBytes);

            Message reply = new MimeMessage(session);
            reply.setFrom(new InternetAddress(user));
            reply.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            reply.setSubject("Re: [ENCRYPTED]" + subject);
            reply.setText(secureBody);

            Properties smtpProps = new Properties();
            smtpProps.put("mail.smtp.host", "smtp.mail.ru");
            smtpProps.put("mail.smtp.port", "465");
            smtpProps.put("mail.smtp.ssl.enable", "true");
            smtpProps.put("mail.smtp.auth", "true");

            Session smtpSession = Session.getInstance(smtpProps, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
        }
    });
        Transport transport = smtpSession.getTransport("smtp");
        transport.connect("smtp.mail.ru", user, password);
        transport.sendMessage(reply, reply.getAllRecipients());
        transport.close();
        System.out.println("\n[OK] Ответ отправлен!");
    } catch (Exception e) {
        System.out.println("\n[!] Ошибка при отправке: " + e.getMessage());
    }}
}