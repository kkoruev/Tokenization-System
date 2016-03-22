/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverPackage;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

/**
 *
 * @author Christopher
 */
public class Server extends javax.swing.JFrame {

    /**
     * Creates new form Server
     */
    public Server() {
        initComponents();
        runServer = Executors.newCachedThreadPool();
        xstream = new XStream(new StaxDriver());
        xstream.alias("User", User.class);
        xstream.alias("Card", Card.class);
        readFromFile();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String xml_user = xstream.toXML(userMap);
                String xml_tokens = xstream.toXML(cardMap);
                writeInFile(xml_user, xml_tokens);
                
            }
        });
    }
    
    private void readFromFile() {
        try {
            ObjectInputStream inputUsers = new ObjectInputStream(new FileInputStream("xml_user.ser"));
            String xml_users = (String) inputUsers.readObject();
            ObjectInputStream inputTokens = new ObjectInputStream(new FileInputStream("xml_tokens.ser"));
            String xml_tokens = (String) inputTokens.readObject();
            if (xml_users == null || xml_tokens == null) {
                userMap = new Hashtable<>();
                cardMap = new Hashtable<>();
            } else {
                userMap = (Hashtable) xstream.fromXML(xml_users);
                cardMap = (Hashtable) xstream.fromXML(xml_tokens);
            }

        } catch (FileNotFoundException ex) {
            userMap = new Hashtable<>();
            cardMap = new Hashtable<>();
        } catch (IOException ex) {
            userMap = new Hashtable<>();
            cardMap = new Hashtable<>();
        } catch (ClassNotFoundException ex) {
            userMap = new Hashtable<>();
            cardMap = new Hashtable<>();
        }
    }

    private void writeInFile(String xml_user, String xml_tokens) {
        try {
            ObjectOutputStream outputUsers = new ObjectOutputStream(new FileOutputStream("xml_user.ser"));
            outputUsers.writeObject(xml_user);
            ObjectOutputStream outputTokens = new ObjectOutputStream(new FileOutputStream("xml_tokens.ser"));
            outputTokens.writeObject(xml_tokens);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void runServer() {
        try {
            server = new ServerSocket(12345, 100);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        int cnt = 0;
        while (true) {
            try {
                runServer.execute(new Users(server.accept()));
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private class Users implements Runnable {

        private Socket connection;
        private ObjectOutputStream output;
        private ObjectInputStream input;
        private Character sign;
        private String userName;
        private String password;
        private Character rightForTokens;
        private Character rightForCreditCardNumber;
        private final Character logInSign = 'L';
        private final Character regSign = 'R';
        private final Character tokenSign = 'T';
        private final Character cardSign = 'C';
        private String cardNumber;
        private Object mutex = new Object();

        public Users(Socket socket) {
            setConnection(socket);
        }

        public void setConnection(Socket connection) {
            if (connection == null) {

            } else {
                this.connection = connection;
                getStreams();
            }

        }

        @Override
        public void run() {
            displayMessage("User" + connection.getInetAddress().getHostName() + " connected\n");
            while (connection.isConnected()) {

                try {
                    sign = (Character) input.readObject();

                    if (sign.equals(regSign)) {

                        receiveUserData();

                    } else if (sign.equals(logInSign)) {

                        proccessWithLoading();

                    } else if (sign.equals(cardSign)) {

                        checkCardNumberAndReturnTokenOrError();

                    } else if (sign.equals(tokenSign)) {
                        findCardNumberAndSendResut();
                    }

                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        private void displayMessage(String message) {
            SwingUtilities.invokeLater(
                    new Runnable() {

                        @Override
                        public void run() {
                            textArea.append(message);
                        }

                    }
            );
        }

        private void findCardNumberAndSendResut() throws IOException, ClassNotFoundException {
            if (rightForCreditCardNumber.equals('N')) {
                output.writeObject("You don't have the rights to get card number");
                String pivot = (String) input.readObject();
                return;
            }
            String token = (String) input.readObject();
            String resultC = returnCardNumber(token);
            if (resultC == null) {
                output.writeObject("Wrong token");
            } else {
                output.writeObject(resultC);
            }
        }

        private String returnCardNumber(String token) {
            Set<String> keys = cardMap.keySet();
            for (String key : keys) {
                Card tempCard = cardMap.get(key);
                if (tempCard.containsT(token)) {
                    return key;
                }
            }

            return null;
        }

        private void checkCardNumberAndReturnTokenOrError() throws IOException, ClassNotFoundException {
            if (rightForTokens.equals('N')) {
                output.writeObject("You don't have the rights to get tokens");
                String pivot = (String) input.readObject();
                return;
            }
            if (!checkIfCardNISValid()) {
                output.writeObject("Invalid card number");
            } else {
                addCardAndCreateToken();
            }
        }

        private void addCardAndCreateToken() throws IOException {
            String token = makeToken(cardNumber);
            token = token.replace(",", "").replace("[", "").replace("]", "");
            token = token.replaceAll("\\s", "");
            if (cardMap.containsKey(cardNumber)) {
                Card tempCard = cardMap.get(cardNumber);
                tempCard.addToken(token);
                cardMap.put(cardNumber, tempCard);
            } else {
                Card tempCard = new Card();
                tempCard.addToken(token);
                cardMap.put(cardNumber, tempCard);
            }
            output.writeObject(token);
        }

        private String makeToken(String cardNumber) {
            char[] cardNumbers = cardNumber.toCharArray();
            int[] tokenNumbers = new int[cardNumbers.length];
            Random rand = new Random();
            for (int i = 0; i < cardNumbers.length - 4; i++) {
                int n;
                if (i == 0) {
                    n = 7;
                    int temp = cardNumbers[i] - '0';
                    if (temp == n) {
                        n = rand.nextInt(3);
                    }
                } else {
                    n = rand.nextInt(10);
                    int temp = cardNumbers[i] - '0';
                    if (temp == n) {
                        if (n < 6) {
                            n = 8;
                        } else {
                            n = 3;
                        }
                    }
                }
                tokenNumbers[i] = n;
            }
            for (int i = (cardNumbers.length - 4) - 1; i < cardNumbers.length; i++) {
                tokenNumbers[i] = cardNumbers[i] - '0';
            }
            if (!checkTokenArr(tokenNumbers)) {
                int temp = cardNumbers[3] - '0';
                if (tokenNumbers[3] < 8 && tokenNumbers[3] > 1) {
                    tokenNumbers[3]++;
                    if (tokenNumbers[3] == temp) {
                        tokenNumbers[3] = tokenNumbers[3] - 2;
                    }
                } else if (tokenNumbers[3] >= 8) {
                    tokenNumbers[3]--;
                    if (tokenNumbers[3] == temp) {
                        tokenNumbers[3]--;
                    }
                } else if (tokenNumbers[3] <= 1) {
                    tokenNumbers[3]++;
                    if (tokenNumbers[3] == temp) {
                        tokenNumbers[3]++;
                    }
                }
            }
            String tempToken = Arrays.toString(tokenNumbers);
            while (checkIfAlreadyExist(tempToken)) {
                tempToken = getNewToken(cardNumbers, tokenNumbers);
            }

            return tempToken;
        }

        private String getNewToken(char[] numbers, int[] tokens) {
            Random rand = new Random();
            while (true) {
                int n = rand.nextInt(16);
                if (n > 1 && n < 15) {
                    int temp = numbers[n] - '0';
                    if (tokens[n] > 5) {
                        tokens[n]--;
                        if (tokens[n] == temp) {
                            tokens[n]--;
                        }
                        if (!checkTokenArr(tokens)) {
                            continue;
                        }
                    } else {
                        tokens[n]++;
                        if (tokens[n] == temp) {
                            tokens[n]++;
                        }
                        if (!checkTokenArr(tokens)) {
                            continue;
                        }
                    }
                    break;
                }
            }
            return Arrays.toString(tokens);
        }

        private boolean checkIfAlreadyExist(String token) {
            Set<String> keys = cardMap.keySet();
            for (String key : keys) {
                Card tempCard = cardMap.get(key);
                if (tempCard.containsT(token)) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkTokenArr(int[] arr) {
            int sum = 0;
            for (int i = 0; i < arr.length; i++) {
                sum = sum + arr[i];
            }
            if (sum % 10 == 0) {
                return false;
            }
            return true;
        }

        private boolean checkIfCardNISValid() throws IOException, ClassNotFoundException {
            cardNumber = (String) input.readObject();
            if (cardNumber == null) {
                return false;
            }
            char[] arr = cardNumber.toCharArray();
            int[] num_arr = new int[arr.length];
            num_arr[0] = arr[0] - '0';

            if (num_arr[0] != 3 && num_arr[0] != 4 && num_arr[0] != 5 && num_arr[0] != 6) {
                return false;
            }

            for (int i = 1; i < num_arr.length; i++) {
                int next_number = arr[i] - '0';
                if ((i + 1) % 2 == 0) {
                    next_number = 2 * next_number;
                    if (next_number > 9) {
                        int second_part = next_number % 10;
                        int first_part = next_number / 10;
                        next_number = second_part + first_part;
                    }
                }
                num_arr[i] = next_number;
            }

            int sum = 0;
            for (int i = 0; i < num_arr.length; i++) {
                sum = sum + num_arr[i];
            }

            if (sum % 10 == 0) {
                return true;
            }
            return false;
        }

        private void proccessWithLoading() throws IOException, ClassNotFoundException {
            readUserNameAndPassowrd();
            if (checkIfUserAndPasswordIsCorrect()) {
                output.writeObject("Login successful");
            } else {
                output.writeObject("Invalid username or password");
            }
        }

        private void receiveUserData() throws IOException, ClassNotFoundException {

            readUserNameAndPassowrd();
            readRights();
            System.out.println(rightForTokens);
            System.out.println(rightForCreditCardNumber);
            if (!writeUserInMap()) {
                output.writeObject("Username already used. Try again");
            } else {
                output.writeObject("Registration successful");
            }

        }

        private void readRights() throws IOException, ClassNotFoundException {
            rightForTokens = (Character) input.readObject();
            rightForCreditCardNumber = (Character) input.readObject();
        }

        private void readUserNameAndPassowrd() throws IOException, ClassNotFoundException {
            userName = (String) input.readObject();
            password = (String) input.readObject();
        }

        private boolean writeUserInMap() {
            if (userMap.containsKey(userName)) {
                return false;
            } else {

                userMap.put(userName, new User(userName, password, rightForTokens, rightForCreditCardNumber));
                return true;
            }
        }

        private boolean checkIfUserAndPasswordIsCorrect() {
            User user = userMap.get(userName);
            if (user == null) {
                return false;
            } else {
                return user.checkPassword(password);
            }
        }

        /*
         private boolean checkIfAlreadyExist(String userName) {
         for (User user : usersList) {
         if (user.getUserName().equals(userName)) {
         return true;
         }
         }
         return false;
         }
         */
        private void getStreams() {
            try {
                output = new ObjectOutputStream(connection.getOutputStream());
                input = new ObjectInputStream(connection.getInputStream());
            } catch (IOException ex) {
                output = null;
                input = null;
            }
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        textArea.setColumns(20);
        textArea.setRows(5);
        jScrollPane1.setViewportView(textArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;

                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Server.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        Server app = new Server();
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                app.setVisible(true);
            }
        });
        app.runServer();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea textArea;
    // End of variables declaration//GEN-END:variables
    private ExecutorService runServer;
    private ServerSocket server;
    // private List<User> usersList;
    private Map<String, User> userMap;
    private Map<String, Card> cardMap;
    private XStream xstream;
}
