LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := cawshell
LOCAL_SRC_FILES := caw_shell.cpp

include $(BUILD_SHARED_LIBRARY)
