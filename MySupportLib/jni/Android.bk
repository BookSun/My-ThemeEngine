LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := \
    liblog \
    libm \
    libjnigraphics

LOCAL_MODULE      := liblewa_imageutils
LOCAL_SRC_FILES   := lewa_imageutils.c
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS      += -ffast-math -O3 -funroll-loops -Wno-unused-parameter -Wno-implicit-function-declaration
LOCAL_ARM_MODE    := arm

include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := \
    liblog

LOCAL_MODULE      := liblewa_shell
LOCAL_SRC_FILES   := lewa_shell.c cp/cp.c  cp/utils.c

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS      += -ffast-math -O3 -funroll-loops -Wno-unused-parameter -Wno-implicit-function-declaration
LOCAL_ARM_MODE    := arm

include $(BUILD_SHARED_LIBRARY)
