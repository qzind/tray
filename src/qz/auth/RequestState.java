package qz.auth;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
import qz.common.Sluggable;

import java.time.Instant;

public class RequestState {

    public enum Validity implements Sluggable {
        TRUSTED("Valid"),
        EXPIRED("Expired Signature"),
        UNSIGNED("Invalid Signature"),
        EXPIRED_CERT("Expired Certificate"),
        FUTURE_CERT("Future Certificate"),
        INVALID_CERT("Invalid Certificate"),
        UNKNOWN("Invalid");

        private final String description;

        Validity(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static Validity getValidity(Certificate cert) {
            if (cert.isTrusted()) {
                return TRUSTED;
            } else if (cert.getValidToDate().isBefore(Instant.now())) {
                return EXPIRED_CERT;
            } else if (cert.getValidFromDate().isAfter(Instant.now())) {
                return FUTURE_CERT;
            } else if (!cert.isValid()) {
                return INVALID_CERT;
            }
            return Validity.UNKNOWN;
        }

        @Override
        public String slug() {
            return Sluggable.slugOf(this);
        }
    }

    private Certificate certificate;
    final private JSONObject requestData;
    private Validity validity;
    boolean initialConnect;

    public RequestState(Certificate certificate, JSONObject requestData) {
        this.certificate = certificate;
        this.requestData = requestData;
        this.validity = Validity.UNKNOWN;
        this.initialConnect = false;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public JSONObject getRequestData() {
        return requestData;
    }

    public boolean isInitialConnect() {
        return initialConnect;
    }

    public void markNewConnection(Certificate cert) {
        this.certificate = cert;
        this.initialConnect = true;
        this.validity = Validity.getValidity(cert);
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity state) {
        validity = state;
    }

    public boolean hasCertificate() {
        return certificate != null && certificate != Certificate.UNKNOWN;
    }

    public boolean hasSavedCert() {
        return isVerified() && certificate.isSaved();
    }

    public boolean hasBlockedCert() {
        return certificate == null || certificate.isBlocked();
    }

    public String getCertName() {
        return certificate.getCommonName();
    }

    public boolean isVerified() {
        return certificate.isTrusted() && validity == Validity.TRUSTED;
    }

    public boolean isSponsored() {
        return certificate.isSponsored();
    }

    public String getValidityString() {
        switch(validity) {
            case TRUSTED:
                return Constants.TRUSTED_CERT;
            case INVALID_CERT:
            case UNKNOWN:
                return Constants.UNTRUSTED_CERT; // TODO: Consolidate wording
            default:
                return String.format("%s - %s", Constants.NO_TRUST, validity.getDescription());
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("validity", validity.slug());
            json.put("_hint", "Possible 'validity' values: " + Sluggable.sluggedArrayString(Validity.values()));
            json.put("initial-connect", initialConnect);
            json.put("validity-description", validity.getDescription());
            json.put("validity-string", getValidityString());
            json.put("has-blocked-cert", hasBlockedCert());
            json.put("has-saved-cert", hasSavedCert());
        } catch(JSONException ignore) {}

        return json;
    }

}
