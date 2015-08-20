LOCAL_PATH := $(call my-dir)

#ACQUIRE_PROJECT := q39
#export ACQUIRE_PROJECT
#$(warning $(CUST_PRJECT_NAME))
#$(warning $(TARGET_PRODUCT))
#$(error '****************begin***********')
### call all subdir Android.mk for prebuild
#ifeq ($(CUST_PRJECT_NAME), $(TARGET_PRODUCT)) 
include $(call first-makefiles-under,$(LOCAL_PATH))
#endif
