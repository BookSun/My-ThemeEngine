// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) fieldsfirst 
// Source File Name:   SimplePool.java

package com.lewa.support.util;

import java.lang.reflect.Array;

public class SimplePool {
    public static <T> PoolInstance<T> newInsance(Manager<T> manager, int size) {
        return new PoolInstance(manager, size);
    }

    public static abstract class Manager<T> {
        public abstract T createInstance();

        public void onAcquire(T element) {
        }

        public void onRelease(T element) {
        }
    }

    public static class PoolInstance<T> {
        private T[]                   mElements;
        private int                   mIndex;
        private SimplePool.Manager<T> mManager;

        public PoolInstance(SimplePool.Manager<T> manager, int size) {
            this.mManager = manager;
            Object[] elements = (Object[]) Array
                    .newInstance(Object.class, size);
            this.mElements = (T[]) elements;
            this.mIndex = -1;
        }

        public T acquire() {
            synchronized (this) {
                Object element;
                int i = this.mIndex;
                Object localObject2 = null;
                if (i >= 0) {
                    localObject2 = this.mElements[this.mIndex];
                    Object[] arrayOfObject = this.mElements;
                    int j = this.mIndex;
                    this.mIndex = (j - 1);
                    arrayOfObject[j] = null;
                }
                if (localObject2 == null) {
                    localObject2 = this.mManager.createInstance();
                }
                this.mManager.onAcquire((T) localObject2);
                return (T) localObject2;
            }
        }

        public void release(T element) {
            this.mManager.onRelease(element);
            synchronized (this) {
                if (1 + this.mIndex < this.mElements.length) {
                    Object[] arrayOfObject = this.mElements;
                    int i = 1 + this.mIndex;
                    this.mIndex = i;
                    arrayOfObject[i] = element;
                }

                return;
            }
        }
    }
}
