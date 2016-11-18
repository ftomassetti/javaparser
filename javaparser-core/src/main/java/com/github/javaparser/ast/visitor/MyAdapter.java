package com.github.javaparser.ast.visitor;

import com.github.javaparser.ast.imports.*;

/**
 * Created by federico on 16/11/16.
 */
public class MyAdapter extends VoidVisitorAdapter {

    @Override
    public void visit(EmptyImportDeclaration n, Object arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(SingleStaticImportDeclaration n, Object arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(SingleTypeImportDeclaration n, Object arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(StaticImportOnDemandDeclaration n, Object arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(TypeImportOnDemandDeclaration n, Object arg) {
        super.visit(n, arg);
    }
}
