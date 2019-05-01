package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

    public static final String TAG = SimpleDynamoProvider.class.getSimpleName();
    public static final String[] REMOTE_PORTS = new String[]{"11108", "11112", "11116", "11120", "11124"};
    public static final String[] REMOTE_PORTS_TRUE = new String[]{"5554", "5556", "5558", "5560", "5562"};
    public static final int SERVER_PORT = 10000;
    public static String CURRENT_NODE;
    public static String CURRENT_NODE_HASH;

    public static String PREDECESSOR_NODE;
    public static String PREDECESSOR_NODE_HASH;
    public static String SUCCESSOR_NODE_1;
    public static String SUCCESSOR_NODE_1_HASH;
    public static String SUCCESSOR_NODE_2;
    public static String SUCCESSOR_NODE_2_HASH;
    public static Map<String, Integer> timeStamps;

    SimpleDynamoDbHelper mDbHelper;


    public static final String MASTER_NODE = "11108";


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        incrementPortTimeStamp(CURRENT_NODE);
        String key = values.getAsString("key");
        String value = values.getAsString("value");
        for (String port : REMOTE_PORTS_TRUE) {
            if (shouldMessageBeAdded(port, key)) {
                incrementPortTimeStamp(CURRENT_NODE);
                sendInsertMessage(values, port);
                break;
            }
        }


        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long newRowId = db.replace(SimpleDynamoContract.KeyValueEntry.TABLE_NAME, null, mDbHelper.formatToSqlContentValues(values));
        Log.v("insert", values.toString());
        return null;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SimpleDynamoDbHelper(getContext());
        Context context = getContext();
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        CURRENT_NODE = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d(TAG, "Current node is: " + CURRENT_NODE);
        CURRENT_NODE_HASH = genHash(String.valueOf(Integer.valueOf(CURRENT_NODE) / 2));
        initTimeStamps();
        // new a ServerTask()
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            new JoinChordAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.v("query", selection);
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static class ClientAsyncTask extends AsyncTask<Void, Void, Void> {

        String recipient;
        String message;
        Util.MessageType messageType;

        public ClientAsyncTask(String recipient, String message, Util.MessageType messageType) {
            this.recipient = recipient;
            this.message = message;
            this.messageType = messageType;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (messageType.equals(Util.MessageType.MESSAGE_TYPE_INSERT_KEY_VALUE)) {
                Log.d(TAG, "Sending insert request to COORDINATOR_NODE.");
                Socket socket;
                PrintWriter printWriter;

                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.valueOf(recipient));
                    printWriter = new PrintWriter(socket.getOutputStream(), true);
                    printWriter.println(message);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String receivedMessage = bufferedReader.readLine();
                    if (Util.getMessageType(receivedMessage) == Util.MessageType.MESSAGE_TYPE_JOIN_CHORD_ACK) {
                        bufferedReader.close();
                        printWriter.close();
                        socket.close();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }




    private static String genHash(String input) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static void initTimeStamps() {
        timeStamps = new HashMap<String, Integer>();
        for (int i = 0; i <= REMOTE_PORTS_TRUE.length; i++) {
            timeStamps.put(REMOTE_PORTS_TRUE[0], 0);
        }
    }

    public static void initSuccessorsAndPredecessors() {
        switch (Integer.parseInt(CURRENT_NODE)) {
            case 5554:
                PREDECESSOR_NODE = "5556";
                SUCCESSOR_NODE_1 = "5558";
                SUCCESSOR_NODE_2 = "5560";
                break;
            case 5558:
                PREDECESSOR_NODE = "5554";
                SUCCESSOR_NODE_1 = "5560";
                SUCCESSOR_NODE_2 = "5562";
                break;
            case 5560:
                PREDECESSOR_NODE = "5558";
                SUCCESSOR_NODE_1 = "5562";
                SUCCESSOR_NODE_2 = "5556";
                break;
            case 5562:
                PREDECESSOR_NODE = "5560";
                SUCCESSOR_NODE_1 = "5556";
                SUCCESSOR_NODE_2 = "5554";
                break;
            case 5556:
                PREDECESSOR_NODE = "5562";
                SUCCESSOR_NODE_1 = "5554";
                SUCCESSOR_NODE_2 = "5558";
                break;
        }
        PREDECESSOR_NODE_HASH = genHash(PREDECESSOR_NODE);
        SUCCESSOR_NODE_1_HASH = genHash(SUCCESSOR_NODE_1);
        SUCCESSOR_NODE_2_HASH = genHash(SUCCESSOR_NODE_2);
    }

    public static void incrementPortTimeStamp(String port) {
        timeStamps.put(port, timeStamps.get(port) + 1);
    }

    public static boolean shouldMessageBeAdded(String port, String key) {
        //TODO: Implement
        return true;
    }

    private void sendInsertMessage(String key, String value) {
        String message = CURRENT_NODE + "." + timeStamps. + Util.INSERT_KEY_VALUE_SEPARATOR + key + "." + value;

    }
}
