/*
 * Copyright 2024 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * {@link ParsedQueryIntrospector} for HQL queries.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("UnreachableCode")
class HqlQueryIntrospector extends HqlBaseVisitor<Void> implements ParsedQueryIntrospector {

	private final HqlQueryRenderer renderer = new HqlQueryRenderer();

	private @Nullable String primaryFromAlias = null;
	private @Nullable List<JpaQueryParsingToken> projection;
	private boolean projectionProcessed;
	private boolean hasConstructorExpression = false;

	@Override
	public String getAlias() {
		return primaryFromAlias;
	}

	@Override
	public List<JpaQueryParsingToken> getProjection() {
		return projection == null ? Collections.emptyList() : projection;
	}

	@Override
	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	@Override
	public Void visitSelectClause(HqlParser.SelectClauseContext ctx) {

		List<HqlParser.SelectionContext> selections = ctx.selectionList().selection();
		List<JpaQueryParsingToken> selectItemTokens = new ArrayList<>(selections.size());

		for (HqlParser.SelectionContext selection : selections) {

			if (!selectItemTokens.isEmpty()) {
				NOSPACE(selectItemTokens);
				selectItemTokens.add(TOKEN_COMMA);
			}

			selectItemTokens.addAll(renderer.visitSelection(selection));
		}

		if (!projectionProcessed) {
			projection = selectItemTokens;
			projectionProcessed = true;
		}

		return super.visitSelectClause(ctx);
	}

	@Override
	public Void visitFromRoot(HqlParser.FromRootContext ctx) {

		HqlParser.VariableContext variable = ctx.variable();

		if (variable != null && (ctx.entityName() != null || ctx.subquery() != null)) {

			if (primaryFromAlias == null) {
				primaryFromAlias = (variable.identifier() != null ? variable.identifier() : variable.reservedWord()).getText();
			}
		}

		return super.visitFromRoot(ctx);
	}

	@Override
	public Void visitInstantiation(HqlParser.InstantiationContext ctx) {

		hasConstructorExpression = true;

		return super.visitInstantiation(ctx);
	}

}
