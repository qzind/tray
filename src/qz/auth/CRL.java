package qz.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qz.utils.ConnectionUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Wrapper class for the Certificate Revocation List
 * Created by Steven on 2/4/2015. Package: qz.auth Project: qz-print
 */
public class CRL {

    private static final Logger log = LogManager.getLogger(CRL.class);

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
                    log.info("Loading CRL from {}", CRL_URL);

                    try(BufferedReader br = new BufferedReader(new InputStreamReader(ConnectionUtilities.getInputStream(CRL_URL)))) {
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
                        log.warn("Unable to access CRL from {}, {}", CRL_URL, e.toString());
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
}
