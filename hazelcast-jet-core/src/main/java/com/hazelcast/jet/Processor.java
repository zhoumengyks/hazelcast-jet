/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet;

import com.hazelcast.logging.ILogger;

import javax.annotation.Nonnull;

/**
 * Does the computation needed to transform zero or more input data streams
 * into zero or more output streams. Each input/output stream corresponds
 * to one edge on the vertex represented by this processor. The
 * correspondence between a stream and an edge is established via the edge's
 * <em>ordinal</em>.
 * <p>
 * The special case of zero input streams applies to a <em>source</em>
 * vertex, which gets its data from the environment. The special case of
 * zero output streams applies to a <em>sink</em> vertex, which pushes its
 * data to the environment.
 * <p>
 * The processor accepts input from instances of {@link Inbox} and pushes
 * its output to an instance of {@link Outbox}.
 * <p>
 * If this processor declares itself as "cooperative" ({@link
 * #isCooperative()} returns {@code true}, the default), it should limit
 * the amount of time it spends per call because it will participate in a
 * cooperative multithreading scheme. The processing methods should also
 * limit the amount of data they output per invocation because the outbox
 * will not be emptied until the processor yields control back to its
 * caller. Specifically, {@code Outbox} has a method {@link
 * Outbox#isHighWater isHighWater()} that can be tested to see whether it's
 * time to stop pushing more data into it.  There is also a finer-grained
 * method {@link Outbox#isHighWater(int) isHighWater(ordinal)}, which tells
 * the state of an individual output bucket.
 * <p>
 * On the other hand, if the processor declares itself as "non-cooperative"
 * ({@link #isCooperative()} returns {@code false}), then each item it
 * emits to the outbox will be immediately pushed into the outbound edge's
 * queue, blocking as needed until the queue accepts it. Therefore there is
 * no limit on the number of items that can be emitted during a single
 * processor call, and there is no limit on the time taken to complete a
 * call. For example, a source processor can do all of its work in a
 * single invocation of {@link Processor#complete() complete()}, even if
 * the stream it generates is infinite.
 */
public interface Processor {

    /**
     * Initializes this processor with the outbox that the processing methods
     * must use to deposit their output items. This method will be called exactly
     * once and strictly before any calls to processing methods
     * ({@link #process(int, Inbox)} and {@link #complete()}).
     * <p>
     * The default implementation does nothing.
     */
    default void init(@Nonnull Outbox outbox, @Nonnull Context context) {
    }

    /**
     * Processes some items in the supplied inbox. Removes the items it's
     * done with. Does not remove an item until it is done with it.
     * <p>
     * The default implementation does nothing.
     *
     * @param ordinal ordinal of the edge the item comes from
     * @param inbox   the inbox containing the pending items
     */
    default void process(int ordinal, @Nonnull Inbox inbox) {
    }

    /**
     * Called after all the inputs are exhausted. If it returns {@code false}, it will be
     * invoked again until it returns {@code true}. After this method is called, no other
     * processing methods will be called on this processor.
     *
     * @return {@code true} if the completing step is now done, {@code false} otherwise.
     */
    default boolean complete() {
        return true;
    }

    /**
     * Tells whether this processor is able to participate in cooperative multithreading.
     * This means that each invocation of a processing method will take a reasonably small
     * amount of time (up to a millisecond). A cooperative processor should not attempt
     * any blocking I/O operations.
     * <p>
     * If this processor declares itself non-cooperative, it will be allocated a dedicated
     * Java thread. Otherwise it will be allocated a tasklet which shares a thread with other
     * tasklets.
     */
    default boolean isCooperative() {
        return true;
    }


    /**
     * Context passed to the processor in the {@link #init(Outbox, Processor.Context) init()} call.
     */
    interface Context {

        /**
         * Returns the current Jet instance
         */
        @Nonnull
        JetInstance jetInstance();

        /**
         *  Return a logger for the processor
         */
        @Nonnull
        ILogger logger();

        /**
         * Returns the index of the current processor among all the processors created for this vertex on this node.
         */
        int index();

        /***
         * Returns the name of the vertex associated with this processor
         */
        @Nonnull
        String vertexName();
    }
}
