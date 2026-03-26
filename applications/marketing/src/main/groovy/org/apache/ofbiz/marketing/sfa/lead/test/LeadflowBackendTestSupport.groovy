/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ofbiz.marketing.sfa.lead.test

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

class LeadflowBackendHttpClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    private final JsonSlurper jsonSlurper = new JsonSlurper()
    private final String baseUrl

    LeadflowBackendHttpClient(String baseUrl) {
        this.baseUrl = baseUrl
    }

    Map createOpportunity(Map payload) {
        return request('POST', '/api/opportunities', payload)
    }

    Map saveRequest(String partyId, Map payload) {
        return request('PUT', "/api/opportunities/${partyId}/request", payload)
    }

    Map getOpportunity(String partyId) {
        return request('GET', "/api/opportunities/${partyId}", null)
    }

    private Map request(String method, String path, Map payload) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("${baseUrl}${path}"))
                .timeout(Duration.ofSeconds(20))
                .header('Accept', 'application/json')
        if (payload != null) {
            builder.header('Content-Type', 'application/json')
            builder.method(method, HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(payload)))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        String message = "Expected ${method} ${path} to succeed but received ${response.statusCode()} with body ${response.body()}"
        assert response.statusCode() in 200..299: message
        return (Map) jsonSlurper.parseText(response.body())
    }

}

class LeadflowBackendTestServer {

    private static final Object LOCK = new Object()
    private static volatile Object applicationContext
    private static volatile URLClassLoader applicationClassLoader
    private static volatile Integer port

    static String ensureStarted() {
        if (applicationContext != null) {
            return "http://127.0.0.1:${port}"
        }

        synchronized (LOCK) {
            if (applicationContext == null) {
                prepareRuntime()
                port = reservePort()
                Map<String, String> previous = applyProperties()
                try {
                    applicationClassLoader = new LeadflowBackendClassLoader(runtimeClasspathUrls(), LeadflowBackendTestServer.classLoader)
                    Thread thread = Thread.currentThread()
                    ClassLoader previousLoader = thread.contextClassLoader
                    thread.contextClassLoader = applicationClassLoader
                    try {
                        Class<?> appClass = applicationClassLoader.loadClass('com.example.leadflow.LeadflowBackendApplication')
                        Class<?> springApplication = applicationClassLoader.loadClass('org.springframework.boot.SpringApplication')
                        applicationContext = springApplication.getMethod('run', Class, String[].class)
                                .invoke(null, appClass, (Object) new String[0])
                    } finally {
                        thread.contextClassLoader = previousLoader
                    }
                } finally {
                    restoreProperties(previous)
                }
                waitForHealth()
            }
        }

        return "http://127.0.0.1:${port}"
    }

    static void stopIfStarted() {
        synchronized (LOCK) {
            if (applicationContext != null) {
                applicationContext.getClass().getMethod('close').invoke(applicationContext)
                applicationContext = null
            }
            if (applicationClassLoader != null) {
                applicationClassLoader.close()
                applicationClassLoader = null
            }
            port = null
        }
    }

    private static Map<String, String> applyProperties() {
        Map<String, String> previous = [
                SERVER_PORT: System.getProperty('SERVER_PORT'),
                LEADFLOW_DB_URL: System.getProperty('LEADFLOW_DB_URL'),
                LEADFLOW_DB_DRIVER: System.getProperty('LEADFLOW_DB_DRIVER'),
                LEADFLOW_CREATED_BY_USER_LOGIN_ID: System.getProperty('LEADFLOW_CREATED_BY_USER_LOGIN_ID'),
                LEADFLOW_LEAD_OWNER_PARTY_ID: System.getProperty('LEADFLOW_LEAD_OWNER_PARTY_ID'),
                'org.springframework.boot.logging.LoggingSystem': System.getProperty('org.springframework.boot.logging.LoggingSystem'),
                'server.error.include-message': System.getProperty('server.error.include-message'),
                'server.error.include-exception': System.getProperty('server.error.include-exception'),
                'server.error.include-stacktrace': System.getProperty('server.error.include-stacktrace')
        ]
        [
                SERVER_PORT: port.toString(),
                LEADFLOW_DB_URL: 'jdbc:derby:ofbiz',
                LEADFLOW_DB_DRIVER: 'org.apache.derby.jdbc.EmbeddedDriver',
                LEADFLOW_CREATED_BY_USER_LOGIN_ID: 'system',
                LEADFLOW_LEAD_OWNER_PARTY_ID: 'LeadParitySystem',
                'org.springframework.boot.logging.LoggingSystem': 'none',
                'server.error.include-message': 'always',
                'server.error.include-exception': 'true',
                'server.error.include-stacktrace': 'always'
        ].each { String key, String value ->
            System.setProperty(key, value)
        }
        return previous
    }

    private static void restoreProperties(Map<String, String> previous) {
        previous.each { String key, String value ->
            if (value == null) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, value)
            }
        }
    }

    private static int reservePort() {
        ServerSocket socket = new ServerSocket(0)
        try {
            return socket.localPort
        } finally {
            socket.close()
        }
    }

    private static URL[] runtimeClasspathUrls() {
        return Files.readAllLines(runtimeClasspathFile())
                .findAll { !it.isBlank() }
                .collect { Path.of(it).toUri().toURL() } as URL[]
    }

    private static Path runtimeClasspathFile() {
        return Path.of(ofbizHome(), 'modern', 'backend', 'build', 'parity', 'main-runtime-classpath.txt')
    }

    private static void prepareRuntime() {
        List<String> command = isWindows()
                ? ['cmd', '/c', 'modern\\backend\\gradlew.bat', '-p', 'modern/backend', 'prepareParityRuntime', '--quiet']
                : ['./modern/backend/gradlew', '-p', 'modern/backend', 'prepareParityRuntime', '--quiet']
        Process process = new ProcessBuilder(command)
                .directory(new File(ofbizHome()))
                .redirectErrorStream(true)
                .start()
        String output = process.inputStream.getText('UTF-8')
        if (!process.waitFor(5, TimeUnit.MINUTES) || process.exitValue() != 0) {
            throw new IllegalStateException("Failed to prepare modern/backend parity runtime.\n${output}")
        }
    }

    private static void waitForHealth() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()
        URI healthUri = URI.create("http://127.0.0.1:${port}/actuator/health")
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(healthUri).timeout(Duration.ofSeconds(2)).GET().build(),
                        HttpResponse.BodyHandlers.ofString()
                )
                if (response.statusCode() in 200..299 && response.body().contains('"status":"UP"')) {
                    return
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(250)
        }
        throw new IllegalStateException("modern/backend did not become healthy on ${healthUri}")
    }

    private static String ofbizHome() {
        return System.getProperty('ofbiz.home', new File('.').absolutePath)
    }

    private static boolean isWindows() {
        return System.getProperty('os.name', '').toLowerCase(Locale.ROOT).contains('win')
    }

}

class LeadflowBackendClassLoader extends URLClassLoader {

    private static final List<String> CHILD_FIRST_PREFIXES = [
            'com.example.leadflow.',
            'org.springframework.',
            'jakarta.',
            'kotlin.',
            'com.fasterxml.',
            'org.yaml.snakeyaml.',
            'ch.qos.logback.',
            'org.slf4j.',
            'reactor.',
            'io.micrometer.'
    ]

    LeadflowBackendClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent)
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name)
            if (loaded == null && isChildFirst(name)) {
                try {
                    loaded = findClass(name)
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (loaded == null) {
                loaded = super.loadClass(name, false)
            }
            if (resolve) {
                resolveClass(loaded)
            }
            return loaded
        }
    }

    private static boolean isChildFirst(String name) {
        for (String prefix : CHILD_FIRST_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

}
