/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2016 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.printer.concretesyntaxmodel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.observer.ObservableProperty;
import com.github.javaparser.printer.ConcreteSyntaxModel;
import com.github.javaparser.printer.SourcePrinter;

import java.util.function.Predicate;

public class CsmConditional implements CsmElement {
    private Condition condition;
    private ObservableProperty property;
    private CsmElement thenElement;
    private CsmElement elseElement;

    public enum Condition {
        IS_EMPTY,
        IS_NOT_EMPTY,
        IS_PRESENT,
        FLAG
    }

    public CsmConditional(ObservableProperty property, Condition condition, CsmElement thenElement, CsmElement elseElement) {
        this.property = property;
        this.condition = condition;
        this.thenElement = thenElement;
        this.elseElement = elseElement;
    }

    public CsmConditional(ObservableProperty property, Condition condition, CsmElement thenElement) {
        this(property, condition, thenElement, new CsmNone());
    }

    @Override
    public void prettyPrint(Node node, SourcePrinter printer) {
        /*boolean test;
        if (condition != null) {
            if (condition.isSingle()) {
                test = condition.singlePropertyFor(node) != null;
            } else {
                test = condition.listValueFor(node) != null && !condition.listValueFor(node).isEmpty();
            }
        } else {
            test = predicateCondition.test(node);
        }
        if (test) {
            thenElement.prettyPrint(node, printer);
        } else {
            if (elseElement != null) {
                elseElement.prettyPrint(node, printer);
            }
        }*/
        throw new UnsupportedOperationException();
    }
}