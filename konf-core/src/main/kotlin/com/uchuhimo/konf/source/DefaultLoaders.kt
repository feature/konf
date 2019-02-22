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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.base.FlatSource
import com.uchuhimo.konf.source.base.KVSource
import com.uchuhimo.konf.source.base.MapSource
import com.uchuhimo.konf.source.env.EnvProvider
import com.uchuhimo.konf.source.hocon.HoconProvider
import com.uchuhimo.konf.source.json.JsonProvider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Default loaders for config.
 *
 * If [transform] is provided, source will be applied the given [transform] function when loaded.
 *
 * @param config parent config for loader
 * @param transform the given transformation function
 */
class DefaultLoaders(
    /**
     * Parent config for loader.
     */
    val config: Config,
    /**
     * The given transformation function.
     */
    private val transform: ((Source) -> Source)? = null
) {
    fun Provider.orMapped(): Provider =
        if (transform != null) this.map(transform) else this

    private fun Source.orMapped(): Source = transform?.invoke(this) ?: this

    /**
     * Returns default loaders applied the given [transform] function.
     *
     * @param transform the given transformation function
     * @return the default loaders applied the given [transform] function
     */
    fun mapped(transform: (Source) -> Source): DefaultLoaders = DefaultLoaders(config) {
        transform(it.orMapped())
    }

    /**
     * Returns default loaders where sources have specified additional prefix.
     *
     * @param prefix additional prefix
     * @return the default loaders where sources have specified additional prefix
     */
    fun prefixed(prefix: String): DefaultLoaders = mapped { it.withPrefix(prefix) }

    /**
     * Returns default loaders where sources are scoped in specified path.
     *
     * @param path path that is the scope of sources
     * @return the default loaders where sources are scoped in specified path
     */
    fun scoped(path: String): DefaultLoaders = mapped { it[path] }

    fun enabled(feature: Feature): DefaultLoaders = mapped { it.enabled(feature) }

    fun disabled(feature: Feature): DefaultLoaders = mapped { it.disabled(feature) }

    /**
     * Loader for HOCON source.
     */
    @JvmField
    val hocon = Loader(config, HoconProvider.orMapped())

    /**
     * Loader for JSON source.
     */
    @JvmField
    val json = Loader(config, JsonProvider.orMapped())

    /**
     * Loader for properties source.
     */
    @JvmField
    val properties = Loader(config, PropertiesProvider.orMapped())

    /**
     * Loader for map source.
     */
    @JvmField
    val map = MapLoader(config, transform)

    /**
     * Returns a child config containing values from system environment.
     *
     * @return a child config containing values from system environment
     */
    fun env(): Config = config.withSource(EnvProvider.fromEnv().orMapped())

    /**
     * Returns a child config containing values from system properties.
     *
     * @return a child config containing values from system properties
     */
    fun systemProperties(): Config = config.withSource(PropertiesProvider.fromSystem().orMapped())

    /**
     * Returns corresponding loader based on extension.
     *
     * @param extension the file extension
     * @param source the source description for error message
     * @return the corresponding loader based on extension
     */
    fun dispatchExtension(extension: String, source: String = ""): Loader =
        Loader(config, Provider.of(extension)?.orMapped()
            ?: throw UnsupportedExtensionException(source))

    /**
     * Returns a child config containing values from specified file.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file
     * @return a child config containing values from specified file
     * @throws UnsupportedExtensionException
     */
    fun file(file: File): Config = dispatchExtension(file.extension, file.name).file(file)

    /**
     * Returns a child config containing values from specified file path.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file path
     * @return a child config containing values from specified file path
     * @throws UnsupportedExtensionException
     */
    fun file(file: String): Config = file(File(file))

    /**
     * Returns a child config containing values from specified file,
     * and reloads values when file content has been changed.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: File,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default
    ): Config = dispatchExtension(file.extension, file.name)
        .watchFile(file, delayTime, unit, context)

    /**
     * Returns a child config containing values from specified file path,
     * and reloads values when file content has been changed.
     *
     * Format of the file is auto-detected from the file extension.
     * Supported file formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the file extension is unsupported.
     *
     * @param file specified file path
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from watched file
     * @throws UnsupportedExtensionException
     */
    fun watchFile(
        file: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default
    ): Config = watchFile(File(file), delayTime, unit, context)

    /**
     * Returns a child config containing values from specified url.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun url(url: URL): Config = dispatchExtension(File(url.path).extension, url.toString()).url(url)

    /**
     * Returns a child config containing values from specified url string.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url string
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun url(url: String): Config = url(URL(url))

    /**
     * Returns a child config containing values from specified url,
     * and reloads values periodically.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from specified url
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: URL,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default
    ): Config = dispatchExtension(File(url.path).extension, url.toString())
        .watchUrl(url, delayTime, unit, context)

    /**
     * Returns a child config containing values from specified url string,
     * and reloads values periodically.
     *
     * Format of the url is auto-detected from the url extension.
     * Supported url formats and the corresponding extensions:
     * - HOCON: conf
     * - JSON: json
     * - Properties: properties
     *
     * Throws [UnsupportedExtensionException] if the url extension is unsupported.
     *
     * @param url specified url string
     * @param delayTime delay to observe between every check. The default value is 5.
     * @param unit time unit of delay. The default value is [TimeUnit.SECONDS].
     * @param context context of the coroutine. The default value is [DefaultDispatcher].
     * @return a child config containing values from specified url string
     * @throws UnsupportedExtensionException
     */
    fun watchUrl(
        url: String,
        delayTime: Long = 5,
        unit: TimeUnit = TimeUnit.SECONDS,
        context: CoroutineContext = Dispatchers.Default
    ): Config = watchUrl(URL(url), delayTime, unit, context)
}

/**
 * Loader to load source from map of variant formats.
 *
 * If [transform] is provided, source will be applied the given [transform] function when loaded.
 *
 * @param config parent config
 */
class MapLoader(
    /**
     * Parent config for all child configs loading source in this loader.
     */
    val config: Config,
    /**
     * The given transformation function.
     */
    private val transform: ((Source) -> Source)? = null
) {
    private fun Source.orMapped(): Source = transform?.invoke(this) ?: this

    /**
     * Returns a child config containing values from specified hierarchical map.
     *
     * @param map a hierarchical map
     * @return a child config containing values from specified hierarchical map
     */
    fun hierarchical(map: Map<String, Any>): Config = config.withSource(MapSource(map).orMapped())

    /**
     * Returns a child config containing values from specified map in key-value format.
     *
     * @param map a map in key-value format
     * @return a child config containing values from specified map in key-value format
     */
    fun kv(map: Map<String, Any>): Config = config.withSource(KVSource(map).orMapped())

    /**
     * Returns a child config containing values from specified map in flat format.
     *
     * @param map a map in flat format
     * @return a child config containing values from specified map in flat format
     */
    fun flat(map: Map<String, String>): Config = config.withSource(FlatSource(map).orMapped())
}
