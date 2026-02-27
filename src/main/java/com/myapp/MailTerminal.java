package com.myapp;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
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

        List<Message> filteredMessages = new ArrayList<>();
        Scanner scanner_main = new Scanner(System.in);

        try {
            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(host, user, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();


            while (true) {
            clearScreen();
            String red = "\u001B[31m";
            String reset = "\u001B[0m";
            String[] hello3D = {
            "     _    _  ______ _      _       ____  ",
            "    | |  | ||  ____| |    | |     / __ \\ ",
            "    | |__| || |__  | |    | |    | |  | |",
            "    |  __  ||  __| | |    | |    | |  | |",
            "    | |  | || |____| |____| |____| |__| |",
            "    |_|  |_||______|______|______|\\____/ "
            };
            for (String line : hello3D) {
                System.out.println(red + line + reset);
            }
            System.out.println("    "+ user + "\n");
            System.out.println("\n[1] Открыть письма (только зашифрованные)");
            System.out.println("[2] Открыть письма (все)");
            System.out.println("[3] Отправить письмо");
            System.out.print("\n[ ]: "); 
            int choice_main = scanner_main.nextInt();

            if (choice_main == 0) { break;}
            filteredMessages.clear();

            if (choice_main == 1 || choice_main == 2) {
                    int limit = Math.max(0, messages.length - 50);
                    
                    for (int i = messages.length - 1; i >= limit && filteredMessages.size() < 5; i--) {
                        if (choice_main == 1) {
                            if (messages[i].getSubject() != null && messages[i].getSubject().contains("[ENCRYPTED]")) {
                                filteredMessages.add(messages[i]);
                            }
                        } else {
                            filteredMessages.add(messages[i]);
                        }
                    }
                    System.out.println("\n--- ПОСЛЕДНИЕ ПИСЬМА ---");
                    if (filteredMessages.isEmpty()) {
                        System.out.println("[!]      Писем не найдено.");
                    } else {
                        for (int i = 0; i < filteredMessages.size(); i++) {
                            String sub = filteredMessages.get(i).getSubject();
                            if (sub != null && sub.contains("[ENCRYPTED]")) {
                                System.out.println("\033[31m[" + (i + 1) + "] [!] " + sub + "\033[0m");
                            } else {
                                System.out.println("[" + (i + 1) + "] " + sub);
                            }
                        }
                        System.out.print("\nВыберите номер письма для чтения (или 0 для возврата): ");
                        int mailChoice = scanner_main.nextInt();
                        scanner_main.nextLine();
                        
                        if (mailChoice > 0 && mailChoice <= filteredMessages.size()) {
                            Message selected = filteredMessages.get(mailChoice - 1);
                            String rawText = getTextFromMessage(selected); // Ваш метод получения текста

                            System.out.println("\n==================================");
                            System.out.println("ОТ: " + selected.getFrom()[0]);
                            System.out.println("ТЕМА: " + selected.getSubject());
                            System.out.println("СОДЕРЖИМОЕ:\n" + rawText);
                            System.out.println("==================================");

                            System.out.println("1) Расшифровать");
                            System.out.println("2) Ответить");
                            System.out.println("0) Назад");

                            int subchoice = scanner_main.nextInt();
                            scanner_main.nextLine();

                            if (subchoice == 1) {
                                decryptAndPrint(rawText, CRYPTO_KEY, scanner_main);
                            } else if (subchoice == 2) {
                                System.out.print("Введите текст ответа: ");
                                String replyText = scanner_main.nextLine();
                                sendReply(selected.getFrom()[0].toString(), selected.getSubject(), replyText, session, user, password, CRYPTO_KEY);
                            }
                        }
                    }
                }
            else if (choice_main == 3) {
                    scanner_main.nextLine();
                    System.out.print("Введите Email получателя: ");
                    String recipient = scanner_main.nextLine();

                    System.out.print("Введите тему письма: ");
                    String subject = scanner_main.nextLine();

                    System.out.print("Введите сообщение:");
                    String messagetext = scanner_main.nextLine();

                    sendReply(recipient, subject, messagetext, session, user, password, CRYPTO_KEY);
                    System.out.println("\nНажмите Enter для возврата в меню...");
                    scanner_main.nextLine();
            }
            clearScreen();
            }    
            inbox.close(false);
            store.close();
    } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private static void decryptAndPrint(String rawText, String key, Scanner scanner) {
    try {
        byte[] encryptedBytes = Base64.getDecoder().decode(rawText.trim());
        SecretKeySpec skey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skey);
        
        byte[] decrypted = cipher.doFinal(encryptedBytes);
        System.out.println("\n[!] РАСШИФРОВАНО:\n" + new String(decrypted, "UTF-8"));
    } catch (Exception e) {
        System.out.println("\n[!] Ошибка: Ключ не подошел. Ввести вручную? (y/n)");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.print("Введите 16-значный ключ: ");
            String altKey = scanner.nextLine();
        }
    }
    System.out.println("\nНажмите Enter...");
    scanner.nextLine();
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
            reply.setSubject("[ENCRYPTED]" + subject);
            reply.setText(secureBody);

            Properties smtpProps = new Properties();
            smtpProps.put("mail.smtp.host", "smtp.mail.ru");
            smtpProps.put("mail.smtp.port", "587");
            smtpProps.put("mail.smtp.ssl.enable", "false");
            smtpProps.put("mail.smtp.starttls.enable", "true");
            smtpProps.put("mail.smtp.auth", "true");

            smtpProps.put("mail.smtp.connectiontimeout", "5000"); 
            smtpProps.put("mail.smtp.timeout", "5000");

            Session smtpSession = Session.getInstance(smtpProps, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);}
            });
            Message message = new MimeMessage(smtpSession);
            message.setFrom(new InternetAddress(user));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));

            String finalSubject = subject.contains("[ENCRYPTED]") ? subject : "[ENCRYPTED] " + subject;
            message.setSubject(finalSubject);
            message.setText(secureBody);

            System.out.println("[...] Установка соединения с SMTP...");
            Transport.send(message);
        
            System.out.println("\n[OK] Сообщение успешно отправлено!");
    } catch (Exception e) {
        System.out.println("\n[!] Ошибка при отправке: " + e.getMessage());
    }}
}