package qz.auth;

import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;

public class RequestState {

    public enum Validity {
        TRUSTED("Valid"),
        EXPIRED("Expired Signature"),
        UNSIGNED("Invalid Signature"),
        UNKNOWN("Invalid");

        private String formatted;

        Validity(String formatted) {
            this.formatted = formatted;
        }

        public String getFormatted() {
            return formatted;
        }
    }

    Certificate certUsed;
    JSONObject requestData;

    boolean initialConnect;
    Validity status;

    public RequestState(Certificate cert, JSONObject data) {
        certUsed = cert;
        requestData = data;
        status = Validity.UNKNOWN;
    }

    public Certificate getCertUsed() {
        return certUsed;
    }

    public JSONObject getRequestData() {
        return requestData;
    }

    public boolean isInitialConnect() {
        return initialConnect;
    }

    public void markNewConnection(Certificate cert) {
        certUsed = cert;
        initialConnect = true;
    }

    public Validity getStatus() {
        return status;
    }

    public void setStatus(Validity state) {
        status = state;
    }

    public boolean hasCertificate() {
        return certUsed != null && certUsed != Certificate.UNKNOWN;
    }

    public boolean hasSavedCert() {
        return certUsed.isTrusted() && certUsed.isSaved();
    }

    public boolean hasBlockedCert() {
        return certUsed == null || certUsed.isBlocked();
    }

    public String getCertName() {
        return certUsed.getCommonName();
    }

    public boolean isTrusted() {
        return certUsed.isTrusted() && status == Validity.TRUSTED;
    }

    public String getValidityInfo() {
        switch(status) {
            case TRUSTED:
                return Constants.TRUSTED_CERT;
            case EXPIRED:
                return Constants.EXPIRED_REQUEST;
            case UNSIGNED:
                return Constants.UNSIGNED_REQUEST;
            default:
                return Constants.UNTRUSTED_CERT;
        }
    }

}
