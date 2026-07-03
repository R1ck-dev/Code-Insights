package com.projeto.codeinsights.infrastructure.metrica;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

/** Parser de teste alinhado ao nivel de linguagem de producao (Java 21). */
final class AnalisadorTestSupport {

    private AnalisadorTestSupport() {
    }

    static CompilationUnit parse(String codigo) {
        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        return parser.parse(codigo).getResult().orElseThrow();
    }
}
