/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class HqlSortedQueryTransformer extends HqlQueryRenderer {

	private final JpaQueryTransformerSupport transformerSupport = new JpaQueryTransformerSupport();
	private final Sort sort;
	private final @Nullable String primaryFromAlias;

	HqlSortedQueryTransformer(Sort sort, @Nullable String primaryFromAlias) {

		Assert.notNull(sort, "Sort must not be null");

		this.sort = sort;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>(128);

		if (ctx.query() != null) {
			tokens.addAll(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.queryExpression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		if (!isSubquery(ctx)) {

			if (ctx.queryOrder() != null) {
				tokens.addAll(visit(ctx.queryOrder()));
			}

			if (sort.isSorted()) {

				if (ctx.queryOrder() != null) {

					NOSPACE(tokens);
					tokens.add(TOKEN_COMMA);
				} else {

					SPACE(tokens);
					tokens.add(TOKEN_ORDER_BY);
				}

				tokens.addAll(transformerSupport.generateOrderByArguments(primaryFromAlias, sort));
			}
		} else {

			if (ctx.queryOrder() != null) {
				tokens.addAll(visit(ctx.queryOrder()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinPath(HqlParser.JoinPathContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitJoinPath(ctx);

		if (ctx.variable() != null) {
			transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoinSubquery(HqlParser.JoinSubqueryContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitJoinSubquery(ctx);

		if (ctx.variable() != null) {
			transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitVariable(HqlParser.VariableContext ctx) {

		List<JpaQueryParsingToken> tokens = super.visitVariable(ctx);

		if (ctx.identifier() != null) {
			transformerSupport.registerAlias(tokens.get(tokens.size() - 1).getToken());
		}

		return tokens;
	}

}
