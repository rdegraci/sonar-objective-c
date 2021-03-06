/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 François Helg, Cyril Picat and OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.objectivec;

import java.io.File;
import java.util.Collection;

import org.sonar.objectivec.api.ObjectiveCGrammar;
import org.sonar.objectivec.api.ObjectiveCMetric;
import org.sonar.objectivec.parser.ObjectiveCParser;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByType;

import com.sonar.sslr.api.CommentAnalyser;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.squid.AstScanner;
import com.sonar.sslr.squid.SquidAstVisitor;
import com.sonar.sslr.squid.SquidAstVisitorContextImpl;
import com.sonar.sslr.squid.metrics.CommentsVisitor;
import com.sonar.sslr.squid.metrics.LinesOfCodeVisitor;
import com.sonar.sslr.squid.metrics.LinesVisitor;

public class ObjectiveCAstScanner {

    private ObjectiveCAstScanner() {
    }

    /**
     * Helper method for testing checks without having to deploy them on a Sonar instance.
     */
    public static SourceFile scanSingleFile(File file, SquidAstVisitor<ObjectiveCGrammar>... visitors) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("File '" + file + "' not found.");
        }
        AstScanner<ObjectiveCGrammar> scanner = create(new ObjectiveCConfiguration(), visitors);
        scanner.scanFile(file);
        Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
        if (sources.size() != 1) {
            throw new IllegalStateException("Only one SourceFile was expected whereas " + sources.size() + " has been returned.");
        }
        return (SourceFile) sources.iterator().next();
    }

    public static AstScanner<ObjectiveCGrammar> create(ObjectiveCConfiguration conf, SquidAstVisitor<ObjectiveCGrammar>... visitors) {
        final SquidAstVisitorContextImpl<ObjectiveCGrammar> context = new SquidAstVisitorContextImpl<ObjectiveCGrammar>(new SourceProject("Objective-C Project"));
        final Parser<ObjectiveCGrammar> parser = ObjectiveCParser.create(conf);

        AstScanner.Builder<ObjectiveCGrammar> builder = AstScanner.<ObjectiveCGrammar> builder(context).setBaseParser(parser);

        /* Metrics */
        builder.withMetrics(ObjectiveCMetric.values());

        /* Comments */
        builder.setCommentAnalyser(
                new CommentAnalyser() {
                    @Override
                    public boolean isBlank(String line) {
                        for (int i = 0; i < line.length(); i++) {
                            if (Character.isLetterOrDigit(line.charAt(i))) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public String getContents(String comment) {
                        return comment.startsWith("//") ? comment.substring(2) : comment.substring(2, comment.length() - 2);
                    }
                });

        /* Files */
        builder.setFilesMetric(ObjectiveCMetric.FILES);

        /* Metrics */
        builder.withSquidAstVisitor(new LinesVisitor<ObjectiveCGrammar>(ObjectiveCMetric.LINES));
        builder.withSquidAstVisitor(new LinesOfCodeVisitor<ObjectiveCGrammar>(ObjectiveCMetric.LINES_OF_CODE));
        builder.withSquidAstVisitor(CommentsVisitor.<ObjectiveCGrammar> builder().withCommentMetric(ObjectiveCMetric.COMMENT_LINES)
                .withBlankCommentMetric(ObjectiveCMetric.COMMENT_BLANK_LINES)
                .withNoSonar(true)
                .withIgnoreHeaderComment(conf.getIgnoreHeaderComments())
                .build());

        return builder.build();
    }

}
