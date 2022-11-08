package qz.printer.status.printer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.status.NativeStatus;
import qz.printer.status.Status;

import java.util.*;

import static  qz.printer.status.printer.CupsPrinterStatusMap.CupsPrinterStatusType.*;

public enum CupsPrinterStatusMap implements NativeStatus.NativeMap {
    // printer-state
    IDLE(STATE, NativePrinterStatus.OK), // idle
    PROCESSING(STATE, NativePrinterStatus.PROCESSING), // processing
    STOPPED(STATE, NativePrinterStatus.PAUSED), // stopped

    // printer-state-reasons.  NativePrinterStatus.UNMAPPED will fallback to the printer-state instead
    // Mapped printer-state-reasons
    OFFLINE_REPORT(REASON, NativePrinterStatus.OFFLINE), // "offline-report"
    OTHER(REASON, NativePrinterStatus.UNMAPPED), // "other"
    MEDIA_NEEDED(REASON, NativePrinterStatus.PAPER_OUT), // "media-needed"
    MEDIA_JAM(REASON, NativePrinterStatus.PAPER_JAM), // "media-jam"
    MOVING_TO_PAUSED(REASON, NativePrinterStatus.OK), // "moving-to-paused"
    PAUSED(REASON, NativePrinterStatus.UNMAPPED), // "paused"
    SHUTDOWN(REASON, NativePrinterStatus.OFFLINE), // "shutdown"
    CONNECTING_TO_DEVICE(REASON, NativePrinterStatus.PROCESSING), // "connecting-to-device"
    TIMED_OUT(REASON, NativePrinterStatus.NOT_AVAILABLE), // "timed-out"
    STOPPING(REASON, NativePrinterStatus.OK), // "stopping"
    STOPPED_PARTLY(REASON, NativePrinterStatus.PAUSED), // "stopped-partly"
    TONER_LOW(REASON, NativePrinterStatus.TONER_LOW), // "toner-low"
    TONER_EMPTY(REASON, NativePrinterStatus.NO_TONER), // "toner-empty"
    SPOOL_AREA_FULL(REASON, NativePrinterStatus.OUT_OF_MEMORY), // "spool-area-full"
    COVER_OPEN(REASON, NativePrinterStatus.DOOR_OPEN), // "cover-open"
    INTERLOCK_OPEN(REASON, NativePrinterStatus.DOOR_OPEN), // "interlock-open"
    DOOR_OPEN(REASON, NativePrinterStatus.DOOR_OPEN), // "door-open"
    INPUT_TRAY_MISSING(REASON, NativePrinterStatus.PAPER_PROBLEM), // "input-tray-missing"
    MEDIA_LOW(REASON, NativePrinterStatus.PAPER_PROBLEM), // "media-low"
    MEDIA_EMPTY(REASON, NativePrinterStatus.PAPER_OUT), // "media-empty"
    OUTPUT_TRAY_MISSING(REASON, NativePrinterStatus.PAPER_PROBLEM), // "output-tray-missing"
    //not a great match
    OUTPUT_AREA_ALMOST_FULL(REASON, NativePrinterStatus.TONER_LOW), // "output-area-almost-full"
    OUTPUT_AREA_FULL(REASON, NativePrinterStatus.OUTPUT_BIN_FULL), // "output-area-full"
    MARKER_SUPPLY_LOW(REASON, NativePrinterStatus.TONER_LOW), // "marker-supply-low"
    MARKER_SUPPLY_EMPTY(REASON, NativePrinterStatus.NO_TONER), // "marker-supply-empty"
    // not a great match
    MARKER_WASTE_ALMOST_FULL(REASON, NativePrinterStatus.TONER_LOW), // "marker-waste-almost-full"
    MARKER_WASTE_FULL(REASON, NativePrinterStatus.NO_TONER), // "marker-waste-full"
    FUSER_OVER_TEMP(REASON, NativePrinterStatus.WARMING_UP), // "fuser-over-temp"
    FUSER_UNDER_TEMP(REASON, NativePrinterStatus.WARMING_UP), // "fuser-under-temp"
    // not a great match
    OPC_NEAR_EOL(REASON, NativePrinterStatus.TONER_LOW), // "opc-near-eol"
    OPC_LIFE_OVER(REASON, NativePrinterStatus.NO_TONER), // "opc-life-over"
    DEVELOPER_LOW(REASON, NativePrinterStatus.TONER_LOW), // "developer-low"
    DEVELOPER_EMPTY(REASON, NativePrinterStatus.NO_TONER), // "developer-empty"
    INTERPRETER_RESOURCE_UNAVAILABLE(REASON, NativePrinterStatus.SERVER_UNKNOWN), // "interpreter-resource-unavailable"

    // CUPS defined states (defined by CUPS, but not part of the IPP specification)
    OFFLINE(REASON, NativePrinterStatus.OFFLINE), // "offline"
    CUPS_INSECURE_FILTER_WARNING(REASON, NativePrinterStatus.SERVER_UNKNOWN), // "cups-insecure-filter-warning"
    CUPS_MISSING_FILTER_WARNING(REASON, NativePrinterStatus.ERROR), // "cups-missing-filter-warning"
    CUPS_WAITING_FOR_JOB_COMPLETED(REASON, NativePrinterStatus.PRINTING), // "cups-waiting-for-job-completed");

    // Deprecated CUPS defined states (outdated or incorrect values known to occur)
    CUPS_INSECURE_FILTER_ERROR(REASON, NativePrinterStatus.SERVER_UNKNOWN), // "cups-insecure-filter-error"
    CUPS_MISSING_FILTER_ERROR(REASON, NativePrinterStatus.ERROR), // "cups-missing-filter-error"
    CUPS_INSECURE_FILTER(REASON, NativePrinterStatus.SERVER_UNKNOWN), // "cups-insecure-filter"
    CUPS_MISSING_FILTER(REASON, NativePrinterStatus.ERROR), // "cups-missing-filter"

    // SNMP statuses with no existing CUPS definition
    SERVICE_NEEDED(REASON, NativePrinterStatus.UNMAPPED), // "service-needed"

    // Unmapped printer-state-reasons
    ALERT_REMOVAL_OF_BINARY_CHANGE_ENTRY(REASON, NativePrinterStatus.UNMAPPED), // alert-removal-of-binary-change-entry
    BANDER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // bander-added
    BANDER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // bander-almost-empty
    BANDER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // bander-almost-full
    BANDER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // bander-at-limit
    BANDER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // bander-closed
    BANDER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // bander-configuration-change
    BANDER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // bander-cover-closed
    BANDER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // bander-cover-open
    BANDER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // bander-empty
    BANDER_FULL(REASON, NativePrinterStatus.UNMAPPED), // bander-full
    BANDER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // bander-interlock-closed
    BANDER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // bander-interlock-open
    BANDER_JAM(REASON, NativePrinterStatus.UNMAPPED), // bander-jam
    BANDER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // bander-life-almost-over
    BANDER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // bander-life-over
    BANDER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // bander-memory-exhausted
    BANDER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // bander-missing
    BANDER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // bander-motor-failure
    BANDER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // bander-near-limit
    BANDER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // bander-offline
    BANDER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // bander-opened
    BANDER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // bander-over-temperature
    BANDER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // bander-power-saver
    BANDER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // bander-recoverable-failure
    BANDER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // bander-recoverable-storage
    BANDER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // bander-removed
    BANDER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // bander-resource-added
    BANDER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // bander-resource-removed
    BANDER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // bander-thermistor-failure
    BANDER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // bander-timing-failure
    BANDER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // bander-turned-off
    BANDER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // bander-turned-on
    BANDER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // bander-under-temperature
    BANDER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // bander-unrecoverable-failure
    BANDER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // bander-unrecoverable-storage-error
    BANDER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // bander-warming-up
    BINDER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // binder-added
    BINDER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // binder-almost-empty
    BINDER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // binder-almost-full
    BINDER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // binder-at-limit
    BINDER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // binder-closed
    BINDER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // binder-configuration-change
    BINDER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // binder-cover-closed
    BINDER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // binder-cover-open
    BINDER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // binder-empty
    BINDER_FULL(REASON, NativePrinterStatus.UNMAPPED), // binder-full
    BINDER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // binder-interlock-closed
    BINDER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // binder-interlock-open
    BINDER_JAM(REASON, NativePrinterStatus.UNMAPPED), // binder-jam
    BINDER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // binder-life-almost-over
    BINDER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // binder-life-over
    BINDER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // binder-memory-exhausted
    BINDER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // binder-missing
    BINDER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // binder-motor-failure
    BINDER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // binder-near-limit
    BINDER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // binder-offline
    BINDER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // binder-opened
    BINDER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // binder-over-temperature
    BINDER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // binder-power-saver
    BINDER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // binder-recoverable-failure
    BINDER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // binder-recoverable-storage
    BINDER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // binder-removed
    BINDER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // binder-resource-added
    BINDER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // binder-resource-removed
    BINDER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // binder-thermistor-failure
    BINDER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // binder-timing-failure
    BINDER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // binder-turned-off
    BINDER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // binder-turned-on
    BINDER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // binder-under-temperature
    BINDER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // binder-unrecoverable-failure
    BINDER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // binder-unrecoverable-storage-error
    BINDER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // binder-warming-up
    CAMERA_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // camera-failure
    CHAMBER_COOLING(REASON, NativePrinterStatus.UNMAPPED), // chamber-cooling
    CHAMBER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // chamber-failure
    CHAMBER_HEATING(REASON, NativePrinterStatus.UNMAPPED), // chamber-heating
    CHAMBER_TEMPERATURE_HIGH(REASON, NativePrinterStatus.UNMAPPED), // chamber-temperature-high
    CHAMBER_TEMPERATURE_LOW(REASON, NativePrinterStatus.UNMAPPED), // chamber-temperature-low
    CLEANER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // cleaner-life-almost-over
    CLEANER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // cleaner-life-over
    CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // configuration-change
    DEACTIVATED(REASON, NativePrinterStatus.UNMAPPED), // deactivated
    DELETED(REASON, NativePrinterStatus.UNMAPPED), // deleted
    DIE_CUTTER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-added
    DIE_CUTTER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-almost-empty
    DIE_CUTTER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-almost-full
    DIE_CUTTER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-at-limit
    DIE_CUTTER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-closed
    DIE_CUTTER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-configuration-change
    DIE_CUTTER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-cover-closed
    DIE_CUTTER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-cover-open
    DIE_CUTTER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-empty
    DIE_CUTTER_FULL(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-full
    DIE_CUTTER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-interlock-closed
    DIE_CUTTER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-interlock-open
    DIE_CUTTER_JAM(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-jam
    DIE_CUTTER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-life-almost-over
    DIE_CUTTER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-life-over
    DIE_CUTTER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-memory-exhausted
    DIE_CUTTER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-missing
    DIE_CUTTER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-motor-failure
    DIE_CUTTER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-near-limit
    DIE_CUTTER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-offline
    DIE_CUTTER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-opened
    DIE_CUTTER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-over-temperature
    DIE_CUTTER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-power-saver
    DIE_CUTTER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-recoverable-failure
    DIE_CUTTER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-recoverable-storage
    DIE_CUTTER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-removed
    DIE_CUTTER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-resource-added
    DIE_CUTTER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-resource-removed
    DIE_CUTTER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-thermistor-failure
    DIE_CUTTER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-timing-failure
    DIE_CUTTER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-turned-off
    DIE_CUTTER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-turned-on
    DIE_CUTTER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-under-temperature
    DIE_CUTTER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-unrecoverable-failure
    DIE_CUTTER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-unrecoverable-storage-error
    DIE_CUTTER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // die-cutter-warming-up
    EXTRUDER_COOLING(REASON, NativePrinterStatus.UNMAPPED), // extruder-cooling
    EXTRUDER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // extruder-failure
    EXTRUDER_HEATING(REASON, NativePrinterStatus.UNMAPPED), // extruder-heating
    EXTRUDER_JAM(REASON, NativePrinterStatus.UNMAPPED), // extruder-jam
    EXTRUDER_TEMPERATURE_HIGH(REASON, NativePrinterStatus.UNMAPPED), // extruder-temperature-high
    EXTRUDER_TEMPERATURE_LOW(REASON, NativePrinterStatus.UNMAPPED), // extruder-temperature-low
    FAN_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // fan-failure
    FAX_MODEM_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // fax-modem-life-almost-over
    FAX_MODEM_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // fax-modem-life-over
    FAX_MODEM_MISSING(REASON, NativePrinterStatus.UNMAPPED), // fax-modem-missing
    FAX_MODEM_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // fax-modem-turned-off
    FAX_MODEM_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // fax-modem-turned-on
    FOLDER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // folder-added
    FOLDER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // folder-almost-empty
    FOLDER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // folder-almost-full
    FOLDER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // folder-at-limit
    FOLDER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // folder-closed
    FOLDER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // folder-configuration-change
    FOLDER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // folder-cover-closed
    FOLDER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // folder-cover-open
    FOLDER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // folder-empty
    FOLDER_FULL(REASON, NativePrinterStatus.UNMAPPED), // folder-full
    FOLDER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // folder-interlock-closed
    FOLDER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // folder-interlock-open
    FOLDER_JAM(REASON, NativePrinterStatus.UNMAPPED), // folder-jam
    FOLDER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // folder-life-almost-over
    FOLDER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // folder-life-over
    FOLDER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // folder-memory-exhausted
    FOLDER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // folder-missing
    FOLDER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // folder-motor-failure
    FOLDER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // folder-near-limit
    FOLDER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // folder-offline
    FOLDER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // folder-opened
    FOLDER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // folder-over-temperature
    FOLDER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // folder-power-saver
    FOLDER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // folder-recoverable-failure
    FOLDER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // folder-recoverable-storage
    FOLDER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // folder-removed
    FOLDER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // folder-resource-added
    FOLDER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // folder-resource-removed
    FOLDER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // folder-thermistor-failure
    FOLDER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // folder-timing-failure
    FOLDER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // folder-turned-off
    FOLDER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // folder-turned-on
    FOLDER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // folder-under-temperature
    FOLDER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // folder-unrecoverable-failure
    FOLDER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // folder-unrecoverable-storage-error
    FOLDER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // folder-warming-up
    HIBERNATE(REASON, NativePrinterStatus.UNMAPPED), // hibernate
    HOLD_NEW_JOBS(REASON, NativePrinterStatus.UNMAPPED), // hold-new-jobs
    IDENTIFY_PRINTER_REQUESTED(REASON, NativePrinterStatus.UNMAPPED), // identify-printer-requested
    IMPRINTER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-added
    IMPRINTER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // imprinter-almost-empty
    IMPRINTER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // imprinter-almost-full
    IMPRINTER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // imprinter-at-limit
    IMPRINTER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-closed
    IMPRINTER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-configuration-change
    IMPRINTER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-cover-closed
    IMPRINTER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // imprinter-cover-open
    IMPRINTER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // imprinter-empty
    IMPRINTER_FULL(REASON, NativePrinterStatus.UNMAPPED), // imprinter-full
    IMPRINTER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-interlock-closed
    IMPRINTER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // imprinter-interlock-open
    IMPRINTER_JAM(REASON, NativePrinterStatus.UNMAPPED), // imprinter-jam
    IMPRINTER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // imprinter-life-almost-over
    IMPRINTER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // imprinter-life-over
    IMPRINTER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-memory-exhausted
    IMPRINTER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // imprinter-missing
    IMPRINTER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-motor-failure
    IMPRINTER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // imprinter-near-limit
    IMPRINTER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-offline
    IMPRINTER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-opened
    IMPRINTER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-over-temperature
    IMPRINTER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // imprinter-power-saver
    IMPRINTER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-recoverable-failure
    IMPRINTER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-recoverable-storage
    IMPRINTER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-removed
    IMPRINTER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-resource-added
    IMPRINTER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // imprinter-resource-removed
    IMPRINTER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-thermistor-failure
    IMPRINTER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-timing-failure
    IMPRINTER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // imprinter-turned-off
    IMPRINTER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // imprinter-turned-on
    IMPRINTER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-under-temperature
    IMPRINTER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // imprinter-unrecoverable-failure
    IMPRINTER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // imprinter-unrecoverable-storage-error
    IMPRINTER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // imprinter-warming-up
    INPUT_CANNOT_FEED_SIZE_SELECTED(REASON, NativePrinterStatus.UNMAPPED), // input-cannot-feed-size-selected
    INPUT_MANUAL_INPUT_REQUEST(REASON, NativePrinterStatus.UNMAPPED), // input-manual-input-request
    INPUT_MEDIA_COLOR_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // input-media-color-change
    INPUT_MEDIA_FORM_PARTS_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // input-media-form-parts-change
    INPUT_MEDIA_SIZE_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // input-media-size-change
    INPUT_MEDIA_TRAY_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // input-media-tray-failure
    INPUT_MEDIA_TRAY_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // input-media-tray-feed-error
    INPUT_MEDIA_TRAY_JAM(REASON, NativePrinterStatus.UNMAPPED), // input-media-tray-jam
    INPUT_MEDIA_TYPE_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // input-media-type-change
    INPUT_MEDIA_WEIGHT_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // input-media-weight-change
    INPUT_PICK_ROLLER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // input-pick-roller-failure
    INPUT_PICK_ROLLER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // input-pick-roller-life-over
    INPUT_PICK_ROLLER_LIFE_WARN(REASON, NativePrinterStatus.UNMAPPED), // input-pick-roller-life-warn
    INPUT_PICK_ROLLER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // input-pick-roller-missing
    INPUT_TRAY_ELEVATION_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // input-tray-elevation-failure
    INPUT_TRAY_POSITION_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // input-tray-position-failure
    INSERTER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // inserter-added
    INSERTER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // inserter-almost-empty
    INSERTER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // inserter-almost-full
    INSERTER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // inserter-at-limit
    INSERTER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // inserter-closed
    INSERTER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // inserter-configuration-change
    INSERTER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // inserter-cover-closed
    INSERTER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // inserter-cover-open
    INSERTER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // inserter-empty
    INSERTER_FULL(REASON, NativePrinterStatus.UNMAPPED), // inserter-full
    INSERTER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // inserter-interlock-closed
    INSERTER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // inserter-interlock-open
    INSERTER_JAM(REASON, NativePrinterStatus.UNMAPPED), // inserter-jam
    INSERTER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // inserter-life-almost-over
    INSERTER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // inserter-life-over
    INSERTER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // inserter-memory-exhausted
    INSERTER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // inserter-missing
    INSERTER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-motor-failure
    INSERTER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // inserter-near-limit
    INSERTER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // inserter-offline
    INSERTER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // inserter-opened
    INSERTER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-over-temperature
    INSERTER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // inserter-power-saver
    INSERTER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-recoverable-failure
    INSERTER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // inserter-recoverable-storage
    INSERTER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // inserter-removed
    INSERTER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // inserter-resource-added
    INSERTER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // inserter-resource-removed
    INSERTER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-thermistor-failure
    INSERTER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-timing-failure
    INSERTER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // inserter-turned-off
    INSERTER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // inserter-turned-on
    INSERTER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-under-temperature
    INSERTER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // inserter-unrecoverable-failure
    INSERTER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // inserter-unrecoverable-storage-error
    INSERTER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // inserter-warming-up
    INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // interlock-closed
    INTERPRETER_CARTRIDGE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // interpreter-cartridge-added
    INTERPRETER_CARTRIDGE_DELETED(REASON, NativePrinterStatus.UNMAPPED), // interpreter-cartridge-deleted
    INTERPRETER_COMPLEX_PAGE_ENCOUNTERED(REASON, NativePrinterStatus.UNMAPPED), // interpreter-complex-page-encountered
    INTERPRETER_MEMORY_DECREASE(REASON, NativePrinterStatus.UNMAPPED), // interpreter-memory-decrease
    INTERPRETER_MEMORY_INCREASE(REASON, NativePrinterStatus.UNMAPPED), // interpreter-memory-increase
    INTERPRETER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // interpreter-resource-added
    INTERPRETER_RESOURCE_DELETED(REASON, NativePrinterStatus.UNMAPPED), // interpreter-resource-deleted
    LAMP_AT_EOL(REASON, NativePrinterStatus.UNMAPPED), // lamp-at-eol
    LAMP_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // lamp-failure
    LAMP_NEAR_EOL(REASON, NativePrinterStatus.UNMAPPED), // lamp-near-eol
    LASER_AT_EOL(REASON, NativePrinterStatus.UNMAPPED), // laser-at-eol
    LASER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // laser-failure
    LASER_NEAR_EOL(REASON, NativePrinterStatus.UNMAPPED), // laser-near-eol
    MAKE_ENVELOPE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-added
    MAKE_ENVELOPE_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-almost-empty
    MAKE_ENVELOPE_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-almost-full
    MAKE_ENVELOPE_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-at-limit
    MAKE_ENVELOPE_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-closed
    MAKE_ENVELOPE_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-configuration-change
    MAKE_ENVELOPE_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-cover-closed
    MAKE_ENVELOPE_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-cover-open
    MAKE_ENVELOPE_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-empty
    MAKE_ENVELOPE_FULL(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-full
    MAKE_ENVELOPE_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-interlock-closed
    MAKE_ENVELOPE_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-interlock-open
    MAKE_ENVELOPE_JAM(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-jam
    MAKE_ENVELOPE_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-life-almost-over
    MAKE_ENVELOPE_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-life-over
    MAKE_ENVELOPE_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-memory-exhausted
    MAKE_ENVELOPE_MISSING(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-missing
    MAKE_ENVELOPE_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-motor-failure
    MAKE_ENVELOPE_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-near-limit
    MAKE_ENVELOPE_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-offline
    MAKE_ENVELOPE_OPENED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-opened
    MAKE_ENVELOPE_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-over-temperature
    MAKE_ENVELOPE_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-power-saver
    MAKE_ENVELOPE_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-recoverable-failure
    MAKE_ENVELOPE_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-recoverable-storage
    MAKE_ENVELOPE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-removed
    MAKE_ENVELOPE_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-resource-added
    MAKE_ENVELOPE_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-resource-removed
    MAKE_ENVELOPE_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-thermistor-failure
    MAKE_ENVELOPE_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-timing-failure
    MAKE_ENVELOPE_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-turned-off
    MAKE_ENVELOPE_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-turned-on
    MAKE_ENVELOPE_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-under-temperature
    MAKE_ENVELOPE_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-unrecoverable-failure
    MAKE_ENVELOPE_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-unrecoverable-storage-error
    MAKE_ENVELOPE_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // make-envelope-warming-up
    MARKER_ADJUSTING_PRINT_QUALITY(REASON, NativePrinterStatus.UNMAPPED), // marker-adjusting-print-quality
    MARKER_CLEANER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-cleaner-missing
    MARKER_DEVELOPER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-developer-almost-empty
    MARKER_DEVELOPER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-developer-empty
    MARKER_DEVELOPER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-developer-missing
    MARKER_FUSER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-fuser-missing
    MARKER_FUSER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // marker-fuser-thermistor-failure
    MARKER_FUSER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // marker-fuser-timing-failure
    MARKER_INK_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-ink-almost-empty
    MARKER_INK_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-ink-empty
    MARKER_INK_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-ink-missing
    MARKER_OPC_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-opc-missing
    MARKER_PRINT_RIBBON_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-print-ribbon-almost-empty
    MARKER_PRINT_RIBBON_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-print-ribbon-empty
    MARKER_PRINT_RIBBON_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-print-ribbon-missing
    MARKER_SUPPLY_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // marker-supply-almost-empty
    MARKER_SUPPLY_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-supply-missing
    MARKER_TONER_CARTRIDGE_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-toner-cartridge-missing
    MARKER_TONER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-toner-missing
    MARKER_WASTE_INK_RECEPTACLE_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-ink-receptacle-almost-full
    MARKER_WASTE_INK_RECEPTACLE_FULL(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-ink-receptacle-full
    MARKER_WASTE_INK_RECEPTACLE_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-ink-receptacle-missing
    MARKER_WASTE_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-missing
    MARKER_WASTE_TONER_RECEPTACLE_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-toner-receptacle-almost-full
    MARKER_WASTE_TONER_RECEPTACLE_FULL(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-toner-receptacle-full
    MARKER_WASTE_TONER_RECEPTACLE_MISSING(REASON, NativePrinterStatus.UNMAPPED), // marker-waste-toner-receptacle-missing
    MATERIAL_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // material-empty
    MATERIAL_LOW(REASON, NativePrinterStatus.UNMAPPED), // material-low
    MATERIAL_NEEDED(REASON, NativePrinterStatus.UNMAPPED), // material-needed
    MEDIA_DRYING(REASON, NativePrinterStatus.UNMAPPED), // media-drying
    MEDIA_PATH_CANNOT_DUPLEX_MEDIA_SELECTED(REASON, NativePrinterStatus.UNMAPPED), // media-path-cannot-duplex-media-selected
    MEDIA_PATH_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // media-path-failure
    MEDIA_PATH_INPUT_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // media-path-input-empty
    MEDIA_PATH_INPUT_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // media-path-input-feed-error
    MEDIA_PATH_INPUT_JAM(REASON, NativePrinterStatus.UNMAPPED), // media-path-input-jam
    MEDIA_PATH_INPUT_REQUEST(REASON, NativePrinterStatus.UNMAPPED), // media-path-input-request
    MEDIA_PATH_JAM(REASON, NativePrinterStatus.UNMAPPED), // media-path-jam
    MEDIA_PATH_MEDIA_TRAY_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // media-path-media-tray-almost-full
    MEDIA_PATH_MEDIA_TRAY_FULL(REASON, NativePrinterStatus.UNMAPPED), // media-path-media-tray-full
    MEDIA_PATH_MEDIA_TRAY_MISSING(REASON, NativePrinterStatus.UNMAPPED), // media-path-media-tray-missing
    MEDIA_PATH_OUTPUT_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // media-path-output-feed-error
    MEDIA_PATH_OUTPUT_FULL(REASON, NativePrinterStatus.UNMAPPED), // media-path-output-full
    MEDIA_PATH_OUTPUT_JAM(REASON, NativePrinterStatus.UNMAPPED), // media-path-output-jam
    MEDIA_PATH_PICK_ROLLER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // media-path-pick-roller-failure
    MEDIA_PATH_PICK_ROLLER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // media-path-pick-roller-life-over
    MEDIA_PATH_PICK_ROLLER_LIFE_WARN(REASON, NativePrinterStatus.UNMAPPED), // media-path-pick-roller-life-warn
    MEDIA_PATH_PICK_ROLLER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // media-path-pick-roller-missing
    MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // motor-failure
    OUTPUT_MAILBOX_SELECT_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // output-mailbox-select-failure
    OUTPUT_MEDIA_TRAY_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // output-media-tray-failure
    OUTPUT_MEDIA_TRAY_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // output-media-tray-feed-error
    OUTPUT_MEDIA_TRAY_JAM(REASON, NativePrinterStatus.UNMAPPED), // output-media-tray-jam
    PERFORATER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // perforater-added
    PERFORATER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // perforater-almost-empty
    PERFORATER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // perforater-almost-full
    PERFORATER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // perforater-at-limit
    PERFORATER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // perforater-closed
    PERFORATER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // perforater-configuration-change
    PERFORATER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // perforater-cover-closed
    PERFORATER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // perforater-cover-open
    PERFORATER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // perforater-empty
    PERFORATER_FULL(REASON, NativePrinterStatus.UNMAPPED), // perforater-full
    PERFORATER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // perforater-interlock-closed
    PERFORATER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // perforater-interlock-open
    PERFORATER_JAM(REASON, NativePrinterStatus.UNMAPPED), // perforater-jam
    PERFORATER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // perforater-life-almost-over
    PERFORATER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // perforater-life-over
    PERFORATER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // perforater-memory-exhausted
    PERFORATER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // perforater-missing
    PERFORATER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-motor-failure
    PERFORATER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // perforater-near-limit
    PERFORATER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // perforater-offline
    PERFORATER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // perforater-opened
    PERFORATER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-over-temperature
    PERFORATER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // perforater-power-saver
    PERFORATER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-recoverable-failure
    PERFORATER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // perforater-recoverable-storage
    PERFORATER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // perforater-removed
    PERFORATER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // perforater-resource-added
    PERFORATER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // perforater-resource-removed
    PERFORATER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-thermistor-failure
    PERFORATER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-timing-failure
    PERFORATER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // perforater-turned-off
    PERFORATER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // perforater-turned-on
    PERFORATER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-under-temperature
    PERFORATER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // perforater-unrecoverable-failure
    PERFORATER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // perforater-unrecoverable-storage-error
    PERFORATER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // perforater-warming-up
    PLATFORM_COOLING(REASON, NativePrinterStatus.UNMAPPED), // platform-cooling
    PLATFORM_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // platform-failure
    PLATFORM_HEATING(REASON, NativePrinterStatus.UNMAPPED), // platform-heating
    PLATFORM_TEMPERATURE_HIGH(REASON, NativePrinterStatus.UNMAPPED), // platform-temperature-high
    PLATFORM_TEMPERATURE_LOW(REASON, NativePrinterStatus.UNMAPPED), // platform-temperature-low
    POWER_DOWN(REASON, NativePrinterStatus.UNMAPPED), // power-down
    POWER_UP(REASON, NativePrinterStatus.UNMAPPED), // power-up
    PRINTER_MANUAL_RESET(REASON, NativePrinterStatus.UNMAPPED), // printer-manual-reset
    PRINTER_NMS_RESET(REASON, NativePrinterStatus.UNMAPPED), // printer-nms-reset
    PRINTER_READY_TO_PRINT(REASON, NativePrinterStatus.UNMAPPED), // printer-ready-to-print
    PUNCHER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // puncher-added
    PUNCHER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // puncher-almost-empty
    PUNCHER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // puncher-almost-full
    PUNCHER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // puncher-at-limit
    PUNCHER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // puncher-closed
    PUNCHER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // puncher-configuration-change
    PUNCHER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // puncher-cover-closed
    PUNCHER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // puncher-cover-open
    PUNCHER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // puncher-empty
    PUNCHER_FULL(REASON, NativePrinterStatus.UNMAPPED), // puncher-full
    PUNCHER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // puncher-interlock-closed
    PUNCHER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // puncher-interlock-open
    PUNCHER_JAM(REASON, NativePrinterStatus.UNMAPPED), // puncher-jam
    PUNCHER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // puncher-life-almost-over
    PUNCHER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // puncher-life-over
    PUNCHER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // puncher-memory-exhausted
    PUNCHER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // puncher-missing
    PUNCHER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-motor-failure
    PUNCHER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // puncher-near-limit
    PUNCHER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // puncher-offline
    PUNCHER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // puncher-opened
    PUNCHER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-over-temperature
    PUNCHER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // puncher-power-saver
    PUNCHER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-recoverable-failure
    PUNCHER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // puncher-recoverable-storage
    PUNCHER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // puncher-removed
    PUNCHER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // puncher-resource-added
    PUNCHER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // puncher-resource-removed
    PUNCHER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-thermistor-failure
    PUNCHER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-timing-failure
    PUNCHER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // puncher-turned-off
    PUNCHER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // puncher-turned-on
    PUNCHER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-under-temperature
    PUNCHER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // puncher-unrecoverable-failure
    PUNCHER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // puncher-unrecoverable-storage-error
    PUNCHER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // puncher-warming-up
    RESUMING(REASON, NativePrinterStatus.UNMAPPED), // resuming
    SCAN_MEDIA_PATH_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-failure
    SCAN_MEDIA_PATH_INPUT_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-input-empty
    SCAN_MEDIA_PATH_INPUT_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-input-feed-error
    SCAN_MEDIA_PATH_INPUT_JAM(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-input-jam
    SCAN_MEDIA_PATH_INPUT_REQUEST(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-input-request
    SCAN_MEDIA_PATH_JAM(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-jam
    SCAN_MEDIA_PATH_OUTPUT_FEED_ERROR(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-output-feed-error
    SCAN_MEDIA_PATH_OUTPUT_FULL(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-output-full
    SCAN_MEDIA_PATH_OUTPUT_JAM(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-output-jam
    SCAN_MEDIA_PATH_PICK_ROLLER_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-pick-roller-failure
    SCAN_MEDIA_PATH_PICK_ROLLER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-pick-roller-life-over
    SCAN_MEDIA_PATH_PICK_ROLLER_LIFE_WARN(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-pick-roller-life-warn
    SCAN_MEDIA_PATH_PICK_ROLLER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-pick-roller-missing
    SCAN_MEDIA_PATH_TRAY_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-tray-almost-full
    SCAN_MEDIA_PATH_TRAY_FULL(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-tray-full
    SCAN_MEDIA_PATH_TRAY_MISSING(REASON, NativePrinterStatus.UNMAPPED), // scan-media-path-tray-missing
    SCANNER_LIGHT_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // scanner-light-failure
    SCANNER_LIGHT_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // scanner-light-life-almost-over
    SCANNER_LIGHT_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // scanner-light-life-over
    SCANNER_LIGHT_MISSING(REASON, NativePrinterStatus.UNMAPPED), // scanner-light-missing
    SCANNER_SENSOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // scanner-sensor-failure
    SCANNER_SENSOR_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // scanner-sensor-life-almost-over
    SCANNER_SENSOR_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // scanner-sensor-life-over
    SCANNER_SENSOR_MISSING(REASON, NativePrinterStatus.UNMAPPED), // scanner-sensor-missing
    SEPARATION_CUTTER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-added
    SEPARATION_CUTTER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-almost-empty
    SEPARATION_CUTTER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-almost-full
    SEPARATION_CUTTER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-at-limit
    SEPARATION_CUTTER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-closed
    SEPARATION_CUTTER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-configuration-change
    SEPARATION_CUTTER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-cover-closed
    SEPARATION_CUTTER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-cover-open
    SEPARATION_CUTTER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-empty
    SEPARATION_CUTTER_FULL(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-full
    SEPARATION_CUTTER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-interlock-closed
    SEPARATION_CUTTER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-interlock-open
    SEPARATION_CUTTER_JAM(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-jam
    SEPARATION_CUTTER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-life-almost-over
    SEPARATION_CUTTER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-life-over
    SEPARATION_CUTTER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-memory-exhausted
    SEPARATION_CUTTER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-missing
    SEPARATION_CUTTER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-motor-failure
    SEPARATION_CUTTER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-near-limit
    SEPARATION_CUTTER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-offline
    SEPARATION_CUTTER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-opened
    SEPARATION_CUTTER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-over-temperature
    SEPARATION_CUTTER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-power-saver
    SEPARATION_CUTTER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-recoverable-failure
    SEPARATION_CUTTER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-recoverable-storage
    SEPARATION_CUTTER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-removed
    SEPARATION_CUTTER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-resource-added
    SEPARATION_CUTTER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-resource-removed
    SEPARATION_CUTTER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-thermistor-failure
    SEPARATION_CUTTER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-timing-failure
    SEPARATION_CUTTER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-turned-off
    SEPARATION_CUTTER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-turned-on
    SEPARATION_CUTTER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-under-temperature
    SEPARATION_CUTTER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-unrecoverable-failure
    SEPARATION_CUTTER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-unrecoverable-storage-error
    SEPARATION_CUTTER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // separation-cutter-warming-up
    SHEET_ROTATOR_ADDED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-added
    SHEET_ROTATOR_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-almost-empty
    SHEET_ROTATOR_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-almost-full
    SHEET_ROTATOR_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-at-limit
    SHEET_ROTATOR_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-closed
    SHEET_ROTATOR_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-configuration-change
    SHEET_ROTATOR_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-cover-closed
    SHEET_ROTATOR_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-cover-open
    SHEET_ROTATOR_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-empty
    SHEET_ROTATOR_FULL(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-full
    SHEET_ROTATOR_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-interlock-closed
    SHEET_ROTATOR_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-interlock-open
    SHEET_ROTATOR_JAM(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-jam
    SHEET_ROTATOR_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-life-almost-over
    SHEET_ROTATOR_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-life-over
    SHEET_ROTATOR_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-memory-exhausted
    SHEET_ROTATOR_MISSING(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-missing
    SHEET_ROTATOR_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-motor-failure
    SHEET_ROTATOR_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-near-limit
    SHEET_ROTATOR_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-offline
    SHEET_ROTATOR_OPENED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-opened
    SHEET_ROTATOR_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-over-temperature
    SHEET_ROTATOR_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-power-saver
    SHEET_ROTATOR_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-recoverable-failure
    SHEET_ROTATOR_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-recoverable-storage
    SHEET_ROTATOR_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-removed
    SHEET_ROTATOR_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-resource-added
    SHEET_ROTATOR_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-resource-removed
    SHEET_ROTATOR_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-thermistor-failure
    SHEET_ROTATOR_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-timing-failure
    SHEET_ROTATOR_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-turned-off
    SHEET_ROTATOR_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-turned-on
    SHEET_ROTATOR_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-under-temperature
    SHEET_ROTATOR_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-unrecoverable-failure
    SHEET_ROTATOR_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-unrecoverable-storage-error
    SHEET_ROTATOR_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // sheet-rotator-warming-up
    SLITTER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // slitter-added
    SLITTER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // slitter-almost-empty
    SLITTER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // slitter-almost-full
    SLITTER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // slitter-at-limit
    SLITTER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // slitter-closed
    SLITTER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // slitter-configuration-change
    SLITTER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // slitter-cover-closed
    SLITTER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // slitter-cover-open
    SLITTER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // slitter-empty
    SLITTER_FULL(REASON, NativePrinterStatus.UNMAPPED), // slitter-full
    SLITTER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // slitter-interlock-closed
    SLITTER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // slitter-interlock-open
    SLITTER_JAM(REASON, NativePrinterStatus.UNMAPPED), // slitter-jam
    SLITTER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // slitter-life-almost-over
    SLITTER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // slitter-life-over
    SLITTER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // slitter-memory-exhausted
    SLITTER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // slitter-missing
    SLITTER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-motor-failure
    SLITTER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // slitter-near-limit
    SLITTER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // slitter-offline
    SLITTER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // slitter-opened
    SLITTER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-over-temperature
    SLITTER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // slitter-power-saver
    SLITTER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-recoverable-failure
    SLITTER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // slitter-recoverable-storage
    SLITTER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // slitter-removed
    SLITTER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // slitter-resource-added
    SLITTER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // slitter-resource-removed
    SLITTER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-thermistor-failure
    SLITTER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-timing-failure
    SLITTER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // slitter-turned-off
    SLITTER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // slitter-turned-on
    SLITTER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-under-temperature
    SLITTER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // slitter-unrecoverable-failure
    SLITTER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // slitter-unrecoverable-storage-error
    SLITTER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // slitter-warming-up
    STACKER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stacker-added
    STACKER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stacker-almost-empty
    STACKER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // stacker-almost-full
    STACKER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stacker-at-limit
    STACKER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stacker-closed
    STACKER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // stacker-configuration-change
    STACKER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stacker-cover-closed
    STACKER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stacker-cover-open
    STACKER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stacker-empty
    STACKER_FULL(REASON, NativePrinterStatus.UNMAPPED), // stacker-full
    STACKER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stacker-interlock-closed
    STACKER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stacker-interlock-open
    STACKER_JAM(REASON, NativePrinterStatus.UNMAPPED), // stacker-jam
    STACKER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // stacker-life-almost-over
    STACKER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // stacker-life-over
    STACKER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // stacker-memory-exhausted
    STACKER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // stacker-missing
    STACKER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-motor-failure
    STACKER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stacker-near-limit
    STACKER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // stacker-offline
    STACKER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // stacker-opened
    STACKER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-over-temperature
    STACKER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // stacker-power-saver
    STACKER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-recoverable-failure
    STACKER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // stacker-recoverable-storage
    STACKER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stacker-removed
    STACKER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stacker-resource-added
    STACKER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stacker-resource-removed
    STACKER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-thermistor-failure
    STACKER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-timing-failure
    STACKER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // stacker-turned-off
    STACKER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // stacker-turned-on
    STACKER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-under-temperature
    STACKER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stacker-unrecoverable-failure
    STACKER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // stacker-unrecoverable-storage-error
    STACKER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // stacker-warming-up
    STANDBY(REASON, NativePrinterStatus.UNMAPPED), // standby
    STAPLER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stapler-added
    STAPLER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stapler-almost-empty
    STAPLER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // stapler-almost-full
    STAPLER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stapler-at-limit
    STAPLER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stapler-closed
    STAPLER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // stapler-configuration-change
    STAPLER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stapler-cover-closed
    STAPLER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stapler-cover-open
    STAPLER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stapler-empty
    STAPLER_FULL(REASON, NativePrinterStatus.UNMAPPED), // stapler-full
    STAPLER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stapler-interlock-closed
    STAPLER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stapler-interlock-open
    STAPLER_JAM(REASON, NativePrinterStatus.UNMAPPED), // stapler-jam
    STAPLER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // stapler-life-almost-over
    STAPLER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // stapler-life-over
    STAPLER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // stapler-memory-exhausted
    STAPLER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // stapler-missing
    STAPLER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-motor-failure
    STAPLER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stapler-near-limit
    STAPLER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // stapler-offline
    STAPLER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // stapler-opened
    STAPLER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-over-temperature
    STAPLER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // stapler-power-saver
    STAPLER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-recoverable-failure
    STAPLER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // stapler-recoverable-storage
    STAPLER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stapler-removed
    STAPLER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stapler-resource-added
    STAPLER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stapler-resource-removed
    STAPLER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-thermistor-failure
    STAPLER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-timing-failure
    STAPLER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // stapler-turned-off
    STAPLER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // stapler-turned-on
    STAPLER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-under-temperature
    STAPLER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stapler-unrecoverable-failure
    STAPLER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // stapler-unrecoverable-storage-error
    STAPLER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // stapler-warming-up
    STITCHER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-added
    STITCHER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stitcher-almost-empty
    STITCHER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // stitcher-almost-full
    STITCHER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stitcher-at-limit
    STITCHER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-closed
    STITCHER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-configuration-change
    STITCHER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-cover-closed
    STITCHER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stitcher-cover-open
    STITCHER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // stitcher-empty
    STITCHER_FULL(REASON, NativePrinterStatus.UNMAPPED), // stitcher-full
    STITCHER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-interlock-closed
    STITCHER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // stitcher-interlock-open
    STITCHER_JAM(REASON, NativePrinterStatus.UNMAPPED), // stitcher-jam
    STITCHER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // stitcher-life-almost-over
    STITCHER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // stitcher-life-over
    STITCHER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-memory-exhausted
    STITCHER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // stitcher-missing
    STITCHER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-motor-failure
    STITCHER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // stitcher-near-limit
    STITCHER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-offline
    STITCHER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-opened
    STITCHER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-over-temperature
    STITCHER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // stitcher-power-saver
    STITCHER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-recoverable-failure
    STITCHER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-recoverable-storage
    STITCHER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-removed
    STITCHER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-resource-added
    STITCHER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // stitcher-resource-removed
    STITCHER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-thermistor-failure
    STITCHER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-timing-failure
    STITCHER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // stitcher-turned-off
    STITCHER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // stitcher-turned-on
    STITCHER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-under-temperature
    STITCHER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // stitcher-unrecoverable-failure
    STITCHER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // stitcher-unrecoverable-storage-error
    STITCHER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // stitcher-warming-up
    SUBUNIT_ADDED(REASON, NativePrinterStatus.UNMAPPED), // subunit-added
    SUBUNIT_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // subunit-almost-empty
    SUBUNIT_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // subunit-almost-full
    SUBUNIT_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // subunit-at-limit
    SUBUNIT_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // subunit-closed
    SUBUNIT_COOLING_DOWN(REASON, NativePrinterStatus.UNMAPPED), // subunit-cooling-down
    SUBUNIT_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // subunit-empty
    SUBUNIT_FULL(REASON, NativePrinterStatus.UNMAPPED), // subunit-full
    SUBUNIT_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // subunit-life-almost-over
    SUBUNIT_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // subunit-life-over
    SUBUNIT_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // subunit-memory-exhausted
    SUBUNIT_MISSING(REASON, NativePrinterStatus.UNMAPPED), // subunit-missing
    SUBUNIT_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-motor-failure
    SUBUNIT_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // subunit-near-limit
    SUBUNIT_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // subunit-offline
    SUBUNIT_OPENED(REASON, NativePrinterStatus.UNMAPPED), // subunit-opened
    SUBUNIT_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-over-temperature
    SUBUNIT_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // subunit-power-saver
    SUBUNIT_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-recoverable-failure
    SUBUNIT_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // subunit-recoverable-storage
    SUBUNIT_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // subunit-removed
    SUBUNIT_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // subunit-resource-added
    SUBUNIT_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // subunit-resource-removed
    SUBUNIT_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-thermistor-failure
    SUBUNIT_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-timing-Failure
    SUBUNIT_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // subunit-turned-off
    SUBUNIT_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // subunit-turned-on
    SUBUNIT_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-under-temperature
    SUBUNIT_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // subunit-unrecoverable-failure
    SUBUNIT_UNRECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // subunit-unrecoverable-storage
    SUBUNIT_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // subunit-warming-up
    SUSPEND(REASON, NativePrinterStatus.UNMAPPED), // suspend
    TESTING(REASON, NativePrinterStatus.UNMAPPED), // testing
    TRIMMER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-added
    TRIMMER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // trimmer-almost-empty
    TRIMMER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // trimmer-almost-full
    TRIMMER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // trimmer-at-limit
    TRIMMER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-closed
    TRIMMER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-configuration-change
    TRIMMER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-cover-closed
    TRIMMER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // trimmer-cover-open
    TRIMMER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // trimmer-empty
    TRIMMER_FULL(REASON, NativePrinterStatus.UNMAPPED), // trimmer-full
    TRIMMER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-interlock-closed
    TRIMMER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // trimmer-interlock-open
    TRIMMER_JAM(REASON, NativePrinterStatus.UNMAPPED), // trimmer-jam
    TRIMMER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // trimmer-life-almost-over
    TRIMMER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // trimmer-life-over
    TRIMMER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-memory-exhausted
    TRIMMER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // trimmer-missing
    TRIMMER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-motor-failure
    TRIMMER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // trimmer-near-limit
    TRIMMER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-offline
    TRIMMER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-opened
    TRIMMER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-over-temperature
    TRIMMER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // trimmer-power-saver
    TRIMMER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-recoverable-failure
    TRIMMER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-recoverable-storage
    TRIMMER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-removed
    TRIMMER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-resource-added
    TRIMMER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // trimmer-resource-removed
    TRIMMER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-thermistor-failure
    TRIMMER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-timing-failure
    TRIMMER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // trimmer-turned-off
    TRIMMER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // trimmer-turned-on
    TRIMMER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-under-temperature
    TRIMMER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // trimmer-unrecoverable-failure
    TRIMMER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // trimmer-unrecoverable-storage-error
    TRIMMER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED), // trimmer-warming-up
    UNKNOWN(REASON, NativePrinterStatus.UNMAPPED), // unknown
    WRAPPER_ADDED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-added
    WRAPPER_ALMOST_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // wrapper-almost-empty
    WRAPPER_ALMOST_FULL(REASON, NativePrinterStatus.UNMAPPED), // wrapper-almost-full
    WRAPPER_AT_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // wrapper-at-limit
    WRAPPER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-closed
    WRAPPER_CONFIGURATION_CHANGE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-configuration-change
    WRAPPER_COVER_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-cover-closed
    WRAPPER_COVER_OPEN(REASON, NativePrinterStatus.UNMAPPED), // wrapper-cover-open
    WRAPPER_EMPTY(REASON, NativePrinterStatus.UNMAPPED), // wrapper-empty
    WRAPPER_FULL(REASON, NativePrinterStatus.UNMAPPED), // wrapper-full
    WRAPPER_INTERLOCK_CLOSED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-interlock-closed
    WRAPPER_INTERLOCK_OPEN(REASON, NativePrinterStatus.UNMAPPED), // wrapper-interlock-open
    WRAPPER_JAM(REASON, NativePrinterStatus.UNMAPPED), // wrapper-jam
    WRAPPER_LIFE_ALMOST_OVER(REASON, NativePrinterStatus.UNMAPPED), // wrapper-life-almost-over
    WRAPPER_LIFE_OVER(REASON, NativePrinterStatus.UNMAPPED), // wrapper-life-over
    WRAPPER_MEMORY_EXHAUSTED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-memory-exhausted
    WRAPPER_MISSING(REASON, NativePrinterStatus.UNMAPPED), // wrapper-missing
    WRAPPER_MOTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-motor-failure
    WRAPPER_NEAR_LIMIT(REASON, NativePrinterStatus.UNMAPPED), // wrapper-near-limit
    WRAPPER_OFFLINE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-offline
    WRAPPER_OPENED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-opened
    WRAPPER_OVER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-over-temperature
    WRAPPER_POWER_SAVER(REASON, NativePrinterStatus.UNMAPPED), // wrapper-power-saver
    WRAPPER_RECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-recoverable-failure
    WRAPPER_RECOVERABLE_STORAGE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-recoverable-storage
    WRAPPER_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-removed
    WRAPPER_RESOURCE_ADDED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-resource-added
    WRAPPER_RESOURCE_REMOVED(REASON, NativePrinterStatus.UNMAPPED), // wrapper-resource-removed
    WRAPPER_THERMISTOR_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-thermistor-failure
    WRAPPER_TIMING_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-timing-failure
    WRAPPER_TURNED_OFF(REASON, NativePrinterStatus.UNMAPPED), // wrapper-turned-off
    WRAPPER_TURNED_ON(REASON, NativePrinterStatus.UNMAPPED), // wrapper-turned-on
    WRAPPER_UNDER_TEMPERATURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-under-temperature
    WRAPPER_UNRECOVERABLE_FAILURE(REASON, NativePrinterStatus.UNMAPPED), // wrapper-unrecoverable-failure
    WRAPPER_UNRECOVERABLE_STORAGE_ERROR(REASON, NativePrinterStatus.UNMAPPED), // wrapper-unrecoverable-storage-error
    WRAPPER_WARMING_UP(REASON, NativePrinterStatus.UNMAPPED); // wrapper-warming-up

    private static final Logger log = LogManager.getLogger(CupsPrinterStatusMap.class);
    private static final String[] SNMP_REDUNDANT_SUFFIXES = { "-warning", "-report" };
    public static SortedMap<String,NativePrinterStatus> sortedReasonLookupTable;
    public static SortedMap<String,NativePrinterStatus> sortedStateLookupTable;

    private NativePrinterStatus parent;
    private CupsPrinterStatusType type;

    enum CupsPrinterStatusType {
        STATE,
        REASON;
    }

    CupsPrinterStatusMap(CupsPrinterStatusType type, NativePrinterStatus parent) {
        this.type = type;
        this.parent = parent;
    }

    public static NativePrinterStatus matchReason(String code) {
        // Initialize a sorted map to speed up lookups
        if(sortedReasonLookupTable == null) {
            sortedReasonLookupTable = new TreeMap<>();
            for(CupsPrinterStatusMap value : values()) {
                if(value.type == REASON) {
                    sortedReasonLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
                }
            }
        }
        NativePrinterStatus status = sortedReasonLookupTable.get(code);
        return status;
    }

    public static NativePrinterStatus matchState(String state) {
        // Initialize a sorted map to speed up lookups
        if(sortedStateLookupTable == null) {
            sortedStateLookupTable = new TreeMap<>();
            for(CupsPrinterStatusMap value : values()) {
                if(value.type == STATE) {
                    sortedStateLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
                }
            }
        }
        return sortedStateLookupTable.getOrDefault(state, NativePrinterStatus.UNMAPPED);
    }

    public static Status createStatus(String reason, String state, String printer) {
        NativePrinterStatus cupsPrinterStatus = matchReason(reason);

        // Edge-case for snmp statuses
        if(cupsPrinterStatus == null) {
            String sanitizedReason = snmpSanitize(reason);
            if (!reason.equals(sanitizedReason)) {
                cupsPrinterStatus = sortedReasonLookupTable.get(sanitizedReason);
                if (cupsPrinterStatus != null) reason = sanitizedReason;
            }
        }

        if(cupsPrinterStatus == null && !reason.equalsIgnoreCase("none")) {
            // Don't warn for "none"
            log.warn("Printer state-reason \"{}\" was not found", reason);
        }

        if(cupsPrinterStatus == null) {
            // Don't return the raw reason if we couldn't find it mapped, return state instead
            return new Status(matchState(state), printer, state);
        } else if(cupsPrinterStatus == NativePrinterStatus.UNMAPPED) {
            // Still return the state, but let the user know what the unmapped state reason was
            return new Status(matchState(state), printer, reason);
        }
        return new Status(cupsPrinterStatus, printer, reason);
    }

    @Override
    public NativePrinterStatus getParent() {
        return parent;
    }

    @Override
    public Object getRawCode() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-");
    }

    /**
     * Removes redundant "-warning" or "-report" from SNMP-originated statuses
     */
    public static String snmpSanitize(String cupsString) {
        for(String suffix : SNMP_REDUNDANT_SUFFIXES) {
            if (cupsString.endsWith(suffix)) {
                return cupsString.substring(0, cupsString.length() - suffix.length());
            }
        }
        return cupsString;
    }
}
