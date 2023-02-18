package qz.installer.certificate;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import org.apache.commons.ssl.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dyorgio.jna.platform.mac.Security;

import static org.dyorgio.jna.platform.mac.Security.Status.SUCCESS;
import static qz.common.Constants.ABOUT_TITLE;
import static qz.installer.certificate.CoreFoundation.INSTANCE;

public class MacCertificateNativeAccess {
    private static final Logger log = LogManager.getLogger(MacCertificateNativeAccess.class);

    static CoreFoundation.CFAllocatorRef alloc = INSTANCE.CFAllocatorGetDefault();
    public static void add(String cert, String certStore) {
        CoreFoundation.CFStringRef cfLabel = CoreFoundation.CFStringRef.createCFString(ABOUT_TITLE + " Certificate");

        CoreFoundation.CFMutableDictionaryRef dict = INSTANCE.CFDictionaryCreateMutable(alloc, new CoreFoundation.CFIndex(3), null, null);
        CoreFoundation.CFTypeRef certRef = getSecCertificateRef(cert);
        INSTANCE.CFDictionaryAddValue(dict, Security.kSecClass, Security.kSecClassCertificate);
        INSTANCE.CFDictionaryAddValue(dict, Security.kSecAttrLabel, cfLabel);
        INSTANCE.CFDictionaryAddValue(dict, Security.kSecValueRef, certRef);

        if (!certStore.equals(MacCertificateInstaller.USER_STORE)) {
            Memory keystoreRefData =  new Memory(8);
            Security.INSTANCE.SecKeychainCopyDomainDefault(1, keystoreRefData.share(0));
            CoreFoundation.CFTypeRef keystoreRef = new CoreFoundation.CFTypeRef(keystoreRefData.getPointer(0));
            INSTANCE.CFDictionaryAddValue(dict, Security.kSecUseKeychain, keystoreRef);
        }

        // Call SecAddItem
        // todo: import the const for this
        int trustRet = Security.INSTANCE.SecTrustSettingsSetTrustSettings(certRef, 1, null);
        int addRet = Security.INSTANCE.SecItemAdd(dict.getPointer(), null);
        Security.Status trustRetCode = Security.Status.parse(trustRet);
        Security.Status addReturnCode = Security.Status.parse(addRet);

        INSTANCE.CFRelease(dict);
        INSTANCE.CFRelease(cfLabel);
        INSTANCE.CFRelease(certRef);

        if (trustRetCode == SUCCESS && addReturnCode == SUCCESS) {
            log.info("Cert added to keystore");
        } else {
            log.error("Failed to add cert to keystore. AddItem Code: {} + TrustSetting Code: {}", addReturnCode, trustRetCode);
        }
    }


    // Attempt to convert data bytes to CFDataRef
    private static CoreFoundation.CFTypeRef getSecCertificateRef (String derEncodedData) {
        String certFixed = derEncodedData.split("-----")[2];
        byte[] certBytes = Base64.decodeBase64(certFixed);
        Memory nativeBytes = new Memory(certBytes.length);
        nativeBytes.write(0, certBytes, 0, certBytes.length);
        CoreFoundation.CFDataRef cfData = INSTANCE.CFDataCreate(alloc, nativeBytes, new CoreFoundation.CFIndex(nativeBytes.size()));
        CoreFoundation.CFTypeRef returnValue = Security.INSTANCE.SecCertificateCreateWithData(alloc, cfData);
        INSTANCE.CFRelease(cfData);
        return returnValue;
    }
}

interface CoreFoundation extends com.sun.jna.platform.mac.CoreFoundation {
    CoreFoundation INSTANCE = Native.load("CoreFoundation", qz.installer.certificate.CoreFoundation.class);
    NativeLibrary NATIVE_INSTANCE =  NativeLibrary.getInstance("CoreFoundation");
    CFBooleanRef kCFBooleanTrue = new CFBooleanRef(NATIVE_INSTANCE.getGlobalVariableAddress("kCFBooleanTrue").getPointer(0));
    void CFDictionaryAddValue(com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef theDict, PointerType key, PointerType value);
    void CFDictionaryGetKeysAndValues(com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef theDict, PointerByReference keys, Pointer[] values);
}
