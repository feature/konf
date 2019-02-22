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

fun main(args: Array<String>) {
    val config = Config()
    config.addSpec(Server)
    run {
        val host = config[Server.host]
    }
    run {
        val host = config.get<String>("server.host")
    }
    run {
        val host = config<String>("server.host")
    }
    config.contains(Server.host)
    config.contains("server.host")
    config[Server.port] = 80
    config["server.port"] = 80
    config.unset(Server.port)
    config.unset("server.port")
    val basePort by ConfigSpec("server").required<Int>()
    config.lazySet(Server.port) { it[basePort] + 1 }
    config.lazySet("server.port") { it[basePort] + 1 }
    run {
        var port by config.property(Server.port)
        port = 9090
        check(port == 9090)
    }
    run {
        val port by config.property(Server.port)
        check(port == 9090)
    }
}
