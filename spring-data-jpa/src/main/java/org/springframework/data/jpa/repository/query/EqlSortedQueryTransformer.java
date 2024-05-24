/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.springframework.data.jpa.repository.query.JpaQueryParsingToken.*;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed EQL query by applying
 * {@link Sort}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.2
 */
@SuppressWarnings("ConstantValue")
class EqlSortedQueryTransformer extends EqlQueryRenderer {

	private final JpaQueryTransformerSupport transformerSupport = new JpaQueryTransformerSupport();
	private final Sort sort;
	private final @Nullable String primaryFromAlias;

	EqlSortedQueryTransformer(Sort sort, @Nullable String primaryFromAlias) {

		Assert.notNull(sort, "Sort must not be null");

		this.sort = sort;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_statement(EqlParser.Select_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitSelect_statement(ctx);

		if (sort.isSorted()) {

			if (ctx.orderby_clause() != null) {

				NOSPACE(tokens);
				tokens.add(TOKEN_COMMA);
			} else {

				SPACE(tokens);
				tokens.add(TOKEN_ORDER_BY);
			}

			tokens.addAll(transformerSupport.generateOrderByArguments(primaryFromAlias, sort));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_item(EqlParser.Select_itemContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitSelect_item(ctx);

		if (ctx.result_variable() != null) {
			transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin(EqlParser.JoinContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitJoin(ctx);

		transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());

		return tokens;
	}

}
