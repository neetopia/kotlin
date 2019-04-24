/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/jvm-abi-gen/testData/compare")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class CompareJvmAbiTestGenerated extends AbstractCompareJvmAbiTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInCompare() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("plugins/jvm-abi-gen/testData/compare"), Pattern.compile("^([^\\.]+)$"), TargetBackend.ANY, false);
    }

    @TestMetadata("classFlags")
    public void testClassFlags() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/classFlags/");
    }

    @TestMetadata("classPrivateMemebers")
    public void testClassPrivateMemebers() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/classPrivateMemebers/");
    }

    @TestMetadata("clinit")
    public void testClinit() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/clinit/");
    }

    @TestMetadata("constant")
    public void testConstant() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/constant/");
    }

    @TestMetadata("functionBody")
    public void testFunctionBody() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/functionBody/");
    }

    @TestMetadata("inlineFunctionBody")
    public void testInlineFunctionBody() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/inlineFunctionBody/");
    }

    @TestMetadata("parameterName")
    public void testParameterName() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/parameterName/");
    }

    @TestMetadata("privateTypealias")
    public void testPrivateTypealias() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/privateTypealias/");
    }

    @TestMetadata("returnType")
    public void testReturnType() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/returnType/");
    }

    @TestMetadata("superClass")
    public void testSuperClass() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/superClass/");
    }

    @TestMetadata("topLevelPrivateMembers")
    public void testTopLevelPrivateMembers() throws Exception {
        runTest("plugins/jvm-abi-gen/testData/compare/topLevelPrivateMembers/");
    }
}
