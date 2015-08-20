package com.lewa.support.provider;

public final class ExtraCallLog {

    public static class Calls{
          /**
         * M: Code added by Mediatek inc.
         * {@hide}
         */
        public static final String SIM_ID = "simid";
        
        /**
         * M: Code added by Mediatek inc.
         * {@hide}
         */
        public static final String VTCALL = "vtcall";

        /**
         * M:
         * save call log corresponding phone number ID
         * {@hide}
         */
        public static final String DATA_ID = "data_id";
        
        /**
         * M: 
         * save raw contact id of a call log corresponding to phone number 
         * {@hide}
         */
        public static final String RAW_CONTACT_ID = "raw_contact_id";   
        
        /**
         * M: 
         * save IP prefix of a call log  
         * {@hide}
         */
        public static final String IP_PREFIX = "ip_prefix";

        /**
        * duration type for active.
        * @hide
        */
        public static final int DURATION_TYPE_ACTIVE = 0;

        /**
         * duration type for call out time.
         * @hide
         */
        public static final int DURATION_TYPE_CALLOUT = 1;


        /**
         *Q:
         * The type of the duration
         * <P>Type: INTEGER (long)</P>
         * @hide
         */
        public static final String DURATION_TYPE = "duration_type";
        
        /**
         * Query parameter used to limit the number of call logs returned.
         * <p>
         * TYPE: integer
         */
        public static final String LIMIT_PARAM_KEY = "limit";
        
        /**
         * Query parameter used to specify the starting record to return.
         * <p>
         * TYPE: integer
         */
        public static final String OFFSET_PARAM_KEY = "offset";

          /**
         * The number presenting rules set by the network.
         *
         * <p>
         * Allowed values:
         * <ul>
         * <li>{@link #PRESENTATION_ALLOWED}</li>
         * <li>{@link #PRESENTATION_RESTRICTED}</li>
         * <li>{@link #PRESENTATION_UNKNOWN}</li>
         * <li>{@link #PRESENTATION_PAYPHONE}</li>
         * </ul>
         * </p>
         *
         * <P>Type: INTEGER</P>
         */
        public static final String NUMBER_PRESENTATION = "presentation";

        
        /** Number is allowed to display for caller id. */
        public static final int PRESENTATION_ALLOWED = 1;
        /** Number is blocked by user. */
        public static final int PRESENTATION_RESTRICTED = 2;
        /** Number is not specified or unknown by network. */
        public static final int PRESENTATION_UNKNOWN = 3;
        /** Number is a pay phone. */
        public static final int PRESENTATION_PAYPHONE = 4;

    }
}
