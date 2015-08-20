LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
	com.lewa.themes \
    android-support-v13 \
    lewa-download-manager \
    lewa-support-v7-appcompat \

LOCAL_RESOURCE_DIR = \
    $(LOCAL_PATH)/res \
    vendor/lewa/apps/LewaSupportLib/actionbar_4.4/res \

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages lewa.support.v7.appcompat

LOCAL_PACKAGE_NAME := LewaThemeChooser

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
include $(BUILD_MULTI_PREBUILT)
# including the test apk
# include $(call all-makefiles-under,$(LOCAL_PATH))
