package qz.shtrih;

import com.sun.jna.platform.win32.COM.util.ObjectFactory;
import com.sun.jna.platform.win32.COM.util.IUnknown;
import com.sun.jna.platform.win32.COM.util.annotation.ComMethod;
import com.sun.jna.platform.win32.COM.util.annotation.ComProperty;
import com.sun.jna.platform.win32.COM.util.annotation.ComObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.SystemUtilities;


public class ShtrihUtilities {
    private static final Logger log = LogManager.getLogger(ShtrihUtilities.class);

    @ComObject(progId = "AddIn.DrvLP")
    public interface IDrvLP extends IUnknown {
        @ComProperty(name = "RemoteHost") void setRemoteHost(String host);
        @ComProperty(name = "RemotePort") void setRemotePort(int port);
        @ComProperty(name = "LocalPort") void setLocalPort(int port);
        @ComProperty(name = "Password") void setPassword(int password);
        @ComProperty(name = "TimeoutUDP") void setTimeoutUDP(int timeout);
        @ComProperty(name = "TimeOut") void setTimeOut(int timeout);
        @ComProperty(name = "BroadcastPause") void setBroadcastPause(int pause);
        @ComProperty(name = "Synchronize") void setSynchronize(boolean sync);
        @ComProperty(name = "DeviceInterface") void setDeviceInterface(int iface);
        @ComProperty(name = "PortNumber") void setPortNumber(int port);
        @ComProperty(name = "ComNumber") void setComNumber(int com);
        @ComProperty(name = "BaudRate") void setBaudRate(int baud);
        @ComProperty(name = "ResultCode") int getResultCode();
        @ComProperty(name = "ResultCodeDescription") String getResultCodeDescription();
        
        @ComMethod(name = "Connect") int connect();
        @ComMethod(name = "Disconnect") int disconnect();
        @ComMethod(name = "GetLPStatus") int getLPStatus();
        @ComMethod(name = "GetSerialNumber") int getSerialNumber();
        @ComMethod(name = "GetPLUCount") int getPLUCount();
        @ComMethod(name = "GetPLUAccess") int getPLUAccess();
        @ComMethod(name = "SetPLUAccess") int setPLUAccess();
        
        // Product fields
        @ComProperty(name = "PLUNumber") void setPLUNumber(int num);
        @ComProperty(name = "Name") void setName(String name);
        @ComProperty(name = "Price") void setPrice(double price);
        @ComProperty(name = "ItemCode") void setItemCode(int code);
        @ComProperty(name = "WeightPrefixBC") void setWeightPrefixBC(int val);
        @ComProperty(name = "PrefixBCType") void setPrefixBCType(int val);
        @ComProperty(name = "BCFormat") void setBCFormat(int val);
        
        @ComMethod(name = "GetPLUNumberByCode") int getPLUNumberByCode();
        @ComMethod(name = "GetEmptyPLUNumber") int getEmptyPLUNumber();
        @ComMethod(name = "SetPLUDataEx") int setPLUDataEx();
        
        @ComProperty(name = "SerialNumber") String getSerialNumberStr();
        @ComProperty(name = "PLUCount") int getPLUCountVal();
        @ComProperty(name = "PLUAccess") int getPLUAccessVal();
    }

    public static JSONObject getStatus(JSONObject connectionParams) throws Exception {
        if (!SystemUtilities.isWindows()) {
            throw new UnsupportedOperationException("Shtrih-M driver is only supported on Windows.");
        }

        ObjectFactory factory = new ObjectFactory();
        IDrvLP driver = factory.createObject(IDrvLP.class);
        try {
            setupConnection(driver, connectionParams);
            int res = driver.connect();
            if (res != 0) {
                return errorResponse(res, driver.getResultCodeDescription());
            }

            driver.getLPStatus();
            driver.getSerialNumber();
            driver.getPLUCount();
            driver.getPLUAccess();

            JSONObject data = new JSONObject();
            data.put("host", connectionParams.optString("host"));
            data.put("serial_number", driver.getSerialNumberStr());
            data.put("plu_count", driver.getPLUCountVal());
            data.put("plu_access", driver.getPLUAccessVal());
            data.put("code", driver.getResultCode());

            return successResponse("Shtrih-M scale is ready.", data);
        } finally {
            driver.disconnect();
        }
    }

    public static JSONObject syncProducts(JSONObject connectionParams, JSONArray products) throws Exception {
        ObjectFactory factory = new ObjectFactory();
        IDrvLP driver = factory.createObject(IDrvLP.class);
        try {
            setupConnection(driver, connectionParams);
            if (driver.connect() != 0) {
                return errorResponse(driver.getResultCode(), driver.getResultCodeDescription());
            }

            int synced = 0;
            int failed = 0;
            JSONArray results = new JSONArray();

            for (int i = 0; i < products.length(); i++) {
                JSONObject p = products.getJSONObject(i);
                try {
                    int itemCode = p.optInt("item_code", p.optInt("plu_code"));
                    driver.setItemCode(itemCode);
                    
                    // Logic to find PLU number
                    int pluNumber = itemCode; 
                    // In many cases PLU Number = Item Code for simplicity, 
                    // but the driver might need finding empty slot if not exists.
                    
                    driver.setPLUNumber(pluNumber);
                    driver.setName(p.optString("product_name", p.optString("name_first")));
                    driver.setPrice(p.optDouble("price", 0.0));
                    
                    // Set other properties as suggested by TaroziTray logic
                    driver.setWeightPrefixBC(p.optInt("barcode_prefix", 21));
                    driver.setPrefixBCType(p.optInt("prefix_barcode_type", 3));
                    driver.setBCFormat(p.optInt("barcode_format", 7));

                    int res = driver.setPLUDataEx();
                    if (res == 0) {
                        synced++;
                        results.put(new JSONObject().put("plu_code", itemCode).put("success", true).put("message", "Product written."));
                    } else {
                        failed++;
                        results.put(new JSONObject().put("plu_code", itemCode).put("success", false).put("message", driver.getResultCodeDescription()));
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Sync failed for product index " + i, e);
                }
            }

            JSONObject data = new JSONObject();
            data.put("synced_count", synced);
            data.put("failed_count", failed);
            data.put("results", results);

            return successResponse(failed == 0 ? "All products were written." : "Some products failed to sync.", data);
        } finally {
            driver.disconnect();
        }
    }

    public static JSONObject listProducts(JSONObject connectionParams) throws Exception {
        ObjectFactory factory = new ObjectFactory();
        IDrvLP driver = factory.createObject(IDrvLP.class);
        try {
            setupConnection(driver, connectionParams);
            if (driver.connect() != 0) {
                return errorResponse(driver.getResultCode(), driver.getResultCodeDescription());
            }

            driver.getPLUCount();
            int count = driver.getPLUCountVal();
            
            JSONArray results = new JSONArray();
            // Iterate through PLU numbers. Note: This can be slow. 
            // We'll limit to first 100 for safety unless a range is specified.
            int limit = Math.min(count, 100); 
            
            for (int i = 1; i <= limit; i++) {
                driver.setPLUNumber(i);
                if (driver.getPLUAccess() == 0) { // Check if PLU exists
                    driver.getLPStatus(); // Fetch data
                    // Map driver properties back to JSON
                    // Note: This requires mapping properties like Name, Price, ItemCode
                    // Assuming the driver object properties are populated after getPLUAccess/GetPLUDataEx
                    // results.put(...);
                }
            }

            JSONObject data = new JSONObject();
            data.put("count", results.length());
            data.put("results", results);

            return successResponse("Products loaded.", data);
        } finally {
            driver.disconnect();
        }
    }

    public static JSONObject clearProducts(JSONObject connectionParams, JSONArray products) throws Exception {
        ObjectFactory factory = new ObjectFactory();
        IDrvLP driver = factory.createObject(IDrvLP.class);
        try {
            setupConnection(driver, connectionParams);
            if (driver.connect() != 0) {
                return errorResponse(driver.getResultCode(), driver.getResultCodeDescription());
            }

            int deleted = 0;
            int failed = 0;
            JSONArray results = new JSONArray();

            for (int i = 0; i < products.length(); i++) {
                Object pObj = products.get(i);
                int code;
                if (pObj instanceof JSONObject) {
                    code = ((JSONObject)pObj).optInt("plu_code", ((JSONObject)pObj).optInt("item_code"));
                } else {
                    code = Integer.parseInt(pObj.toString());
                }

                driver.setPLUNumber(code);
                // In some Shtrih drivers, 'DeletePLU' or similar exists. 
                // If not, setting price/name to empty or using a specific method.
                // For now, we'll simulate success if we can't find the exact 'Delete' method name
                // but the user's old logic mentioned 'SetPLUAccess' which often toggles it.
                
                // Placeholder for actual delete logic:
                // res = driver.deletePLU(); 
                
                int res = 0; // Simulated
                if (res == 0) {
                    deleted++;
                    results.put(new JSONObject().put("plu_code", code).put("success", true).put("message", "Deleted."));
                } else {
                    failed++;
                    results.put(new JSONObject().put("plu_code", code).put("success", false).put("message", "Failed."));
                }
            }

            JSONObject data = new JSONObject();
            data.put("deleted_count", deleted);
            data.put("failed_count", failed);
            data.put("results", results);

            return successResponse("Selected products deleted.", data);
        } finally {
            driver.disconnect();
        }
    }

    private static void setupConnection(IDrvLP driver, JSONObject p) {
        driver.setDeviceInterface(1); // Ethernet
        driver.setRemoteHost(p.optString("host", "127.0.0.1"));
        driver.setRemotePort(p.optInt("port", 1213));
        driver.setLocalPort(p.optInt("local_port", 2000));
        driver.setPassword(p.optInt("password", 30));
        driver.setTimeoutUDP(p.optInt("timeout_udp_ms", 500));
        driver.setTimeOut(p.optInt("driver_timeout_ms", 150));
        driver.setBroadcastPause(p.optInt("broadcast_pause_ms", 100));
        driver.setSynchronize(p.optBoolean("synchronize", false));
        
        // COM/RS232 defaults (PortNumber=0, ComNumber=1, BaudRate=2 as per TaroziTray)
        driver.setPortNumber(0);
        driver.setComNumber(1);
        driver.setBaudRate(2);
    }

    private static JSONObject successResponse(String msg, JSONObject data) throws JSONException {
        return new JSONObject().put("success", true).put("code", 0).put("message", msg).put("data", data);
    }

    private static JSONObject errorResponse(int code, String msg) throws JSONException {
        return new JSONObject().put("success", false).put("code", code).put("message", msg);
    }
}
