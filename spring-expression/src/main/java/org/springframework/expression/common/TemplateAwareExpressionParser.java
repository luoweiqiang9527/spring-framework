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

package org.springframework.expression.common;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * An expression parser that understands templates. It can be subclassed by expression
 * parsers that do not offer first class support for templating.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	/**
	 * 解析给定的表达式字符串并返回对应的Expression对象。
	 * 此方法是与语法解析相关的入口点，用于处理没有特定语法上下文的表达式解析场景。
	 *
	 * @param expressionString 表达式字符串，这是要解析的原始输入。
	 * @return 返回解析后的Expression对象，表示输入字符串的抽象语法树。
	 * @throws ParseException 如果解析过程中发生错误，会抛出此异常。
	 */
	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
	    return parseExpression(expressionString, null);
	}

	/**
	 * 根据给定的表达式字符串和解析上下文，解析出一个表达式对象。
	 * 如果提供了上下文且为模板类型，则调用特定的模板解析方法；
	 * 否则，执行普通的表达式解析。
	 *
	 * @param expressionString 表达式字符串，不能为空或空白。
	 * @param context          解析上下文，可能为null或非模板类型。
	 * @return 解析后的表达式对象。
	 * @throws ParseException 如果表达式字符串无效或解析出错。
	 */
	@Override
	public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {

		// 当存在上下文且上下文指示为模板时，调用模板解析方法
		if (context != null && context.isTemplate()) {
			Assert.notNull(expressionString, "'expressionString' must not be null");
			return parseTemplate(expressionString, context);
		}
		// 否则，执行普通表达式的解析
		else {
			Assert.hasText(expressionString, "'expressionString' must not be null or blank");
			return doParseExpression(expressionString, context);
		}
	}


	/**
	 * 解析模板字符串表达式
	 *
	 * @param expressionString 模板字符串，可能包含多个表达式
	 * @param context          解析上下文，提供解析所需的环境信息
	 * @return 返回解析后的Expression对象，可能是字面量表达式、单个表达式或复合字符串表达式
	 * @throws ParseException 当解析过程中发生错误时抛出此异常
	 */
	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		// 如果输入字符串为空，则返回空字符串的字面量表达式
		if (expressionString.isEmpty()) {
			return new LiteralExpression("");
		}

		// 解析输入字符串中的所有表达式
		Expression[] expressions = parseExpressions(expressionString, context);
		// 根据解析出的表达式数量决定返回哪种类型的Expression对象
		if (expressions.length == 1) {
			// 如果只有一个表达式，则直接返回该表达式
			return expressions[0];
		} else {
			// 如果有多个表达式，则返回一个复合字符串表达式，包含所有解析出的表达式
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * 辅助方法，使用配置的解析器解析给定的表达式字符串。表达式字符串可以包含任意数量的表达式，
	 * 所有这些表达式都被包含在“${...}”标记中。例如：“foo${expr0}bar${expr1}”。静态的文本片段也会
	 * 被返回为返回静态文本的Expression对象。因此，评估所有返回的表达式并将结果连接起来会生成完整的
	 * 评估后的字符串。只解析最外层定界符，所以字符串'hello ${foo${abc}}'会被分解为'hello '和'foo${abc}'。
	 * 这意味着使用${..}作为其功能一部分的表达式语言没有问题地得到支持。解析过程会注意到嵌入表达式的
	 * 结构。它假定圆括号'('、方括号'['和花括号'}'必须在表达式中成对出现，除非它们在字符串字面量内，而该
	 * 字符串字面量以单引号'开头和结尾。
	 *
	 * @param expressionString 表达式字符串
	 * @return 解析后的表达式数组
	 * @throws ParseException 当表达式无法解析时抛出异常
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
	    // 存储解析后的表达式列表
	    List<Expression> expressions = new ArrayList<>();
	    // 获取表达式的前缀和后缀
	    String prefix = context.getExpressionPrefix();
	    String suffix = context.getExpressionSuffix();
	    // 初始化起始索引
	    int startIdx = 0;

	    // 遍历整个表达式字符串
	    while (startIdx < expressionString.length()) {
	        // 查找下一个前缀的位置
	        int prefixIndex = expressionString.indexOf(prefix, startIdx);
	        if (prefixIndex >= startIdx) {
	            // 如果找到了前缀
	            // 检查前缀之前是否有非前缀文本，如果有则添加为静态文本表达式
	            if (prefixIndex > startIdx) {
	                expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
	            }
	            // 计算前缀后的位置
	            int afterPrefixIndex = prefixIndex + prefix.length();
	            // 查找与当前前缀对应的后缀位置
	            int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
	            if (suffixIndex == -1) {
	                // 如果没有找到对应的后缀，抛出异常
	                throw new ParseException(expressionString, prefixIndex,
	                        "No ending suffix '" + suffix + "' for expression starting at character " +
	                        prefixIndex + ": " + expressionString.substring(prefixIndex));
	            }
	            if (suffixIndex == afterPrefixIndex) {
	                // 如果找到了后缀，但前缀和后缀之间没有内容，抛出异常
	                throw new ParseException(expressionString, prefixIndex,
	                        "No expression defined within delimiter '" + prefix + suffix +
	                        "' at character " + prefixIndex);
	            }
	            // 提取前缀和后缀之间的表达式内容
	            String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
	            expr = expr.trim();
	            if (expr.isEmpty()) {
	                // 如果表达式内容为空，抛出异常
	                throw new ParseException(expressionString, prefixIndex,
	                        "No expression defined within delimiter '" + prefix + suffix +
	                        "' at character " + prefixIndex);
	            }
	            // 解析表达式并添加到列表中
	            expressions.add(doParseExpression(expr, context));
	            // 更新起始索引为当前后缀后的位置
	            startIdx = suffixIndex + suffix.length();
	        } else {
	            // 如果没有找到前缀，将剩余的字符串作为静态文本添加到列表中
	            expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
	            // 更新起始索引为字符串的末尾
	            startIdx = expressionString.length();
	        }
	    }

	    // 将列表转换为数组并返回
	    return expressions.toArray(new Expression[0]);
	}

	/**
	 * Return true if the specified suffix can be found at the supplied position in the
	 * supplied expression string.
	 *
	 * @param expressionString the expression string which may contain the suffix
	 * @param pos              the start position at which to check for the suffix
	 * @param suffix           the suffix string
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			// the expressionString ran out before the suffix could entirely be found
			return false;
		}
		return true;
	}

	/**
	 * Copes with nesting, for example '${...${...}}' where the correct end for the first
	 * ${ is the final }.
	 *
	 * @param suffix           the suffix
	 * @param expressionString the expression string
	 * @param afterPrefixIndex the most recently found prefix location for which the
	 *                         matching end suffix is being sought
	 * @return the position of the correct matching nextSuffix or -1 if none can be found
	 */
	private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
			throws ParseException {

		// Chew on the expression text - relying on the rules:
		// brackets must be in pairs: () [] {}
		// string literals are "..." or '...' and these may contain unmatched brackets
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // the suffix is missing
		}
		Deque<Bracket> stack = new ArrayDeque<>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				break;
			}
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{', '[', '(' -> {
					stack.push(new Bracket(ch, pos));
				}
				case '}', ']', ')' -> {
					if (stack.isEmpty()) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " without an opening '" +
								Bracket.theOpenBracketFor(ch) + "'");
					}
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " but most recent opening is '" + p.bracket +
								"' at position " + p.pos);
					}
				}
				case '\'', '"' -> {
					// jump to the end of the literal
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position " + pos);
					}
					pos = endLiteral;
				}
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '" +
					Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			return -1;
		}
		return pos;
	}


	/**
	 * Actually parse the expression string and return an Expression object.
	 *
	 * @param expressionString the raw expression string to parse
	 * @param context          a context for influencing this expression parsing routine (optional)
	 * @return an evaluator for the parsed expression
	 * @throws ParseException an exception occurred during parsing
	 */
	protected abstract Expression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException;


	/**
	 * This captures a type of bracket and the position in which it occurs in the
	 * expression. The positional information is used if an error has to be reported
	 * because the related end bracket cannot be found. Bracket is used to describe
	 * square brackets [], round brackets (), and curly brackets {}.
	 */
	private record Bracket(char bracket, int pos) {

		boolean compatibleWithCloseBracket(char closeBracket) {
			return switch (this.bracket) {
				case '{' -> closeBracket == '}';
				case '[' -> closeBracket == ']';
				default -> closeBracket == ')';
			};
		}

		static char theOpenBracketFor(char closeBracket) {
			return switch (closeBracket) {
				case '}' -> '{';
				case ']' -> '[';
				default -> '(';
			};
		}

		static char theCloseBracketFor(char openBracket) {
			return switch (openBracket) {
				case '{' -> '}';
				case '[' -> ']';
				default -> ')';
			};
		}
	}

}
