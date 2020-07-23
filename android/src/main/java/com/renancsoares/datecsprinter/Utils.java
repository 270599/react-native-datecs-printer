package com.renancsoares.datecsprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * Created by imrankst1221@gmail.com
 *
 */

public class Utils {

    public static byte[] getBytesTaggedText(byte[] b) throws IOException {
        boolean LEN = true;
        boolean ELE = true;
        boolean END = true;
        boolean DET = true;
        int BREAK = "br".hashCode();
        int SMALL = "s".hashCode();
        int BOLD = "b".hashCode();
        int HIGH = "h".hashCode();
        int WIDE = "w".hashCode();
        int UNDERLINE = "u".hashCode();
        int ITALIC = "i".hashCode();
        int RESET = "reset".hashCode();
        int LEFT = "left".hashCode();
        int CENTER = "center".hashCode();
        int RIGHT = "right".hashCode();
        if(b == null) {
            throw new NullPointerException("The b is null");
        } else {
            int len = b.length;
            byte[] tbuf = new byte[6 + len];
            byte toffs = 0;
            byte mode = 0;
            int pos = 0;
            int var32 = toffs + 1;
            tbuf[toffs] = 27;
            tbuf[var32++] = 33;
            tbuf[var32++] = mode;
            tbuf[var32++] = 27;
            tbuf[var32++] = 73;
            tbuf[var32++] = 0;

            for(int i = 0; i < len; ++i) {
                byte value = b[i];
                tbuf[var32++] = value;
                if(value == 123) {
                    pos = var32;
                } else if(value == 125 && pos >= 1 && var32 - 1 - 6 <= pos) {
                    int index;
                    boolean set;
                    if(tbuf[pos] == 47) {
                        set = false;
                        index = pos + 1;
                    } else {
                        set = true;
                        index = pos;
                    }

                    int tmp = 0;
                    int hashlen = var32 - 1 - index;

                    for(int j = 0; j < hashlen; ++j) {
                        int c = tbuf[index + j] & 255;
                        if(c >= 65 && c <= 90) {
                            c += 32;
                        }

                        tmp = 31 * tmp + c;
                    }

                    if(tmp == BREAK) {
                        var32 = pos - 1;
                        tbuf[var32++] = 10;
                    } else if(tmp == SMALL) {
                        if(set) {
                            mode = (byte)(mode | 1);
                        } else {
                            mode &= -2;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == BOLD) {
                        if(set) {
                            mode = (byte)(mode | 8);
                        } else {
                            mode &= -9;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == HIGH) {
                        if(set) {
                            mode = (byte)(mode | 16);
                        } else {
                            mode &= -17;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == WIDE) {
                        if(set) {
                            mode = (byte)(mode | 32);
                        } else {
                            mode &= -33;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == UNDERLINE) {
                        if(set) {
                            mode = (byte)(mode | 128);
                        } else {
                            mode = (byte)(mode & -129);
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == ITALIC) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 73;
                        tbuf[var32++] = (byte)(set?1:0);
                    } else if(tmp == RESET) {
                        mode = 0;
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 73;
                        tbuf[var32++] = 0;
                    } else if(tmp == LEFT) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 0;
                    } else if(tmp == CENTER) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 1;
                    } else if(tmp == RIGHT) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 2;
                    }
                }
            }

            byte[] var33 = new byte[var32];
            System.arraycopy(tbuf, 0, var33, 0, var33.length);
            return var33;
        }
    }

    public static Bitmap addPaddingLeftForBitmap(Bitmap bitmap, int paddingLeft) {
        Bitmap outputBitmap = Bitmap.createBitmap(bitmap.getWidth() + paddingLeft, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(WHITE);
        canvas.drawBitmap(bitmap, paddingLeft, 0, null);
        return outputBitmap;
    }

    public static Bitmap getBitmapFromURL(Context context, String urlImage) {
        try {
            Log.v("url", urlImage);
            if(getString(context, urlImage).equals("")){
                URL url = new URL(urlImage);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                myBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] b = baos.toByteArray();

                String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
                Utils.setString(context, urlImage, encodedImage);
                return myBitmap;
            } else {
                String previouslyEncodedImage = getString(context, urlImage);
                byte[] b = Base64.decode(previouslyEncodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(b, 0, b.length);
            }
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    public static String getString(Context context, String string){
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(context);
        String data = shre.getString(string, "");
        return data;
    }

    public static void setString(Context context, String string, String value){
        SharedPreferences shre = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit=shre.edit();
        edit.putString(string, value);
        edit.commit();
    }

    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";

    public static String getRandomString(final int sizeOfRandomString)
    {
        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(sizeOfRandomString);
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            Log.v("finalWidth", finalWidth + "");
            Log.v("finalHeight", finalHeight + "");
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    // UNICODE 0x23 = #
    public static final byte[] UNICODE_TEXT = new byte[] {0x23, 0x23, 0x23,
            0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,
            0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,0x23, 0x23, 0x23,
            0x23, 0x23, 0x23};

    private static String hexStr = "0123456789ABCDEF";
    private static String[] binaryArray = { "0000", "0001", "0010", "0011",
            "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011",
            "1100", "1101", "1110", "1111" };

    public static byte[] decodeBitmap(Bitmap oldBmp, int maxWidth, int maxHeight){
        Bitmap bmp = resize(oldBmp, maxWidth, maxHeight);
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        List<String> list = new ArrayList<String>(); //binaryString list
        StringBuffer sb;


        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;

        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160)
                    sb.append("0");
                else
                    sb.append("1");
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer
                .toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8
                        : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
            Log.e("decodeBitmap error", " width is too large : " + widthHexString);
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
            Log.e("decodeBitmap error", " height is too large" + heightHexString);
//            resizedbitmap1=Bitmap.createBitmap(bmp, 0,0,yourwidth, yourheight);
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString+widthHexString+heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    public static List<String> binaryListToHexStringList(List<String> list) {
        List<String> hexList = new ArrayList<String>();
        for (String binaryStr : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    public static String myBinaryStrToHexString(String binaryStr) {
        String hex = "";
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i]))
                hex += hexStr.substring(i, i + 1);
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i]))
                hex += hexStr.substring(i, i + 1);
        }

        return hex;
    }

    public static byte[] hexList2Byte(List<String> list) {
        List<byte[]> commandList = new ArrayList<byte[]>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        byte[] bytes = sysCopy(commandList);
        return bytes;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    public static byte[] sysCopy(List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }

    // Interesting method
    public static String decodeQRUrlImage(Context context, String url) {
        Bitmap bMap = getBitmapFromURL(context, url);
        String decoded = null;

        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(),
                bMap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(),
                bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new QRCodeReader();
        try {
            Result result = reader.decode(bitmap);
            decoded = result.getText();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return decoded;
    }

    public static Bitmap encodeAsBitmap(String str) throws WriterException {
        Bitmap bitmap = null;
        int width=250,height=250;

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(str, BarcodeFormat.QR_CODE, width, height);//256, 256
       /* int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();*/
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);//guest_pass_background_color
//                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);//Color.WHITE
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight());
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, buffer);
        return buffer.toByteArray();
    }

    public static byte[] decodeBitmap(Bitmap bmp){
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();

        List<String> list = new ArrayList<String>(); //binaryString list
        StringBuffer sb;


        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;

        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;

                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160)
                    sb.append("0");
                else
                    sb.append("1");
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer
                .toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8
                        : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
            Log.e("decodeBitmap error", " width is too large");
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
            Log.e("decodeBitmap error", " height is too large");
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString+widthHexString+heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}