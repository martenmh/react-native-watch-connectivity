package com.canvasheroes.ommetje;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

/**
 * A message that is sent between the wearable and handheld device
 */
public final class MessageObject {
    enum MessageType {
        start,
        stop,
        data,
        acknowledgement
    }

    public MessageObject(String data) throws JSONException {
        this(new JSONObject(data));
    }

    public MessageObject(JSONObject json) throws JSONException {
        sender = new Sender();
        String TAG = "MessageObject";

        JSONObject senderObj = json.getJSONObject("sender");
        sender.platform = Sender.Platform.valueOf(senderObj.getString("platform"));
        sender.direction = Sender.Direction.valueOf(senderObj.getString("direction"));
        sender.deviceId = senderObj.getString("deviceId");

        data = json.getString("data").getBytes();
        timestamp = Timestamp.valueOf(json.getString("timestamp"));
        type = MessageType.valueOf(json.getString("type"));

    }

    /**
     * Information about the sender, includes:
     * * platform
     * * direction
     */
    public static final class Sender {
        /**
         * The Direction specifies the source destination
         * handheld = handheld -> wearable
         * wearable = wearable -> handheld
         */
        public enum Direction {
            handheld,
            wearable
        }

        /**
         * The platform defines which platform of the supported platforms the message came from
         */
        public enum Platform {
            tizen,
            wearos,
            watchos
        }

        public Platform platform;
        public Direction direction;
        public String deviceId;
    }

    /**
     * Contains all information about the sender, this is:
     * platform
     * direction
     * deviceId
     **/
    Sender sender;
    /**
     * Contains the type of message
     **/
    MessageType type;

    /**
     * Contains any necessary data
     **/
    byte[] data;
    /**
     * Contains the timestamp of when the message
     **/
    Timestamp timestamp;


    MessageObject(Sender.Platform platform,
                  Sender.Direction direction,
                  String deviceId,
                  MessageType type,
                  byte[] data,
                  Timestamp timestamp) {
        this.sender = new Sender();
        this.sender.platform = platform;
        this.sender.direction = direction;
        this.sender.deviceId = deviceId;
        this.type = type;
        this.data = data;
        this.timestamp = timestamp;
    }

    static public MessageObject createStop(Sender.Platform platform,
                                           Sender.Direction direction,
                                           String deviceId,
                                           Timestamp timestamp) {
        return new MessageObject(platform, direction, deviceId, MessageType.stop, null, timestamp);
    }

    static public MessageObject createStart(Sender.Platform platform,
                                            Sender.Direction direction,
                                            String deviceId,
                                            Timestamp timestamp) {
        return new MessageObject(platform, direction, deviceId, MessageType.start, null, timestamp);
    }

    static public MessageObject createAcknowledgement(Sender.Platform platform,
                                                      Sender.Direction direction,
                                                      String deviceId,
                                                      Timestamp timestamp) {
        return new MessageObject(platform, direction, deviceId, MessageType.acknowledgement, null, timestamp);
    }

    /**
     * Creates a JSON object from the Java object
     *
     * @return a JSONObject which contains all member variables of the message
     * The JSON object has the following structure:
     * {
     * sender: {
     * platform:
     * deviceId:
     * direction:
     * }
     * type:
     * data:
     * timestamp:
     * }
     */
    public JSONObject toJSON() {
        try {
            JSONObject messageObj = new JSONObject();

            JSONObject senderObj = new JSONObject();
            senderObj.put("platform", sender.platform);
            senderObj.put("direction", sender.direction);
            senderObj.put("deviceId", sender.deviceId);

            messageObj.put("sender", senderObj);
            messageObj.put("type", type);
            messageObj.put("data", data);
            messageObj.put("timestamp", timestamp);
            return messageObj;
        } catch (Exception e) {

        }
        return new JSONObject();
    }

    public byte[] toJSONBytes() {
        return toJSON().toString().getBytes();
    }

}
