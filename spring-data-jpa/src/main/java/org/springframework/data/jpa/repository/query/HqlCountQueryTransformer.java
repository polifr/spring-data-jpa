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
 * An ANTLR {@link org.antlr.v4.runtime.tree.ParseTreeVisitor} that transforms a parsed HQL query into a
 * {@code COUNT(…)} query.
 *
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.1
 */
@SuppressWarnings("ConstantValue")
class HqlCountQueryTransformer extends HqlQueryRenderer {

	private final @Nullable String countProjection;
	private final @Nullable String primaryFromAlias;

	HqlCountQueryTransformer(@Nullable String countProjection, @Nullable String primaryFromAlias) {
		this.countProjection = countProjection;
		this.primaryFromAlias = primaryFromAlias;
	}

	@Override
	public List<JpaQueryParsingToken> visitOrderedQuery(HqlParser.OrderedQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		if (ctx.query() != null) {
			tokens.addAll(visit(ctx.query()));
		} else if (ctx.queryExpression() != null) {

			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.queryExpression()));
			tokens.add(TOKEN_CLOSE_PAREN);
		}

		if (ctx.queryOrder() != null) {
			tokens.addAll(visit(ctx.queryOrder()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFromQuery(HqlParser.FromQueryContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		if (!isSubquery(ctx) && ctx.selectClause() == null) {

			tokens.add(TOKEN_SELECT_COUNT);

			if (countProjection != null) {
				tokens.add(new JpaQueryParsingToken(countProjection));
			} else {
				if (primaryFromAlias == null) {
					tokens.add(TOKEN_DOUBLE_UNDERSCORE);
					NOSPACE(tokens);
				} else {
					tokens.add(new JpaQueryParsingToken(primaryFromAlias, false));
				}

			}

			tokens.add(TOKEN_CLOSE_PAREN);
		}

		if (ctx.fromClause() != null) {
			tokens.addAll(visit(ctx.fromClause()));
		}

		if (ctx.whereClause() != null) {
			tokens.addAll(visit(ctx.whereClause()));
		}

		if (ctx.groupByClause() != null) {
			tokens.addAll(visit(ctx.groupByClause()));
		}

		if (ctx.havingClause() != null) {
			tokens.addAll(visit(ctx.havingClause()));
		}

		if (ctx.selectClause() != null) {
			tokens.addAll(visit(ctx.selectClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitQueryOrder(HqlParser.QueryOrderContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		if (ctx.limitClause() != null) {
			SPACE(tokens);
			tokens.addAll(visit(ctx.limitClause()));
		}

		if (ctx.offsetClause() != null) {
			tokens.addAll(visit(ctx.offsetClause()));
		}

		if (ctx.fetchClause() != null) {
			tokens.addAll(visit(ctx.fetchClause()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitFromRoot(HqlParser.FromRootContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		if (ctx.entityName() != null) {

			tokens.addAll(visit(ctx.entityName()));

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));

			} else {

				tokens.add(TOKEN_AS);
				tokens.add(TOKEN_DOUBLE_UNDERSCORE);
			}
		} else if (ctx.subquery() != null) {

			if (ctx.LATERAL() != null) {
				tokens.add(new JpaQueryParsingToken(ctx.LATERAL()));
			}
			tokens.add(TOKEN_OPEN_PAREN);
			tokens.addAll(visit(ctx.subquery()));
			tokens.add(TOKEN_CLOSE_PAREN);

			if (ctx.variable() != null) {
				tokens.addAll(visit(ctx.variable()));
			}
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitJoin(HqlParser.JoinContext ctx) {

		List<JpaQueryParsingToken> tokens = new ArrayList<>(visit(ctx.joinType()));
		tokens.add(new JpaQueryParsingToken(ctx.JOIN()));

		tokens.addAll(visit(ctx.joinTarget()));

		if (ctx.joinRestriction() != null) {
			tokens.addAll(visit(ctx.joinRestriction()));
		}

		return tokens;
	}

	@Override
	public List<JpaQueryParsingToken> visitSelectClause(HqlParser.SelectClauseContext ctx) {

		List<JpaQueryParsingToken> tokens = newArrayList();

		tokens.add(new JpaQueryParsingToken(ctx.SELECT()));

		if (!isSubquery(ctx)) {
			tokens.add(TOKEN_COUNT_FUNC);

			if (countProjection != null) {
				tokens.add(new JpaQueryParsingToken(countProjection));
			}
		}

		if (ctx.DISTINCT() != null) {
			tokens.add(new JpaQueryParsingToken(ctx.DISTINCT()));
		}

		List<JpaQueryParsingToken> selectionListTokens = visit(ctx.selectionList());

		if (!isSubquery(ctx)) {

			if (countProjection == null) {

				if (ctx.DISTINCT() != null) {

					List<JpaQueryParsingToken> countSelection = QueryTransformers.filterCountSelection(selectionListTokens);

					if (countSelection.stream().anyMatch(hqlToken -> hqlToken.getToken().contains("new"))) {
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
		} else {
			tokens.addAll(selectionListTokens);
		}

		return tokens;
	}

	static <T> ArrayList<T> newArrayList() {
		return new ArrayList<>();
	}

}
