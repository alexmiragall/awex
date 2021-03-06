package com.raycoarana.awex;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

class AwexTaskQueue {

    private static final int INITIAL_CAPACITY = 4;

    private final PriorityBlockingQueue<Task> mTaskQueue;
    private final AtomicInteger mWaitersCount = new AtomicInteger();
    private final AtomicInteger mSize = new AtomicInteger();
    private final int mId;
    private boolean mDie = false;

    public AwexTaskQueue(int id) {
        mId = id;
        mTaskQueue = new PriorityBlockingQueue<>(INITIAL_CAPACITY, new TaskPriorityComparator());
    }

    public Task take(Worker worker) throws InterruptedException {
        try {
            if (mDie) {
                throw new IllegalStateException("Queue is die!");
            }

            mWaitersCount.incrementAndGet();
            Task task = mTaskQueue.take();
            mSize.decrementAndGet();
            task.setWorker(worker);
            return task;
        } finally {
            mWaitersCount.decrementAndGet();
        }
    }

    public synchronized void insert(Task task) {
        if (mDie) {
            throw new IllegalStateException("Queue is die!");
        }

        mTaskQueue.offer(task);
        mSize.incrementAndGet();
    }

    public synchronized <Result, Progress> boolean remove(Task<Result, Progress> task) {
        if (mDie) {
            throw new IllegalStateException("Queue is die!");
        }

        boolean removed = mTaskQueue.remove(task);
        if (removed) {
            mSize.decrementAndGet();
        }
        return removed;
    }

    public int waiters() {
        return mWaitersCount.get();
    }

    public int size() {
        return mSize.get();
    }

    public int getId() {
        return mId;
    }

    public synchronized void destroy() {
        synchronized (this) {
            mDie = true;
        }

        for (Task task : mTaskQueue) {
            task.getPromise().cancelTask();
        }
        mTaskQueue.clear();
    }
}