/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test construction of arrays.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class ArrayConstructorTests extends AbstractExpressionTests {

	@Test
	void conversion() {
		// 验证数组的第一个元素是否为正确的类型和值
		// 这里我们验证字符串数组的第一个元素是否为"1"
		evaluate("new String[]{1,2,3}[0]", "1", String.class);
		// 验证整型数组的第一个元素是否为正确的数值123
		// 这里数组的元素是通过字符的ASCII值初始化的，我们验证第一个元素的值是否为123
		evaluate("new int[]{'123'}[0]", 123, Integer.class);
	}

	/**
	 * 测试基本类型数组构造器
	 * 该方法旨在验证不同基本类型数组的创建和内容
	 */
	@Test
	void primitiveTypeArrayConstructors() {
		// 验证空的整型数组
		evaluateArrayBuildingExpression("new int[]{}", "{}");
		// 验证含有具体值的整型数组
		evaluateArrayBuildingExpression("new int[]{1,2,3,4}", "{1, 2, 3, 4}");
		// 验证布尔型数组
		evaluateArrayBuildingExpression("new boolean[]{true,false,true}", "{true, false, true}");
		// 验证字符型数组
		evaluateArrayBuildingExpression("new char[]{'a','b','c'}", "{'a', 'b', 'c'}");
		// 验证长整型数组
		evaluateArrayBuildingExpression("new long[]{1,2,3,4,5}", "{1, 2, 3, 4, 5}");
		// 验证短整型数组
		evaluateArrayBuildingExpression("new short[]{2,3,4,5,6}", "{2, 3, 4, 5, 6}");
		// 验证双精度浮点型数组
		evaluateArrayBuildingExpression("new double[]{1d,2d,3d,4d}", "{1.0, 2.0, 3.0, 4.0}");
		// 验证单精度浮点型数组
		evaluateArrayBuildingExpression("new float[]{1f,2f,3f,4f}", "{1.0, 2.0, 3.0, 4.0}");
		// 验证字节型数组
		evaluateArrayBuildingExpression("new byte[]{1,2,3,4}", "{1, 2, 3, 4}");

		// 验证空数组的长度属性
		evaluate("new int[]{}.length", "0", Integer.class);
	}

	@Test
	void primitiveTypeArrayConstructorsElements() {
		// 评估并验证不同基本类型数组的第一个元素
		evaluate("new int[]{1,2,3,4}[0]", 1, Integer.class);
		evaluate("new boolean[]{true,false,true}[0]", true, Boolean.class);
		evaluate("new char[]{'a','b','c'}[0]", 'a', Character.class);
		evaluate("new long[]{1,2,3,4,5}[0]", 1L, Long.class);
		evaluate("new short[]{2,3,4,5,6}[0]", (short) 2, Short.class);
		evaluate("new double[]{1d,2d,3d,4d}[0]", (double) 1, Double.class);
		evaluate("new float[]{1f,2f,3f,4f}[0]", (float) 1, Float.class);
		evaluate("new byte[]{1,2,3,4}[0]", (byte) 1, Byte.class);

		// 通过构造字符数组并转换为字符串来评估第一个元素
		evaluate("new String(new char[]{'h','e','l','l','o'})", "hello", String.class);
	}

	/**
	 * 测试各种错误情况下的表达式解析和错误处理
	 * 该方法通过提供错误的数组初始化表达式，验证系统是否能正确识别并抛出相应的错误
	 */
	@Test
	void errorCases() {
		// 以下各调用验证了在数组声明中缺少维度时系统是否抛出正确的错误
		evaluateAndCheckError("new int[]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new String[]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new int[3][]", SpelMessage.MISSING_ARRAY_DIMENSION);
		evaluateAndCheckError("new int[][1]", SpelMessage.MISSING_ARRAY_DIMENSION);

		// 验证数组初始化时长度不匹配是否触发相应的错误
		evaluateAndCheckError("new char[7]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);
		evaluateAndCheckError("new char[3]{'a','c','d','e'}", SpelMessage.INITIALIZER_LENGTH_INCORRECT);

		// 验证多维数组初始化不支持的情况
		evaluateAndCheckError("new int[][]{{1,2},{3,4}}", SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);

		// 验证类型转换错误是否能被正确捕获
		evaluateAndCheckError("new char[2]{'hello','world'}", SpelMessage.TYPE_CONVERSION_ERROR);
		// 下面的注释解释了为什么使用TYPE_CONVERSION_ERROR而不是INCORRECT_ELEMENT_TYPE_FOR_ARRAY
		// Could conceivably be a SpelMessage.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, but it appears
		// that SpelMessage.INCORRECT_ELEMENT_TYPE_FOR_ARRAY is not actually (no longer?) used
		// in the code base.
		evaluateAndCheckError("new Integer[3]{'3','ghi','5'}", SpelMessage.TYPE_CONVERSION_ERROR);

		// 验证构造函数调用问题是否能被正确识别
		evaluateAndCheckError("new String('a','c','d')", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);
		// 下面的注释解释了构造函数调用失败的根本原因
		// Root cause: java.lang.OutOfMemoryError: Requested array size exceeds VM limit
		evaluateAndCheckError("new java.util.ArrayList(T(java.lang.Integer).MAX_VALUE)", SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM);

		// 验证是否在尝试创建超过最大阈值的数组时抛出超出阈值的错误
		int threshold = 256 * 1024; // ConstructorReference.MAX_ARRAY_ELEMENTS
		evaluateAndCheckError("new int[T(java.lang.Integer).MAX_VALUE]", SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, 0, threshold);
		evaluateAndCheckError("new int[1024 * 1024][1024 * 1024]", SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, 0, threshold);
	}

	/**
	 * 测试字符串数组构造函数和属性访问
	 */
	@Test
	void typeArrayConstructors() {
		// 测试字符串数组初始化及索引访问
		evaluate("new String[]{'a','b','c','d'}[1]", "b", String.class);

		// 测试字符串数组上不存在的方法调用时的错误处理
		evaluateAndCheckError("new String[]{'a','b','c','d'}.size()", SpelMessage.METHOD_NOT_FOUND, 30, "size()",
				"java.lang.String[]");

		// 测试获取字符串数组的长度
		evaluate("new String[]{'a','b','c','d'}.length", 4, Integer.class);
	}


	/**
	 * 测试创建字符串数组的基本用法
	 * 本测试用例用于验证创建一个长度为3的String类型数组，并检查其初始状态是否正确
	 */
	@Test
	void basicArray() {
	    // 验证新创建的字符串数组的类型和初始状态
	    evaluate("new String[3]", "java.lang.String[3]{null,null,null}", String[].class);
	}

	/**
	 * 测试多维数组的评估功能
	 * 本测试旨在验证系统正确处理和评估多维字符串数组的能力
	 */
	@Test
	void multiDimensionalArrays() {
	    // 评估并验证二维字符串数组的创建和初始化
	    evaluate("new String[2][2]", "[Ljava.lang.String;[2]{[2]{null,null},[2]{null,null}}", String[][].class);

	    // 评估并验证三维字符串数组的创建和初始化
	    evaluate("new String[3][2][1]",
	            "[[Ljava.lang.String;[3]{[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}},[2]{[1]{null},[1]{null}}]",
	            String[][][].class);
	}

	/**
	 * 测试SpEL表达式中不允许创建数组
	 * 这个测试用例验证了在使用SpEL解析器解析表达式并尝试创建数组时，是否会抛出预期的异常
	 */
	@Test
	void noArrayConstruction() {
	    // 创建一个简单的评估上下文，用于读写数据绑定
	    EvaluationContext context = SimpleEvaluationContext.forReadWriteDataBinding().build();

	    // 验证解析器在尝试评估"new int[2]"表达式时，是否抛出SpEL评估异常
	    // 这个表达式尝试创建一个长度为2的int数组，这在SpEL表达式中是不允许的
	    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
	            parser.parseExpression("new int[2]").getValue(context));
	}


	/**
	 * 评估数组构建表达式的解析和表示是否正确
	 * 该方法通过一个字符串表达式构建数组，并验证其解析后的值是否符合预期的字符串表示
	 * 主要用于测试SPEL表达式解析器对于数组构建的支持程度
	 *
	 * @param expression       用于构建数组的SPEL表达式字符串
	 * @param expectedToString 预期的数组字符串表示
	 */
	private void evaluateArrayBuildingExpression(String expression, String expectedToString) {
		// 实例化SPEL表达式解析器，用于解析传入的表达式字符串
		SpelExpressionParser parser = new SpelExpressionParser();
		// 解析给定的表达式字符串，返回一个Expression对象
		Expression e = parser.parseExpression(expression);
		// 计算表达式的值，期望它是一个数组对象
		Object array = e.getValue();
		// 验证解析后的值是否为非空
		assertThat(array).isNotNull();
		// 验证解析后的值是否确实是一个数组
		assertThat(array.getClass().isArray()).isTrue();
		// 验证数组的字符串表示是否与预期相符
		assertThat(ObjectUtils.nullSafeToString(array)).isEqualTo(expectedToString);
	}

}
