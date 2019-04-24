/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir.semantics;

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
@TestMetadata("compiler/testData/codegen/boxInline/callableReference")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class IrCallableReferenceInlineTestsGenerated extends AbstractIrCallableReferenceInlineTests {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest0(this::doTest, TargetBackend.JS_IR, testDataFilePath);
    }

    public void testAllFilesPresentInCallableReference() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/callableReference"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS_IR, true);
    }

    @TestMetadata("classLevel.kt")
    public void testClassLevel() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/classLevel.kt");
    }

    @TestMetadata("classLevel2.kt")
    public void testClassLevel2() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/classLevel2.kt");
    }

    @TestMetadata("constructor.kt")
    public void testConstructor() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/constructor.kt");
    }

    @TestMetadata("intrinsic.kt")
    public void testIntrinsic() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/intrinsic.kt");
    }

    @TestMetadata("kt15449.kt")
    public void testKt15449() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/kt15449.kt");
    }

    @TestMetadata("kt15751_2.kt")
    public void testKt15751_2() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/kt15751_2.kt");
    }

    @TestMetadata("kt16411.kt")
    public void testKt16411() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/kt16411.kt");
    }

    @TestMetadata("propertyIntrinsic.kt")
    public void testPropertyIntrinsic() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/propertyIntrinsic.kt");
    }

    @TestMetadata("propertyReference.kt")
    public void testPropertyReference() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/propertyReference.kt");
    }

    @TestMetadata("topLevel.kt")
    public void testTopLevel() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/topLevel.kt");
    }

    @TestMetadata("topLevelExtension.kt")
    public void testTopLevelExtension() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/topLevelExtension.kt");
    }

    @TestMetadata("topLevelProperty.kt")
    public void testTopLevelProperty() throws Exception {
        runTest("compiler/testData/codegen/boxInline/callableReference/topLevelProperty.kt");
    }

    @TestMetadata("compiler/testData/codegen/boxInline/callableReference/bound")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Bound extends AbstractIrCallableReferenceInlineTests {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest0(this::doTest, TargetBackend.JS_IR, testDataFilePath);
        }

        public void testAllFilesPresentInBound() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/boxInline/callableReference/bound"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.JS_IR, true);
        }

        @TestMetadata("classProperty.kt")
        public void testClassProperty() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/classProperty.kt");
        }

        @TestMetadata("emptyLhsFunction.kt")
        public void testEmptyLhsFunction() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/emptyLhsFunction.kt");
        }

        @TestMetadata("expression.kt")
        public void testExpression() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/expression.kt");
        }

        @TestMetadata("extensionReceiver.kt")
        public void testExtensionReceiver() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/extensionReceiver.kt");
        }

        @TestMetadata("filter.kt")
        public void testFilter() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/filter.kt");
        }

        @TestMetadata("intrinsic.kt")
        public void testIntrinsic() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/intrinsic.kt");
        }

        @TestMetadata("kt18728.kt")
        public void testKt18728() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/kt18728.kt");
        }

        @TestMetadata("kt18728_2.kt")
        public void testKt18728_2() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/kt18728_2.kt");
        }

        @TestMetadata("kt18728_3.kt")
        public void testKt18728_3() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/kt18728_3.kt");
        }

        @TestMetadata("kt18728_4.kt")
        public void testKt18728_4() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/kt18728_4.kt");
        }

        @TestMetadata("map.kt")
        public void testMap() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/map.kt");
        }

        @TestMetadata("mixed.kt")
        public void testMixed() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/mixed.kt");
        }

        @TestMetadata("objectProperty.kt")
        public void testObjectProperty() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/objectProperty.kt");
        }

        @TestMetadata("propertyImportedFromObject.kt")
        public void testPropertyImportedFromObject() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/propertyImportedFromObject.kt");
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/simple.kt");
        }

        @TestMetadata("topLevelExtensionProperty.kt")
        public void testTopLevelExtensionProperty() throws Exception {
            runTest("compiler/testData/codegen/boxInline/callableReference/bound/topLevelExtensionProperty.kt");
        }
    }
}
