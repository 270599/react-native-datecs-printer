package com.renancsoares.datecsprinter;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;


import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;

public class USBPrinterAdapter {
    private static USBPrinterAdapter mInstance;


    private String LOG_TAG = "RNUSBPrinter";

    private Context mContext;

    private UsbManager mUSBManager;
    private PendingIntent mPermissionIndent;
    private UsbDevice mUsbDevice ;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint mEndPoint;
    private boolean isActive = false;
    private static final String ACTION_USB_PERMISSION = "com.renancsoares.datecsprinter.USBPrinter.USB_PERMISSION";



    private USBPrinterAdapter(){}

    public static USBPrinterAdapter getInstance() {
        if(mInstance == null) {
            mInstance = new USBPrinterAdapter();
        }
        return mInstance;
    }

    private final BroadcastReceiver mUsbDeviceReceiver  = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice dev = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.d("PrinterManager", "Device Attached: " + dev.getDeviceName() + " " + dev.getProductId() + " " + dev.getVendorId());
                selectDevice(dev);
            }else if(ACTION_USB_PERMISSION.equals(action)){
                synchronized (this) {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        Toast.makeText(context, "Perangkat USB telah tersambung", Toast.LENGTH_LONG).show();
                        mUsbDevice = usbDevice;
                        openConnection();
                    }else {
                        Toast.makeText(context, "Pengguna menolak untuk mendapatkan izin perangkat USB" + usbDevice.getDeviceName(), Toast.LENGTH_LONG).show();
                    }
                }
            } else if(ACTION_USB_DEVICE_DETACHED.equals(action)){
                synchronized (this) {
                    Log.d("PrinterManager", "Printer detached!");
                    if(mUsbDevice != null) {
                        closeConnectionIfExists();
                        Toast.makeText(context, "Perangkat USB telah dicabut", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    public void init(Context reactContext) {
        this.mContext = reactContext;
        this.mUSBManager = (UsbManager) this.mContext.getSystemService(Context.USB_SERVICE);
        this.mPermissionIndent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        mContext.registerReceiver(mUsbDeviceReceiver, filter);

        Log.v(LOG_TAG, "RNUSBPrinter initialized");
    }


    public void closeConnectionIfExists() {
        if(isActive) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbInterface = null;
            mEndPoint = null;
            isActive = false;
            mUsbDeviceConnection = null;
        }
    }

    public List<UsbDevice> getDeviceList() {
        if (mUSBManager == null) {
            Toast.makeText(mContext, "USBManager is not initialized while get device list", Toast.LENGTH_LONG).show();
            return Collections.emptyList();
        }
        return new ArrayList<UsbDevice>(mUSBManager.getDeviceList().values());
    }

    public void selectDevice(UsbDevice usbDevice) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUSBManager.requestPermission(usbDevice, permissionIntent);
    }

    private boolean openConnection() {
        if(mUsbDevice == null){
            Log.e(LOG_TAG, "USB Deivce is not initialized");
            return false;
        }
        if(mUSBManager == null) {
            Log.e(LOG_TAG, "USB Manager is not initialized");
            return false;
        }

        if(mUsbDeviceConnection != null) {
            Log.i(LOG_TAG, "USB Connection already connected");
            return true;
        }

        UsbInterface usbInterface = mUsbDevice.getInterface(0);
        for(int i = 0; i < usbInterface.getEndpointCount(); i++){
            final UsbEndpoint ep = usbInterface.getEndpoint(i);
            if(ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if(ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    UsbDeviceConnection usbDeviceConnection = mUSBManager.openDevice(mUsbDevice);
                    if(usbDeviceConnection == null) {
                        selectDevice(mUsbDevice);
                        Toast.makeText(mContext, "Gagal menghubungkan USB, silahkan coba lagi.", Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "failed to open USB Connection");
                        return false;
                    }
                    if (usbDeviceConnection.claimInterface(usbInterface, true)){
                        mEndPoint = ep;
                        mUsbInterface = usbInterface;
                        mUsbDeviceConnection = usbDeviceConnection;
                        isActive = true;
                        return true;
                    }else{
                        usbDeviceConnection.close();
                        Toast.makeText(mContext, "failed to claim USB Connection", Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "failed to claim usb connection");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean printText(String text){
        final String printData = text;
        Log.v(LOG_TAG, "start to print text");
        boolean isConnected = openConnection();
        if(isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte [] bytes = printData.getBytes(Charset.forName("UTF-8"));
                    int b = mUsbDeviceConnection.bulkTransfer(mEndPoint, bytes, bytes.length, 100000);
                    Log.i(LOG_TAG, "Return Status: b-->"+b);
                }
            }).start();
            return true;
        }else{
            Log.v(LOG_TAG, "failed to connected to device");
            return false;
        }
    }

    public boolean printBytes(final byte [] bytes){
        boolean isConnected = openConnection();
        if(isConnected) {
            int b = mUsbDeviceConnection.bulkTransfer(mEndPoint, bytes, bytes.length, 0);
            return true;
        }else{
            Log.v(LOG_TAG, "failed to connected to device");
            return false;
        }
    }
}