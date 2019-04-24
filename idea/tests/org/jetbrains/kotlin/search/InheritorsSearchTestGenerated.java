/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search;

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
@TestMetadata("idea/testData/search/inheritance")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class InheritorsSearchTestGenerated extends AbstractInheritorsSearchTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInInheritance() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/search/inheritance"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("annotationClass.kt")
    public void testAnnotationClass() throws Exception {
        runTest("idea/testData/search/inheritance/annotationClass.kt");
    }

    @TestMetadata("enum.kt")
    public void testEnum() throws Exception {
        runTest("idea/testData/search/inheritance/enum.kt");
    }

    @TestMetadata("interfaces.kt")
    public void testInterfaces() throws Exception {
        runTest("idea/testData/search/inheritance/interfaces.kt");
    }

    @TestMetadata("object.kt")
    public void testObject() throws Exception {
        runTest("idea/testData/search/inheritance/object.kt");
    }

    @TestMetadata("simpleClass.kt")
    public void testSimpleClass() throws Exception {
        runTest("idea/testData/search/inheritance/simpleClass.kt");
    }

    @TestMetadata("testInheritanceFromJavaClass.kt")
    public void testTestInheritanceFromJavaClass() throws Exception {
        runTest("idea/testData/search/inheritance/testInheritanceFromJavaClass.kt");
    }

    @TestMetadata("testInheritanceFromKotlinClass.kt")
    public void testTestInheritanceFromKotlinClass() throws Exception {
        runTest("idea/testData/search/inheritance/testInheritanceFromKotlinClass.kt");
    }
}
