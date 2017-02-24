package qz.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qz.common.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Wrapper class for the Certificate Revocation List
 * Created by Steven on 2/4/2015. Package: qz.auth Project: qz-print
 */
public class CRL {

    private static final Logger log = LoggerFactory.getLogger(CRL.class);

    /** The URL to the QZ CRL. Should not be changed except for dev tests */
    public static final String CRL_URL = "https://crl.qz.io";

    private static CRL instance = null;

    private ArrayList<String> revokedHashes = new ArrayList<String>();
    private boolean loaded = false;


    private CRL() {}

    public static CRL getInstance() {
        if (instance == null) {
            instance = new CRL();

            new Thread() {
                @Override
                public void run() {
                    try(BufferedReader br = crlReader()) {
                        String line;
                        while((line = br.readLine()) != null) {
                            //Ignore empty and commented lines
                            if (!line.isEmpty() && line.charAt(0) != '#') {
                                instance.revokedHashes.add(line);
                            }
                        }

                        instance.loaded = true;
                        log.info("Successfully loaded {} CRL entries from {}", instance.revokedHashes.size(), CRL_URL);
                    }
                    catch(IOException e) {
                        log.error("Error loading CRL from {}", CRL_URL, e);
                    }
                }
            }.start();
        }

        return instance;
    }

    public boolean isRevoked(String fingerprint) {
        return revokedHashes.contains(fingerprint);
    }

    public boolean isLoaded() {
        return loaded;
    }

    private static BufferedReader crlReader() throws MalformedURLException, IOException {
        log.info("Loading CRL from {}", CRL_URL);
        URLConnection urlConn = new URL(CRL_URL).openConnection();
        urlConn.setRequestProperty("User-Agent", Constants.HTTP_USER_AGENT);
        return new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
    }
}
