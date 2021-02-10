package qz.printer.status.job;

import qz.printer.status.NativeStatus;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Tres on 12/23/2020
 */
public enum CupsJobStatusMap implements NativeStatus.NativeMap {
    // job-state
    PENDING(NativeJobStatus.SPOOLING), // pending
    PENDING_HELD(NativeJobStatus.PAUSED), // pending-held
    PROCESSING(NativeJobStatus.SPOOLING), // processing
    PROCESSING_STOPPED(NativeJobStatus.PAUSED), // processing-stopped
    CANCELED(NativeJobStatus.CANCELED), // canceled
    ABORTED(NativeJobStatus.ABORTED), // aborted
    COMPLETED(NativeJobStatus.COMPLETE), // completed

    // job-state-reasons.  NativeJobStatus.EMPTY will fallback to the job-state instead
    ABORTED_BY_SYSTEM(NativeJobStatus.ABORTED), // aborted-by-system
    ACCOUNT_AUTHORIZATION_FAILED(NativeJobStatus.EMPTY), // account-authorization-failed
    ACCOUNT_CLOSED(NativeJobStatus.EMPTY), // account-closed
    ACCOUNT_INFO_NEEDED(NativeJobStatus.USER_INTERVENTION), // account-info-needed
    ACCOUNT_LIMIT_REACHED(NativeJobStatus.EMPTY), // account-limit-reached
    COMPRESSION_ERROR(NativeJobStatus.EMPTY), // compression-error
    CONFLICTING_ATTRIBUTES(NativeJobStatus.EMPTY), // conflicting-attributes
    CONNECTED_TO_DESTINATION(NativeJobStatus.EMPTY), // connected-to-destination
    CONNECTING_TO_DESTINATION(NativeJobStatus.EMPTY), // connecting-to-destination
    DESTINATION_URI_FAILED(NativeJobStatus.EMPTY), // destination-uri-failed
    DIGITAL_SIGNATURE_DID_NOT_VERIFY(NativeJobStatus.EMPTY), // digital-signature-did-not-verify
    DIGITAL_SIGNATURE_TYPE_NOT_SUPPORTED(NativeJobStatus.EMPTY), // digital-signature-type-not-supported
    DOCUMENT_ACCESS_ERROR(NativeJobStatus.EMPTY), // document-access-error
    DOCUMENT_FORMAT_ERROR(NativeJobStatus.EMPTY), // document-format-error
    DOCUMENT_PASSWORD_ERROR(NativeJobStatus.EMPTY), // document-password-error
    DOCUMENT_PERMISSION_ERROR(NativeJobStatus.EMPTY), // document-permission-error
    DOCUMENT_SECURITY_ERROR(NativeJobStatus.EMPTY), // document-security-error
    DOCUMENT_UNPRINTABLE_ERROR(NativeJobStatus.EMPTY), // document-unprintable-error
    ERRORS_DETECTED(NativeJobStatus.EMPTY), // errors-detected
    JOB_CANCELED_AT_DEVICE(NativeJobStatus.CANCELED), // job-canceled-at-device
    JOB_CANCELED_BY_OPERATOR(NativeJobStatus.CANCELED), // job-canceled-by-operator
    JOB_CANCELED_BY_USER(NativeJobStatus.CANCELED), // job-canceled-by-user
    JOB_COMPLETED_SUCCESSFULLY(NativeJobStatus.COMPLETE), // job-completed-successfully
    JOB_COMPLETED_WITH_ERRORS(NativeJobStatus.COMPLETE), // job-completed-with-errors
    JOB_COMPLETED_WITH_WARNINGS(NativeJobStatus.COMPLETE), // job-completed-with-warnings
    JOB_DATA_INSUFFICIENT(NativeJobStatus.EMPTY), // job-data-insufficient
    JOB_DELAY_OUTPUT_UNTIL_SPECIFIED(NativeJobStatus.SCHEDULED), // job-delay-output-until-specified
    JOB_DIGITAL_SIGNATURE_WAIT(NativeJobStatus.EMPTY), // job-digital-signature-wait
    JOB_FETCHABLE(NativeJobStatus.EMPTY), // job-fetchable
    JOB_HELD_FOR_REVIEW(NativeJobStatus.EMPTY), // job-held-for-review
    JOB_HOLD_UNTIL_SPECIFIED(NativeJobStatus.SCHEDULED), // job-hold-until-specified
    JOB_INCOMING(NativeJobStatus.EMPTY), // job-incoming
    JOB_INTERPRETING(NativeJobStatus.EMPTY), // job-interpreting
    JOB_OUTGOING(NativeJobStatus.EMPTY), // job-outgoing
    JOB_PASSWORD_WAIT(NativeJobStatus.USER_INTERVENTION), // job-password-wait
    JOB_PRINTED_SUCCESSFULLY(NativeJobStatus.COMPLETE), // job-printed-successfully
    JOB_PRINTED_WITH_ERRORS(NativeJobStatus.COMPLETE), // job-printed-with-errors
    JOB_PRINTED_WITH_WARNINGS(NativeJobStatus.COMPLETE), // job-printed-with-warnings
    JOB_PRINTING(NativeJobStatus.PRINTING), // job-printing
    JOB_QUEUED(NativeJobStatus.SPOOLING), // job-queued
    JOB_QUEUED_FOR_MARKER(NativeJobStatus.SPOOLING), // job-queued-for-marker
    JOB_RELEASE_WAIT(NativeJobStatus.EMPTY), // job-release-wait
    JOB_RESTARTABLE(NativeJobStatus.EMPTY), // job-restartable
    JOB_RESUMING(NativeJobStatus.SPOOLING), // job-resuming
    JOB_SAVED_SUCCESSFULLY(NativeJobStatus.RETAINED), // job-saved-successfully
    JOB_SAVED_WITH_ERRORS(NativeJobStatus.RETAINED), // job-saved-with-errors
    JOB_SAVED_WITH_WARNINGS(NativeJobStatus.RETAINED), // job-saved-with-warnings
    JOB_SAVING(NativeJobStatus.EMPTY), // job-saving
    JOB_SPOOLING(NativeJobStatus.EMPTY), // job-spooling
    JOB_STREAMING(NativeJobStatus.EMPTY), // job-streaming
    JOB_SUSPENDED(NativeJobStatus.PAUSED), // job-suspended
    JOB_SUSPENDED_BY_OPERATOR(NativeJobStatus.PAUSED), // job-suspended-by-operator
    JOB_SUSPENDED_BY_SYSTEM(NativeJobStatus.PAUSED), // job-suspended-by-system
    JOB_SUSPENDED_BY_USER(NativeJobStatus.PAUSED), // job-suspended-by-user
    JOB_SUSPENDING(NativeJobStatus.EMPTY), // job-suspending
    JOB_TRANSFERRING(NativeJobStatus.EMPTY), // job-transferring
    JOB_TRANSFORMING(NativeJobStatus.EMPTY), // job-transforming
    PRINTER_STOPPED(NativeJobStatus.EMPTY), // printer-stopped
    PRINTER_STOPPED_PARTLY(NativeJobStatus.EMPTY), // printer-stopped-partly
    PROCESSING_TO_STOP_POINT(NativeJobStatus.EMPTY), // processing-to-stop-point
    QUEUED_IN_DEVICE(NativeJobStatus.EMPTY), // queued-in-device
    RESOURCES_ARE_NOT_READY(NativeJobStatus.EMPTY), // resources-are-not-ready
    RESOURCES_ARE_NOT_SUPPORTED(NativeJobStatus.EMPTY), // resources-are-not-supported
    SERVICE_OFF_LINE(NativeJobStatus.EMPTY), // service-off-line
    SUBMISSION_INTERRUPTED(NativeJobStatus.EMPTY), // submission-interrupted
    UNSUPPORTED_ATTRIBUTES_OR_VALUES(NativeJobStatus.EMPTY), // unsupported-attributes-or-values
    UNSUPPORTED_COMPRESSION(NativeJobStatus.EMPTY), // unsupported-compression
    UNSUPPORTED_DOCUMENT_FORMAT(NativeJobStatus.EMPTY), // unsupported-document-format
    WAITING_FOR_USER_ACTION(NativeJobStatus.USER_INTERVENTION), // waiting-for-user-action
    WARNINGS_DETECTED(NativeJobStatus.EMPTY), // warnings-detected

    EMPTY(NativeJobStatus.EMPTY);

    private static SortedMap<String,NativeStatus> sortedLookupTable;

    private final NativeStatus parent;

    CupsJobStatusMap(NativeStatus parent) {
        this.parent = parent;
    }

    public static NativeStatus match(String code, String state) {
        // Initialize a sorted map to speed up lookups
        if(sortedLookupTable == null) {
            sortedLookupTable = new TreeMap<>();
            for(CupsJobStatusMap value : values()) {
                sortedLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
            }
        }
        // If code maps to empty, use state instead
        NativeStatus status = sortedLookupTable.getOrDefault(code, NativeJobStatus.EMPTY);
        if (status == NativeJobStatus.EMPTY) status = sortedLookupTable.getOrDefault(state, NativeJobStatus.EMPTY);
        return status;
    }

    @Override
    public NativeStatus getParent() {
        return parent;
    }

    @Override
    public Object getRawCode() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-");
    }
}
