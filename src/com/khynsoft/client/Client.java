package com.khynsoft.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static BufferedReader inputStream;
    public static AtomicBoolean isRunning = new AtomicBoolean(false);

    public static void main(String[] args) {
        System.out.println("Finding server...");
        try {
            Socket socket = new Socket("localhost", 60000);
            PrintWriter outputStream = new PrintWriter(socket.getOutputStream(), true);
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning.set(true);

            System.out.println("Server found!");

            Scanner sc = new Scanner(System.in);
            String msg = "";

            AtomicBoolean isRunning = new AtomicBoolean(true);

            Thread outMsgs = new Thread(() -> {
                try {
                    while(isRunning.get()) {
                        System.out.println(inputStream.readLine());
                    }
                } catch (IOException e) {
                    System.err.println("You left from the server or the server is now unreachable. [Press Enter]");
                }
                Thread.currentThread().interrupt();
                isRunning.set(false);
            });
            outMsgs.start();

            while(!msg.equals("/logout") && isRunning.get()) {
                msg = sc.nextLine();
                if(!msg.isEmpty()){
                    outputStream.println(msg);
                }
            }

            System.out.println("Logged out.");

            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Server is unreachable. Info: " + e.getMessage());
        }

    }
}
