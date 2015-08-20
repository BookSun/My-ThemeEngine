LOCAL_PATH := $(call my-dir)

# Build lewa-theme library
include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	$(call all-java-files-under, src)# \
    #$(call all-Iaidl-files-under, src)
#LOCAL_AIDL_INCLUDES:= src

LOCAL_MODULE:= lewa-theme

include $(BUILD_STATIC_JAVA_LIBRARY)
