package com.aantik.vmrunnerlogger;

import java.lang.reflect.Method;

public final class RePairipRestore {

    private RePairipRestore() {
    }

    public static Method get(String value) {
        try {
            int mOpen = value.indexOf('(');

            if (mOpen < 0) {
                return null;
            }

            String mHead = value.substring(0, mOpen).trim();
            int mSpace = mHead.lastIndexOf(' ');

            if (mSpace < 0) {
                return null;
            }

            String mClassAndMethod = mHead.substring(mSpace + 1).trim();
            int mDot = mClassAndMethod.lastIndexOf('.');

            if (mDot < 0) {
                return null;
            }

            String mClassName = mClassAndMethod.substring(0, mDot);
            Class<?> mClass = Class.forName(mClassName, false, RePairipRestore.class.getClassLoader());

            for (Method mMethod : mClass.getDeclaredMethods()) {
                if (mMethod.toString().equals(value) || mMethod.toGenericString().equals(value)) {
                    return mMethod;
                }
            }
        } catch (Exception mError) {
            mError.printStackTrace();
        }

        return null;
    }
}
