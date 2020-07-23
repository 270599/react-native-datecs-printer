package com.renancsoares.datecsprinter;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import android.content.Context;
import android.graphics.Bitmap;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

import com.datecs.api.printer.ProtocolAdapter;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import okhttp3.internal.Util;

public class RNDatecsPrinterModule extends ReactContextBaseJavaModule implements LifecycleEventListener{

	// Debugging
	private static final Boolean DEBUG = true;
	private static final String LOG_TAG = "RNDatecsPrinterModule";

	//Members
	private Printer mPrinter;
	private ProtocolAdapter mProtocolAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket mmSocket;
	private BluetoothDevice mmDevice;
	private OutputStream mmOutputStream;
	private OutputStream newOutputPrint;
	private InputStream mmInputStream;
	private Context context;
	private USBPrinterAdapter usbAdapter;
	private boolean isConnected = false;

	private final ProtocolAdapter.PrinterListener mChannelListener = new ProtocolAdapter.PrinterListener(){
		@Override
		public void onPaperStateChanged(boolean hasNoPaper) {
			if (!hasNoPaper) {
				showToast("Kertas tersedia");
			} else {
				disconnect();
				showToast("Kertas habis");
			}
		}

		@Override
		public void onThermalHeadStateChanged(boolean overheated) {
			if (overheated) {
				showToast("Printer terlalu panas");
			}
		}

		@Override
		public void onBatteryStateChanged(boolean lowBattery) {
			if (lowBattery) {
				showToast("Baterai printer hampir habis");
			}
		}
	};

	public RNDatecsPrinterModule(ReactApplicationContext reactContext) {
		super(reactContext);
		context = reactContext;
		reactContext.addLifecycleEventListener(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.usbAdapter = USBPrinterAdapter.getInstance();
		this.usbAdapter.init(reactContext);
	}

	@Override
	public void onHostResume() {
	    // Activity `onResume`
	}

	@Override
	public void onHostPause() {
		disconnect();
	}

	@Override
	public void onHostDestroy() {
		disconnect();
	}

	@Override
	public String getName() {
		return "DatecsPrinter";
	}

	/**
	 * Get list of paired devices
	 *
	 * @param promise
	 */
	@ReactMethod
	public void getPairedDevices(Promise promise){
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		WritableArray list = new WritableNativeArray();
		for(BluetoothDevice device : pairedDevices){
			WritableMap resultData = new WritableNativeMap();
			String name;
			String savedPrinterName = Utils.getString(getReactApplicationContext(), "savedPrinterName");
			String savedPrinterAddress = Utils.getString(getReactApplicationContext(), "savedPrinterAddress");
			if(device.getAddress().equals(savedPrinterAddress)){
				name = savedPrinterName;
			}else{
				name = device.getName() == null ? Utils.getRandomString(5) : device.getName();
			}
			resultData.putString("name", name);
			resultData.putString("address", device.getAddress());
			resultData.putBoolean("isUsed", device.getAddress().equals(savedPrinterAddress));
			list.pushMap(resultData);
		}

		if(list.size() > 0){
			promise.resolve(list);
		}else{
			promise.reject("Silahkan cek konektivitas printer bluetooth dengan handphone anda di menu Setting - Bluetooth");
		}
	}

	@ReactMethod
	public void setSavedPrinter(String printerName, String printerAddress, Promise promise){
		Utils.setString(getReactApplicationContext(), "savedPrinterName", printerName);
		Utils.setString(getReactApplicationContext(), "savedPrinterAddress", printerAddress);
		promise.resolve(null);
	}

	@ReactMethod
	public void getSavedPrinter(Promise promise){
		String savedPrinterName = Utils.getString(getReactApplicationContext(), "savedPrinterName");
		if(!savedPrinterName.equals("")){
			promise.resolve(savedPrinterName);
		}else{
			promise.resolve(null);
		}
	}

	@ReactMethod
	public void connect(String typePrinter, Promise promise) throws IOException {
		try {
			if(typePrinter.equals("bluetooth")){
				String savedPrinterName = Utils.getString(getReactApplicationContext(), "savedPrinterName");
				String savedPrinterAddress = Utils.getString(getReactApplicationContext(), "savedPrinterAddress");
				if(!savedPrinterName.equals("")) {
					Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
					mmDevice = null;
					for (BluetoothDevice device : pairedDevices) {
						if(device.getAddress().equals(savedPrinterAddress)){
							mmDevice = device;
						}
					}
					if (mmDevice == null) {
						showToast("Silahkan cek konektivitas printer bluetooth dengan handphone anda di menu Setting - Bluetooth");
						return;
					}

					UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
					mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
					mmSocket.connect();
					mmOutputStream = mmSocket.getOutputStream();
					mmInputStream = mmSocket.getInputStream();

					try {
						initializePrinter(mmInputStream, mmOutputStream, promise);
					} catch (Exception e) {
						promise.reject("Gagal menghubungkan printer: " + e.getMessage());
						return;
					}
				}else{
					promise.reject("Silahkan pilih printer pada menu printer terlebih dahulu.");
				}
			} else if(typePrinter.equals("otg")) {
				if(usbAdapter.getDeviceList().size() > 0){
					countDown();
					promise.resolve("PRINTER_INITIALIZED");
				} else {
					promise.reject("Silahkan hubungkan printer USB anda.");
					return;
				}
			}
		}catch(Exception e){
			promise.reject("Gagal menghubungkan printer: " + e.getMessage());
		}
	}

	protected void countDown(){
		isConnected = true;
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				disconnect();
			}
		}, 10000);
	}

	/**
     * Connect printer
     *
     * @param promise
     */
	protected void initializePrinter(InputStream inputStream, OutputStream outputStream, final Promise promise) throws IOException {
		mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
		if (mProtocolAdapter.isProtocolEnabled()) {
			mProtocolAdapter.setPrinterListener(mChannelListener);

			final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);

			mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
		} else {
			mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
		}


		mPrinter.setConnectionListener(new Printer.ConnectionListener() {
			@Override
			public void onDisconnect() {
				promise.reject("Whoops!!!");
			}
		});
		countDown();
		promise.resolve("PRINTER_INITIALIZED");
	}

	/**
     * Get Printer status
     *
     * @param promise
     */
	@ReactMethod
	public void getStatus(Promise promise) {
		try {
			int status = mPrinter.getStatus();
			promise.resolve(status);
		} catch (Exception e) {
			promise.reject("Gagal menghubungkan printer: " + e.getMessage());
		}
	}
	/**
     * Print custom text
     *
     * @param text
     * @param promise
     */
	@ReactMethod
	public void printText(String typePrinter, String text, Promise promise) {
		String charset = "ISO-8859-1";
		try {
			if (typePrinter.equals("bluetooth")) {
				if(!isConnected){
					connect(typePrinter, promise);
				}
				mPrinter.printTaggedText(text, charset);
				mPrinter.flush();
				promise.resolve("PRINTED");
			}else if (typePrinter.equals("otg")) {
				if(!isConnected){
					connect(typePrinter, promise);
				}
				byte[] data = Utils.getBytesTaggedText(text.getBytes(charset));
				usbAdapter.printBytes(data);
				promise.resolve("PRINTED");
			}
		} catch (Exception e) {
			promise.reject("Gagal menghubungkan printer: " + e.getMessage());
		}
	}

	@ReactMethod
	public void printImage(String typePrinter, String urlImage, int maxWidth, int maxHeight, Promise promise) {
		try {
			Bitmap bmp = Utils.getBitmapFromURL(context, urlImage);
			if(bmp!=null){
				if (typePrinter.equals("bluetooth")) {
					if(!isConnected){
						connect(typePrinter, promise);
					}
					byte[] command = Utils.decodeBitmap(bmp, maxWidth, maxHeight);
					mmOutputStream.write(PrinterCommands.ALIGN_CENTER);
					mmOutputStream.write(command);
					promise.resolve("PRINTED");
				}else if (typePrinter.equals("otg")) {
					if(!isConnected) {
						connect(typePrinter, promise);
					}
					byte[] command = Utils.decodeBitmap(bmp, maxWidth, maxHeight);
					usbAdapter.printBytes(command);
					promise.resolve("PRINTED");
				}
			}else{
				promise.reject("Gagal menghubungkan printer: the file isn't exists");
			}
		} catch (Exception e) {
			promise.reject("Gagal menghubungkan printer: " + e.getMessage());
		}
	}

	@ReactMethod
	public void printQR(String typePrinter, String urlImage, int maxWidth, int maxHeight, Promise promise) {
		try {
			String code = Utils.decodeQRUrlImage(context, urlImage);
			if(code !=null){
				if (typePrinter.equals("bluetooth")) {
					if(!isConnected){
						connect(typePrinter, promise);
					}
					Bitmap bmp = Utils.encodeAsBitmap(code);
					if (bmp != null) {
						byte[] command = Utils.decodeBitmap(bmp);
						mmOutputStream.write(PrinterCommands.ALIGN_CENTER);
						mmOutputStream.write(command);
						promise.resolve("PRINTED");
					} else {
						promise.reject("Gagal menghubungkan printer: encode bitmap failed");
					}
				}else if (typePrinter.equals("otg")) {
					if(!isConnected) {
						connect(typePrinter, promise);
					}
					Bitmap bmp = Utils.encodeAsBitmap(code);
					if (bmp != null) {
						byte[] command = Utils.decodeBitmap(bmp);
						usbAdapter.printBytes(command);
						promise.resolve("PRINTED");
					} else {
						promise.reject("Gagal menghubungkan printer: encode bitmap failed");
					}
				}
			}else{
				promise.reject("Gagal menghubungkan printer: code not exists");
			}
		} catch (Exception e) {
			promise.reject("Gagal menghubungkan printer: " + e.getMessage());
		}
	}

	/**
	 * Disconnect printer
	 *
	 */
	public void disconnect(){
		try {
			isConnected = false;
			mPrinter.close();
			mProtocolAdapter.close();
			mmSocket.close();
			mmOutputStream.close();
			newOutputPrint.close();
			mmInputStream.close();
		} catch (Exception e) {
		}
	}

	private void showToast(final String text) {
		Toast.makeText(getReactApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}
}
