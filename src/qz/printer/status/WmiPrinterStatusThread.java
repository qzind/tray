package qz.printer.status;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.sun.jna.platform.win32.Winspool.*;

public class WmiPrinterStatusThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private boolean closing = false;
    private final String printerName;
    private final Winspool spool = Winspool.INSTANCE;
    private int lastStatus = -1;

    private HashMap<Short, String> notifyPrinterFieldMap;
    private HashMap<Short, String> notifyJobFieldMap;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    Winspool.PRINTER_NOTIFY_OPTIONS options;

    public WmiPrinterStatusThread(String name) {
        super("Printer Status Monitor " + name);
        printerName = name;

        notifyPrinterFieldMap = new HashMap<>();
        notifyPrinterFieldMap.put((short)0x00, "PRINTER_NOTIFY_FIELD_SERVER_NAME");
        notifyPrinterFieldMap.put((short)0x01, "PRINTER_NOTIFY_FIELD_PRINTER_NAME");
        notifyPrinterFieldMap.put((short)0x02, "PRINTER_NOTIFY_FIELD_SHARE_NAME");
        notifyPrinterFieldMap.put((short)0x03, "PRINTER_NOTIFY_FIELD_PORT_NAME");
        notifyPrinterFieldMap.put((short)0x04, "PRINTER_NOTIFY_FIELD_DRIVER_NAME");
        notifyPrinterFieldMap.put((short)0x05, "PRINTER_NOTIFY_FIELD_COMMENT");
        notifyPrinterFieldMap.put((short)0x06, "PRINTER_NOTIFY_FIELD_LOCATION");
        notifyPrinterFieldMap.put((short)0x07, "PRINTER_NOTIFY_FIELD_DEVMODE");
        notifyPrinterFieldMap.put((short)0x08, "PRINTER_NOTIFY_FIELD_SEPFILE");
        notifyPrinterFieldMap.put((short)0x09, "PRINTER_NOTIFY_FIELD_PRINT_PROCESSOR");
        notifyPrinterFieldMap.put((short)0x0A, "PRINTER_NOTIFY_FIELD_PARAMETERS");
        notifyPrinterFieldMap.put((short)0x0B, "PRINTER_NOTIFY_FIELD_DATATYPE");
        notifyPrinterFieldMap.put((short)0x0C, "PRINTER_NOTIFY_FIELD_SECURITY_DESCRIPTOR");
        notifyPrinterFieldMap.put((short)0x0D, "PRINTER_NOTIFY_FIELD_ATTRIBUTES");
        notifyPrinterFieldMap.put((short)0x0E, "PRINTER_NOTIFY_FIELD_PRIORITY");
        notifyPrinterFieldMap.put((short)0x0F, "PRINTER_NOTIFY_FIELD_DEFAULT_PRIORITY");
        notifyPrinterFieldMap.put((short)0x10, "PRINTER_NOTIFY_FIELD_START_TIME");
        notifyPrinterFieldMap.put((short)0x11, "PRINTER_NOTIFY_FIELD_UNTIL_TIME");
        notifyPrinterFieldMap.put((short)0x12, "PRINTER_NOTIFY_FIELD_STATUS");
        notifyPrinterFieldMap.put((short)0x13, "PRINTER_NOTIFY_FIELD_STATUS_STRING");
        notifyPrinterFieldMap.put((short)0x14, "PRINTER_NOTIFY_FIELD_CJOBS");
        notifyPrinterFieldMap.put((short)0x15, "PRINTER_NOTIFY_FIELD_AVERAGE_PPM");
        notifyPrinterFieldMap.put((short)0x16, "PRINTER_NOTIFY_FIELD_TOTAL_PAGES");
        notifyPrinterFieldMap.put((short)0x17, "PRINTER_NOTIFY_FIELD_PAGES_PRINTED");
        notifyPrinterFieldMap.put((short)0x18, "PRINTER_NOTIFY_FIELD_TOTAL_BYTES");
        notifyPrinterFieldMap.put((short)0x19, "PRINTER_NOTIFY_FIELD_BYTES_PRINTED");
        notifyPrinterFieldMap.put((short)0x1A, "PRINTER_NOTIFY_FIELD_OBJECT_GUID");
        notifyPrinterFieldMap.put((short)0x1B, "PRINTER_NOTIFY_FIELD_FRIENDLY_NAME");
        notifyPrinterFieldMap.put((short)0x1C, "PRINTER_NOTIFY_FIELD_BRANCH_OFFICE_PRINTING");
        notifyJobFieldMap = new HashMap<>();
        notifyJobFieldMap.put((short)0x00, "JOB_NOTIFY_FIELD_PRINTER_NAME");
        notifyJobFieldMap.put((short)0x01, "JOB_NOTIFY_FIELD_MACHINE_NAME");
        notifyJobFieldMap.put((short)0x02, "JOB_NOTIFY_FIELD_PORT_NAME");
        notifyJobFieldMap.put((short)0x03, "JOB_NOTIFY_FIELD_USER_NAME");
        notifyJobFieldMap.put((short)0x04, "JOB_NOTIFY_FIELD_NOTIFY_NAME");
        notifyJobFieldMap.put((short)0x05, "JOB_NOTIFY_FIELD_DATATYPE");
        notifyJobFieldMap.put((short)0x06, "JOB_NOTIFY_FIELD_PRINT_PROCESSOR");
        notifyJobFieldMap.put((short)0x07, "JOB_NOTIFY_FIELD_PARAMETERS");
        notifyJobFieldMap.put((short)0x08, "JOB_NOTIFY_FIELD_DRIVER_NAME");
        notifyJobFieldMap.put((short)0x09, "JOB_NOTIFY_FIELD_DEVMODE");
        notifyJobFieldMap.put((short)0x0A, "JOB_NOTIFY_FIELD_STATUS");
        notifyJobFieldMap.put((short)0x0B, "JOB_NOTIFY_FIELD_STATUS_STRING");
        notifyJobFieldMap.put((short)0x0C, "JOB_NOTIFY_FIELD_SECURITY_DESCRIPTOR");
        notifyJobFieldMap.put((short)0x0D, "JOB_NOTIFY_FIELD_DOCUMENT");
        notifyJobFieldMap.put((short)0x0E, "JOB_NOTIFY_FIELD_PRIORITY");
        notifyJobFieldMap.put((short)0x0F, "JOB_NOTIFY_FIELD_POSITION");
        notifyJobFieldMap.put((short)0x10, "JOB_NOTIFY_FIELD_SUBMITTED");
        notifyJobFieldMap.put((short)0x11, "JOB_NOTIFY_FIELD_START_TIME");
        notifyJobFieldMap.put((short)0x12, "JOB_NOTIFY_FIELD_UNTIL_TIME");
        notifyJobFieldMap.put((short)0x13, "JOB_NOTIFY_FIELD_TIME");
        notifyJobFieldMap.put((short)0x14, "JOB_NOTIFY_FIELD_TOTAL_PAGES");
        notifyJobFieldMap.put((short)0x15, "JOB_NOTIFY_FIELD_PAGES_PRINTED");
        notifyJobFieldMap.put((short)0x16, "JOB_NOTIFY_FIELD_TOTAL_BYTES");
        notifyJobFieldMap.put((short)0x17, "JOB_NOTIFY_FIELD_BYTES_PRINTED");
        notifyJobFieldMap.put((short)0x18, "JOB_NOTIFY_FIELD_REMOTE_JOB_ID");

        options = new Winspool.PRINTER_NOTIFY_OPTIONS();
        options.Version = 2;
        options.Flags = Winspool.PRINTER_NOTIFY_OPTIONS_REFRESH;
        options.Count = 1;
        PRINTER_NOTIFY_OPTIONS_TYPE.ByReference optionsType = new Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference();
        optionsType.Type = JOB_NOTIFY_TYPE;
        // The logic for handling the encoding of the fields was moved to the
        // implementation of the PRINTER_NOTIFY_OPTIONS_TYPE structure
        optionsType.setFields(new short[] { JOB_NOTIFY_FIELD_STATUS });
        optionsType.toArray(1);
        options.pTypes = optionsType;

    }

    @Override
    public void run() {

        attachToSystem();

        if (hChangeObject != null) {
            while(!closing) {
                waitOnChange();
                if (closing) { break; }
                ingestChange();
            }
        }
    }

    private void attachToSystem() {
        WinNT.HANDLEByReference phPrinterObject = new WinNT.HANDLEByReference();
        spool.OpenPrinter(printerName, phPrinterObject, null);
        pdwChangeResult = new WinDef.DWORDByReference();
        //The second param determines what kind of event releases our lock
        //See https://msdn.microsoft.com/en-us/library/windows/desktop/dd162722(v=vs.85).aspx
        hChangeObject = spool.FindFirstPrinterChangeNotification(phPrinterObject.getValue(), Winspool.PRINTER_CHANGE_JOB, 0, options);
    }

    private void waitOnChange() {
        Kernel32.INSTANCE.WaitForSingleObject(hChangeObject, WinBase.INFINITE);
    }

    private void ingestChange() {

        PRINTER_NOTIFY_OPTIONS notifyOptions = new Winspool.PRINTER_NOTIFY_OPTIONS();
        notifyOptions.Version = 2;
        //notifyOptions.Flags = Winspool.PRINTER_NOTIFY_OPTIONS_REFRESH;
        notifyOptions.Count = 0;
        PRINTER_NOTIFY_OPTIONS_TYPE.ByReference optionsType = new Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference();
        optionsType.Type = JOB_NOTIFY_TYPE;
        // The logic for handling the encoding of the fields was moved to the
        // implementation of the PRINTER_NOTIFY_OPTIONS_TYPE structure
        optionsType.setFields(new short[] { JOB_NOTIFY_FIELD_STATUS, JOB_NOTIFY_FIELD_DOCUMENT });
        optionsType.toArray(1);
        notifyOptions.pTypes = optionsType;


        PointerByReference dataPointer = new PointerByReference();
        if (spool.FindNextPrinterChangeNotification(hChangeObject, pdwChangeResult, notifyOptions, dataPointer)) {
            // Handle printer status changes
            //Requesting an info object every time is required
            //int statusCode = WinspoolUtil.getPrinterInfo2(printerName).Status;
            //if (lastStatus != statusCode) {
            //    lastStatus = statusCode;
            //    Status[] statuses = NativeStatus.fromWmiPrinterStatus(statusCode, printerName);
            //    StatusMonitor.statusChanged(statuses);
            //}
            if (dataPointer.getValue() != null) {
                Winspool.PRINTER_NOTIFY_INFO data = Structure.newInstance(Winspool.PRINTER_NOTIFY_INFO.class, dataPointer.getValue());
                data.read();
                //Todo Remove this debugging log
                //log.warn(pdwChangeResult.toString()); type
                // Handle job status changes
                for(int i = 0; i < data.Count; i++) {
                    Winspool.PRINTER_NOTIFY_INFO_DATA d = data.aData[i];
                    //Todo Remove this debugging log
                    //log.warn(d.Type == 0? "printer":"job");
                    //log.warn("field :" + notifyJobFieldMap.get(d.Field));
                    //log.warn("id: " + d.Id);
                    //Todo Remove this debugging log
                    //log.warn(d.NotifyData.Data.toString());
                    if (d.Type == JOB_NOTIFY_TYPE && d.Field == JOB_NOTIFY_FIELD_STATUS) {
                        int[] code = (int[])d.NotifyData.getTypedValue(int[].class);
                        //log.warn("status {} {}", statuses[0], statuses[1]);
                        //new Thread(() -> {
                        //Todo Remove this debugging log
                        log.warn("Raw Code {}", code[0]);
                            Status[] statuses = NativeStatus.fromWmiJobStatus(code[0], printerName, d.Id, "testing");
                            StatusMonitor.statusChanged(statuses);
                        //try {
                        //    sleep(1000);
                        //}
                        //catch(InterruptedException e) {
                        //    e.printStackTrace();
                        //}
                        //
                        //}).start();
                    }
                }
                //WinNT.HANDLEByReference phPrinter = new WinNT.HANDLEByReference();
                //Winspool.INSTANCE.OpenPrinter(printerName, phPrinter, null);
                //for(Winspool.JOB_INFO_1 info : WinspoolUtil.getJobInfo1(phPrinter)) {
                //    //Todo Remove this debugging log
                //    //log.warn(info.pStatus);
                //    Status[] statuses = NativeStatus.fromWmiJobStatus(info.Status, printerName, info.JobId, info.pDocument);
                //    StatusMonitor.statusChanged(statuses);
                //}
                Winspool.INSTANCE.FreePrinterNotifyInfo(data.getPointer());
            } else {
                //Todo Remove this debugging log
                log.warn("null");
            }
        } else {
            issueError();
        }
    }

    private void issueError() {
        int errorCode = Kernel32.INSTANCE.GetLastError();
        log.error("WMI Error number: {}, This should be reported", errorCode);
        Status[] unknownError = { new Status(NativePrinterStatus.UNMAPPED, printerName, WmiPrinterStatusMap.UNKNOWN_STATUS.getRawCode()) };
        StatusMonitor.statusChanged(unknownError);
        try {
            //if the error repeats, we don't want to lock up the cpu
            Thread.sleep(1000);
        }
        catch(Exception ignore) {}
    }

    @Override
    public void interrupt() {
        closing = true;
        spool.FindClosePrinterChangeNotification(hChangeObject);
        super.interrupt();
    }

    public static ArrayList<Status> getAllStatuses() {
        ArrayList<Status> statuses = new ArrayList<>();
        Winspool.PRINTER_INFO_2[] wmiPrinters = WinspoolUtil.getPrinterInfo2();
        for(Winspool.PRINTER_INFO_2 printerInfo : wmiPrinters) {
            WinNT.HANDLEByReference phPrinter = new WinNT.HANDLEByReference();
            Winspool.INSTANCE.OpenPrinter(printerInfo.pPrinterName, phPrinter, null);
            for(Winspool.JOB_INFO_1 info : WinspoolUtil.getJobInfo1(phPrinter)) {
                statuses.addAll(Arrays.asList(NativeStatus.fromWmiJobStatus(info.Status, printerInfo.pPrinterName, info.JobId, info.pDocument)));
            }
            statuses.addAll(Arrays.asList(NativeStatus.fromWmiPrinterStatus(printerInfo.Status, printerInfo.pPrinterName)));
        }
        return statuses;
    }
}
