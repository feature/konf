/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source

import java.io.File
import java.io.OutputStream
import java.io.StringWriter

/**
 * Save config to various output format.
 */
interface Writer {
    /**
     * Save to specified writer.
     *
     * @param writer specified writer for writing character streams
     */
    fun toWriter(writer: java.io.Writer)

    /**
     * Save to specified output stream.
     *
     * @param outputStream specified output stream of bytes
     */
    fun toOutputStream(outputStream: OutputStream)

    /**
     * Save to specified file.
     *
     * @param file specified file
     * @return a new source from specified file
     */
    fun toFile(file: File) = toOutputStream(file.outputStream())

    /**
     * Save to specified file path.
     *
     * @param file specified file path
     */
    fun toFile(file: String) = toFile(File(file))

    /**
     * Save to string.
     *
     * @return string
     */
    fun toText(): String = StringWriter().apply { toWriter(this) }.toString()

    /**
     * Save to byte array.
     *
     * @return byte array
     */
    fun toBytes(): ByteArray = toText().toByteArray()
}
