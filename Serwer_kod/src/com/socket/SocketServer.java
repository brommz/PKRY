package com.socket;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.swing.JOptionPane;

/**
 * klasa bedaca glownym watkiem serwera
 * @author Bartek
 */

public class SocketServer implements Runnable {
    
    public List<ClientHandleThread> clients;
    public SSLServerSocket server = null;
    public Thread thread;
    private boolean stopThread = false;
    public int port = 13000;
    public ServerFrame ui;
    private PKI pki;

    
    /**
     * konstruktor, uruchamia gniazdo serwera
     * @param frame 
     * @param Port 
     */
    public SocketServer(ServerFrame frame, int Port) {
        try {
            pki = new PKI();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(ui, "CRL loading problem or cert loading", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        clients = new ArrayList<>();
        ui = frame;

        if(Port != 0) port = Port;
        
	try {
            String pkcs12 = "server.p12";
            String certPass = "polska";
            
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(pkcs12), certPass.toCharArray());
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, certPass.toCharArray());
            
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);

            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
            
           
	    server = (SSLServerSocket)ssf.createServerSocket(port);
            server.setNeedClientAuth(true);

	    ui.msgTextArea.append("Server started. IP : " + InetAddress.getLocalHost() + ", Port : " + server.getLocalPort());
	    start(); 
        } 
        catch (Exception ex) {
            JOptionPane.showMessageDialog(ui, "Server starting problem. Check cert, port", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } 
	
    }

    /**
     * metoda uruchamiajaca watek, ktory czeka na polaczenia od klientow
     */
    public void run() {  
	while(!stopThread) {
            
            try {  
		ui.msgTextArea.append("\nWaiting for a client ..."); 
                
                SSLSocket clientSocket = (SSLSocket)server.accept();
                SSLSession session = clientSocket.getSession();
                X509Certificate cert = (X509Certificate)session.getPeerCertificates()[0];
                ui.msgTextArea.append("/n/n" + pki.verifyCert(cert) + "/n/n");
               
                if(pki.isRevoked(cert) || !pki.verifyCert(cert)) { 
                    ui.msgTextArea.append("\nClient cannot be connected - CRL or not verified\n");
                    clientSocket.close();
                }
                else
                    addThread(clientSocket); 
	    }
	    catch(Exception e) { 
                ui.msgTextArea.append("\nServer accept error: \n");
                //Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, e);
                //ui.RetryStart(0);
	    }
        }
    }
	
    /**
     * metoda startujaca watek
     */
    public void start(){  
    	if (thread == null) {  
            thread = new Thread(this); 
	    thread.start();
	}
    }
    
    /**
     * funkcja konczaca watek
    */
    public void stop() {  
        stopThread = true;
    }
    
    /**
     * funkcja zwracajaca klienta w liscie clients po jego ID
     * @param ID
     * @return nr klienta w liscie
     */
    private int findClient(int ID) {
        int clientCount = clients.size();
    	for(int i = 0; i < clientCount; i++)
            if(clients.get(i).getID() == ID) return i;
	return -1;
    }
	
    /**
     * metoda zajmujaca sie przetwarzaniem kontekstu wiadomosci i robieniem akcji z tego
     * @param ID
     * @param msg 
     */
    public synchronized void handle(int ID, Message msg) {  
	if(msg.content.equals(".bye")) {
            Announce("signout", "SERVER", msg.sender);
            remove(ID); 
        }
        else {
            
            /**
             * logowanie
            */
            if(msg.type.equals("login")) {
                if(findUserThread(msg.sender) == null) {
                    if(true) {
                        clients.get(findClient(ID)).username = msg.sender;
                        clients.get(findClient(ID)).send(new Message("login", "SERVER", "TRUE", msg.sender));
                        Announce("newuser", "SERVER", msg.sender);
                        SendUserList(msg.sender);
                    }
                    else{
                        clients.get(findClient(ID)).send(new Message("login", "SERVER", "FALSE", msg.sender));
                    } 
                }
                else{
                    clients.get(findClient(ID)).send(new Message("login", "SERVER", "FALSE", msg.sender));
                }
            }
            
            /**
             * wiadomosc
             */
            else if(msg.type.equals("message")) {
                findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
                clients.get(findClient(ID)).send(new Message(msg.type, msg.sender, msg.content, msg.recipient));
            }
            
            /**
             * test polaczenia z serwerem
             */
            else if(msg.type.equals("test")){
                clients.get(findClient(ID)).send(new Message("test", "SERVER", "OK", msg.sender));
            }
	}
    }
    
    /**
     * wysyla liste klientow polaczonych
     * @param toWhom 
     */
    public void SendUserList(String toWhom){
        for(int i = 0; i < clients.size(); i++){
            findUserThread(toWhom).send(new Message("newuser", "SERVER", clients.get(i).username, toWhom));
        }
    }
    
    /**
     * funkcja zwraca watek obslugujacy danego klienta
     * @param usr
     * @return
     */
    public ClientHandleThread findUserThread(String usr) {
        int clientCount = clients.size();
        for(int i = 0; i < clientCount; i++)
            if(clients.get(i).username.equals(usr)) return clients.get(i);
        return null;
    }
	
    /**
     * funkcja usuwajaca klienta o ID
     * @param ID 
     */
    public synchronized void remove(int ID) {  
    int pos = findClient(ID);
        if(pos >= 0) {  
            ClientHandleThread toTerminate = clients.get(pos);
            ui.msgTextArea.append("\nRemoving client thread " + ID + " at " + pos);
	    try {  
	      	toTerminate.close(); 
	    }
	    catch(IOException ioe){  
	      	ui.msgTextArea.append("\nError closing thread: " + ioe); 
	    }
	    toTerminate.stopThread(); 
            clients.remove(pos);
	}
    }
    
    /**
     * metoda dodajaca watek obsluguacy klienta do listy takich klientow
     * @param socket 
     */
    private void addThread(SSLSocket socket) {
        //socket.addHandshakeCompletedListener();
        
        ui.msgTextArea.append("\nClient accepted: " + socket);
	clients.add(new ClientHandleThread(this, socket));
        
        try {
            int lastElement = clients.size()-1;
	    clients.get(lastElement).open(); 
	    clients.get(lastElement).start();  
	}
	catch(IOException ioe) {  
            ui.msgTextArea.append("\nError opening thread: " + ioe); 
	} 
    }

    /**
     * metoda wysyla do wszystkich klientow jakas informacje
     * @param type
     * @param sender
     * @param content 
     */
    public void Announce(String type, String sender, String content){
        Message msg = new Message(type, sender, content, "All");
        for(int i = 0; i < clients.size(); i++){
            clients.get(i).send(msg);
        }
    }
}