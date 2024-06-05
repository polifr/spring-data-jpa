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

import java.util.List;

import org.springframework.data.jpa.repository.query.QueryRenderer.QueryRendererBuilder;
import org.springframework.lang.Nullable;

/**
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed JPQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class JpqlCountQueryTransformer extends JpqlQueryRenderer {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;

	JpqlCountQueryTransformer(@Nullable String countProjection, @Nullable String primaryFromAlias) {
		this.countProjection = countProjection;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public QueryRenderer.QueryRendererBuilder visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.appendExpression(visit(ctx.select_clause()));
		builder.appendExpression(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			builder.appendExpression(visit(ctx.where_clause()));
		}
		if (ctx.groupby_clause() != null) {
			builder.appendExpression(visit(ctx.groupby_clause()));
		}
		if (ctx.having_clause() != null) {
			builder.appendExpression(visit(ctx.having_clause()));
		}

		return builder;
	}

	@Override
	public QueryRendererBuilder visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		QueryRendererBuilder builder = QueryRenderer.builder();

		builder.append(JpaQueryParsingToken.expression(ctx.SELECT()));
		builder.append(TOKEN_COUNT_FUNC);

		if (countProjection != null) {
			builder.append(JpaQueryParsingToken.token(countProjection));
		}

		QueryRendererBuilder nested = QueryRenderer.builder();

		if (ctx.DISTINCT() != null) {
			nested.append(JpaQueryParsingToken.expression(ctx.DISTINCT()));
		}

		if (countProjection == null) {

			if (ctx.DISTINCT() != null) {

				QueryRendererBuilder selectionListbuilder = QueryRendererBuilder.concat(ctx.select_item(), this::visit,
						TOKEN_COMMA);

				List<QueryToken> countSelection = QueryTransformers
						.filterCountSelection(selectionListbuilder.build().stream().toList());

				if (countSelection.stream().anyMatch(jpqlToken -> jpqlToken.value().contains("new"))) {
					// constructor
					nested.append(new JpaQueryParsingToken(primaryFromAlias));
				} else {
					// keep all the select items to distinct against
					nested.append(countSelection);
				}
			} else {
				nested.append(new JpaQueryParsingToken(primaryFromAlias));
			}
		}

		builder.appendInline(nested);
		builder.append(TOKEN_CLOSE_PAREN);

		return builder;
	}

}
