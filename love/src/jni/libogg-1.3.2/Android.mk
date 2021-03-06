LOCAL_PATH:= $(call my-dir)

# libogg
include $(CLEAR_VARS)

LOCAL_MODULE    := libogg
LOCAL_CFLAGS    := -fexceptions -g -Dlinux -DHAVE_GCC_DESTRUCTOR=1 -DOPT_GENERIC -DREAL_IS_FLOAT
LOCAL_CPPFLAGS  := ${LOCAL_CFLAGS}

LOCAL_C_INCLUDES  :=  \
	${LOCAL_PATH}/include
		
LOCAL_SRC_FILES := \
	$(subst $(LOCAL_PATH)/,, \
	${LOCAL_PATH}/src/framing.c \
	${LOCAL_PATH}/src/bitwise.c )

# $(info local includes $(LOCAL_C_INCLUDES))

include $(BUILD_STATIC_LIBRARY)


