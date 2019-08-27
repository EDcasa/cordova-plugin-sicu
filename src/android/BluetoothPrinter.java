package com.ru.cordova.printer.bluetooth;

import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Xml.Encoding;
import android.util.Base64;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import com.bxl.BXLConst;
import com.bxl.config.editor.BXLConfigLoader;
import java.io.IOException;
import java.nio.ByteBuffer;
import jpos.JposConst;
import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;
import android.os.Environment;

public class BluetoothPrinter extends CordovaPlugin {

  private static final String LOG_TAG = "BluetoothPrinter";
  BluetoothAdapter mBluetoothAdapter;
  BluetoothSocket mmSocket;
  BluetoothDevice mmDevice;
  OutputStream mmOutputStream;
  InputStream mmInputStream;
  Thread workerThread;
  byte[] readBuffer;
  int readBufferPosition;
  int counter;
  volatile boolean stopWorker;
  Bitmap bitmap;

  private BXLConfigLoader bxlConfigLoader;
  private POSPrinter posPrinter;
  private String logicalName;
  Context context;

  private int brightness = 80;
  private int compress = 3;

  public BluetoothPrinter() {
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("printImage")) {
      Context context = this.cordova.getActivity().getApplicationContext();
      String namePrint = args.getString(0);
      String addressPrint = args.getString(1);
      String pathImage = args.getString(2);
      int width = args.getString(2);
      printImageBixolon(context,namePrint, addressPrint, pathImage, width);
      return true;
    } else if (action.equals("printText")) {
      Context context = this.cordova.getActivity().getApplicationContext();
      String namePrint = args.getString(0);
      String addressPrint = args.getString(1);
      String content = args.getString(2);
      print(context,namePrint, addressPrint, content);
      return true;
    }
    return false;
  }

  /**
   * Functions of bixolon
   */

  public boolean start(final Context context, String name, String address) {

    // String name = "SICU-151";
    // String address = "74:F0:7D:E6:29:F6";
    this.context = context;

    bxlConfigLoader = new BXLConfigLoader(context);
    try {
      bxlConfigLoader.openFile();
    } catch (Exception e) {
      e.printStackTrace();
      bxlConfigLoader.newFile();
    }
    posPrinter = new POSPrinter(context);

    try {
      for (Object entry : bxlConfigLoader.getEntries()) {
        JposEntry jposEntry = (JposEntry) entry;
        bxlConfigLoader.removeEntry(jposEntry.getLogicalName());
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

    try {
      logicalName = setProductName("SICU-151");
      bxlConfigLoader.addEntry(logicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, logicalName,
          BXLConfigLoader.DEVICE_BUS_BLUETOOTH, address);

      bxlConfigLoader.saveFile();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public void printImageBixolon(final Context context, String namePrint, String addressPrint, String pathImage, int width) {
    String path = Environment.getExternalStorageDirectory().toString() + pathImage;
    this.context = context;
    if (start(this.context, namePrint, addressPrint)) {
      if (openPrinter()) {
        InputStream is = null;
        try {
          ByteBuffer buffer = ByteBuffer.allocate(4);
          buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
          buffer.put((byte) brightness);
          buffer.put((byte) compress);
          buffer.put((byte) 0x00);
          Log.v("PRINT",path);
          posPrinter.printBitmap(buffer.getInt(0), path, width , POSPrinterConst.PTR_BM_LEFT);
        } catch (JposException e) {
          e.printStackTrace();
          Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
          if (is != null) {
            try {
              is.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        closePrinter();
      }
    }
  }

  public void print(final Context context,  String name, String address, String content) {

    this.context = context;
    if (start(context, name, address)) {

      try {
        posPrinter.open(logicalName);
        posPrinter.claim(0);
        posPrinter.setDeviceEnabled(true);

        String ESC = new String(new byte[] { 0x1b, 0x7c });
        String LF = "\n";

        posPrinter.setCharacterEncoding(BXLConst.CS_858_EURO);
        posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, content + "\n");

      } catch (JposException e) {
        e.printStackTrace();
      } finally {
        try {
          posPrinter.close();
        } catch (JposException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private String setProductName(String name) {

    String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;

    return productName;
  }

  private boolean openPrinter() {
    try {
      posPrinter.open(logicalName);
      posPrinter.claim(0);
      posPrinter.setDeviceEnabled(true);
      return true;
    } catch (JposException e) {
      e.printStackTrace();

      try {
        posPrinter.close();
      } catch (JposException e1) {
        e1.printStackTrace();
      }
    }
    return false;
  }

  private void closePrinter() {
    try {
      posPrinter.close();
    } catch (JposException e) {
      e.printStackTrace();
    }
  }
}
