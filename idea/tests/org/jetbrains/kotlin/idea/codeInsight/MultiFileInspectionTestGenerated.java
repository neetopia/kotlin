/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight;

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
@TestMetadata("idea/testData/multiFileInspections")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class MultiFileInspectionTestGenerated extends AbstractMultiFileInspectionTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInMultiFileInspections() throws Exception {
        KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File("idea/testData/multiFileInspections"), Pattern.compile("^(.+)\\.test$"), TargetBackend.ANY);
    }

    @TestMetadata("fakeJvmFieldConstant/fakeJvmFieldConstant.test")
    public void testFakeJvmFieldConstant_FakeJvmFieldConstant() throws Exception {
        runTest("idea/testData/multiFileInspections/fakeJvmFieldConstant/fakeJvmFieldConstant.test");
    }

    @TestMetadata("invalidBundleOrProperty/invalidBundleOrProperty.test")
    public void testInvalidBundleOrProperty_InvalidBundleOrProperty() throws Exception {
        runTest("idea/testData/multiFileInspections/invalidBundleOrProperty/invalidBundleOrProperty.test");
    }

    @TestMetadata("kotlinInternalInJava/kotlinInternalInJava.test")
    public void testKotlinInternalInJava_KotlinInternalInJava() throws Exception {
        runTest("idea/testData/multiFileInspections/kotlinInternalInJava/kotlinInternalInJava.test");
    }

    @TestMetadata("mainInTwoModules/mainInTwoModules.test")
    public void testMainInTwoModules_MainInTwoModules() throws Exception {
        runTest("idea/testData/multiFileInspections/mainInTwoModules/mainInTwoModules.test");
    }

    @TestMetadata("mismatchedProjectAndDirectory/mismatchedProjectAndDirectory.test")
    public void testMismatchedProjectAndDirectory_MismatchedProjectAndDirectory() throws Exception {
        runTest("idea/testData/multiFileInspections/mismatchedProjectAndDirectory/mismatchedProjectAndDirectory.test");
    }

    @TestMetadata("mismatchedProjectAndDirectoryRoot/mismatchedProjectAndDirectoryRoot.test")
    public void testMismatchedProjectAndDirectoryRoot_MismatchedProjectAndDirectoryRoot() throws Exception {
        runTest("idea/testData/multiFileInspections/mismatchedProjectAndDirectoryRoot/mismatchedProjectAndDirectoryRoot.test");
    }

    @TestMetadata("platformExtensionReceiverOfInline/platformExtensionReceiverOfInline.test")
    public void testPlatformExtensionReceiverOfInline_PlatformExtensionReceiverOfInline() throws Exception {
        runTest("idea/testData/multiFileInspections/platformExtensionReceiverOfInline/platformExtensionReceiverOfInline.test");
    }
}
