/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clietnPackage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JToggleButton;

/**
 *
 * @author Christopher
 */
public class Client extends javax.swing.JFrame {

    /**
     * Creates new form Client
     */
    public Client(String serverIp) {
        initComponents();
        setServerIp(serverIp);
        prepareRegOrLOgButton();
        prepareToggleButtonLogOrReg();
        mainPanel.setVisible(false);
        prepareTokenButton();
        prepareCardNumberButton();
    }

    public void prepareCardNumberButton() {
        getCardNumber = mainPanel.getGetNumberButton();
        getCardNumber.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            sendTokenInfo();
                            getCardNumberOrError();
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            mainPanel.clearTokenField();
                            return;
                        }
                    }
                }
        );
    }

    public void getCardNumberOrError() throws IOException, ClassNotFoundException {
        String errorOrCard = (String) input.readObject();
        mainPanel.setInfoField(errorOrCard);
        mainPanel.clearCardNField();
    }

    public void sendTokenInfo() throws IOException, Exception {
        String token = mainPanel.getTokenFieldText();
        if (token == null) {
            return;
        }else if(token.length() != 16){
            mainPanel.setInfoField("Token must be 16 digits");
            throw new Exception();
        } 
        else {
            output.writeObject('T');
            output.writeObject(token);
        }
    }

    public void prepareTokenButton() {
        getToken = mainPanel.getGetTokenButton();
        getToken.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            sendCardInfo();
                            getTokenOrError();
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            mainPanel.clearCardNField();
                            return;
                        }
                    }
                }
        );
    }

    public void sendCardInfo() throws IOException, Exception {
        String cardNumber = mainPanel.getCardNumber();
        if (cardNumber == null) {
            return;
        } 
        else if(cardNumber.length() != 16){
            mainPanel.setInfoField("Card number must be 16 digits");
            throw new Exception();
        }
        else {
            output.writeObject('C');
            output.writeObject(cardNumber);
        }
    }

    public void getTokenOrError() throws IOException, ClassNotFoundException {
        String errorOrToken = (String) input.readObject();
        mainPanel.setInfoField(errorOrToken);
        mainPanel.clearTokenField();
    }

    public void setServerIp(String serverIp) {
        if (serverIp == null) {

        } else {
            this.serverIp = serverIp;
        }

    }

    public void startCLient() {
        try {
            prepareClient();
        } catch (IOException ex) {
            return;
        }
        loginButton.setEnabled(true);
        logOrReg.setEnabled(true);
    }

    private void sendRegInfo() throws IOException {
        output.writeObject('R');
        output.writeObject(userName);
        output.writeObject(password);
        sendRightsInfo();
    }

    private void sendLogInfo() throws IOException {
        output.writeObject('L');
        output.writeObject(userName);
        output.writeObject(password);
    }

    private void sendRightsInfo() throws IOException {
        String s = logInPanel1.getRightsName();
        if (s.equals("None")) {
            output.writeObject('N');
            output.writeObject('N');
        } else if (s.equals("Token")) {
            output.writeObject('Y');
            output.writeObject('N');
        } else if (s.equals("Card number")) {
            output.writeObject('N');
            output.writeObject('Y');
        } else {
            output.writeObject('Y');
            output.writeObject('Y');
        }
        output.flush();
    }

    private void setLogInInformation() {
        userName = logInPanel1.getUserNameFieldText();
        if (userName == null) {
            userName = "";
        }
        
        password = logInPanel1.getPasswordFieldText();
        if (password == null) {
            password = "";
        }
    }
    
    private boolean validateUserName(String userName){
        return userName.matches("[A-Za-z0-9_-]{5,40}");
    }
    
    private boolean validatePassword(String password){
        return password.matches("[A-Za-z0-9]{5,40}");
    }

    private void prepareRegOrLOgButton() {
        loginButton = logInPanel1.getLoginButton();
        loginButton.setEnabled(false);
        loginButton.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            setLogInInformation();
                            if(!validatePassword(password)){
                                logInPanel1.setMessage("Invalid password");
                                clearFields();
                                return;
                            }
                            if(!validateUserName(userName)){
                                logInPanel1.setMessage("Invalid userName");
                                clearFields();
                                return;
                            }
                            if (loginButton.getText() == "Register") {
                                sendRegInfo();
                                getInfoFromServer();
                                clearFields();
                            }
                            if (loginButton.getText() == "Login") {
                                sendLogInfo();
                                getInfoFromServer();
                                clearFields();

                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
        );

    }

    private void prepareToggleButtonLogOrReg() {
        logOrReg = logInPanel1.getToggleButtonForRegOrLog();
        logOrReg.setEnabled(false);
        logOrReg.setText("Login");
        logOrReg.addActionListener(
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        if (logOrReg.isSelected()) {
                            loginButton.setText("Login");
                            logOrReg.setText("Register");
                            logInPanel1.makeBoxWithRightsInvisible();
                        } else {
                            logOrReg.setText("Login");
                            loginButton.setText("Register");
                            logInPanel1.makeBoxWithRightsVisible();
                        }
                    }
                }
        );
    }

    private void clearFields() {
        logInPanel1.clearFields();
    }

    private void getInfoFromServer() throws IOException, ClassNotFoundException {
        String info = (String) input.readObject();
        if (info.equals("Login successful")) {
            mainPanel.setVisible(true);
            logInPanel1.setVisible(false);
            mainPanel.setUserNameLabel(userName);
        } else {
            logInPanel1.setMessage(info);
        }

    }

    private void prepareClient() throws UnknownHostException, IOException {
        connection = new Socket(InetAddress.getByName(serverIp), 12345);
        input = new ObjectInputStream(connection.getInputStream());
        output = new ObjectOutputStream(connection.getOutputStream());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        logInPanel1 = new clietnPackage.LogInPanel();
        mainPanel = new clietnPackage.mainPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.CardLayout());

        jLayeredPane1.setLayout(new java.awt.CardLayout());
        jLayeredPane1.add(logInPanel1, "card2");
        jLayeredPane1.add(mainPanel, "card3");

        getContentPane().add(jLayeredPane1, "card2");

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
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Client client = new Client("127.0.0.1");
                client.setVisible(true);
                client.startCLient();
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane jLayeredPane1;
    private clietnPackage.LogInPanel logInPanel1;
    private clietnPackage.mainPanel mainPanel;
    // End of variables declaration//GEN-END:variables
    Socket connection;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String serverIp;
    private JButton loginButton;
    private String userName;
    private String password;
    private JToggleButton logOrReg;
    private JButton getToken;
    private JButton getCardNumber;
}
