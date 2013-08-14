/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2009 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.designer.core.actions.elements.distribute;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.Action;

import org.pentaho.reporting.designer.core.actions.AbstractElementSelectionAction;
import org.pentaho.reporting.designer.core.actions.ActionMessages;
import org.pentaho.reporting.designer.core.editor.report.drag.MoveDragOperation;
import org.pentaho.reporting.designer.core.editor.report.snapping.EmptySnapModel;
import org.pentaho.reporting.designer.core.model.CachedLayoutData;
import org.pentaho.reporting.designer.core.model.ModelUtility;
import org.pentaho.reporting.designer.core.model.selection.ReportSelectionModel;
import org.pentaho.reporting.designer.core.util.IconLoader;
import org.pentaho.reporting.designer.core.util.undo.MassElementStyleUndoEntry;
import org.pentaho.reporting.designer.core.util.undo.MassElementStyleUndoEntryBuilder;
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.event.ReportModelEvent;
import org.pentaho.reporting.engine.classic.core.util.geom.StrictGeomUtility;

public final class DistributeVerticalJustifyAction extends AbstractElementSelectionAction
{
  private static class ElementPositionComparator implements Comparator<Element>
  {
    public int compare(final Element o1, final Element o2)
    {
      final CachedLayoutData data1 = ModelUtility.getCachedLayoutData(o1);
      final long x1 = data1.getY();
      final CachedLayoutData data2 = ModelUtility.getCachedLayoutData(o2);
      final long x2 = data2.getY();
      if (x1 < x2)
      {
        return -1;
      }
      if (x1 > x2)
      {
        return +1;
      }
      return 0;
    }
  }

  public DistributeVerticalJustifyAction()
  {
    putValue(Action.NAME, ActionMessages.getString("DistributeVerticalJustifyAction.Text"));
    putValue(Action.SHORT_DESCRIPTION, ActionMessages.getString("DistributeVerticalJustifyAction.Description"));
    putValue(Action.MNEMONIC_KEY, ActionMessages.getOptionalMnemonic("DistributeVerticalJustifyAction.Mnemonic"));
    putValue(Action.SMALL_ICON, IconLoader.getInstance().getDistributeGapsVerticalIcon());
    putValue(Action.ACCELERATOR_KEY, ActionMessages.getOptionalKeyStroke("DistributeVerticalJustifyAction.Accelerator"));
  }

  protected void selectedElementPropertiesChanged(final ReportModelEvent event)
  {
  }

  /**
   * Invoked when an action occurs.
   */
  public void actionPerformed(final ActionEvent e)
  {
    final ReportSelectionModel model = getSelectionModel();
    if (model == null)
    {
      return;
    }
    final Element[] visualElements = model.getSelectedVisualElements();
    if (visualElements.length <= 2)
    {
      return;
    }

    final Element[] reportElements = ModelUtility.filterParents(visualElements);
    if (reportElements.length <= 2)
    {
      return;
    }

    Arrays.sort(reportElements, new ElementPositionComparator());
    final MassElementStyleUndoEntryBuilder builder = new MassElementStyleUndoEntryBuilder(reportElements);
    final Element[] carrier = new Element[1];

    long totalHeight = 0;
    long topmostY = Long.MAX_VALUE;
    long bottommostY = Long.MIN_VALUE;
    for (int i = 0; i < reportElements.length; i++)
    {
      final Element element = reportElements[i];
      final CachedLayoutData layoutData = ModelUtility.getCachedLayoutData(element);
      totalHeight += layoutData.getHeight();
      topmostY = Math.min(topmostY, layoutData.getY());
      bottommostY = Math.max(bottommostY, layoutData.getY() + layoutData.getHeight());
    }

    final long gap = (bottommostY - topmostY - totalHeight) / (reportElements.length - 1);
    long currentY = topmostY;
    for (int i = 0; i < reportElements.length; i++)
    {
      final Element reportElement = reportElements[i];
      final CachedLayoutData layoutData = ModelUtility.getCachedLayoutData(reportElement);
      final long height = layoutData.getHeight();

      carrier[0] = reportElement;
      final Point2D.Double originPoint = new Point2D.Double(StrictGeomUtility.toExternalValue(layoutData.getX()), 0);
      final MoveDragOperation mop = new MoveDragOperation
          (carrier, originPoint, EmptySnapModel.INSTANCE, EmptySnapModel.INSTANCE);
      mop.update(new Point2D.Double(0, StrictGeomUtility.toExternalValue(currentY)), 1);
      mop.finish();

      currentY += height + gap;
    }
    final MassElementStyleUndoEntry massElementStyleUndoEntry = builder.finish();
    getActiveContext().getUndo().addChange(ActionMessages.getString("DistributeVerticalJustifyAction.UndoName"), massElementStyleUndoEntry);
  }
}
