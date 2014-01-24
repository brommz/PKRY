package com.socket;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.Enumeration;

/**
 * klasa obslugujaca certyfikat, CRL
 * @author Bartek
 */

public class PKI {
    private X509Certificate cert = null;
    private X509CRL crl = null;
 
    public PKI() throws Exception {
        loadCert("user.p12");
        loadCRL("https://rosa.ca/crl/ca.crl");
    }
  
    /**
     * Laduje zdalne CRL
     * @param url
     * @return 
     */
    private void loadCRL(String url) throws Exception  {
        InputStream inStream = new URL(url).openStream();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        crl = (X509CRL)cf.generateCRL(inStream);
        inStream.close();
    }
    
    /**
     * Laduje lokalny certyfikat
     * @param name
     * @return 
     */
    private void loadCert(String name) throws Exception {
          KeyStore p12 = KeyStore.getInstance("pkcs12");
          p12.load(new FileInputStream(name), "polska".toCharArray());
          Enumeration e = p12.aliases();
          while (e.hasMoreElements()) {
               String alias = (String) e.nextElement();
               cert = (X509Certificate) p12.getCertificate(alias);
          }
    }
    
    /**
     * Sprawdza certyfikat, czy znajduje sie w CRL
     * @return 
     */
    public boolean isRevoked(X509Certificate cert) {
        return crl.isRevoked(cert);
    }
    
    /**
     * Sprawdza, czy certyfikat posiada okreslone warunki jak dobre CA, algorytm szyfrowania, rozmiar klucza,..  
     * @return true jezeli OK
     */
    public boolean verifyCert(X509Certificate cert) {
        if(validKeyUsage(cert) == false) { System.err.println("Not valid key usage"); return false;}
        if(validIssuer(cert) == false) { System.err.println("Not valid issuer name"); return false;}
        if(validSignature(cert) == false) { System.err.println("Not valid sig alg"); return false;}
        if(validKeyStrong(cert) == false) { System.err.println("Not valid key strong"); return false;}
        return true;
    }
    
     /**
     * sprawdza, czy zgodne sa wszystkie keyUsage
     * @param keyUsage
     * @return 
     */
    public boolean validKeyUsage(X509Certificate c) {
        /*
     
        KeyUsage ::= BIT STRING {
     digitalSignature        (0),
     nonRepudiation          (1),
     keyEncipherment         (2),
     dataEncipherment        (3),
     keyAgreement            (4),
     keyCertSign             (5),
     cRLSign                 (6),
     encipherOnly            (7),
     decipherOnly            (8) }
     */
        
        boolean[] keyUsage = c.getKeyUsage();
        
        boolean[] req = new boolean[9];
        req[0] = true;
        req[1] = true;
        req[2] = true;
        req[3] = true;
        req[4] = true;
        req[5] = false;
        req[6] = false;
        req[7] = false;
        req[8] = false;
        
        for(int i = 0; i < req.length; i++)
            if(keyUsage[i] != req[i]) return false;
        return true;
    }
    
    /**
     * sprawdza, czy podpisujace CA sie zgadza
     * @param issuer
     * @return 
     */
    public boolean validIssuer(X509Certificate c) {
        String issuer = c.getIssuerDN().toString();
        String myIssuer = "CN=ROSA CA, O=ROSA";
        return issuer.equals(myIssuer);
    }
    
    /**
     * sprawdza, czy podpis jest odpowiedni
     * @return 
     */
    public boolean validSignature(X509Certificate c) {
        String sigAlg = cert.getSigAlgName().toString();
        String mySignature = "SHA1withRSA";
        return sigAlg.equals(mySignature);
    }
    
    /**
     * sprawdza, czy sila klucza nie jest zbyt mala
     * @return 
     */
    public boolean validKeyStrong(X509Certificate c) {
        return ((RSAKey)c.getPublicKey()).getModulus().bitLength() >= 2048;
    }
    
   /**
    * zwraca common name z certyfikatu
    * @param c
    * @return 
    */
    public String getName() {
        String DN = cert.getSubjectDN().toString();
        return DN.substring(DN.indexOf("CN=")+3, DN.indexOf(", O="));
    }
     
}
