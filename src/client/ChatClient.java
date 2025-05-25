package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) throws Exception {
        Socket clientSocket = new Socket("localhost", 6789);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter outToServer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        Scanner scanner = new Scanner(System.in);

        System.out.println("Conectado ao servidor.");
        System.out.println("Digite 'help' para ver todos os comandos e exemplos de uso.");

        Thread leitor = new Thread(() -> {
            try {
                String linha;
                while ((linha = inFromServer.readLine()) != null) {
                    System.out.println(linha);
                }
            } catch (IOException e) {
                System.out.println("Desconectado do servidor.");
            }
        });
        leitor.start();

        while (true) {
            String comando = scanner.nextLine();
            outToServer.write(comando);
            outToServer.newLine();
            outToServer.flush();
            if (comando.equalsIgnoreCase("sair")) break;
        }
        clientSocket.close();
    }
}
