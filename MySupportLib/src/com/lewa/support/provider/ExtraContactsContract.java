package com.lewa.support.provider;

import android.net.Uri;

public final class ExtraContactsContract {
	
	 /** The authority for the contacts provider */
    public static final String AUTHORITY = "com.android.contacts";
    /** A content:// style uri to the authority for the contacts provider */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Columns in the Data table.
     *
     * @see ContactsContract.Data
     */
    protected interface DataColumns {
        /** 
         * M: Code add by Mediatek inc.
         * @hide
         */
        public static final String SIM_ASSOCIATION_ID = "sim_id";
        /** 
         * M: Code add by Mediatek inc.
         * This variable is same as SIM_ASSOCIATION_ID.
         * @hide
         * @deprecated
         */
        public static final String SIM_ID = "sim_id";
        /** 
         * M: Code add by Mediatek inc.
         * @hide
         */
        public static final String IS_ADDITIONAL_NUMBER = "is_additional_number";
        
          /** The last time (in milliseconds) this {@link Data} was used. */
        public static final String LAST_TIME_USED = "last_time_used";

        /** The number of times the referenced {@link Data} has been used. */
        public static final String TIMES_USED = "times_used";
    }

    public final static class Data implements DataColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Data() {}
    }

    protected interface RawContactsColumns {
        /**
         * An opaque value that indicate contact store location. 
         * "-1", indicates phone contacts
         * others, indicate sim id of a sim contact
         * 
         * @hide
         */
        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

        /**
         * For a SIM/USIM contact, this value is its index in the relative SIM
         * card.
         * 
         * @hide
         */
        public static final String INDEX_IN_SIM = "index_in_sim";

        /**
         * Whether the contact should always be sent to voicemail for VT. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         * 
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";

        /**
         * Whether the contact should always be sent to voicemail for SIP. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         * 
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";

         /**
         * M:
         * For SIM contact's flag, SDN's contacts value is 1, ADN's contacts value is 0
         * card.
         * 
         * @hide
         */
        public static final String IS_SDN_CONTACT = "is_sdn_contact";

    }

	 protected interface DeletedContactsColumns {

        /**
         * A reference to the {@link ContactsContract.Contacts#_ID} that was deleted.
         * <P>Type: INTEGER</P>
         */
        public static final String CONTACT_ID = "contact_id";

        /**
         * Time (milliseconds since epoch) that the contact was deleted.
         */
        public static final String CONTACT_DELETED_TIMESTAMP = "contact_deleted_timestamp";
    }
    
     /**
     * Columns in the Data_Usage_Stat table
     */
    protected interface DataUsageStatColumns {
        /** The last time (in milliseconds) this {@link Data} was used. */
        public static final String LAST_TIME_USED = "last_time_used";

        /** The number of times the referenced {@link Data} has been used. */
        public static final String TIMES_USED = "times_used";
    }

    
    public static final class RawContacts implements RawContactsColumns {
        /**
         * This utility class cannot be instantiated
         */
        private RawContacts() {
        }

        public static final int INDICATE_PHONE = -1;

        /**
         * Indicate flag: Indicate it is a SIM card contact.
         * @hide
         */
        public static final int INDICATE_SIM = 0;
        
        
        /**
         * Indicate flag: Indicate it is a SIM1 card contact.
         * @hide
         */
        public static final int INDICATE_SIM1 = 1;
        
        
        /**
         * Indicate flag: Indicate it is a SIM2 card contact.
         * @hide
         */
        public static final int INDICATE_SIM2 = 2;

        /**
         * Indicate flag: Indicate it is a USIM card contact.
         * @hide
         */
        public static final int INDICATE_USIM = 10;
        
        /**
         * Indicate flag: Indicate it is a USIM1 card contact.
         * @hide
         */
        public static final int INDICATE_USIM1 = 11;
        
        /**
         * Indicate flag: Indicate it is a USIM2 card contact.
         * @hide
         */
        public static final int INDICATE_USIM2 = 12;

        /**
         * M: 
         * time stamp that is updated whenever version changes.
         * <P>
         * Type: INTEGER
         * </P>
         * 
         * @hide
         */
        public static final String TIMESTAMP = "timestamp";
    }

    protected interface ContactsColumns {
        /**
         * Photo file ID of the full-size sticker photo(large-head).  If present, this will be used to populate
         * {@link #PHOTO_STICKER_URI}.  The ID can also be used with
         * {@link ContactsContract.DisplayStickerPhoto#CONTENT_URI} to create a URI to the sticker photo.
         * If this is present, {@link #PHOTO_ID} is also guaranteed to be populated.
         *
         * <P>Type: INTEGER</P>
         */
        public static final String STICKER_PHOTO_FILE_ID = "sticker_photo_file_id";

        /**
         * A URI that can be used to retrieve a stiker(big-head) of the contact's photo.
        * If STICK_PHOTO_ID is not null, STICK_PHOTO_URI  shall not be null 
        * (but not necessarily vice versa).
        
         *
         * <P>Type: TEXT</P>
         */
         public static final String STICKER_PHOTO_URI = "sticker_photo_uri";

        /**
         * An opaque value that indicate contact store location. 
         * "-1", indicates phone contacts
         * others, indicate sim id of a sim contact
         * 
         * @hide
         */

        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

        /**
         * For a SIM/USIM contact, this value is its index in the relative SIM
         * card.
         * 
         * @hide
         */
        public static final String INDEX_IN_SIM = "index_in_sim";

        /**
         * Whether the contact should always be sent to voicemail for VT. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         * 
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";

        /**
         * Whether the contact should always be sent to voicemail for SIP. If
         * missing, defaults to false.
         * <P>
         * Type: INTEGER (0 for false, 1 for true)
         * </P>
         * 
         * @hide
         */
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";

        /**
         * To filter the Contact for Widget
         * 
         * @hide
         */
        public static final String FILTER = "filter";
        /**
         * To filter the Contact for Widget
         * 
         * @hide
         */
        public static final int FILTER_NONE = 0;
        /**
         * To filter the Contact for Widget
         * 
         * @hide
         */
        public static final int FILTER_WIDGET = 1;

        /**
         * For SIM contact's flag, SDN's contacts value is 1, ADN's contacts value is 0
         * card.
         * 
         * @hide
         */
        public static final String IS_SDN_CONTACT = "is_sdn_contact";  
        
           /**
         * Timestamp (milliseconds since epoch) of when this contact was last updated.  This
         * includes updates to all data associated with this contact including raw contacts.  Any
         * modification (including deletes and inserts) of underlying contact data are also
         * reflected in this timestamp.
         */
        public static final String CONTACT_LAST_UPDATED_TIMESTAMP =
                "contact_last_updated_timestamp";
    }

    public static class Contacts implements ContactsColumns {
        /**
         * This utility class cannot be instantiated
         */
        private Contacts()  {}
    }

    public static final class Intents {
        
        /**
         * A broadcast action which is sent when any change has been made to the profile, such
         * as the profile name or the picture.  A receiver must have
         * the android.permission.READ_PROFILE permission.
         *
         * @hide
         */
        public static final String ACTION_PROFILE_CHANGED =
                "android.provider.Contacts.PROFILE_CHANGED";

        public static final class Insert {
            /**
             * M: 
             * The extra field for the sip address flag.
             * <P>Type: boolean</P>
             * 
             * @hide
             */
           public static final String SIP_ADDRESS = "sip_address";
        }
    }
    public static final class PhoneLookup implements  ContactsColumns {
        /**
         * This utility class cannot be instantiated
         */
        private PhoneLookup() {}

       /**
        * Boolean parameter that is used to look up a SIP address.
        *
        * @hide
        */
        public static final String QUERY_PARAMETER_SIP_ADDRESS = "sip";
    }
    
      /**
     * Container for definitions of common data types stored in the {@link ContactsContract.Data}
     * table.
     */
    public static final class CommonDataKinds {
        /**
         * This utility class cannot be instantiated
         */
        private CommonDataKinds() {}
        
        /**
         * A special class of data items, used to refer to types of data that can be used to attempt
         * to start communicating with a person ({@link Phone} and {@link Email}). Note that this
         * is NOT a separate data kind.
         *
         * This URI allows the ContactsProvider to return a unified result for data items that users
         * can use to initiate communications with another contact. {@link Phone} and {@link Email}
         * are the current data types in this category.
         */
        public static final class Contactables {
           
            /**
             * A boolean parameter for {@link Data#CONTENT_URI}.
             * This specifies whether or not the returned data items should be filtered to show
             * data items belonging to visible contacts only.
             */
            public static final String VISIBLE_CONTACTS_ONLY = "visible_contacts_only";
        }
	}
    
        /**
     * Constants for the deleted contact table.  This table holds a log of deleted contacts.
     * <p>
     * Log older than {@link #DAYS_KEPT_MILLISECONDS} may be deleted.
     */
    public static final class DeletedContacts implements DeletedContactsColumns {

        /**
         * This utility class cannot be instantiated
         */
        private DeletedContacts() {
        }

        /**
         * The content:// style URI for this table, which requests a directory of raw contact rows
         * matching the selection criteria.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI,
                "deleted_contacts");

        /**
         * Number of days that the delete log will be kept.  After this time, delete records may be
         * deleted.
         *
         * @hide
         */
        private static final int DAYS_KEPT = 30;

        /**
         * Milliseconds that the delete log will be kept.  After this time, delete records may be
         * deleted.
         */
        public static final long DAYS_KEPT_MILLISECONDS = 1000L * 60L * 60L * 24L * (long)DAYS_KEPT;
    }
    
      /**
     * Columns for dialer search displayed information
     * 
     * @hide
     */
    public static final class DialerSearch {

            /**
             * Not instantiable.
             * @hide
             */
            public static final String VTCALL = "vds_vtcall";
            public static final String MATCHED_DATA_OFFSETS = "matched_data_offsets";//For results
            public static final String MATCHED_NAME_OFFSETS = "matched_name_offsets";
            private DialerSearch() {
        }
    }


       public static final class GroupRing {
        /**
         * This utility class cannot be instantiated
         */
        private GroupRing() {
        }

        /**
         * The content:// style URI for this table, which requests the contact entry
         * representing the user's personal profile data.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "group_ringtones");

         /**
         * Lewa add 
         * URI for a group ringtone associated with the contact. If null or missing,
         * the default ringtone is used. custom ringtone > group ringtone > default ringtone
         * <P>Type: TEXT (URI to the ringtone)</P>
         */
        public static final String GROUP_RINGTONE = "group_ringtone";
    }

}
