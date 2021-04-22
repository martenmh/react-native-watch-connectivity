package com.canvasheroes.ommetje;

/* Android */

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
/* Annotations */
import androidx.annotation.NonNull;
/* Android messaging */
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
/* Collections */
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A bound service for the communication between the Wearable and Handheld
 */
public class MessageService extends Service
    implements MessageClient.OnMessageReceivedListener {

    // Create a thread pool of a single thread
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * Interface that is required to be extended from when
     * wanting to receive messages from the MessageService
     */
    public interface OnMessageReceivedListener {
        void onMessageReceived(@NonNull String data);
    }

    List<OnMessageReceivedListener> listeners;

    String TAG = "MessageService";

    /**
     * The Message Path is the path used for messages between the handheld and wearable
     */
    private final String MESSAGE_PATH = "/ommetje_messages";

    /**
     * An instance of the MessageBinder
     */
    private final IBinder binder = new MessageBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC(Inter Process Communication).
     */
    public class MessageBinder extends Binder {
        /**
         * Get an instance of MessageService so users of the service can call public methods
         *
         * @return the instance of MessageService
         */
        public MessageService getService() {
            return MessageService.this;
        }
    }

    /**
     * Called when bindService() is used
     *
     * @param intent the Intent that was used
     * @return an IBinder interface
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Get all connected Wearable devices
     * If no connected nodes are returned it means the connection was not able to be established
     *
     * @return A collection of all connected nodes
     */
    public Collection<Node> getNodes() {
        HashSet<Node> results = new HashSet<Node>();
        Task<List<Node>> nodeListTask = Wearable.getNodeClient(this).getConnectedNodes();
        try {
            List<Node> nodes = Tasks.await(nodeListTask);
            results.addAll(nodes);
        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
        return results;
    }

    boolean isConnected() {
        return !getNodes().isEmpty();
    }

    Collection<Node> checkConnectivity() {
        Collection<Node> nodes = getNodes();
        if (nodes.isEmpty()) {
            // start activity
            return null;
        } else {
            return nodes;
        }
    }

    /**
     * Sends a message to all connected nodes
     *
     * @param data to send to all nodes
     */
    public void sendMessageToAll(byte[] data) {
        // Execute on separate thread
        executorService.execute(() -> {
            // Get all connected nodes
            Collection<Node> nodes = getNodes();
            for (Node node : nodes) {
                Task<Integer> sendTask =
                    Wearable.getMessageClient(this).sendMessage(
                        node.getId(), MESSAGE_PATH, data);
                sendTask.addOnSuccessListener(it -> {
                    Log.d(TAG, "SUCCESS");
                });
                sendTask.addOnFailureListener(it -> {
                    Log.d(TAG, "FAILURE");
                });
            }
        });
    }

    /**
     * Send a message to a single node, nodes can be received by using getNodes
     *
     * @param node to send the data to
     * @param data to bent sent
     */
    public void sendMessage(Node node, byte[] data) {
        Task<Integer> sendTask =
            Wearable.getMessageClient(this).sendMessage(
                node.getId(), MESSAGE_PATH, data);
        sendTask.addOnSuccessListener(it -> {
            Log.d(TAG, "SUCCESS");
        });
        sendTask.addOnFailureListener(it -> {
            Log.d(TAG, "FAILURE");
        });
    }

    /**
     * Add a listener that is called when a message is received
     *
     * @param listener a class that extends MessageService.OnMessageReceived
     */
    public void addMessageReceiveListener(@NonNull MessageService.OnMessageReceivedListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a previously added listener
     *
     * @param listener a class that extends MessageService.OnMessageReceived
     */
    public void removeMessageReceiveListener(@NonNull MessageService.OnMessageReceivedListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listeners = new ArrayList<OnMessageReceivedListener>();
        try {
            // Instantiate clients
            Wearable.getMessageClient(this).addListener(this);   // onMessageReceived
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            // Show a message upon Service
            Toast.makeText(this, "MessageService Starting", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Wearable.getMessageClient(this).removeListener(this);     // onMessageReceived
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when a message is received, calls all listeners with the data as a string
     *
     * @param messageEvent the messageEvent that is received containing the data, path and source node
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(MESSAGE_PATH)) {
            if (listeners == null) {
                Log.w(TAG, "No listener has been set");
                return;
            }
            for (OnMessageReceivedListener listener : listeners) {
                listener.onMessageReceived(new String(messageEvent.getData(), StandardCharsets.UTF_8));
            }
        }
    }

}
