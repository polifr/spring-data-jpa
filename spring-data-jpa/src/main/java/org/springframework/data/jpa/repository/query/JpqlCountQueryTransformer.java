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
	public List<JpaQueryParsingToken> visitSelect_statement(JpqlParser.Select_statementContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>(128);

		tokens.addAll(visit(ctx.select_clause()));
		tokens.addAll(visit(ctx.from_clause()));

		if (ctx.where_clause() != null) {
			tokens.addAll(visit(ctx.where_clause()));
		}

		if (ctx.groupby_clause() != null) {
			tokens.addAll(visit(ctx.groupby_clause()));
		}

		if (ctx.having_clause() != null) {
			tokens.addAll(visit(ctx.having_clause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelect_clause(JpqlParser.Select_clauseContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>(ctx.select_item().size() * 2 + 20);

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));
		tokens.add(TOKEN_COUNT_FUNC);

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		List<JpaQueryParsingToken> selectItemTokens = new ArrayList<>(ctx.select_item().size() * 2);

		for (JpqlParser.Select_itemContext selectItem : ctx.select_item()) {

			if (!selectItemTokens.isEmpty()) {
				selectItemTokens.add(TOKEN_COMMA);
			}

			selectItemTokens.addAll(visit(selectItem));
		}

		SPACE(selectItemTokens);

		if (countProjection != null) {
			tokens.add(new JpaQueryParsingToken(countProjection));
		} else {

			if (ctx.DISTINCT() != null) {

				List<JpaQueryParsingToken> countSelection = QueryTransformers.filterCountSelection(selectItemTokens);

				if (countSelection.stream().anyMatch(jpqlToken -> jpqlToken.getToken().contains("new"))) {
					// constructor
					tokens.add(new JpaQueryParsingToken(primaryFromAlias));
				} else {
					// keep all the select items to distinct against
					tokens.addAll(countSelection);
				}
			} else {
				tokens.add(new JpaQueryParsingToken(primaryFromAlias));
			}
		}

		NOSPACE(tokens);
		tokens.add(TOKEN_CLOSE_PAREN);

		return tokens;
	}

}
