package org.example;

import java.io.IOException;
import java.util.Scanner;

public class AppAudioService {

    public  static void main(String[] args) {
        try {
            System.out.println("Starting audio...");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            //
            AudioService audioService = new AudioService(() ->  System.out.println("file ready"));
            audioService.startLine();
            //
            System.out.println("Stopping audio...");
            line = scanner.nextLine();
            audioService.stopLine();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
