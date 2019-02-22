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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.properties.PropertiesProvider
import com.uchuhimo.konf.tempFileOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import spark.Service
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object DefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig)
        }.from
    }

    val item = DefaultLoadersConfig.type

    given("a loader") {
        on("load from system environment") {
            val config = subject.env()
            it("should return a config which contains value from system environment") {
                assertThat(config[item], equalTo("env"))
            }
        }
        on("load from system properties") {
            System.setProperty(DefaultLoadersConfig.qualify(DefaultLoadersConfig.type), "system")
            val config = subject.systemProperties()
            it("should return a config which contains value from system properties") {
                assertThat(config[item], equalTo("system"))
            }
        }
        on("dispatch loader based on extension") {
            it("should throw UnsupportedExtensionException when the extension is unsupported") {
                assertThat({ subject.dispatchExtension("txt") }, throws<UnsupportedExtensionException>())
            }
            it("should return the corresponding loader when the extension is registered") {
                Provider.registerExtension("txt", PropertiesProvider)
                subject.dispatchExtension("txt")
                Provider.unregisterExtension("txt")
            }
        }
        on("load from URL") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url(URL("http://localhost:${service.port()}/source.properties"))
            it("should load as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("load from URL string") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url("http://localhost:${service.port()}/source.properties")
            it("should load as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("load from file") {
            val config = subject.file(tempFileOf(propertiesContent, suffix = ".properties"))
            it("should load as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
        on("load from file path") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.file(file.path)
            it("should load as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
        on("load from watched file") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file, 1, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched file path") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file.path, 1, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(URL(url), delayTime = 1, context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL string") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(url, delayTime = 1, context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from git repository") {
            createTempDir().let { dir ->
                Git.init().apply {
                    setDirectory(dir)
                }.call().use { git ->
                    Paths.get(dir.path, "source.properties").toFile().writeText(propertiesContent)
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "init commit"
                    }.call()
                }
                val repo = dir.toURI()
                val config = subject.git(repo.toString(), "source.properties")
                it("should load as auto-detected file format") {
                    assertThat(config[item], equalTo("properties"))
                }
            }
        }
        on("load from watched git repository") {
            createTempDir(prefix = "remote_git_repo", suffix = ".git").let { dir ->
                val file = Paths.get(dir.path, "source.properties").toFile()
                Git.init().apply {
                    setDirectory(dir)
                }.call().use { git ->
                    file.writeText(propertiesContent)
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "init commit"
                    }.call()
                }
                val repo = dir.toURI()
                val config = subject.watchGit(
                    repo.toString(), "source.properties",
                    period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
                val originalValue = config[item]
                file.writeText(propertiesContent.replace("properties", "newValue"))
                Git.open(dir).use { git ->
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "update value"
                    }.call()
                }
                runBlocking(Dispatchers.Sequential) {
                    delay(TimeUnit.SECONDS.toMillis(1))
                }
                val newValue = config[item]
                it("should load as auto-detected file format") {
                    assertThat(originalValue, equalTo("properties"))
                }
                it("should load new value after file content in git repository has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
    }
})

object MappedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig["source"])
        }.from.mapped { it["source"] }
    }

    itBehavesLike(DefaultLoadersSpec)
})

object PrefixedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig.withPrefix("prefix"))
        }.from.prefixed("prefix")
    }

    itBehavesLike(DefaultLoadersSpec)
})

object ScopedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig["source"])
        }.from.scoped("source")
    }

    itBehavesLike(DefaultLoadersSpec)
})

/*
object MultipleDefaultLoadersSpec : Spek({
    on("load from multiple sources") {
        val config = Config {
            addSpec(DefaultLoadersConfig)
        }
        val item = DefaultLoadersConfig.type
        val afterLoadEnv = config.from.env()
        System.setProperty(config.nameOf(DefaultLoadersConfig.type), "system")
        val afterLoadSystemProperties = afterLoadEnv.from.systemProperties()
        val afterLoadHocon = afterLoadSystemProperties.from.hocon.string(hoconContent)
        val afterLoadJson = afterLoadHocon.from.json.string(jsonContent)
        val afterLoadProperties = afterLoadJson.from.properties.string(propertiesContent)
        val afterLoadToml = afterLoadProperties.from.toml.string(tomlContent)
        val afterLoadXml = afterLoadToml.from.xml.string(xmlContent)
        val afterLoadYaml = afterLoadXml.from.yaml.string(yamlContent)
        val afterLoadFlat = afterLoadYaml.from.map.flat(mapOf("source.test.type" to "flat"))
        val afterLoadKv = afterLoadFlat.from.map.kv(mapOf("source.test.type" to "kv"))
        val afterLoadHierarchical = afterLoadKv.from.map.hierarchical(
            mapOf("source" to
                mapOf("test" to
                    mapOf("type" to "hierarchical"))))
        it("should load the corresponding value in each layer") {
            assertThat(afterLoadEnv[item], equalTo("env"))
            assertThat(afterLoadSystemProperties[item], equalTo("system"))
            assertThat(afterLoadHocon[item], equalTo("conf"))
            assertThat(afterLoadJson[item], equalTo("json"))
            assertThat(afterLoadProperties[item], equalTo("properties"))
            assertThat(afterLoadToml[item], equalTo("toml"))
            assertThat(afterLoadXml[item], equalTo("xml"))
            assertThat(afterLoadYaml[item], equalTo("yaml"))
            assertThat(afterLoadFlat[item], equalTo("flat"))
            assertThat(afterLoadKv[item], equalTo("kv"))
            assertThat(afterLoadHierarchical[item], equalTo("hierarchical"))
        }
    }
})
*/

private object DefaultLoadersConfig : ConfigSpec("source.test") {
    val type by required<String>()
}

private const val hoconContent = "source.test.type = conf"

private const val jsonContent = """
{
  "source": {
    "test": {
      "type": "json"
    }
  }
}
"""

private const val propertiesContent = "source.test.type = properties"

private const val tomlContent = """
[source.test]
type = "toml"
"""

private val xmlContent = """
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property>
        <name>source.test.type</name>
        <value>xml</value>
    </property>
</configuration>
""".trim()

private const val yamlContent = """
source:
    test:
        type: yaml
"""
