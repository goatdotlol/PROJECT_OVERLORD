package com.speedrun.bot.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DebugLogger {
    private static final String LOG_FILE = "latestlg.txt";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static void log(String message) {
        // Also print to console for standard logging
        System.out.println("[GhostRunner] " + message);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write("[" + dtf.format(LocalDateTime.now()) + "] " + message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void clear() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE))) {
            writer.write("--- Starting Project OVERLORD Session ---\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
