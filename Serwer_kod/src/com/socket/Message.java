package com.socket;

import java.io.Serializable;

/**
 * klasa formatu wiadomosci
 * @author Bartek
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    public String type, sender, content, recipient;
    
    public Message(String type, String sender, String content, String recipient){
        this.type = type; this.sender = sender; this.content = content; this.recipient = recipient;
    }
    
     /**
     * taki format ma miec wiadomosc
     * @return 
     */
    @Override
    public String toString(){
        return "{type='"+type+"', sender='"+sender+"', content='"+content+"', recipient='"+recipient+"'}";
    }
}
