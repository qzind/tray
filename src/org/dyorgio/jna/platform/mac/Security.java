/*
 *    Copyright (C) 2015 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dyorgio.jna.platform.mac;

import com.sun.jna.*;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * The JNA library interface to access the Security library under MacOS.
 *
 * @author lreimer
 */
public interface Security extends Library {
    Security INSTANCE = Native.load("Security", Security.class);
    NativeLibrary NATIVE_INSTANCE =  NativeLibrary.getInstance("Security");

    // String constants. These are needed as reference instead of value, especially kSecValueRef
    CFStringRef kSecClass = new CFStringRef(NATIVE_INSTANCE.getGlobalVariableAddress("kSecClass").getPointer(0));
    CFStringRef kSecAttrLabel = new CFStringRef(NATIVE_INSTANCE.getGlobalVariableAddress("kSecAttrLabel").getPointer(0));
    CFStringRef kSecValueRef = new CFStringRef(NATIVE_INSTANCE.getGlobalVariableAddress("kSecValueRef").getPointer(0));
    CFStringRef kSecClassCertificate = new CFStringRef(NATIVE_INSTANCE.getGlobalVariableAddress("kSecClassCertificate").getPointer(0));
    CFStringRef kSecUseKeychain = new CFStringRef(NATIVE_INSTANCE.getGlobalVariableAddress("kSecUseKeychain").getPointer(0));
    enum Status {
        SUCCESS("errSecSuccess", 0, "No error."),
        UNIMPLEMENTED("errSecUnimplemented", -4, "Function or operation not implemented."),
        IO("errSecIO", -36, "I/O error (bummers)"),
        OPWR("errSecOpWr", -49, "File already open with write permission"),
        PARAM("errSecParam", -50, "One or more parameters passed to a function where not valid."),
        WRITE_PERMISSION("wrPermErr", -61, "Write permissions error."),
        ALLOCATE("errSecAllocate", -108, "Failed to allocate memory."),
        USER_CANCELED("errSecUserCanceled", -128, "User canceled the operation."),
        BAD_REQ("errSecBadReq", -909, "Bad parameter or invalid state for operation."),
        INTERNAL_COMPONENT("errSecInternalComponent", -2070,"Internal component failure"),
        NOT_AVAILABLE("errSecNotAvailable", -25291, "No keychain is available. You may need to restart your computer."),
        DUPLICATE_ITEM("errSecDuplicateItem", -25299, "The specified item already exists in the keychain."),
        ITEM_NOT_FOUND("errSecItemNotFound", -25300, "The specified item could not be found in the keychain."),
        INTERACTION_NOT_ALLOWED("errSecInteractionNotAllowed", -25308, "User interaction is not allowed."),
        DECODE ("errSecDecode", -26275, "Unable to decode the provided data."),
        AUTH_FAILED("errSecAuthFailed", -25293, "The user name or passphrase you entered is not correct."),
        UNKNOWN("unknown", -99999, "Code not mapped");

        int code;
        String name;
        String message;
        Status(String name, int code, String message) {
            this.name = name;
            this.code = code;
            this.message = message;
        }

        public static Status parse(int code) {
            for(Status status : Status.values()) {
                if(code == status.code) {
                    return status;
                }
            }
            return UNKNOWN;
        }

        @Override
        public String toString() {
            return String.format("(%d) [%s] %s", this.code, name, this.message);
        }
    }

    int SecKeychainCopyDomainDefault(int domain, Pointer SecKeychainRef);

    /**
     * Returns one or more keychain items that match a search query, or copies attributes of specific keychain items.
     * <br/>
     * OSStatus SecItemCopyMatching ( CFDictionaryRef query, CFTypeRef _Nullable *result );
     *
     * @param query  A dictionary containing an item class specification (Keychain Item Class Keys and Values) and optional attributes for controlling the search. See Keychain Services Constants for a description of currently defined search attributes.
     * @param result On return, a reference to the found items. The exact type of the result is based on the search attributes supplied in the query, as discussed below.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecItemCopyMatching(Pointer query, PointerByReference result);

    /**
     * Adds one or more items to a keychain.
     * <br/>
     * OSStatus SecItemAdd ( CFDictionaryRef attributes, CFTypeRef _Nullable *result );
     *
     * @param attributes A dictionary containing an item class key-value pair (Keychain Item Class Keys and Values) and optional attribute key-value pairs (Attribute Item Keys and Values) specifying the item's attribute values.
     * @param result     On return, a reference to the newly added items. The exact type of the result is based on the values supplied in attributes, as discussed below. Pass NULL if this result is not required.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecItemAdd(Pointer attributes, PointerByReference result);

    /**
     * Modifies items that match a search query.
     * <br/>
     * OSStatus SecItemUpdate ( CFDictionaryRef query, CFDictionaryRef attributesToUpdate );
     *
     * @param query              A dictionary containing an item class specification and optional attributes for controlling the search. Specify the items whose values you wish to change. See Search Keys for a description of currently defined search attributes.
     * @param attributesToUpdate A dictionary containing the attributes whose values should be changed, along with the new values. Only real keychain attributes are permitted in this dictionary (no "meta" attributes are allowed.) See Attribute Item Keys and Values for a description of currently defined value attributes.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecItemUpdate(Pointer query, Pointer attributesToUpdate);

    /**
     * Deletes items that match a search query.
     * <br/>
     * OSStatus SecItemDelete ( CFDictionaryRef query );
     *
     * @param query A dictionary containing an item class specification and optional attributes for controlling the search. See Search Keys for a description of currently defined search attributes.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecItemDelete(Pointer query);

    /**
     * Returns a string explaining the meaning of a security result code.
     * <br/>
     * CFStringRef SecCopyErrorMessageString ( OSStatus status, void *reserved );
     *
     * @param status   A result code of type OSStatus or CSSM_RETURN, returned by a security or CSSM function.
     * @param reserved Reserved for future use. Pass NULL for this parameter.
     * @return A human-readable string describing the result, or NULL if no string is available for the specified result code. You must call the CFRelease function to release this object when you are finished using it.
     */
    Pointer SecCopyErrorMessageString(int status, Pointer reserved);

    /**
     * Opens a keychain.
     *
     * @param pathName    A constant character string representing the POSIX path to the keychain to open.
     * @param keychainRef On return, a pointer to the keychain object. You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code. See Keychain Services Result Codes. The result code errSecNoDefaultKeychain indicates that no default keychain could be found. The result code errSecDuplicateItem indicates that you tried to add a password that already exists in the keychain. The result code errSecDataTooLarge indicates that you tried to add more data than is allowed for a structure of this type. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainOpen(String pathName, PointerByReference keychainRef);

    /**
     * Adds a new generic password to a keychain.
     * <br/>
     * OSStatus SecKeychainAddGenericPassword ( SecKeychainRef keychain, UInt32 serviceNameLength, const char *serviceName, UInt32 accountNameLength, const char *accountName, UInt32 passwordLength, const void *passwordData, SecKeychainItemRef _Nullable *itemRef );
     *
     * @param keychain          A reference to the keychain in which to store a generic password. Pass NULL to specify the default keychain.
     * @param serviceNameLength The length of the serviceName character string.
     * @param serviceName       A UTF-8 encoded character string representing the service name.
     * @param accountNameLength The length of the accountName character string.
     * @param accountName       A UTF-8 encoded character string representing the account name.
     * @param passwordLength    The length of the passwordData buffer.
     * @param passwordData      A pointer to a buffer containing the password data to be stored in the keychain. Before calling this function, allocate enough memory for the buffer to hold the data you want to store.
     * @param itemRef           On return, a pointer to a reference to the new keychain item. Pass NULL if you don’t want to obtain this object. You must allocate the memory for this pointer. You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code. See Keychain Services Result Codes. The result code errSecNoDefaultKeychain indicates that no default keychain could be found. The result code errSecDuplicateItem indicates that you tried to add a password that already exists in the keychain. The result code errSecDataTooLarge indicates that you tried to add more data than is allowed for a structure of this type. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainAddGenericPassword(Pointer keychain, int serviceNameLength, byte[] serviceName, int accountNameLength, byte[] accountName, int passwordLength, byte[] passwordData, PointerByReference itemRef);

    /**
     * Finds the first generic password based on the attributes passed.
     * <br/>
     * OSStatus SecKeychainFindGenericPassword ( CFTypeRef keychainOrArray, UInt32 serviceNameLength, const char *serviceName, UInt32 accountNameLength, const char *accountName, UInt32 *passwordLength, void * _Nullable *passwordData, SecKeychainItemRef _Nullable *itemRef );
     *
     * @param keychainOrArray   A reference to an array of keychains to search, a single keychain, or NULL to search the user’s default keychain search list.
     * @param serviceNameLength The length of the serviceName character string.
     * @param serviceName       A UTF-8 encoded character string representing the service name.
     * @param accountNameLength The length of the accountName character string.
     * @param accountName       A UTF-8 encoded character string representing the account name.
     * @param passwordLength    On return, the length of the buffer pointed to by passwordData.
     * @param passwordData      On return, a pointer to a buffer that holds the password data. Pass NULL if you want to obtain the item object but not the password data. In this case, you must also pass NULL in the passwordLength parameter. You should use the SecKeychainItemFreeContent function to free the memory pointed to by this parameter.
     * @param itemRef           On return, a pointer to the item object of the generic password. You are responsible for releasing your reference to this object. Pass NULL if you don’t want to obtain this object.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainFindGenericPassword(Pointer keychainOrArray, int serviceNameLength, byte[] serviceName, int accountNameLength, byte[] accountName, IntByReference passwordLength, PointerByReference passwordData, PointerByReference itemRef);


    /**
     * Adds a new Internet password to a keychain.
     * <br/>
     * OSStatus SecKeychainAddInternetPassword ( SecKeychainRef keychain, UInt32 serverNameLength, const char *serverName, UInt32 securityDomainLength, const char *securityDomain, UInt32 accountNameLength, const char *accountName, UInt32 pathLength, const char *path, UInt16 port, SecProtocolType protocol, SecAuthenticationType authenticationType, UInt32 passwordLength, const void *passwordData, SecKeychainItemRef _Nullable *itemRef );
     *
     * @param keychain             A reference to the keychain in which to store an Internet password. Pass NULL to specify the user’s default keychain.
     * @param serverNameLength     The length of the serverName character string.
     * @param serverName           A UTF-8 encoded character string representing the server name.
     * @param securityDomainLength The length of the securityDomain character string.
     * @param securityDomain       A UTF-8 encoded character string representing the security domain. This parameter is optional. Pass NULL if the protocol does not require it.
     * @param accountNameLength    The length of the accountName character string.
     * @param accountName          A UTF-8 encoded character string representing the account name.
     * @param pathLength           The length of the path character string.
     * @param path                 A UTF-8 encoded character string representing the path.
     * @param port                 The TCP/IP port number. If no specific port number is associated with this password, pass 0.
     * @param protocol             The protocol associated with this password. See Keychain Protocol Type Constants for a description of possible values.
     * @param authenticationType   The authentication scheme used. See Keychain Authentication Type Constants for a description of possible values. Pass the constant kSecAuthenticationTypeDefault, to specify the default authentication scheme.
     * @param passwordLength       The length of the passwordData buffer.
     * @param passwordData         A pointer to a buffer containing the password data to be stored in the keychain.
     * @param itemRef              On return, a pointer to a reference to the new keychain item. Pass NULL if you don’t want to obtain this object. You must allocate the memory for this pointer. You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code. See Keychain Services Result Codes. The result code errSecNoDefaultKeychain indicates that no default keychain could be found. The result code errSecDuplicateItem indicates that you tried to add a password that already exists in the keychain. The result code errSecDataTooLarge indicates that you tried to add more data than is allowed for a structure of this type. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainAddInternetPassword(Pointer keychain, int serverNameLength, byte[] serverName, int securityDomainLength, byte[] securityDomain, int accountNameLength, byte[] accountName, int pathLength, byte[] path, int port, int protocol, int authenticationType, int passwordLength, byte[] passwordData, PointerByReference itemRef);

    /**
     * Finds the first Internet password based on the attributes passed.
     * <br/>
     * OSStatus SecKeychainFindInternetPassword ( CFTypeRef keychainOrArray, UInt32 serverNameLength, const char *serverName, UInt32 securityDomainLength, const char *securityDomain, UInt32 accountNameLength, const char *accountName, UInt32 pathLength, const char *path, UInt16 port, SecProtocolType protocol, SecAuthenticationType authenticationType, UInt32 *passwordLength, void * _Nullable *passwordData, SecKeychainItemRef _Nullable *itemRef );
     *
     * @param keychainOrArray      A reference to an array of keychains to search, a single keychain or NULL to search the user’s default keychain search list.
     * @param serverNameLength     The length of the serverName character string.
     * @param serverName           A UTF-8 encoded character string representing the server name.
     * @param securityDomainLength The length of the securityDomain character string.
     * @param securityDomain       A UTF-8 encoded character string representing the security domain. This parameter is optional, as not all protocols require it. Pass NULL if it is not required.
     * @param accountNameLength    The length of the accountName character string.
     * @param accountName          A UTF-8 encoded character string representing the account name.
     * @param pathLength           The length of the path character string.
     * @param path                 A UTF-8 encoded character string representing the path.
     * @param port                 The TCP/IP port number. Pass 0 to ignore the port number.
     * @param protocol             The protocol associated with this password. See Keychain Protocol Type Constants for a description of possible values.
     * @param authenticationType   The authentication scheme used. See Keychain Authentication Type Constants for a description of possible values. Pass the constant kSecAuthenticationTypeDefault, to specify the default authentication scheme.
     * @param passwordLength       On return, the length of the buffer pointed to by passwordData.
     * @param passwordData         On return, a pointer to a buffer containing the password data. Pass NULL if you want to obtain the item object but not the password data. In this case, you must also pass NULL in the passwordLength parameter. You should use the SecKeychainItemFreeContent function to free the memory pointed to by this parameter.
     * @param itemRef              On return, a pointer to the item object of the Internet password. You are responsible for releasing your reference to this object. Pass NULL if you don’t want to obtain this object.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainFindInternetPassword(Pointer keychainOrArray, int serverNameLength, byte[] serverName, int securityDomainLength, byte[] securityDomain, int accountNameLength, byte[] accountName, int pathLength, byte[] path, int port, int protocol, int authenticationType, IntByReference passwordLength, PointerByReference passwordData, PointerByReference itemRef);

    /**
     * Deletes a keychain item from the default keychain’s permanent data store.
     * <br/>
     * OSStatus SecKeychainItemDelete ( SecKeychainItemRef itemRef );
     *
     * @param itemRef A keychain item object of the item to delete. You must call the CFRelease function to release this object when you are finished using it.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainItemDelete(Pointer itemRef);


    /**
     * Updates an existing keychain item after changing its attributes and/or data.
     * <br/>
     * OSStatus SecKeychainItemModifyContent ( SecKeychainItemRef itemRef, const SecKeychainAttributeList *attrList, UInt32 length, const void *data );
     *
     * @param itemRef  A reference to the keychain item to modify.
     * @param attrList A pointer to the list of attributes to set and their new values. Pass NULL if you have no need to modify attributes.
     * @param length   The length of the buffer pointed to by the data parameter. Pass 0 if you pass NULL in the data parameter.
     * @param data     A pointer to a buffer containing the data to store. Pass NULL if you do not need to modify the data.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainItemModifyContent(Pointer itemRef, Pointer attrList, int length, byte[] data);

    /**
     * Releases the memory used by the keychain attribute list and the keychain data retrieved in a call to the SecKeychainItemCopyContent function.
     * <br/>
     * OSStatus SecKeychainItemFreeContent ( SecKeychainAttributeList *attrList, void *data );
     *
     * @param attrList A pointer to the attribute list to release. Pass NULL if there is no attribute list to release.
     * @param data     A pointer to the data buffer to release. Pass NULL if there is no data to release.
     * @return A result code. See Keychain Services Result Codes. Call SecCopyErrorMessageString (OS X only) to get a human-readable string explaining the result.
     */
    int SecKeychainItemFreeContent(Pointer attrList, Pointer data);

    /**
     * Returns a DER representation of a certificate given a certificate object.
     *
     * @param certificate The certificate object for which you wish to return the DER (Distinguished Encoding Rules)
     *                    representation of the X.509 certificate.
     * @return The DER representation of the certificate. Call the CFRelease function to release this object when you
     * are finished with it. Returns NULL if the data passed in the certificate parameter is not a valid certificate
     * object.
     */
    CoreFoundation.CFDataRef SecCertificateCopyData(CoreFoundation.CFDataRef certificate);

    CoreFoundation.CFTypeRef SecCertificateCreateWithData(CoreFoundation.CFAllocatorRef allocator, CoreFoundation.CFDataRef data);

    CFStringRef SecCertificateCopySubjectSummary(CoreFoundation.CFTypeRef certificate);
}