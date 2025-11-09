package org.example;

import java.util.Scanner;

public class AppAudioSplitService {

    public  static void main(String[] args) {
        try {
            System.out.println("Starting audio...");
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            //
            AudioService audioService = new AudioService();
            audioService.init();
            audioService.recordSplitStart();
            //
            System.out.println("Stopping audio...");
            line = scanner.nextLine();
            audioService.recordSplitStop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
