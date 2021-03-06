package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.AlwaysCallback;
import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.ProgressCallback;
import com.raycoarana.awex.callbacks.ThenCallback;
import com.raycoarana.awex.callbacks.UIAlwaysCallback;
import com.raycoarana.awex.callbacks.UICancelCallback;
import com.raycoarana.awex.callbacks.UIDoneCallback;
import com.raycoarana.awex.callbacks.UIFailCallback;
import com.raycoarana.awex.callbacks.UIProgressCallback;

import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AwexPromiseTest extends BasePromiseTest {

    private static final Integer SOME_RESULT = 666;
    private static final Integer SOME_DEFAULT_RESULT = 999;
    private static final float SOME_PROGRESS = 0.6f;

    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private static final Exception REJECT_EXCEPTION = new Exception();

    @Mock
    private DoneCallback<Integer> mDoneCallback;

    @Mock
    private UIDoneCallback<Integer> mUIDoneCallback;

    @Mock
    private FailCallback mFailCallback;

    @Mock
    private UIFailCallback mUIFailCallback;

    @Mock
    private ProgressCallback<Float> mProgressCallback;

    @Mock
    private UIProgressCallback<Float> mUIProgressCallback;

    @Mock
    private AlwaysCallback mAlwaysCallback;

    @Mock
    private UIAlwaysCallback mUIAlwaysCallback;

    @Mock
    private CancelCallback mCancelCallback;

    @Mock
    private UICancelCallback mUICancelCallback;

    private AwexPromise<Integer, Float> mPromise;

    @Test
    public void shouldCreateAValidPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);

        assertEquals(Promise.STATE_PENDING, mPromise.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetResultOfCancelledPromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancelTask();
        mPromise.getResult();
    }

    @Test
    public void shouldReturnDefaultValueWhenGetResultOrDefaultOfCancelledPromise() throws InterruptedException {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancelTask();
        assertEquals(SOME_DEFAULT_RESULT, mPromise.getResultOrDefault(SOME_DEFAULT_RESULT));
    }

    @Test
    public void shouldResolveThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);

        assertEquals(Promise.STATE_RESOLVED, mPromise.getState());
        assertEquals(SOME_RESULT, mPromise.getResult());
        assertEquals(SOME_RESULT, mPromise.getResultOrDefault(SOME_DEFAULT_RESULT));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToResolveTwiceThePromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);
        mPromise.resolve(SOME_RESULT);
    }

    @Test
    public void shouldExecuteDoneCallbackAddedBeforeResolveThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.done(mDoneCallback)
                .always(mAlwaysCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteDoneCallbackAddedAfterResolveThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);
        mPromise.done(mDoneCallback)
                .always(mAlwaysCallback);

        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteUIDoneCallbackAddedBeforeResolveThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.done(mUIDoneCallback)
                .always(mUIAlwaysCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mThreadHelper, times(2)).post(any(Runnable.class));
        verify(mUIDoneCallback).onDone(SOME_RESULT);
        verify(mUIAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteUIDoneCallbackInCurrentThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);
        mPromise.done(mUIDoneCallback)
                .always(mUIAlwaysCallback);

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mAwex, never()).submit(any(Runnable.class));
        verify(mUIDoneCallback).onDone(SOME_RESULT);
        verify(mUIAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteDoneCallbackInBackgroundThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);
        mPromise.done(mDoneCallback)
                .always(mAlwaysCallback);

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mAwex, times(2)).submit(any(Runnable.class));
        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteUIDoneCallbackAddedAfterResolveThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.resolve(SOME_RESULT);
        mPromise.done(mUIDoneCallback)
                .always(mUIAlwaysCallback);

        verify(mThreadHelper, times(2)).post(any(Runnable.class));
        verify(mUIDoneCallback).onDone(SOME_RESULT);
        verify(mUIAlwaysCallback).onAlways();
    }

    @Test
    public void shouldContinueExecutingOtherDoneCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.done(buildFailingDoneCallback());
        mPromise.done(mDoneCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    private DoneCallback<Integer> buildFailingDoneCallback() {
        DoneCallback<Integer> callback = mock(DoneCallback.class);
        doThrow(Exception.class).when(callback).onDone(anyInt());
        return callback;
    }

    @Test
    public void shouldContinueExecutingOtherAlwaysCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.always(buildFailingAlwaysCallback());
        mPromise.always(mAlwaysCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mAlwaysCallback).onAlways();
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    private AlwaysCallback buildFailingAlwaysCallback() {
        AlwaysCallback callback = mock(AlwaysCallback.class);
        doThrow(Exception.class).when(callback).onAlways();
        return callback;
    }

    @Test
    public void shouldExecuteFailCallbackAddedBeforeRejectThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.fail(mFailCallback)
                .always(mAlwaysCallback);
        mPromise.reject(REJECT_EXCEPTION);

        verify(mFailCallback).onFail(REJECT_EXCEPTION);
        verify(mAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteFailCallbackAddedAfterRejectThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(REJECT_EXCEPTION);
        mPromise.fail(mFailCallback)
                .always(mAlwaysCallback);

        verify(mFailCallback).onFail(REJECT_EXCEPTION);
        verify(mAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteUIFailCallbackAddedBeforeRejectThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.fail(mUIFailCallback)
                .always(mUIAlwaysCallback);
        mPromise.reject(REJECT_EXCEPTION);

        verify(mThreadHelper, times(2)).post(any(Runnable.class));
        verify(mUIFailCallback).onFail(REJECT_EXCEPTION);
        verify(mUIAlwaysCallback).onAlways();
    }

    @Test
    public void shouldExecuteUIFailCallbackAddedAfterRejectThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(REJECT_EXCEPTION);
        mPromise.fail(mUIFailCallback)
                .always(mUIAlwaysCallback);

        verify(mThreadHelper, times(2)).post(any(Runnable.class));
        verify(mUIFailCallback).onFail(REJECT_EXCEPTION);
        verify(mUIAlwaysCallback).onAlways();
    }

    @Test
    public void shouldContinueExecutingOtherFailCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.fail(buildFailingFailCallback());
        mPromise.fail(mFailCallback);
        mPromise.reject(new RuntimeException());

        verify(mFailCallback).onFail(any(Exception.class));
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    private FailCallback buildFailingFailCallback() {
        FailCallback callback = mock(FailCallback.class);
        doThrow(Exception.class).when(callback).onFail(any(Exception.class));
        return callback;
    }

    @Test
    public void shouldExecuteFailCallbackInBackgroundThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(new RuntimeException());
        mPromise.fail(mFailCallback)
                .always(mAlwaysCallback);

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mAwex, times(2)).submit(any(Runnable.class));
        verify(mFailCallback).onFail(any(RuntimeException.class));
        verify(mAlwaysCallback).onAlways();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldRejectThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(new IndexOutOfBoundsException());

        assertEquals(Promise.STATE_REJECTED, mPromise.getState());
        mPromise.getResult();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToRejectTwiceThePromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(new IndexOutOfBoundsException());
        mPromise.reject(new IndexOutOfBoundsException());
    }

    @Test
    public void shouldGetDefaultValueWhenPromiseIsRejected() throws InterruptedException {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.reject(new IndexOutOfBoundsException());

        assertEquals(SOME_DEFAULT_RESULT, mPromise.getResultOrDefault(SOME_DEFAULT_RESULT));
    }

    @Test
    public void shouldExecuteCancelCallbackWhenCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancel(mCancelCallback);
        mPromise.cancelTask();

        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackAfterCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancelTask();
        mPromise.cancel(mCancelCallback);

        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldContinueExecutingOtherCancelCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancel(buildFailingCancelCallback());
        mPromise.cancel(mCancelCallback);
        mPromise.cancelTask();

        verify(mCancelCallback).onCancel();
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    private CancelCallback buildFailingCancelCallback() {
        CancelCallback callback = mock(CancelCallback.class);
        doThrow(Exception.class).when(callback).onCancel();
        return callback;
    }

    @Test
    public void shouldExecuteUICancelCallbackWhenCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancel(mUICancelCallback);
        mPromise.cancelTask();

        verify(mThreadHelper).post(any(Runnable.class));
        verify(mUICancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackInBackgroundThreadWhenCancellingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancel(mCancelCallback);
        mPromise.cancelTask();

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mAwex).submit(any(Runnable.class));
        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackInBackgroundThreadAfterCancellingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.cancelTask();
        mPromise.cancel(mCancelCallback);

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mAwex).submit(any(Runnable.class));
        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteProgressCallback() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.progress(mProgressCallback);
        mPromise.notifyProgress(SOME_PROGRESS);

        verify(mProgressCallback).onProgress(SOME_PROGRESS);
    }

    @Test
    public void shouldExecuteASecondProgressCallbackWhenFirstProgressCallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.progress(new ProgressCallback<Float>() {
            @Override
            public void onProgress(Float progress) {
                throw new RuntimeException("Some error!");
            }
        });
        mPromise.progress(mProgressCallback);
        mPromise.notifyProgress(SOME_PROGRESS);

        verify(mProgressCallback).onProgress(SOME_PROGRESS);
    }

    @Test
    public void shouldExecuteProgressCallbackInUIThread() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.progress(mUIProgressCallback);
        mPromise.notifyProgress(SOME_PROGRESS);

        verify(mThreadHelper).post(any(Runnable.class));
        verify(mUIProgressCallback).onProgress(SOME_PROGRESS);
    }

    @Test
    public void shouldExecuteProgressCallbackInCurrentThreadIsUIThreadIsCurrentThread() {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mTask);
        mPromise.progress(mUIProgressCallback);
        mPromise.notifyProgress(SOME_PROGRESS);

        verify(mThreadHelper, never()).post(any(Runnable.class));
        verify(mUIProgressCallback).onProgress(SOME_PROGRESS);
    }

    @Test
    public void shouldCreateOrPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        Promise<Integer, Float> anyOfPromise = mPromise.or(new AwexPromise<Integer, Float>(mAwex, mTask));

        assertThat(anyOfPromise, instanceOf(OrPromise.class));
    }

    @Test
    public void shouldCreateAllOfPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mTask);
        Promise<Collection<Integer>, Float> allOfPromise = mPromise.and(new AwexPromise<Integer, Float>(mAwex, mTask));

        assertThat(allOfPromise, instanceOf(AllOfPromise.class));
    }

    @Test
    public void shouldPipeResolveData() {
        setUpAwex();

        AwexPromise<Integer, Void> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Void> pipedPromise = new AwexPromise<>(mAwex, mTask);
        pipedPromise.done(mDoneCallback);

        originalPromise.pipe(pipedPromise);
        originalPromise.resolve(SOME_RESULT);

        verify(mDoneCallback).onDone(SOME_RESULT);
    }

    @Test
    public void shouldPipeProgressData() throws Exception {
        setUpAwex();

        AwexPromise<Integer, Float> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Float> pipedPromise = new AwexPromise<>(mAwex, mTask);
        pipedPromise.progress(mProgressCallback);

        originalPromise.pipe(pipedPromise);
        originalPromise.notifyProgress(SOME_PROGRESS);

        verify(mProgressCallback).onProgress(SOME_PROGRESS);
    }

    @Test
    public void shouldPipeFailureData() throws Exception {
        setUpAwex();

        AwexPromise<Integer, Float> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Float> pipedPromise = new AwexPromise<>(mAwex, mTask);
        pipedPromise.fail(mFailCallback);

        Exception someException = new RuntimeException();

        originalPromise.pipe(pipedPromise);
        originalPromise.reject(someException);

        verify(mFailCallback).onFail(someException);
    }

    @Test
    public void shouldPipeCancelEvent() throws Exception {
        setUpAwex();

        AwexPromise<Integer, Float> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Float> pipedPromise = new AwexPromise<>(mAwex, mTask);
        pipedPromise.cancel(mCancelCallback);

        originalPromise.pipe(pipedPromise);
        originalPromise.cancelTask();

        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldNotCancelOriginalPromiseWhenCancelPipedPromise() {
        setUpAwex();

        AwexPromise<Integer, Void> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Void> pipedPromise = new AwexPromise<>(mAwex, mTask);

        originalPromise.pipe(pipedPromise);
        pipedPromise.cancelTask();

        assertFalse(originalPromise.isCancelled());
        assertTrue(pipedPromise.isCancelled());
    }

    @Test
    public void shouldNotFailWhenCancelPipedPromiseAndThenOriginalPromiseIsResolved() {
        setUpAwex();

        AwexPromise<Integer, Void> originalPromise = new AwexPromise<>(mAwex, mTask);
        AwexPromise<Integer, Void> pipedPromise = new AwexPromise<>(mAwex, mTask);

        originalPromise.pipe(pipedPromise);
        pipedPromise.cancelTask();
        originalPromise.resolve(SOME_RESULT);
    }

    @Test
    public void shouldExecuteSubsequentThenOperatorInOrder() {
        setUpAwex();

        AwexPromise<Integer, Void> originalPromise = new AwexPromise<>(mAwex, mTask);
        final AwexPromise<Integer, Void> firstOperationPromise = new AwexPromise<>(mAwex, mTask);
        final AwexPromise<Integer, Void> secondOperationPromise = new AwexPromise<>(mAwex, mTask);

        final List<Integer> values = new ArrayList<>();

        originalPromise.then(new ThenCallback<Integer, Integer, Void>() {
            @Override
            public Promise<Integer, Void> then(Integer result) {
                values.add(result);
                return firstOperationPromise;
            }
        }).then(new ThenCallback<Integer, Integer, Void>() {
            @Override
            public Promise<Integer, Void> then(Integer result) {
                values.add(result);
                return secondOperationPromise;
            }
        }).done(new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                values.add(result);
            }
        });

        secondOperationPromise.resolve(3);
        firstOperationPromise.resolve(2);
        originalPromise.resolve(1);

        assertArrayEquals(new Integer[]{1, 2, 3}, values.toArray(new Integer[3]));
    }

}