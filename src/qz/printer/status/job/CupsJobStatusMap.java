package qz.printer.status.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.status.NativeStatus;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import static qz.printer.status.job.CupsJobStatusMap.CupsJobStatusType.*;

/**
 * Created by Tres on 12/23/2020
 */
public enum CupsJobStatusMap implements NativeStatus.NativeMap {
    // job-state
    PENDING(STATE, NativeJobStatus.SPOOLING), // pending
    PENDING_HELD(STATE, NativeJobStatus.PAUSED), // pending-held
    PROCESSING(STATE, NativeJobStatus.SPOOLING), // processing
    PROCESSING_STOPPED(STATE, NativeJobStatus.PAUSED), // processing-stopped
    CANCELED(STATE, NativeJobStatus.CANCELED), // canceled
    ABORTED(STATE, NativeJobStatus.ABORTED), // aborted
    COMPLETED(STATE, NativeJobStatus.COMPLETE), // completed

    // job-state-reasons.  NativeJobStatus.UNMAPPED will fallback to the job-state instead
    ABORTED_BY_SYSTEM(REASON, NativeJobStatus.ABORTED), // aborted-by-system
    ACCOUNT_AUTHORIZATION_FAILED(REASON, NativeJobStatus.UNMAPPED), // account-authorization-failed
    ACCOUNT_CLOSED(REASON, NativeJobStatus.UNMAPPED), // account-closed
    ACCOUNT_INFO_NEEDED(REASON, NativeJobStatus.USER_INTERVENTION), // account-info-needed
    ACCOUNT_LIMIT_REACHED(REASON, NativeJobStatus.UNMAPPED), // account-limit-reached
    COMPRESSION_ERROR(REASON, NativeJobStatus.UNMAPPED), // compression-error
    CONFLICTING_ATTRIBUTES(REASON, NativeJobStatus.UNMAPPED), // conflicting-attributes
    CONNECTED_TO_DESTINATION(REASON, NativeJobStatus.UNMAPPED), // connected-to-destination
    CONNECTING_TO_DESTINATION(REASON, NativeJobStatus.UNMAPPED), // connecting-to-destination
    DESTINATION_URI_FAILED(REASON, NativeJobStatus.UNMAPPED), // destination-uri-failed
    DIGITAL_SIGNATURE_DID_NOT_VERIFY(REASON, NativeJobStatus.UNMAPPED), // digital-signature-did-not-verify
    DIGITAL_SIGNATURE_TYPE_NOT_SUPPORTED(REASON, NativeJobStatus.UNMAPPED), // digital-signature-type-not-supported
    DOCUMENT_ACCESS_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-access-error
    DOCUMENT_FORMAT_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-format-error
    DOCUMENT_PASSWORD_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-password-error
    DOCUMENT_PERMISSION_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-permission-error
    DOCUMENT_SECURITY_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-security-error
    DOCUMENT_UNPRINTABLE_ERROR(REASON, NativeJobStatus.UNMAPPED), // document-unprintable-error
    ERRORS_DETECTED(REASON, NativeJobStatus.UNMAPPED), // errors-detected
    JOB_CANCELED_AT_DEVICE(REASON, NativeJobStatus.CANCELED), // job-canceled-at-device
    JOB_CANCELED_BY_OPERATOR(REASON, NativeJobStatus.CANCELED), // job-canceled-by-operator
    JOB_CANCELED_BY_USER(REASON, NativeJobStatus.CANCELED), // job-canceled-by-user
    JOB_COMPLETED_SUCCESSFULLY(REASON, NativeJobStatus.COMPLETE), // job-completed-successfully
    JOB_COMPLETED_WITH_ERRORS(REASON, NativeJobStatus.COMPLETE), // job-completed-with-errors
    JOB_COMPLETED_WITH_WARNINGS(REASON, NativeJobStatus.COMPLETE), // job-completed-with-warnings
    JOB_DATA_INSUFFICIENT(REASON, NativeJobStatus.UNMAPPED), // job-data-insufficient
    JOB_DELAY_OUTPUT_UNTIL_SPECIFIED(REASON, NativeJobStatus.SCHEDULED), // job-delay-output-until-specified
    JOB_DIGITAL_SIGNATURE_WAIT(REASON, NativeJobStatus.UNMAPPED), // job-digital-signature-wait
    JOB_FETCHABLE(REASON, NativeJobStatus.UNMAPPED), // job-fetchable
    JOB_HELD_FOR_REVIEW(REASON, NativeJobStatus.SPOOLING), // job-held-for-review
    JOB_HOLD_UNTIL_SPECIFIED(REASON, NativeJobStatus.PAUSED), // job-hold-until-specified
    JOB_INCOMING(REASON, NativeJobStatus.UNMAPPED), // job-incoming
    JOB_INTERPRETING(REASON, NativeJobStatus.UNMAPPED), // job-interpreting
    JOB_OUTGOING(REASON, NativeJobStatus.UNMAPPED), // job-outgoing
    JOB_PASSWORD_WAIT(REASON, NativeJobStatus.USER_INTERVENTION), // job-password-wait
    JOB_PRINTED_SUCCESSFULLY(REASON, NativeJobStatus.COMPLETE), // job-printed-successfully
    JOB_PRINTED_WITH_ERRORS(REASON, NativeJobStatus.COMPLETE), // job-printed-with-errors
    JOB_PRINTED_WITH_WARNINGS(REASON, NativeJobStatus.COMPLETE), // job-printed-with-warnings
    JOB_PRINTING(REASON, NativeJobStatus.PRINTING), // job-printing
    JOB_QUEUED(REASON, NativeJobStatus.SPOOLING), // job-queued
    JOB_QUEUED_FOR_MARKER(REASON, NativeJobStatus.SPOOLING), // job-queued-for-marker
    JOB_RELEASE_WAIT(REASON, NativeJobStatus.UNMAPPED), // job-release-wait
    JOB_RESTARTABLE(REASON, NativeJobStatus.UNMAPPED), // job-restartable
    JOB_RESUMING(REASON, NativeJobStatus.SPOOLING), // job-resuming
    JOB_SAVED_SUCCESSFULLY(REASON, NativeJobStatus.RETAINED), // job-saved-successfully
    JOB_SAVED_WITH_ERRORS(REASON, NativeJobStatus.RETAINED), // job-saved-with-errors
    JOB_SAVED_WITH_WARNINGS(REASON, NativeJobStatus.RETAINED), // job-saved-with-warnings
    JOB_SAVING(REASON, NativeJobStatus.UNMAPPED), // job-saving
    JOB_SPOOLING(REASON, NativeJobStatus.UNMAPPED), // job-spooling
    JOB_STREAMING(REASON, NativeJobStatus.UNMAPPED), // job-streaming
    JOB_SUSPENDED(REASON, NativeJobStatus.PAUSED), // job-suspended
    JOB_SUSPENDED_BY_OPERATOR(REASON, NativeJobStatus.PAUSED), // job-suspended-by-operator
    JOB_SUSPENDED_BY_SYSTEM(REASON, NativeJobStatus.PAUSED), // job-suspended-by-system
    JOB_SUSPENDED_BY_USER(REASON, NativeJobStatus.PAUSED), // job-suspended-by-user
    JOB_SUSPENDING(REASON, NativeJobStatus.UNMAPPED), // job-suspending
    JOB_TRANSFERRING(REASON, NativeJobStatus.UNMAPPED), // job-transferring
    JOB_TRANSFORMING(REASON, NativeJobStatus.UNMAPPED), // job-transforming
    PRINTER_STOPPED(REASON, NativeJobStatus.PAUSED), // printer-stopped
    PRINTER_STOPPED_PARTLY(REASON, NativeJobStatus.UNMAPPED), // printer-stopped-partly
    PROCESSING_TO_STOP_POINT(REASON, NativeJobStatus.UNMAPPED), // processing-to-stop-point
    QUEUED_IN_DEVICE(REASON, NativeJobStatus.UNMAPPED), // queued-in-device
    RESOURCES_ARE_NOT_READY(REASON, NativeJobStatus.UNMAPPED), // resources-are-not-ready
    RESOURCES_ARE_NOT_SUPPORTED(REASON, NativeJobStatus.UNMAPPED), // resources-are-not-supported
    SERVICE_OFF_LINE(REASON, NativeJobStatus.UNMAPPED), // service-off-line
    SUBMISSION_INTERRUPTED(REASON, NativeJobStatus.UNMAPPED), // submission-interrupted
    UNSUPPORTED_ATTRIBUTES_OR_VALUES(REASON, NativeJobStatus.UNMAPPED), // unsupported-attributes-or-values
    UNSUPPORTED_COMPRESSION(REASON, NativeJobStatus.UNMAPPED), // unsupported-compression
    UNSUPPORTED_DOCUMENT_FORMAT(REASON, NativeJobStatus.UNMAPPED), // unsupported-document-format
    WAITING_FOR_USER_ACTION(REASON, NativeJobStatus.USER_INTERVENTION), // waiting-for-user-action
    WARNINGS_DETECTED(REASON, NativeJobStatus.UNKNOWN); // warnings-detected

    private static final Logger log = LogManager.getLogger(CupsJobStatusMap.class);
    private static SortedMap<String,NativeJobStatus> sortedReasonLookupTable;
    private static SortedMap<String,NativeJobStatus> sortedStateLookupTable;

    private final NativeJobStatus parent;
    private final CupsJobStatusType type;

    enum CupsJobStatusType {
        STATE,
        REASON;
    }

    CupsJobStatusMap(CupsJobStatusType type, NativeJobStatus parent) {
        this.type = type;
        this.parent = parent;
    }

    public static NativeJobStatus matchReason(String code) {
        // Initialize a sorted map to speed up lookups
        if(sortedReasonLookupTable == null) {
            sortedReasonLookupTable = new TreeMap<>();
            for(CupsJobStatusMap value : values()) {
                if(value.type == REASON) {
                    sortedReasonLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
                }
            }
        }

        NativeJobStatus status = sortedReasonLookupTable.get(code);
        if(status == null && !code.equalsIgnoreCase("none")) {
            // Don't warn for "none"
            log.warn("Printer job state-reason \"{}\" was not found", code);
        }
        return status;
    }

    public static NativeJobStatus matchState(String state) {
        // Initialize a sorted map to speed up lookups
        if(sortedStateLookupTable == null) {
            sortedStateLookupTable = new TreeMap<>();
            for(CupsJobStatusMap value : values()) {
                if(value.type == STATE) {
                    sortedStateLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
                }
            }
        }
        return sortedStateLookupTable.getOrDefault(state, NativeJobStatus.UNKNOWN);
    }

    @Override
    public NativeJobStatus getParent() {
        return parent;
    }

    @Override
    public Object getRawCode() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-");
    }
}
