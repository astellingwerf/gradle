/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.tooling.internal.consumer.loader

import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.logging.ProgressLoggerFactory
import org.gradle.messaging.actor.ActorFactory
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.tooling.internal.consumer.Distribution
import org.gradle.tooling.internal.consumer.connection.AdaptedConnection
import org.gradle.tooling.internal.consumer.connection.BuildActionRunnerBackedConsumerConnection
import org.gradle.tooling.internal.consumer.connection.InternalConnectionBackedConsumerConnection
import org.gradle.tooling.internal.protocol.*
import org.gradle.util.ClasspathUtil
import org.gradle.util.GradleVersion
import org.gradle.util.TemporaryFolder
import org.junit.Rule
import org.slf4j.Logger
import spock.lang.Specification

class DefaultToolingImplementationLoaderTest extends Specification {
    @Rule public final TemporaryFolder tmpDir = new TemporaryFolder()
    Distribution distribution = Mock()
    ProgressLoggerFactory loggerFactory = Mock()

    def usesMetaInfServiceToDetermineFactoryImplementation() {
        given:
        def loader = new DefaultToolingImplementationLoader()
        distribution.getToolingImplementationClasspath(loggerFactory) >> new DefaultClassPath(
                getToolingApiResourcesDir(connectionImplementation),
                ClasspathUtil.getClasspathForClass(TestConnection.class),
                ClasspathUtil.getClasspathForClass(ActorFactory.class),
                ClasspathUtil.getClasspathForClass(Logger.class),
                ClasspathUtil.getClasspathForClass(GroovyObject.class),
                getVersionResourcesDir(),
                ClasspathUtil.getClasspathForClass(GradleVersion.class))

        when:
        def adaptedConnection = loader.create(distribution, loggerFactory, true)

        then:
        adaptedConnection.delegate.class != connectionImplementation //different classloaders
        adaptedConnection.delegate.class.name == connectionImplementation.name

        and:
        adaptedConnection.class == adapter

        where:
        connectionImplementation      | adapter
        TestConnection.class          | BuildActionRunnerBackedConsumerConnection.class
        TestOldConnection.class       | InternalConnectionBackedConsumerConnection.class
        TestEvenOlderConnection.class | AdaptedConnection.class
    }

    private getToolingApiResourcesDir(Class implementation) {
        tmpDir.file("META-INF/services/org.gradle.tooling.internal.protocol.ConnectionVersion4") << implementation.name
        return tmpDir.dir;
    }

    private getVersionResourcesDir() {
        return ClasspathUtil.getClasspathForResource(getClass().classLoader, "org/gradle/build-receipt.properties")
    }

    def failsWhenNoImplementationDeclared() {
        ClassLoader cl = new ClassLoader() {}
        def loader = new DefaultToolingImplementationLoader(cl)

        when:
        loader.create(distribution, loggerFactory, true)

        then:
        UnsupportedVersionException e = thrown()
        e.message == "The specified <dist-display-name> is not supported by this tooling API version (${GradleVersion.current().version}, protocol version 4)"
        _ * distribution.getToolingImplementationClasspath(loggerFactory) >> new DefaultClassPath()
        _ * distribution.displayName >> '<dist-display-name>'
    }
}

class TestConnection implements ConnectionVersion4, BuildActionRunner {
    def <T> BuildResult<T> run(Class<T> type, BuildParameters parameters) {
        throw new UnsupportedOperationException()
    }

    void stop() {
        throw new UnsupportedOperationException()
    }

    ConnectionMetaDataVersion1 getMetaData() {
        throw new UnsupportedOperationException()
    }

    ProjectVersion3 getModel(Class<? extends ProjectVersion3> type, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }

    void executeBuild(BuildParametersVersion1 buildParameters, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }
}

class TestOldConnection implements InternalConnection {
    def <T> T getTheModel(Class<T> type, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }

    void stop() {
        throw new UnsupportedOperationException()
    }

    ConnectionMetaDataVersion1 getMetaData() {
        throw new UnsupportedOperationException()
    }

    ProjectVersion3 getModel(Class<? extends ProjectVersion3> type, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }

    void executeBuild(BuildParametersVersion1 buildParameters, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }
}

class TestEvenOlderConnection implements ConnectionVersion4 {
    void stop() {
        throw new UnsupportedOperationException()
    }

    ConnectionMetaDataVersion1 getMetaData() {
        throw new UnsupportedOperationException()
    }

    ProjectVersion3 getModel(Class<? extends ProjectVersion3> type, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }

    void executeBuild(BuildParametersVersion1 buildParameters, BuildOperationParametersVersion1 operationParameters) {
        throw new UnsupportedOperationException()
    }
}
