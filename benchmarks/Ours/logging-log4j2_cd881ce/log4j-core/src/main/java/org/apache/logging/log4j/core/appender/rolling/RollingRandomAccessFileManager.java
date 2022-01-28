/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.NullOutputStream;

/**
 * Extends RollingFileManager but instead of using a buffered output stream, this class uses a {@code ByteBuffer} and a
 * {@code RandomAccessFile} to do the I/O.
 */
public class RollingRandomAccessFileManager extends RollingFileManager {
    /**
     * The default buffer size.
     */
    public static final int DEFAULT_BUFFER_SIZE = 256 * 1024;

    private static final RollingRandomAccessFileManagerFactory FACTORY = new RollingRandomAccessFileManagerFactory();

    private final boolean isImmediateFlush;
    private RandomAccessFile randomAccessFile;
    private final ByteBuffer buffer;
    private final ThreadLocal<Boolean> isEndOfBatch = new ThreadLocal<>();

    public RollingRandomAccessFileManager(final RandomAccessFile raf, final String fileName, final String pattern,
            final OutputStream os, final boolean append, final boolean immediateFlush, final int bufferSize,
            final long size, final long time, final TriggeringPolicy policy, final RolloverStrategy strategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final boolean writeHeader) {
        super(fileName, pattern, os, append, size, time, policy, strategy, advertiseURI, layout, bufferSize,
                writeHeader);
        this.isImmediateFlush = immediateFlush;
        this.randomAccessFile = raf;
        isEndOfBatch.set(Boolean.FALSE);
        this.buffer = ByteBuffer.allocate(bufferSize);
        writeHeader();
    }

    /**
     * Writes the layout's header to the file if it exists.
     */
    private void writeHeader() {
        if (layout == null) {
            return;
        }
        final byte[] header = layout.getHeader();
        if (header == null) {
            return;
        }
        try {
            // write to the file, not to the buffer: the buffer may not be empty
            randomAccessFile.write(header, 0, header.length);
        } catch (final IOException e) {
            logError("unable to write header", e);
        }
    }

    public static RollingRandomAccessFileManager getRollingRandomAccessFileManager(final String fileName,
            final String filePattern, final boolean isAppend, final boolean immediateFlush, final int bufferSize,
            final TriggeringPolicy policy, final RolloverStrategy strategy, final String advertiseURI,
            final Layout<? extends Serializable> layout) {
        return (RollingRandomAccessFileManager) getManager(fileName, new FactoryData(filePattern, isAppend,
                immediateFlush, bufferSize, policy, strategy, advertiseURI, layout), FACTORY);
    }

    public Boolean isEndOfBatch() {
        return isEndOfBatch.get();
    }

    public void setEndOfBatch(final boolean endOfBatch) {
        this.isEndOfBatch.set(Boolean.valueOf(endOfBatch));
    }

    @Override
    protected synchronized void write(final byte[] bytes, int offset, int length, final boolean immediateFlush) {
        super.write(bytes, offset, length, immediateFlush); // writes to dummy output stream, needed to track file size

        int chunk = 0;
        do {
            if (length > buffer.remaining()) {
                flush();
            }
            chunk = Math.min(length, buffer.remaining());
            buffer.put(bytes, offset, chunk);
            offset += chunk;
            length -= chunk;
        } while (length > 0);

        if (immediateFlush || isImmediateFlush || isEndOfBatch.get() == Boolean.TRUE) {
            flush();
        }
    }

    @Override
    protected void createFileAfterRollover() throws IOException {
        this.randomAccessFile = new RandomAccessFile(getFileName(), "rw");
        if (isAppend()) {
            randomAccessFile.seek(randomAccessFile.length());
        }
        writeHeader();
    }

    @Override
    public synchronized void flush() {
        buffer.flip();
        try {
            randomAccessFile.write(buffer.array(), 0, buffer.limit());
        } catch (final IOException ex) {
            final String msg = "Error writing to RandomAccessFile " + getName();
            throw new AppenderLoggingException(msg, ex);
        }
        buffer.clear();
    }

    @Override
    public synchronized void close() {
        flush();
        try {
            randomAccessFile.close();
        } catch (final IOException e) {
            logError("unable to close RandomAccessFile", e);
        }
    }

    /**
     * Returns the buffer capacity.
     * 
     * @return the buffer size
     */
    @Override
    public int getBufferSize() {
        return buffer.capacity();
    }

    /**
     * Factory to create a RollingRandomAccessFileManager.
     */
    private static class RollingRandomAccessFileManagerFactory implements
            ManagerFactory<RollingRandomAccessFileManager, FactoryData> {

        /**
         * Create the RollingRandomAccessFileManager.
         *
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return a RollingFileManager.
         */
        @Override
        public RollingRandomAccessFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }

            if (!data.append) {
                file.delete();
            }
            final long size = data.append ? file.length() : 0;
            final long time = file.exists() ? file.lastModified() : System.currentTimeMillis();

            final boolean writeHeader = !data.append || !file.exists();
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(name, "rw");
                if (data.append) {
                    final long length = raf.length();
                    LOGGER.trace("RandomAccessFile {} seek to {}", name, length);
                    raf.seek(length);
                } else {
                    LOGGER.trace("RandomAccessFile {} set length to 0", name);
                    raf.setLength(0);
                }
                return new RollingRandomAccessFileManager(raf, name, data.pattern, NullOutputStream.NULL_OUTPUT_STREAM,
                        data.append, data.immediateFlush, data.bufferSize, size, time, data.policy, data.strategy,
                        data.advertiseURI, data.layout, writeHeader);
            } catch (final IOException ex) {
                LOGGER.error("Cannot access RandomAccessFile {}) " + ex);
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (final IOException e) {
                        LOGGER.error("Cannot close RandomAccessFile {}", name, e);
                    }
                }
            }
            return null;
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final String pattern;
        private final boolean append;
        private final boolean immediateFlush;
        private final int bufferSize;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Create the data for the factory.
         *
         * @param pattern The pattern.
         * @param append The append flag.
         * @param immediateFlush
         * @param bufferSize
         * @param policy
         * @param strategy
         * @param advertiseURI
         * @param layout
         */
        public FactoryData(final String pattern, final boolean append, final boolean immediateFlush,
                final int bufferSize, final TriggeringPolicy policy, final RolloverStrategy strategy,
                final String advertiseURI, final Layout<? extends Serializable> layout) {
            this.pattern = pattern;
            this.append = append;
            this.immediateFlush = immediateFlush;
            this.bufferSize = bufferSize;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
        }

        public TriggeringPolicy getTriggeringPolicy()
        {
            return this.policy;
        }

        public RolloverStrategy getRolloverStrategy()
        {
            return this.strategy;
        }
    }

    @Override
    public void updateData(final Object data)
    {
        final FactoryData factoryData = (FactoryData) data;
        setRolloverStrategy(factoryData.getRolloverStrategy());
        setTriggeringPolicy(factoryData.getTriggeringPolicy());
    }
}
