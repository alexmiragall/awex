package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.exceptions.AllFailException;

import java.util.Collection;

class AnyOfPromise<Result, Progress> extends AwexPromise<Result, Progress> {

    private final Collection<Promise<Result, Progress>> mPromises;
    private final Exception[] mExceptions;

    private int mFailedPromises = 0;

    @SuppressWarnings("unchecked")
    public AnyOfPromise(Awex awex, Collection<Promise<Result, Progress>> promises) {
        super(awex);

        mExceptions = new Exception[promises.size()];
        mPromises = promises;

        DoneCallback<Result> mDoneCallback = buildDoneCallback();
        CancelCallback mCancelCallback = buildCancelCallback();

        int i = 0;
        for (Promise<Result, Progress> promise : mPromises) {
            final int promiseIndex = i;
            promise.done(mDoneCallback).fail(new FailCallback() {
                @Override
                public void onFail(Exception ex) {
                    synchronized (AnyOfPromise.this) {
                        if (getState() == STATE_PENDING) {
                            mExceptions[promiseIndex] = ex;
                            mFailedPromises++;
                            if (mFailedPromises == mPromises.size()) {
                                reject(new AllFailException(mExceptions));
                            }
                        }
                    }
                }
            }).cancel(mCancelCallback);

            i++;
        }
    }

    private DoneCallback<Result> buildDoneCallback() {
        return new DoneCallback<Result>() {
            @Override
            public void onDone(Result result) {
                synchronized (AnyOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        resolve(result);
                    }
                }
            }
        };
    }

    private CancelCallback buildCancelCallback() {
        return new CancelCallback() {
            @Override
            public void onCancel() {
                synchronized (AnyOfPromise.this) {
                    if (getState() == STATE_PENDING) {
                        cancelTask(false);
                    }
                }
            }
        };
    }

    @Override
    public void cancelTask(boolean mayInterrupt) {
        synchronized (this) {
            super.cancelTask(mayInterrupt);

            for (Promise<Result, Progress> promise : mPromises) {
                promise.cancelTask(mayInterrupt);
            }
        }
    }

}