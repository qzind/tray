package qz.auth;

import qz.common.Constants;

public class RequestState {

    public enum Validity {
        TRUSTED,
        EXPIRED,
        UNSIGNED,
        UNKNOWN
    }

    Certificate certUsed;
    Validity status;

    public RequestState(Certificate cert) {
        certUsed = cert;
        status = Validity.UNKNOWN;
    }

    public Certificate getCertUsed() {
        return certUsed;
    }

    public void setCertUsed(Certificate cert) {
        certUsed = cert;
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
                return Constants.EXPIRED_CERT;
            case UNSIGNED:
                return Constants.UNSIGNED_REQUEST;
            default:
                return Constants.UNTRUSTED_CERT;
        }
    }

}
