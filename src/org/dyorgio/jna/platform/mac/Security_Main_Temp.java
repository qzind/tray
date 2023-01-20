package org.dyorgio.jna.platform.mac;

import com.sun.jna.Memory;
import org.apache.commons.ssl.Base64;

import static com.sun.jna.platform.mac.CoreFoundation.*;

public class Security_Main_Temp {
    public static void main(String ... args) {
        CFAllocatorRef alloc = INSTANCE.CFAllocatorGetDefault();
        CFMutableDictionaryRef dict = INSTANCE.CFDictionaryCreateMutable(alloc, new CFIndex(2), null, null);

        // Keys
        CFStringRef kSecClass = CFStringRef.createCFString("kSecClass");
        CFStringRef kSecAttrLabel = CFStringRef.createCFString("kSecAttrLabel");
        CFStringRef kSecValueRef = CFStringRef.createCFString("kSecValueRef");

        // Values
        CFStringRef kSecClassCertificate = CFStringRef.createCFString("kSecClassCertificate");
        CFStringRef cfLabel = CFStringRef.createCFString("My Certificate");
        CFDataRef cfValueAsData = getCFDataCert(DER_ENCODED_CERT);
        CFTypeRef cfValueAsCert = Security.INSTANCE.SecCertificateCreateWithData(null, cfValueAsData);

        CFStringRef summary = Security.INSTANCE.SecCertificateCopySubjectSummary(cfValueAsCert);
        System.out.println("SecCertificateCopySubjectSummary: " + summary.stringValue());
        summary.release();

        // Write to dictionary
        dict.setValue(kSecClass, kSecClassCertificate);
        dict.setValue(kSecAttrLabel, cfLabel);
        dict.setValue(kSecValueRef, cfValueAsCert);

        // Call SecAddItem
        int retVal = Security.INSTANCE.SecItemAdd(dict.getPointer(), null);
        Security.Status code = Security.Status.parse(retVal);

        // Show output
        switch(code) {
            case SUCCESS:
                System.out.println(code);
                break;
            default:
                System.err.println(code);
        }

        // Dispose
        kSecClass.release();
        kSecAttrLabel.release();
        kSecValueRef.release();

        kSecClassCertificate.release();
        cfLabel.release();
        cfValueAsData.release();

        dict.release();
        alloc.release();
    }

    // Attempt to convert data bytes to CFDataRef
    private static CFDataRef getCFDataCert(String derEncodedData) {
        String certFixed = DER_ENCODED_CERT.split("-----")[2];
        byte[] certBytes = Base64.decodeBase64(certFixed);
        Memory nativeBytes = new Memory(certBytes.length);
        nativeBytes.write(0, certBytes, 0, certBytes.length);
        return INSTANCE.CFDataCreate(null, nativeBytes, new CFIndex(certBytes.length));
    }


    // SEE ALSO: https://www.andyibanez.com/posts/using-ios-keychain-swift/

    /**
     * let secCert = SecCertificateCreateWithData(nil, certInDer as CFData) // certInDer is Certificate(.der) data
     *         var keychainQueryDictionary = [String : Any]()
     *
     *         if let tempSecCert = secCert {
     *             keychainQueryDictionary = [kSecClass as String : kSecClassCertificate, kSecValueRef as String : tempSecCert, kSecAttrLabel as String: "My Certificate"]
     *         }
     *
     *         let summary = SecCertificateCopySubjectSummary(secCert!)! as String
     *         print("Cert summary: \(summary)")
     *
     *         let status = SecItemAdd(keychainQueryDictionary as CFDictionary, nil)
     *
     *         guard status == errSecSuccess else {
     *             print("Error")
     *             return
     *         }
     *
     *         print("success")
     */

    static String DER_ENCODED_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIELzCCAxegAwIBAgIJALm151zCHDxiMA0GCSqGSIb3DQEBCwUAMIGsMQswCQYD\n" +
            "VQQGEwJVUzELMAkGA1UECAwCTlkxEjAQBgNVBAcMCUNhbmFzdG90YTEbMBkGA1UE\n" +
            "CgwSUVogSW5kdXN0cmllcywgTExDMRswGQYDVQQLDBJRWiBJbmR1c3RyaWVzLCBM\n" +
            "TEMxGTAXBgNVBAMMEHF6aW5kdXN0cmllcy5jb20xJzAlBgkqhkiG9w0BCQEWGHN1\n" +
            "cHBvcnRAcXppbmR1c3RyaWVzLmNvbTAgFw0xNTAzMDEyMzM4MjlaGA8yMTE1MDMw\n" +
            "MjIzMzgyOVowgawxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJOWTESMBAGA1UEBwwJ\n" +
            "Q2FuYXN0b3RhMRswGQYDVQQKDBJRWiBJbmR1c3RyaWVzLCBMTEMxGzAZBgNVBAsM\n" +
            "ElFaIEluZHVzdHJpZXMsIExMQzEZMBcGA1UEAwwQcXppbmR1c3RyaWVzLmNvbTEn\n" +
            "MCUGCSqGSIb3DQEJARYYc3VwcG9ydEBxemluZHVzdHJpZXMuY29tMIIBIjANBgkq\n" +
            "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuWsBa6uk+RM4OKBZTRfIIyqaaFD71FAS\n" +
            "7kojAQ+ySMpYuqLjIVZuCh92o1FGBvyBKUFc6knAHw5749yhLCYLXhzWwiNW2ri1\n" +
            "Jwx/d83Wnaw6qA3lt++u3tmiA8tsFtss0QZW0YBpFsIqhamvB3ypwu0bdUV/oH7g\n" +
            "/s8TFR5LrDfnfxlLFYhTUVWuWzMqEFAGnFG3uw/QMWZnQgkGbx0LMcYzdqFb7/vz\n" +
            "rTSHfjJsisUTWPjo7SBnAtNYCYaGj0YH5RFUdabnvoTdV2XpA5IPYa9Q597g/M0z\n" +
            "icAjuaK614nKXDaAUCbjki8RL3OK9KY920zNFboq/jKG6rKW2t51ZQIDAQABo1Aw\n" +
            "TjAdBgNVHQ4EFgQUA0XGTcD6jqkL2oMPQaVtEgZDqV4wHwYDVR0jBBgwFoAUA0XG\n" +
            "TcD6jqkL2oMPQaVtEgZDqV4wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
            "AQEAijcT5QMVqrWWqpNEe1DidzQfSnKo17ZogHW+BfUbxv65JbDIntnk1XgtLTKB\n" +
            "VAdIWUtGZbXxrp16NEsh96V2hjDIoiAaEpW+Cp6AHhIVgVh7Q9Knq9xZ1t6H8PL5\n" +
            "QiYQKQgJ0HapdCxlPKBfUm/Mj1ppNl9mPFJwgHmzORexbxrzU/M5i2jlies+CXNq\n" +
            "cvmF2l33QNHnLwpFGwYKs08pyHwUPp6+bfci6lRvavztgvnKroWWIRq9ZPlC0yVK\n" +
            "FFemhbCd7ZVbrTo0NcWZM1PTAbvlOikV9eh3i1Vot+3dJ8F27KwUTtnV0B9Jrxum\n" +
            "W9P3C48mvwTxYZJFOu0N9UBLLg==\n" +
            "-----END CERTIFICATE-----";
}
