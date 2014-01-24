package com.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

   /**
     * przedstawia watek obslugi klienta ID
    */
class ClientHandleThread extends Thread { 
    public String username = "";
   
    private SocketServer server = null;
    private Socket socket = null;
    private int ID = -1; //port polaczonego klient
    private ObjectInputStream streamIn  =  null;
    private ObjectOutputStream streamOut = null;
    private final ServerFrame ui;
    private boolean stopThread = false;

    public ClientHandleThread(SocketServer _server, Socket _socket){  
    	super();
        server = _server;
        socket = _socket;
        ID     = socket.getPort();
        ui = _server.ui;
    }
    
    /**
     * metoda wysylajaca wiadomosc
    */
    public void send(Message msg) {
        try {
            streamOut.writeObject(msg);
            streamOut.flush();
        } 
        catch (IOException ex) {
            System.err.println("Exception [SocketClient : send(...)]");
        }
    }
    
    /**
     * funkcja zwracajaca ID klienta
    */
    public int getID(){  
	    return ID;
    }
   
    /**
     * metoda obslugi odbierania i przetwarzania wiadomosci od klienta ID
     */
    @Override
	public void run() {  
    	ui.msgTextArea.append("\nServer Thread " + ID + " running.");
        while(!stopThread) {  
    	    try {  
                Message msg = (Message) streamIn.readObject();//odebranie info 
    	    	server.handle(ID, msg); //przetwarza wiadomosc odebrana od klienta ID
            }
            catch(IOException | ClassNotFoundException ioe){  
            	System.err.println(ID + " ERROR reading: " + ioe.getMessage());
                server.remove(ID);
                stopThread = true;
            }
        }
    }
    
    /**
     * metoda uruchomienia strumieni wej i wyj
     * @throws IOException 
     */
    public void open() throws IOException {  
        streamOut = new ObjectOutputStream(socket.getOutputStream());
        streamOut.flush();
        streamIn = new ObjectInputStream(socket.getInputStream());
    }
    
    /**
     * metoda zamkniecia strumieni wej i wyj
     * @throws IOException 
     */
    public void close() throws IOException {  
    	if (socket != null)    socket.close();
        if (streamIn != null)  streamIn.close();
        if (streamOut != null) streamOut.close();
    }
    
    public void stopThread() {
        stopThread = true;
    }
}
