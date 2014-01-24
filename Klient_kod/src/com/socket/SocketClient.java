package com.socket;

import com.ui.ChatFrame;
import java.io.*;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JOptionPane;


/**
 * klasa rozszerzająca gniazdo klienta
 * @author Bartek
 */
public class SocketClient implements Runnable {
    
    public int port;
    public String serverAddr;
    public SSLSocket socket;
    public ChatFrame ui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    static public boolean doit = true;
    
    /**
     * konstruktor, pobiera certyfikat, uruchamia gniazdo
     * @param frame 
     */
    public SocketClient(ChatFrame frame) {
        try {
            String pkcs12 = "user.p12";
            String certPass = "polska";
            
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(pkcs12), certPass.toCharArray());
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, certPass.toCharArray());
           
            
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
           
            SSLSocketFactory sf = sc.getSocketFactory();
            
            ui = frame;
            this.serverAddr = ui.serverAddr;
            this.port = ui.port;
            
            socket = (SSLSocket) sf.createSocket("localhost", port);
            socket.setNeedClientAuth(true);
            
            socket.startHandshake();
            

            Out = new ObjectOutputStream(socket.getOutputStream());
            Out.flush();
            In = new ObjectInputStream(socket.getInputStream());
        } 
        catch (Exception ex) {
            JOptionPane.showMessageDialog(ui, "Problem with cert or check server", "Error", JOptionPane.ERROR_MESSAGE);
        } 
    }

    /**
     * funkcja watku
     */
    @Override
    public void run() {
        boolean keepRunning = true;
        while(keepRunning){
            try {
                Message msg = (Message) In.readObject();
                System.out.println("Incoming : " + msg.toString());
                
                if(msg.type.equals("message")){
                    if(msg.recipient.equals(ui.username)){
                        ui.msgTextArea.append("["+msg.sender +" > Me] : " + msg.content + "\n");
                    }
                    else{
                        ui.msgTextArea.append("["+ msg.sender +" > "+ msg.recipient +"] : " + msg.content + "\n");
                    }
                }
                else if(msg.type.equals("login")){
                    if(msg.content.equals("TRUE")){
                        ui.loginButton.setEnabled(false);                      
                        ui.sendButton.setEnabled(true);
                        ui.msgTextArea.append("[SERVER > Me] : Login Successful\n");
                    }
                    else{
                        ui.msgTextArea.append("[SERVER > Me] : Login Failed\n");
                    }
                }
                else if(msg.type.equals("test")){
                    ui.connectButton.setEnabled(false);
                    ui.loginButton.setEnabled(true);
                    
                    ui.hostTextField.setEditable(false); ui.portTextField.setEditable(false);
                }
                else if(msg.type.equals("newuser")){
                    if(!msg.content.equals(ui.username)){
                        boolean exists = false;
                        for(int i = 0; i < ui.model.getSize(); i++){
                            if(ui.model.getElementAt(i).equals(msg.content)){
                                exists = true; break;
                            }
                        }
                        if(!exists){ ui.model.addElement(msg.content); }
                    }
                }
                else if(msg.type.equals("signup")){
                    if(msg.content.equals("TRUE")){
                        ui.loginButton.setEnabled(false);
                        ui.sendButton.setEnabled(true);
                        ui.msgTextArea.append("[SERVER > Me] : Singup Successful\n");
                    }
                    else{
                        ui.msgTextArea.append("[SERVER > Me] : Signup Failed\n");
                    }
                }
                else if(msg.type.equals("signout")){
                    if(msg.content.equals(ui.username)){
                        ui.msgTextArea.append("["+ msg.sender +" > Me] : Bye\n");
                        ui.connectButton.setEnabled(true); ui.sendButton.setEnabled(false); 
                        ui.hostTextField.setEditable(true); ui.portTextField.setEditable(true);
                        
                        for(int i = 1; i < ui.model.size(); i++){
                            ui.model.removeElementAt(i);
                        }
                        
                        ui.clientThread.stop();
                    }
                    else{
                        ui.model.removeElement(msg.content);
                        ui.msgTextArea.append("["+ msg.sender +" > All] : "+ msg.content +" has signed out\n");
                    }
                }
                
                else{
                    ui.msgTextArea.append("[SERVER > Me] : Unknown message type\n");
                }
            }
            catch(Exception ex) {
                keepRunning = false;
                ui.msgTextArea.append("[Application > Me] : Connection Failure\n");
                ui.connectButton.setEnabled(true); ui.hostTextField.setEditable(true); ui.portTextField.setEditable(true);
                ui.sendButton.setEnabled(false); //ui.jButton5.setEnabled(false); ui.jButton5.setEnabled(false);
                
                for(int i = 1; i < ui.model.size(); i++){
                    ui.model.removeElementAt(i);
                }
                
                ui.clientThread.stop();
                
                System.err.println("Exception SocketClient run()");
            }
        }
    }
    
    /**
     * funkcja do wysylania wiadomosci
     * @param msg 
     */
    public void send(Message msg) {
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
        } 
        catch (IOException ex) {
            System.err.println("Exception SocketClient send()");
        }
    }
    
    /**
     * funkcja do zamkniecia watku
     * @param t 
     */
    public void closeThread(Thread t){
        t = null;
    }
}
