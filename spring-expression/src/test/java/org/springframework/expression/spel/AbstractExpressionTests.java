/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Common superclass for expression tests.
 *
 * @author Andy Clement
 */
public abstract class AbstractExpressionTests {

	protected static final boolean DEBUG = false;

	protected static final boolean SHOULD_BE_WRITABLE = true;

	protected static final boolean SHOULD_NOT_BE_WRITABLE = false;


	protected final ExpressionParser parser = new SpelExpressionParser();

	protected final StandardEvaluationContext context = TestScenarioCreator.getTestEvaluationContext();


	/**
	 * 评估表达式并检查实际结果是否与 expectedValue 匹配，并且结果类型是否与 expectedResultType 相同。
	 *
	 * @param expression         要评估的表达式
	 * @param expectedValue      表达式评估后的预期结果
	 * @param expectedResultType 表达式评估结果的预期类型
	 */
	public void evaluate(String expression, Object expectedValue, Class<?> expectedResultType) {
		// 解析表达式并确保其不为空
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();

		// 用于调试目的，打印抽象语法树
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}

		// 评估表达式以获取实际值
		Object value = expr.getValue(context);

		// 检查返回值
		if (value == null) {
			// 如果实际值和预期值都为 null，则无需进一步检查
			if (expectedValue == null) {
				return;
			}
			// 如果实际值为 null 但预期值不为 null，则抛出错误
			assertThat(expectedValue).as("表达式返回了 null 值，但预期值为 '" + expectedValue + "'").isNull();
		}

		// 检查实际结果类型是否与预期结果类型相同
		Class<?> resultType = value.getClass();
		assertThat(resultType).as("实际结果类型不符合预期。预期类型为 '" + expectedResultType +
				"' 但实际类型为 '" + resultType + "'").isEqualTo(expectedResultType);

		// 比较实际值与预期值，单独处理字符串类型
		if (expectedValue instanceof String) {
			assertThat(AbstractExpressionTests.stringValueOf(value)).as("表达式 '" + expression + "' 的结果不符合预期。").isEqualTo(expectedValue);
		} else {
			assertThat(value).as("表达式 '" + expression + "' 的结果不符合预期。").isEqualTo(expectedValue);
		}
	}


	public void evaluateAndAskForReturnType(String expression, Object expectedValue, Class<?> expectedResultType) {
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}

		Object value = expr.getValue(context, expectedResultType);
		if (value == null) {
			if (expectedValue == null) {
				return;  // no point doing other checks
			}
			assertThat(expectedValue).as("Expression returned null value, but expected '" + expectedValue + "'").isNull();
		}

		Class<?> resultType = value.getClass();
		assertThat(resultType).as("Type of the actual result was not as expected.  Expected '" + expectedResultType +
				"' but result was of type '" + resultType + "'").isEqualTo(expectedResultType);
		assertThat(value).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
	}

	/**
	 * Evaluate an expression and check that the actual result matches the
	 * expectedValue and the class of the result matches the expectedResultType.
	 * This method can also check if the expression is writable (for example,
	 * it is a variable or property reference).
	 *
	 * @param expression         the expression to evaluate
	 * @param expectedValue      the expected result for evaluating the expression
	 * @param expectedResultType the expected class of the evaluation result
	 * @param shouldBeWritable   should the parsed expression be writable?
	 */
	public void evaluate(String expression, Object expectedValue, Class<?> expectedResultType, boolean shouldBeWritable) {
		Expression expr = parser.parseExpression(expression);
		assertThat(expr).as("expression").isNotNull();
		if (DEBUG) {
			SpelUtilities.printAbstractSyntaxTree(System.out, expr);
		}
		Object value = expr.getValue(context);
		if (value == null) {
			if (expectedValue == null) {
				return;  // no point doing other checks
			}
			assertThat(expectedValue).as("Expression returned null value, but expected '" + expectedValue + "'").isNull();
		}
		Class<?> resultType = value.getClass();
		if (expectedValue instanceof String) {
			assertThat(AbstractExpressionTests.stringValueOf(value)).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		} else {
			assertThat(value).as("Did not get expected value for expression '" + expression + "'.").isEqualTo(expectedValue);
		}
		assertThat(expectedResultType.equals(resultType)).as("Type of the result was not as expected.  Expected '" + expectedResultType +
				"' but result was of type '" + resultType + "'").isTrue();

		assertThat(expr.isWritable(context)).as("isWritable").isEqualTo(shouldBeWritable);
	}

	/**
	 * 评估指定的表达式并确保预期的消息出现。
	 * 消息可能包含插入项，如果指定了otherProperties，则会检查这些插入项。
	 * otherProperties中的第一个条目始终应该是位置信息。
	 *
	 * @param expression      需要评估的表达式
	 * @param expectedMessage 预期的消息
	 * @param otherProperties 消息内的预期插入项
	 */
	protected void evaluateAndCheckError(String expression, SpelMessage expectedMessage, Object... otherProperties) {
		evaluateAndCheckError(expression, null, expectedMessage, otherProperties);
	}


	/**
	 * 评估指定的表达式并确保预期的消息被输出。
	 * 消息可能包含插入项，并且如果指定了otherProperties，将检查这些插入项。
	 * otherProperties中的第一个条目始终应该是位置。
	 *
	 * @param expression         需要评估的表达式
	 * @param expectedReturnType 如果可能，请求表达式的返回值为此类型
	 *                           （{@code null} 表示不请求转换）
	 * @param expectedMessage    预期的消息
	 * @param otherProperties    消息中的预期插入项
	 */
	protected void evaluateAndCheckError(String expression, Class<?> expectedReturnType, SpelMessage expectedMessage,
										 Object... otherProperties) {

		// 委托给一个重载方法，并提供解析器实例
		evaluateAndCheckError(this.parser, expression, expectedReturnType, expectedMessage, otherProperties);
	}


	/**
	 * 评估指定的表达式并确保预期的消息出现。
	 * 消息可能包含插入项，并且如果指定了 otherProperties，则会检查这些插入项。
	 * otherProperties 中的第一个条目始终应为错误在字符串中的位置。
	 *
	 * @param parser             要使用的表达式解析器
	 * @param expression         要评估的表达式
	 * @param expectedReturnType 请求表达式的返回值为此类型（如果可能的话）
	 *                           （{@code null} 表示不需要转换）
	 * @param expectedMessage    预期的消息
	 * @param otherProperties    消息中的预期插入项
	 */
	protected void evaluateAndCheckError(ExpressionParser parser, String expression, Class<?> expectedReturnType, SpelMessage expectedMessage,
										 Object... otherProperties) {

		// 使用 assertThat() 判断以下代码块是否抛出 SpelEvaluationException 异常
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() -> {
			// 解析表达式，并断言表达式不为空
			Expression expr = parser.parseExpression(expression);
			assertThat(expr).as("expression").isNotNull();
			// 如果指定了预期返回类型，则调用带有类型转换的 getValue 方法；否则，调用不指定类型的 getValue 方法
			if (expectedReturnType != null) {
				expr.getValue(context, expectedReturnType);
			} else {
				expr.getValue(context);
			}
		}).satisfies(ex -> {
			// 断言异常消息代码与预期消息一致
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			// 如果 otherProperties 不为空，则执行以下检查
			if (!ObjectUtils.isEmpty(otherProperties)) {
				// otherProperties 中的第一个元素是错误在字符串中的预期位置
				int pos = (Integer) otherProperties[0];
				assertThat(ex.getPosition()).as("position").isEqualTo(pos);
				// 如果 otherProperties 的长度大于一个元素，则检查插入项是否匹配
				if (otherProperties.length > 1) {
					Object[] inserts = ex.getInserts();
					assertThat(inserts).as("inserts").hasSizeGreaterThanOrEqualTo(otherProperties.length - 1);
					Object[] expectedInserts = new Object[inserts.length];
					// 将 otherProperties（除去第一个元素）复制到 expectedInserts
					System.arraycopy(otherProperties, 1, expectedInserts, 0, expectedInserts.length);
					assertThat(inserts).as("inserts").containsExactly(expectedInserts);
				}
			}
		});
	}


	/**
	 * Parse the specified expression and ensure the expected message comes out.
	 * The message may have inserts and they will be checked if otherProperties is specified.
	 * The first entry in otherProperties should always be the position.
	 *
	 * @param expression      the expression to evaluate
	 * @param expectedMessage the expected message
	 * @param otherProperties the expected inserts within the message
	 */
	protected void parseAndCheckError(String expression, SpelMessage expectedMessage, Object... otherProperties) {
		assertThatExceptionOfType(SpelParseException.class).isThrownBy(() -> {
			Expression expr = parser.parseExpression(expression);
			if (DEBUG) {
				SpelUtilities.printAbstractSyntaxTree(System.out, expr);
			}
		}).satisfies(ex -> {
			assertThat(ex.getMessageCode()).isEqualTo(expectedMessage);
			if (otherProperties != null && otherProperties.length != 0) {
				// first one is expected position of the error within the string
				int pos = (Integer) otherProperties[0];
				assertThat(ex.getPosition()).as("reported position").isEqualTo(pos);
				if (otherProperties.length > 1) {
					// Check inserts match
					Object[] inserts = ex.getInserts();
					assertThat(inserts).as("inserts").hasSizeGreaterThanOrEqualTo(otherProperties.length - 1);
					Object[] expectedInserts = new Object[inserts.length];
					System.arraycopy(otherProperties, 1, expectedInserts, 0, expectedInserts.length);
					assertThat(inserts).as("inserts").containsExactly(expectedInserts);
				}
			}
		});
	}


	protected static String stringValueOf(Object value) {
		return stringValueOf(value, false);
	}

	/**
	 * Produce a nice string representation of the input object.
	 *
	 * @param value object to be formatted
	 * @return a nice string
	 */
	protected static String stringValueOf(Object value, boolean isNested) {
		// do something nice for arrays
		if (value == null) {
			return "null";
		}
		if (value.getClass().isArray()) {
			StringBuilder sb = new StringBuilder();
			if (value.getClass().componentType().isPrimitive()) {
				Class<?> primitiveType = value.getClass().componentType();
				if (primitiveType == int.class) {
					int[] l = (int[]) value;
					sb.append("int[").append(l.length).append("]{");
					for (int j = 0; j < l.length; j++) {
						if (j > 0) {
							sb.append(',');
						}
						sb.append(stringValueOf(l[j]));
					}
					sb.append('}');
				} else if (primitiveType == long.class) {
					long[] l = (long[]) value;
					sb.append("long[").append(l.length).append("]{");
					for (int j = 0; j < l.length; j++) {
						if (j > 0) {
							sb.append(',');
						}
						sb.append(stringValueOf(l[j]));
					}
					sb.append('}');
				} else {
					throw new RuntimeException("Please implement support for type " + primitiveType.getName() +
							" in ExpressionTestCase.stringValueOf()");
				}
			} else if (value.getClass().componentType().isArray()) {
				List<Object> l = Arrays.asList((Object[]) value);
				if (!isNested) {
					sb.append(value.getClass().componentType().getName());
				}
				sb.append('[').append(l.size()).append("]{");
				int i = 0;
				for (Object object : l) {
					if (i > 0) {
						sb.append(',');
					}
					i++;
					sb.append(stringValueOf(object, true));
				}
				sb.append('}');
			} else {
				List<Object> l = Arrays.asList((Object[]) value);
				if (!isNested) {
					sb.append(value.getClass().componentType().getName());
				}
				sb.append('[').append(l.size()).append("]{");
				int i = 0;
				for (Object object : l) {
					if (i > 0) {
						sb.append(',');
					}
					i++;
					sb.append(stringValueOf(object));
				}
				sb.append('}');
			}
			return sb.toString();
		} else {
			return value.toString();
		}
	}

}
