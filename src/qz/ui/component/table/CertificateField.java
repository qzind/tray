package qz.ui.component.table;

import qz.auth.Certificate;
import qz.common.Constants;
import qz.common.Sluggable;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.function.Function;

import static qz.auth.Certificate.*;
import static qz.ui.component.table.FieldStyle.NORMAL;
import static qz.ui.component.table.FieldStyle.WARNING;

public class CertificateField {
    public enum CertificateFieldType implements Sluggable {
        ORGANIZATION("Organization", Certificate::getOrganization),
        COMMON_NAME("Common Name", Certificate::getCommonName),
        TRUSTED("Trusted", Certificate::isTrusted),
        VALID_FROM("Valid From", Certificate::getValidFrom),
        VALID_TO("Valid To", Certificate::getValidTo),
        FINGERPRINT("Fingerprint", Certificate::getFingerprint);

        private final String description;
        private final Function<Certificate, Object> getter;

        CertificateFieldType(String description, Function<Certificate, Object> getter) {
            this.description = description;
            this.getter = getter;
        }

        @Override
        public String toString() {
            return description;
        }

        public static int size() {
            return values().length;
        }

        public boolean isDateField() {
            switch(this) {
                case VALID_TO:
                case VALID_FROM:
                    return true;
            }
            return false;
        }

        public String slug() {
            return Sluggable.slugOf(this);
        }

        public String calculateDescription(Certificate certificate) {
            String warning = getLabelWarning(certificate);
            if(warning != null) {
                return String.format("%s (%s)", description, warning);
            }
            return description;
        }

        public String calculateValue(Certificate certificate) {
            if (this == CertificateFieldType.TRUSTED) {
                if (certificate.isValid()) {
                    if(certificate.isThirdParty()) {
                        return Constants.THIRD_PARTY_CERT;
                    }
                    if(Certificate.isTrustBuiltIn()) {
                        return certificate.isSponsored() ? Constants.SPONSORED_CERT : Constants.TRUSTED_CERT;
                    }
                    // Not a QZ cert, change wording
                    return Constants.STRICT_MODE_CERT;
                } else {
                    return Constants.UNTRUSTED_CERT;
                }
            }

            String value = getter.apply(certificate).toString();

            // Format time zone
            if(isDateField() && !value.equals(NOT_PROVIDED)) {
                try {
                    // Parse the date string as UTC (Z/GMT)
                    ZonedDateTime utcTime = LocalDateTime.from(DATE_PARSE.parse(value)).atZone(ZoneOffset.UTC);
                    // Shift to the new timezone
                    ZonedDateTime zonedTime = Instant.from(utcTime).atZone(TIME_ZONE.toZoneId());
                    // Append a short timezone name e.g. "EST"
                    value = DATE_PARSE.format(zonedTime) + " " + TIME_ZONE.getDisplayName(false, TimeZone.SHORT);
                } catch (DateTimeException ignore) {}
            }

            return value;
        }

        public FieldStyle calculateStyle(Certificate certificate) {
            switch(this) {
                case TRUSTED:
                    if(certificate.isTrusted()) {
                        return FieldStyle.TRUSTED;
                    }
                    break;
                case VALID_FROM:
                    if(certificate.getValidFromDate().isAfter(Instant.now())) {
                        return WARNING;
                    }
                    break;
                case VALID_TO:
                    if(certificate.getValidToDate().isBefore(Instant.now())) {
                        return WARNING;
                    }
                    if(certificate.getValidToDate().isBefore(Instant.now().plus(Constants.EXPIRY_WARN, ChronoUnit.DAYS))) {
                        return WARNING;
                    }
                    break;
                default:
            }
            return NORMAL;
        }

        private String getLabelWarning(Certificate certificate) {
            switch(this) {
                case VALID_FROM:
                    if(certificate.getValidFromDate().isAfter(Instant.now())) {
                        return FUTURE_INCEPTION;
                    }
                    break;
                case VALID_TO:
                    if(certificate.getValidToDate().isBefore(Instant.now())) {
                        return EXPIRED;
                    }
                    if(certificate.getValidToDate().isBefore(Instant.now().plus(Constants.EXPIRY_WARN, ChronoUnit.DAYS))) {
                        return EXPIRES_SOON;
                    }
                    break;
                default:
            }
            return null;
        }
    }

    public static final String FUTURE_INCEPTION = "future inception";
    public static final String EXPIRED = "expired";
    public static final String EXPIRES_SOON = "expires soon";

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final TimeZone ALTERNATE_TIME_ZONE = TimeZone.getDefault();
    private static TimeZone TIME_ZONE = DEFAULT_TIME_ZONE;

    private final CertificateFieldType type;
    private final Certificate cert;
    private final String label;
    private final String value;
    private final FieldStyle style;

    /**
     * Basic container for Certificate fields
     */
    public CertificateField(CertificateFieldType type, Certificate cert) {
        this.type = type;
        this.cert = cert;
        this.label = type.calculateDescription(cert);
        this.value = type.calculateValue(cert);
        this.style = type.calculateStyle(cert);
    }

    public boolean isDateField() {
        return type.isDateField();
    }

    /**
     * Gross, a static setter...
     * Timezone is calculated for both validFrom and validTo when retrieving the field
     * so we do this globally to hit them both at once.  This has the side effect of updating
     * any and all visible tables (e.g. SiteManager + DetailsDialog)
     */
    public static void toggleTimeZone() {
        TIME_ZONE = (TIME_ZONE == DEFAULT_TIME_ZONE? ALTERNATE_TIME_ZONE:DEFAULT_TIME_ZONE);
    }

    public FieldStyle getStyle() {
        return style;
    }

    public CertificateFieldType getType() {
        return type;
    }

    public Certificate getCert() {
        return cert;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

}


