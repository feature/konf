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

package com.uchuhimo.konf.source.json

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.ConfigForLoad
import com.uchuhimo.konf.source.SourceLoadSpec
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike

object JsonSourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
            enable(Feature.FAIL_ON_UNKNOWN_PATH)
        }.from.json.resource("source/source.json")
    }

    itBehavesLike(SourceLoadSpec)
})

object JsonSourceReloadSpec : SubjectSpek<Config>({

    subject {
        val config = Config {
            addSpec(ConfigForLoad)
        }.from.json.resource("source/source.json")
        val json = config.toJson.toText()
        Config {
            addSpec(ConfigForLoad)
        }.from.json.string(json)
    }

    itBehavesLike(SourceLoadSpec)
})
