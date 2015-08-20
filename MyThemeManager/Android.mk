LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#add for icon customizer by fan.yang
LOCAL_JNI_SHARED_LIBRARIES := liblewa_imageutils
LOCAL_JNI_SHARED_LIBRARIES += liblewa_shell

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := LewaThemeManager
LOCAL_CERTIFICATE := platform

LOCAL_STATIC_JAVA_LIBRARIES := \
	com.lewa.themes

include $(BUILD_PACKAGE)

# including the test apk
# include $(call all-makefiles-under,$(LOCAL_PATH))
