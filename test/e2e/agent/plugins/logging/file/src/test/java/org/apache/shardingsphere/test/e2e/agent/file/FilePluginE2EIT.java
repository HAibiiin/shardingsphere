/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.e2e.agent.file;

import org.apache.shardingsphere.test.e2e.agent.common.AgentTestActionExtension;
import org.apache.shardingsphere.test.e2e.agent.common.cases.AgentE2ETestCasesLoader;
import org.apache.shardingsphere.test.e2e.agent.common.env.AgentE2ETestEnvironment;
import org.apache.shardingsphere.test.e2e.agent.file.asserts.ContentAssert;
import org.apache.shardingsphere.test.e2e.agent.file.cases.LogE2ETestCases;
import org.apache.shardingsphere.test.e2e.agent.file.cases.LogTestCase;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(AgentTestActionExtension.class)
class FilePluginE2EIT {
    
    @EnabledIf("isEnabled")
    @ParameterizedTest
    @ArgumentsSource(TestCaseArgumentsProvider.class)
    void assertWithAgent(final LogTestCase testCase) {
        assertFalse(AgentE2ETestEnvironment.getInstance().getActualLogs().isEmpty(), "The actual log is empty");
        ContentAssert.assertIs(AgentE2ETestEnvironment.getInstance().getActualLogs(), testCase.getLogRegex());
    }
    
    private static boolean isEnabled() {
        return AgentE2ETestEnvironment.getInstance().containsTestParameter();
    }
    
    private static class TestCaseArgumentsProvider implements ArgumentsProvider {
        
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
            return new AgentE2ETestCasesLoader(LogE2ETestCases.class).loadTestCases(AgentE2ETestEnvironment.getInstance().getAdapter()).stream().map(Arguments::of);
        }
    }
}
