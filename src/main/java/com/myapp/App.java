package com.myapp;

import java.util.Scanner;

public class App{
    public static void main(String[] args) throws InterruptedException {
       Scanner scanner = new Scanner(System.in);
       MailTerminal.clearScreen();
       loading("Подключение к серверам Mail.ru", 2);
       MailTerminal.clearScreen();
       MailTerminal.main(new String[0]);
    }

    public static void loading(String message, int seconds) throws InterruptedException {
        String[] animation = {"|", "/", "-", "\\"};
        long startTime = System.currentTimeMillis();
        int i = 0;
        System.out.println(message + " ");

    while (System.currentTimeMillis() - startTime < seconds * 1000) {
        System.out.print("\r" + animation[i % 4]);
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            break;
        }
        i++;
        }
    }
}