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

package com.github.javaparser.ast.observer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;

import static com.github.javaparser.utils.Utils.capitalize;

/**
 * Properties considered by the AstObserver
 */
public enum ObservableProperty {
    ANNOTATIONS,
    ANONYMOUS_CLASS_BODY,
    ARGUMENTS,
    BLOCK,
    BODY,
    CATCH_CLAUSES,
    CHECK,
    CLASS_BODY,
    CLASS_EXPR,
    COMMENT,
    COMMENTED_NODE,
    COMPARE,
    COMPONENT_TYPE,
    CONDITION,
    CONTENT,
    DEFAULT_VALUE,
    DIMENSION,
    ELEMENTS,
    ELSE_EXPR,
    ELSE_STMT,
    ENTRIES,
    EXPRESSION,
    EXTENDED_TYPES,
    FIELD,
    FINALLY_BLOCK,
    IDENTIFIER,
    IMPLEMENTED_TYPES,
    IMPORTS,
    INDEX,
    INITIALIZER,
    INNER,
    IS_INTERFACE,
    ITERABLE,
    IS_THIS,
    LABEL,
    LEFT,
    LEVELS,
    MEMBERS,
    MEMBER_VALUE,
    MODIFIERS,
    MESSAGE,
    NAME,
    OPERATOR,
    PACKAGE_DECLARATION,
    PAIRS,
    PARAMETER,
    PARAMETERS,
    ENCLOSING_PARAMETERS,
    QUALIFIER,
    RANGE,
    RESOURCES,
    RIGHT,
    SCOPE,
    SELECTOR,
    IS_ASTERISK,
    IS_STATIC,
    STATIC_MEMBER,
    STATEMENT,
    STATEMENTS,
    SUPER,
    TARGET,
    THEN_EXPR,
    THEN_STMT,
    THROWN_TYPES,
    TRY_BLOCK,
    TYPE,
    TYPES,
    TYPE_ARGUMENTS,
    TYPE_BOUND,
    CLASS_DECLARATION,
    TYPE_PARAMETERS,
    UPDATE,
    VALUE,
    VALUES,
    VARIABLE,
    VARIABLES,
    ELEMENT_TYPE,
    VAR_ARGS;

    public static ObservableProperty fromCamelCaseName(String camelCaseName) {
        Optional<ObservableProperty> observableProperty = Arrays.stream(values()).filter(v ->
                v.camelCaseName().equals(camelCaseName)).findFirst();
        if (observableProperty.isPresent()) {
            return observableProperty.get();
        } else {
            throw new IllegalArgumentException("No property found with the given camel case name: " + camelCaseName);
        }
    }

    public String camelCaseName() {
        String[] parts = this.name().split("_");
        StringBuffer sb = new StringBuffer();
        sb.append(parts[0].toLowerCase());
        for (int i=1;i<parts.length;i++) {
            sb.append(capitalize(parts[i].toLowerCase()));
        }
        return sb.toString();
    }

    public Node singleValueFor(Node node) {
        String getterName = "get" + capitalize(camelCaseName());
        try {
            return (Node)node.getClass().getMethod(getterName).invoke(node);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Unable to get single value for " + this.name() + " from " + node, e);
        }
    }
}
