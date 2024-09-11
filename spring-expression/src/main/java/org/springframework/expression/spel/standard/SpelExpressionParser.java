/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.standard;

import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateAwareExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * SpEL parser. Instances are reusable and thread-safe.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class SpelExpressionParser extends TemplateAwareExpressionParser {

	private final SpelParserConfiguration configuration;


	/**
	 * Create a parser with default settings.
	 */
	public SpelExpressionParser() {
		this.configuration = new SpelParserConfiguration();
	}

	/**
	 * Create a parser with the specified configuration.
	 * @param configuration custom configuration options
	 */
	public SpelExpressionParser(SpelParserConfiguration configuration) {
		Assert.notNull(configuration, "SpelParserConfiguration must not be null");
		this.configuration = configuration;
	}


	/**
	 * 解析原始的SpEL表达式字符串
	 * 此方法主要用于解析未经过预编译的SpEL表达式字符串，它将调用内部的解析逻辑来处理表达式
	 *
	 * @param expressionString 表达式字符串，不能为空或空白
	 * @return 返回解析后的SpelExpression对象
	 * @throws ParseException 如果表达式字符串不符合语法要求，将抛出此异常
	 */
	public SpelExpression parseRaw(String expressionString) throws ParseException {
		// 校验表达式字符串是否有效
		Assert.hasText(expressionString, "'expressionString' must not be null or blank");
		// 调用内部解析方法处理表达式字符串，传递null作为上下文参数
		return doParseExpression(expressionString, null);
	}

	/**
	 * 解析SpEL表达式
	 *
	 * @param expressionString SpEL表达式的字符串表示
	 * @param context 解析上下文，可能为空
	 * @return 解析后的SpelExpression对象
	 * @throws ParseException 如果解析过程中发生错误
	 */
	@Override
	protected SpelExpression doParseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
	    // 委托内部SpelExpressionParser进行实际的表达式解析
	    return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
	}

}
