package com.lups.cluster_executor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AppExecutorService {

    private final String storagePath = "/home/ubuntu/cloud";
    //private final String storagePath = "/home/javadev/cloud";
    private final String sshKnownHostsPath = "/home/ubuntu/.ssh/known_hosts";
    //private final String sshKnownHostsPath = "/home/javadev/.ssh/known_hosts";

    public String codeExecutor(List<MultipartFile> files) {

        String codeKeyID;
        String vmNumberKey = "#number_of_virtual_machines:";
        int numberVMs = 0;
        String resultDirID;
        StringBuilder output = new StringBuilder();
        resultDirID = generateUniqueDirectory();

        File workingDirectory = new File(storagePath + "/" + resultDirID);
        Boolean dirIsCreated = workingDirectory.mkdir();

        for (MultipartFile file : files) {
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line);
                    text.append("\n");
                }
                if (Objects.equals(file.getOriginalFilename(), "Makefile")) {
                    int aux = text.indexOf(vmNumberKey);
                    numberVMs = Character.getNumericValue(text.toString().charAt(aux + vmNumberKey.length()));
                }
                try (FileWriter writer = new FileWriter(storagePath + "/" + resultDirID + "/" + file.getOriginalFilename())) {
                    writer.write(text.toString());
                    System.out.println("Text has been successfully saved to " + storagePath);
                } catch (IOException e) {
                    System.out.println("An error occurred while saving the text to the file.");
                }

            } catch (Exception e) {
                System.out.println("Error receiving code files.");
            }
        }

        output.append(executeKeyScan(numberVMs));

        try {
            output.append(executeMakeCommand(storagePath + "/" + resultDirID));
        } catch (Exception e) {
            output.append("Error executing make command.").append(e.getMessage());
        }

        return output.toString();
    }

    public String generateUniqueDirectory() {

        return UUID.randomUUID().toString();
    }

    private String executeMakeCommand(String directory) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("make", "run");
            processBuilder.directory(new File(directory));
            Process process = processBuilder.start();

            Boolean finished = process.waitFor(10, TimeUnit.SECONDS);

            System.out.println(finished);
            if (!finished) {
                Thread errorChecking = new Thread(() -> {
                    BufferedReader reader = process.errorReader();
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            output.append(line);
                            output.append("\n");
                        }
                    } catch (Exception e) {
                        System.out.println("Error");
                    }
                });
                errorChecking.start();
                process.waitFor(3, TimeUnit.SECONDS);
                errorChecking.interrupt();
                process.destroy();
                output.append("An error has occurred. The process has reached its time limit and is being destroyed.\n");
            } else {
                String line;
                try {
                    BufferedReader reader = process.inputReader();
                    line = reader.readLine();
                    if (line.isEmpty()) {
                        output.append("Command executed successfully!\n");
                    } else {
                        output.append("Command executed successfully!\n");
                        do {
                            output.append(line);
                            output.append("\n");
                        } while ((line = reader.readLine()) != null);
                    }
                } catch (Exception e) {
                    System.out.println("error");
                }
            }
        } catch (Exception e) {
            System.out.println("error..");
        }
        return output.toString();
    }

    private String executeKeyScan(int workers) {
        StringBuilder output = new StringBuilder();
        StringBuilder result = new StringBuilder();
        StringBuilder workerNames = new StringBuilder();

        for (int i = 0; i < (workers - 1); i++) {
            //String host = "manager";
            String host;

            // host = workerNames.append("worker").append(i).toString();
            host = "worker" + i;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("ssh-keyscan", "-H", host);
                Process process = processBuilder.start();
                process.waitFor(3, TimeUnit.SECONDS);
                String line;
                try {
                    BufferedReader reader = process.inputReader();
                    line = reader.readLine();
                    if (line.isEmpty()) {
                        output.append("#Command executed successfully!\n");
                    } else {
                        output.append("#Command executed successfully!\n");
                        do {
                            output.append(line);
                            output.append("\n");
                        } while ((line = reader.readLine()) != null);
                    }
                } catch (Exception e) {
                    System.out.println("error");
                }
                result.append("Hosts scan result was successful: ").append(process.exitValue()).append("\n");

            } catch (Exception e) {
                result.append("Error trying to scan workers.");
                break;
            }
        }

        try (FileWriter writer = new FileWriter(this.sshKnownHostsPath)) {
            writer.write(output.toString());
        } catch (IOException e) {
            System.out.println("An error occurred on the known_hosts write.");
        }

        return result.toString();
    }
}
