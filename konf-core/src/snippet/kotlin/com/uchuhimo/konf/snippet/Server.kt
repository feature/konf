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

package com.uchuhimo.konf.snippet

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec

data class Server(val host: String, val port: Int) {
    constructor(config: Config) : this(config[Server.host], config[Server.port])

    fun start() {}

    companion object : ConfigSpec("server") {
        val host by optional("0.0.0.0", description = "host IP of server")
        val port by required<Int>(description = "port of server")
        val nextPort by lazy { config -> config[port] + 1 }
    }
}
