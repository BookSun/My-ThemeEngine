package com.lewa.support.provider;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
// import android.telephony.CbSmsMessage;

public class ExtraTelephony {
    /**
     * Base columns for tables that contain text based CbSMSs.
     */
    public interface TextBasedCbSmsColumns {

        /**
         * The SIM ID which indicated which SIM the CbSMS comes from
         * Reference to Telephony.SIMx
         * <P>Type: INTEGER</P>
         */
        public static final String SIM_ID = "sim_id";

        /**
         * The channel ID of the message
         * which is the message identifier defined in the Spec. 3GPP TS 23.041
         * <P>Type: INTEGER</P>
         */
        public static final String CHANNEL_ID = "channel_id";

        /**
         * The date the message was sent
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DATE = "date";

        /**
         * Has the message been read
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String READ = "read";

        /**
         * The body of the message
         * <P>Type: TEXT</P>
         */
        public static final String BODY = "body";
        
        /**
         * The thread id of the message
         * <P>Type: INTEGER</P>
         */
        public static final String THREAD_ID = "thread_id";

        /**
         * Indicates whether this message has been seen by the user. The "seen" flag will be
         * used to figure out whether we need to throw up a statusbar notification or not.
         */
        public static final String SEEN = "seen";

        /**
         * Has the message been locked?
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String LOCKED = "locked";
    }

    public static final class Threads {
        public static final int WAPPUSH_THREAD = 2;

        public static final int CELL_BROADCAST_THREAD = 3;

        /**
         * Whether a thread is being writen or not
         * 0: normal 1: being writen
         * <P>Type: INTEGER (boolean)</P>
         */
        public static final String STATUS = "status";

         /**
         * The read message count of the thread.
         * <P>Type: INTEGER</P>
         */
        public static final String READCOUNT = "readcount";

        // No one should construct an instance of this class.
        private Threads() {
        }
    }


    public static final class Mms {
        /**
         * The sub_id to which the message belongs to
         * <p>Type: INTEGER</p>
         */
        public static final String SUB_ID = "sim_id";

        /**
         * The service center (SC) through which to send the message, if present
         * <P>Type: TEXT</P>
         */
        public static final String SERVICE_CENTER = "service_center";
    }

    public static final class Sms {
        /**
         * The sub_id to which the message belongs to
         * <p>Type: INTEGER</p>
         */
        public static final String SUB_ID = "sim_id";

        /**
         * Contains all text based SMS messages in the SMS app's inbox.
         */
        public static final class Inbox {
            /**
             * The sub_id to which the message belongs to
             * <p>Type: INTEGER</p>
             */
            public static final String SUB_ID = "sim_id";
        }
    }

    /**
     * Contains all MMS and SMS messages.
     */
    public static final class MmsSms {

        public static final Uri CONTENT_URI_QUICKTEXT =
            Uri.parse("content://mms-sms/quicktext");

        public static final class PendingMessages {
            public static final String SIM_ID = "pending_sim_id";

        }
    }

    public static final class CbSms implements BaseColumns, TextBasedCbSmsColumns {
        
        public static final Cursor query(ContentResolver cr, String[] projection) {
            return cr.query(CONTENT_URI, projection, null, null, DEFAULT_SORT_ORDER);
        }

        public static final Cursor query(ContentResolver cr, String[] projection,
                String where, String orderBy) {
            return cr.query(CONTENT_URI, projection, where,
                                         null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
        }

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI =
            Uri.parse("content://cb/messages");
        
        /**
         * The content:// style URL for "canonical_addresses" table
         */
        public static final Uri ADDRESS_URI = Uri.parse("content://cb/addresses");

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "date DESC";

        /**
         * Add an SMS to the given URI with thread_id specified.
         *
         * @param resolver the content resolver to use
         * @param uri the URI to add the message to
         * @param sim_id the id of the SIM card
         * @param channel_id the message identifier of the CB message         
         * @param date the timestamp for the message
         * @param read true if the message has been read, false if not
         * @param body the body of the message
         * @return the URI for the new message
         */
        public static Uri addMessageToUri(ContentResolver resolver,
                Uri uri, int sim_id, int channel_id, long date, 
                boolean read, String body) {
            ContentValues values = new ContentValues(5);

            values.put(SIM_ID, Integer.valueOf(sim_id));
            values.put(DATE, Long.valueOf(date));
            values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
            values.put(BODY, body);          
            values.put(CHANNEL_ID, Integer.valueOf(channel_id));
            
            return resolver.insert(uri, values);
        }

        /**
         * Contains all received CbSMS messages in the SMS app's.
         */
        public static final class Conversations
                implements BaseColumns, TextBasedCbSmsColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI =
                Uri.parse("content://cb/threads");

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = "date DESC";

            /**
             * The first 45 characters of the body of the message
             * <P>Type: TEXT</P>
             */
            public static final String SNIPPET = "snippet";

        /**
             * The number of messages in the conversation
             * <P>Type: INTEGER</P>
         */
            public static final String MESSAGE_COUNT = "msg_count";
            
            /**
             * The _id of address table in the conversation
             * <P>Type: INTEGER</P>
             */
            public static final String ADDRESS_ID = "address_id";
        }

            /**
         * Columns for the "canonical_addresses" table used by CB-SMS
             */
        public interface CanonicalAddressesColumns extends BaseColumns {
            /**
             * An address used in CB-SMS. Just a channel number
             * <P>Type: TEXT</P>
             */
            public static final String ADDRESS = "address";
        }

            /**
         * Columns for the "canonical_addresses" table used by CB-SMS
             */
        public static final class CbChannel implements BaseColumns {
            /**
             * The content:// style URL for this table
             */
            public static final Uri CONTENT_URI =
                Uri.parse("content://cb/channel");

            public static final String NAME = "name";
      
            public static final String NUMBER = "number";

            public static final String ENABLE = "enable";
            
        }
        // TODO open when using CB Message
        public static final class Intents {
           
            /**
             * Broadcast Action: A new cell broadcast sms message has been received
             * by the device. The intent will have the following extra
             * values:</p>
             *
             * <ul>
             *   <li><em>pdus</em> - An Object[] od byte[]s containing the PDUs
             *   that make up the message.</li>
             * </ul>
             *
             * <p>The extra values can be extracted using
             * {@link #getMessagesFromIntent(Intent)}.</p>
             */
            @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
            public static final String CB_SMS_RECEIVED_ACTION =
                    "android.provider.Telephony.CB_SMS_RECEIVED";                        

            /**
             * Read the PDUs out of an {@link #CB_SMS_RECEIVED_ACTION}.
             *
             * @param intent the intent to read from
             * @return an array of CbSmsMessages for the PDUs
             */
            // public static final CbSmsMessage[] getMessagesFromIntent(
            //         Intent intent) {
            //     // by Fanzhong
            //     return null;
            // }
        }
    }

    //Wap Push
    // WapPush table columns
    public static final class WapPush implements BaseColumns{
        
        //public static final Uri CONTENT_URI = 
        public static final String DEFAULT_SORT_ORDER = "date ASC";
        public static final Uri CONTENT_URI = Uri.parse("content://wappush");
        public static final Uri CONTENT_URI_SI = Uri.parse("content://wappush/si");
        public static final Uri CONTENT_URI_SL = Uri.parse("content://wappush/sl");
        public static final Uri CONTENT_URI_THREAD = Uri.parse("content://wappush/thread_id");
        
        //Database Columns
        public static final String THREAD_ID = "thread_id";
        public static final String ADDR = "address";
        public static final String SERVICE_ADDR = "service_center";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String LOCKED = "locked";
        public static final String ERROR = "error";
        public static final String DATE = "date";
        public static final String TYPE = "type";
        public static final String SIID = "siid";
        public static final String URL = "url";
        public static final String CREATE = "created";
        public static final String EXPIRATION = "expiration";
        public static final String ACTION = "action";
        public static final String TEXT = "text";
        public static final String SIM_ID = "sim_id";
        
        //
        public static final int TYPE_SI = 0;
        public static final int TYPE_SL = 1;
        
        public static final int STATUS_SEEN = 1;
        public static final int STATUS_UNSEEN = 0;
        
        public static final int STATUS_READ = 1;
        public static final int STATUS_UNREAD = 0;
        
        public static final int STATUS_LOCKED = 1;
        public static final int STATUS_UNLOCKED = 0;
    }
    
    public static final class Intents {
        private Intents() {
        // Not instantiable
        }

        /**
        * Broadcast Action: A "secret code" has been entered in the dialer. Secret codes are
        * of the form *#*#<code>#*#*. The intent will have the data URI:</p>
        *
        * <p><code>android_secret_code://&lt;code&gt;</code></p>
        */
        public static final String SECRET_CODE_ACTION =
        "android.provider.Telephony.SECRET_CODE";

        /**
        * Broadcast Action: The Service Provider string(s) have been updated. Activities or
        * services that use these strings should update their display.
        * The intent will have the following extra values:</p>
        * <ul>
        * <li><em>showPlmn</em> - Boolean that indicates whether the PLMN should be shown.</li>
        * <li><em>plmn</em> - The operator name of the registered network, as a string.</li>
        * <li><em>showSpn</em> - Boolean that indicates whether the SPN should be shown.</li>
        * <li><em>spn</em> - The service provider name, as a string.</li>
        * </ul>
        * Note that <em>showPlmn</em> may indicate that <em>plmn</em> should be displayed, even
        * though the value for <em>plmn</em> is null. This can happen, for example, if the phone
        * has not registered to a network yet. In this case the receiver may substitute an
        * appropriate placeholder string (eg, "No service").
        *
        * It is recommended to display <em>plmn</em> before / above <em>spn</em> if
        * both are displayed.
        *
        * <p>Note this is a protected intent that can only be sent
        * by the system.
        */
        public static final String SPN_STRINGS_UPDATED_ACTION =
        "android.provider.Telephony.SPN_STRINGS_UPDATED";

        public static final String EXTRA_SHOW_PLMN = "showPlmn";
        public static final String EXTRA_PLMN = "plmn";
        public static final String EXTRA_SHOW_SPN = "showSpn";
        public static final String EXTRA_SPN = "spn";
    }

    public static final int[] SIMBackgroundRes = new int[] {
        lewa.R.drawable.sim_background_blue,
        lewa.R.drawable.sim_background_orange//,
        //com.mediatek.internal.R.drawable.sim_background_green,
        //com.mediatek.internal.R.drawable.sim_background_purple
    };
}