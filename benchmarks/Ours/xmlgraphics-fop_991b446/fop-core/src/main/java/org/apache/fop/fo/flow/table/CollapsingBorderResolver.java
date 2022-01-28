/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.fo.flow.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.fop.fo.properties.CommonBorderPaddingBackground;
import org.apache.fop.layoutmgr.table.CollapsingBorderModel;

/**
 * A class that implements the border-collapsing model.
 */
class CollapsingBorderResolver implements BorderResolver {

    private Table table;

    private CollapsingBorderModel collapsingBorderModel;

    /**
     * The previously registered row, either in the header or the body(-ies), but not in
     * the footer (handled separately).
     */
    private List<GridUnit> previousRow;

    private boolean firstInTable;

    private List<GridUnit> footerFirstRow;

    /** The last currently registered footer row. */
    private List<GridUnit> footerLastRow;

    private Resolver delegate;

    // Re-use the same ResolverInBody for every table-body
    // Important to properly handle firstInBody!!
    private Resolver resolverInBody = new ResolverInBody();

    private Resolver resolverInFooter;

    private List<ConditionalBorder> leadingBorders;

    private List<ConditionalBorder> trailingBorders;

    /* TODO Temporary hack for resolved borders in header */
    /* Currently the normal border is always used. */
    private List<GridUnit> headerLastRow;
    /* End of temporary hack */

    /**
     * Base class for delegate resolvers. Implementation of the State design pattern: the
     * treatment differs slightly whether we are in the table's header, footer or body. To
     * avoid complicated if statements, specialised delegate resolvers will be used
     * instead.
     */
    private abstract class Resolver {

        protected TablePart tablePart;

        protected boolean firstInPart;

        private BorderSpecification borderStartTableAndBody;
        private BorderSpecification borderEndTableAndBody;

        /**
         * Integrates border-before specified on the table and its column.
         *
         * @param row the first row of the table (in the header, or in the body if the
         * table has no header)
         * @param withNormal
         * @param withLeadingTrailing
         * @param withRest
         */
        void resolveBordersFirstRowInTable(List<GridUnit> row, boolean withNormal,
                boolean withLeadingTrailing, boolean withRest) {
            assert firstInTable;
            for (int i = 0; i < row.size(); i++) {
                TableColumn column = table.getColumn(i);
                row.get(i).integrateBorderSegment(
                        CommonBorderPaddingBackground.BEFORE, column, withNormal,
                        withLeadingTrailing, withRest);
            }
            firstInTable = false;
        }

        /**
         * Resolves border-after for the first row, border-before for the second one.
         *
         * @param rowBefore
         * @param rowAfter
         */
        void resolveBordersBetweenRows(List<GridUnit> rowBefore, List<GridUnit> rowAfter) {
            assert rowBefore != null && rowAfter != null;
            for (int i = 0; i < rowAfter.size(); i++) {
                GridUnit gu = rowAfter.get(i);
                if (gu.getRowSpanIndex() == 0) {
                    GridUnit beforeGU = rowBefore.get(i);
                    gu.resolveBorder(beforeGU, CommonBorderPaddingBackground.BEFORE);
                }
            }
        }

        /** Integrates the border-after of the part. */
        void resolveBordersLastRowInPart(List<GridUnit> row, boolean withNormal,
                boolean withLeadingTrailing, boolean withRest) {
            for (Object aRow : row) {
                ((GridUnit) aRow).integrateBorderSegment(CommonBorderPaddingBackground.AFTER,
                        tablePart, withNormal, withLeadingTrailing, withRest);
            }
        }

        /**
         * Integrates border-after specified on the table and its columns.
         *
         * @param row the last row of the footer, or of the last body if the table has no
         * footer
         * @param withNormal
         * @param withLeadingTrailing
         * @param withRest
         */
        void resolveBordersLastRowInTable(List<GridUnit> row, boolean withNormal,
                boolean withLeadingTrailing, boolean withRest) {
            for (int i = 0; i < row.size(); i++) {
                TableColumn column = table.getColumn(i);
                row.get(i).integrateBorderSegment(CommonBorderPaddingBackground.AFTER,
                        column, withNormal, withLeadingTrailing, withRest);
            }
        }

        /**
         * Integrates either border-before specified on the table and its columns if the
         * table has no header, or border-after specified on the cells of the header's
         * last row. For the case the grid unit are at the top of a page.
         *
         * @param row
         */
        void integrateLeadingBorders(List<GridUnit> row) {
            for (int i = 0; i < table.getNumberOfColumns(); i++) {
                GridUnit gu = row.get(i);
                ConditionalBorder border = leadingBorders.get(i);
                gu.integrateCompetingBorder(CommonBorderPaddingBackground.BEFORE, border,
                        false, true, true);
            }
        }

        /**
         * Integrates either border-after specified on the table and its columns if the
         * table has no footer, or border-before specified on the cells of the footer's
         * first row. For the case the grid unit are at the bottom of a page.
         *
         * @param row
         */
        void integrateTrailingBorders(List<GridUnit> row) {
            for (int i = 0; i < table.getNumberOfColumns(); i++) {
                GridUnit gu = row.get(i);
                ConditionalBorder border = trailingBorders.get(i);
                gu.integrateCompetingBorder(CommonBorderPaddingBackground.AFTER, border,
                        false, true, true);
            }
        }

        void startPart(TablePart part) {
            tablePart = part;
            firstInPart = true;
            borderStartTableAndBody = collapsingBorderModel.determineWinner(table.borderStart,
                    tablePart.borderStart);
            borderEndTableAndBody = collapsingBorderModel.determineWinner(table.borderEnd,
                    tablePart.borderEnd);
        }

        /**
         * Resolves the applicable borders for the given row.
         * <ul>
         * <li>Integrates the border-before/after of the containing table-row if any;</li>
         * <li>Integrates the border-before of the containing part, if first row;</li>
         * <li>Resolves border-start/end between grid units.</li>
         * </ul>
         *
         * @param row the row being finished
         * @param container the containing element
         */
        void endRow(List<GridUnit> row, TableCellContainer container) {
            BorderSpecification borderStart = borderStartTableAndBody;
            BorderSpecification borderEnd = borderEndTableAndBody;
            // Resolve before- and after-borders for the table-row
            if (container instanceof TableRow) {
                TableRow tableRow = (TableRow) container;
                for (Object aRow : row) {
                    GridUnit gu = (GridUnit) aRow;
                    boolean first = (gu.getRowSpanIndex() == 0);
                    boolean last = gu.isLastGridUnitRowSpan();
                    gu.integrateBorderSegment(CommonBorderPaddingBackground.BEFORE, tableRow,
                            first, first, true);
                    gu.integrateBorderSegment(CommonBorderPaddingBackground.AFTER, tableRow,
                            last, last, true);
                }
                borderStart = collapsingBorderModel.determineWinner(borderStart,
                        tableRow.borderStart);
                borderEnd = collapsingBorderModel.determineWinner(borderEnd,
                        tableRow.borderEnd);
            }
            if (firstInPart) {
                // Integrate the border-before of the part
                for (Object aRow : row) {
                    ((GridUnit) aRow).integrateBorderSegment(
                            CommonBorderPaddingBackground.BEFORE, tablePart, true, true, true);
                }
                firstInPart = false;
            }
            // Resolve start/end borders in the row
            Iterator guIter = row.iterator();
            GridUnit gu = (GridUnit) guIter.next();
            Iterator colIter = table.getColumns().iterator();
            TableColumn col = (TableColumn) colIter.next();
            gu.integrateBorderSegment(CommonBorderPaddingBackground.START, col);
            gu.integrateBorderSegment(CommonBorderPaddingBackground.START, borderStart);
            while (guIter.hasNext()) {
                GridUnit nextGU = (GridUnit) guIter.next();
                TableColumn nextCol = (TableColumn) colIter.next();
                if (gu.isLastGridUnitColSpan()) {
                    gu.integrateBorderSegment(CommonBorderPaddingBackground.END, col);
                    nextGU.integrateBorderSegment(CommonBorderPaddingBackground.START, nextCol);
                    gu.resolveBorder(nextGU, CommonBorderPaddingBackground.END);
                }
                gu = nextGU;
                col = nextCol;
            }
            gu.integrateBorderSegment(CommonBorderPaddingBackground.END, col);
            gu.integrateBorderSegment(CommonBorderPaddingBackground.END, borderEnd);
        }

        void endPart() {
            resolveBordersLastRowInPart(previousRow, true, true, true);
        }

        abstract void endTable();
    }

    private class ResolverInHeader extends Resolver {

        void endRow(List<GridUnit> row, TableCellContainer container) {
            super.endRow(row, container);
            if (previousRow != null) {
                resolveBordersBetweenRows(previousRow, row);
            } else {
                /*
                 * This is a bit hacky...
                 * The two only sensible values for border-before on the header's first row are:
                 * - at the beginning of the table (normal case)
                 * - if the header is repeated after each page break
                 * To represent those values we (ab)use the normal and the rest fields of
                 * ConditionalBorder. But strictly speaking this is not their purposes.
                 */
                for (Object aRow : row) {
                    ConditionalBorder borderBefore = ((GridUnit) aRow).borderBefore;
                    borderBefore.leadingTrailing = borderBefore.normal;
                    borderBefore.rest = borderBefore.normal;
                }
                resolveBordersFirstRowInTable(row, true, false, true);
            }
            previousRow = row;
        }

        void endPart() {
            super.endPart();
            leadingBorders = new ArrayList(table.getNumberOfColumns());
            /*
             * Another hack...
             * The border-after of a header is always the same. Leading and rest don't
             * apply to cells in the header since they are never broken. To ease
             * resolution we override the (normally unused) leadingTrailing and rest
             * fields of ConditionalBorder with the only sensible normal field. That way
             * grid units from the body will always resolve against the same, normal
             * header border.
             */
            for (Object aPreviousRow : previousRow) {
                ConditionalBorder borderAfter = ((GridUnit) aPreviousRow).borderAfter;
                borderAfter.leadingTrailing = borderAfter.normal;
                borderAfter.rest = borderAfter.normal;
                leadingBorders.add(borderAfter);
            }
            /* TODO Temporary hack for resolved borders in header */
            headerLastRow = previousRow;
            /* End of temporary hack */
        }

        void endTable() {
            throw new IllegalStateException();
        }
    }

    private class ResolverInFooter extends Resolver {

        void endRow(List<GridUnit> row, TableCellContainer container) {
            super.endRow(row, container);
            if (footerFirstRow == null) {
                footerFirstRow = row;
            } else {
                // There is a previous row
                resolveBordersBetweenRows(footerLastRow, row);
            }
            footerLastRow = row;
        }

        void endPart() {
            resolveBordersLastRowInPart(footerLastRow, true, true, true);
            trailingBorders = new ArrayList(table.getNumberOfColumns());
            // See same method in ResolverInHeader for an explanation of the hack
            for (Object aFooterFirstRow : footerFirstRow) {
                ConditionalBorder borderBefore = ((GridUnit) aFooterFirstRow).borderBefore;
                borderBefore.leadingTrailing = borderBefore.normal;
                borderBefore.rest = borderBefore.normal;
                trailingBorders.add(borderBefore);
            }
        }

        void endTable() {
            // Resolve after/before border between the last row of table-body and the
            // first row of table-footer
            resolveBordersBetweenRows(previousRow, footerFirstRow);
            // See endRow method in ResolverInHeader for an explanation of the hack
            for (Object aFooterLastRow : footerLastRow) {
                ConditionalBorder borderAfter = ((GridUnit) aFooterLastRow).borderAfter;
                borderAfter.leadingTrailing = borderAfter.normal;
                borderAfter.rest = borderAfter.normal;
            }
            resolveBordersLastRowInTable(footerLastRow, true, false, true);
        }
    }

    private class ResolverInBody extends Resolver {

        private boolean firstInBody = true;

        void endRow(List<GridUnit> row, TableCellContainer container) {
            super.endRow(row, container);
            if (firstInTable) {
                resolveBordersFirstRowInTable(row, true, true, true);
            } else {
                // Either there is a header, and then previousRow is set to the header's last row,
                // or this is not the first row in the body, and previousRow is not null
                resolveBordersBetweenRows(previousRow, row);
                integrateLeadingBorders(row);
            }
            integrateTrailingBorders(row);
            previousRow = row;
            if (firstInBody) {
                firstInBody = false;
                for (Object aRow : row) {
                    GridUnit gu = (GridUnit) aRow;
                    gu.borderBefore.leadingTrailing = gu.borderBefore.normal;
                }
            }
        }

        void endTable() {
            if (resolverInFooter != null) {
                resolverInFooter.endTable();
            } else {
                // Trailing and rest borders already resolved with integrateTrailingBorders
                resolveBordersLastRowInTable(previousRow, true, false, false);
            }
            for (Object aPreviousRow : previousRow) {
                GridUnit gu = (GridUnit) aPreviousRow;
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
    }

    CollapsingBorderResolver(Table table) {
        this.table = table;
        collapsingBorderModel = CollapsingBorderModel.getBorderModelFor(table.getBorderCollapse());
        firstInTable = true;
        // Resolve before and after borders between the table and each table-column
        int index = 0;
        do {
            TableColumn col = table.getColumn(index);
            // See endRow method in ResolverInHeader for an explanation of the hack
            col.borderBefore.integrateSegment(table.borderBefore, true, false, true);
            col.borderBefore.leadingTrailing = col.borderBefore.rest;
            col.borderAfter.integrateSegment(table.borderAfter, true, false, true);
            col.borderAfter.leadingTrailing = col.borderAfter.rest;
            /*
             * TODO The border resolution must be done only once for each table column,
             * even if it's repeated; otherwise, re-resolving against the table's borders
             * will lead to null border specifications.
             *
             * Eventually table columns should probably be cloned instead.
             */
            index += col.getNumberColumnsRepeated();
        } while (index < table.getNumberOfColumns());
    }

    /** {@inheritDoc} */
    public void endRow(List<GridUnit> row, TableCellContainer container) {
        delegate.endRow(row, container);
    }

    /** {@inheritDoc} */
    public void startPart(TablePart part) {
        if (part instanceof TableHeader) {
            delegate = new ResolverInHeader();
        } else {
            if (leadingBorders == null || table.omitHeaderAtBreak()) {
                // No header, leading borders determined by the table
                leadingBorders = new ArrayList(table.getNumberOfColumns());
                for (Object o : table.getColumns()) {
                    ConditionalBorder border = ((TableColumn) o).borderBefore;
                    leadingBorders.add(border);
                }
            }
            if (part instanceof TableFooter) {
                resolverInFooter = new ResolverInFooter();
                delegate = resolverInFooter;
            } else {
                if (trailingBorders == null || table.omitFooterAtBreak()) {
                    // No footer, trailing borders determined by the table
                    trailingBorders = new ArrayList(table.getNumberOfColumns());
                    for (Object o : table.getColumns()) {
                        ConditionalBorder border = ((TableColumn) o).borderAfter;
                        trailingBorders.add(border);
                    }
                }
                delegate = resolverInBody;
            }
        }
        delegate.startPart(part);
    }

    /** {@inheritDoc} */
    public void endPart() {
        delegate.endPart();
    }

    /** {@inheritDoc} */
    public void endTable() {
        delegate.endTable();
        delegate = null;
        /* TODO Temporary hack for resolved borders in header */
        if (headerLastRow != null) {
            for (Object aHeaderLastRow : headerLastRow) {
                GridUnit gu = (GridUnit) aHeaderLastRow;
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
        if (footerLastRow != null) {
            for (Object aFooterLastRow : footerLastRow) {
                GridUnit gu = (GridUnit) aFooterLastRow;
                gu.borderAfter.leadingTrailing = gu.borderAfter.normal;
            }
        }
        /* End of temporary hack */
    }
}
