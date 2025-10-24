package com.example.ivpinggradle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PingUtils {
    public static void runPing(HostData host, boolean continuous) {
        if (host == null) {
            System.err.println("Nenhum host selecionado!");
            return;
        }

        String hostName = host.host();
        String ipAddress = host.ip();

        String tempDir = System.getProperty("java.io.tmpdir");
        File batFile = new File(tempDir, "ping_test.bat");

        StringBuilder sb = new StringBuilder();
        sb.append("@echo off").append(System.lineSeparator());
        sb.append("@cls").append(System.lineSeparator());
        sb.append("@color 17").append(System.lineSeparator());
        sb.append("@title Ping  ").append(hostName).append("  [").append(ipAddress).append("]").append(System.lineSeparator());

        if (continuous) {
            sb.append("@ping -t ").append(ipAddress).append(System.lineSeparator());
        } else {
            sb.append("@ping -n 8 ").append(ipAddress).append(System.lineSeparator());
        }

        sb.append("@pause").append(System.lineSeparator());
        sb.append("@exit").append(System.lineSeparator());

        try (FileWriter writer = new FileWriter(batFile)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException(e);
            //return;
        }

        try {
            new ProcessBuilder("cmd", "/c", "start", batFile.getAbsolutePath())
                    .inheritIO()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
