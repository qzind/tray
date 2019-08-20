package qz.ui.component;

import qz.auth.Certificate;

public class CertificateDisplay {

    private Certificate cert;
    private boolean local = true;

    public CertificateDisplay(Certificate cert, boolean local) {
        this.cert = cert;
        this.local = local;
    }

    public Certificate getCert() {
        return cert;
    }

    public boolean isLocal() {
        return local;
    }

    @Override
    public String toString() {
        return cert.toString();
    }

    @Override
    public boolean equals(Object obj) {
        Object compareTo = obj;
        if (obj instanceof CertificateDisplay) {
            compareTo = ((CertificateDisplay)obj).getCert();
        }

        //true if the passed object is a matching certificate (either directly or from another CertificateDisplay object)
        return cert.equals(compareTo);
    }
}
