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

package org.apache.fop.render.gradient;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint;
import org.apache.batik.ext.awt.RadialGradientPaint;

import org.apache.xmlgraphics.java2d.color.ColorUtil;

import org.apache.fop.pdf.PDFDeviceColorSpace;

public final class GradientMaker {

    public interface DoubleFormatter {

        String formatDouble(double d);
    }

    private GradientMaker() { }

    public static Pattern makeLinearGradient(LinearGradientPaint gp,
            AffineTransform baseTransform, AffineTransform transform) {
        Point2D startPoint = gp.getStartPoint();
        Point2D endPoint = gp.getEndPoint();
        List<Double> coords = new java.util.ArrayList<Double>(4);
        coords.add(startPoint.getX());
        coords.add(startPoint.getY());
        coords.add(endPoint.getX());
        coords.add(endPoint.getY());
        return makeGradient(gp, coords, baseTransform, transform);
    }

    public static Pattern makeRadialGradient(RadialGradientPaint gradient,
            AffineTransform baseTransform, AffineTransform transform) {
        double radius = gradient.getRadius();
        Point2D center = gradient.getCenterPoint();
        Point2D focus = gradient.getFocusPoint();
        double dx = focus.getX() - center.getX();
        double dy = focus.getY() - center.getY();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d > radius) {
            // The focal point must be within the circle with
            // radius radius centered at center so limit it to that.
            double scale = (radius * .9999) / d;
            dx *= scale;
            dy *= scale;
        }
        List<Double> coords = new java.util.ArrayList<Double>(6);
        coords.add(center.getX() + dx);
        coords.add(center.getY() + dy);
        coords.add(0d);
        coords.add(center.getX());
        coords.add(center.getY());
        coords.add(radius);
        return makeGradient(gradient, coords, baseTransform, transform);
    }

    private static Pattern makeGradient(MultipleGradientPaint gradient, List<Double> coords,
            AffineTransform baseTransform, AffineTransform transform) {
        List<Double> matrix = makeTransform(gradient, baseTransform, transform);
        List<Float> bounds = makeBounds(gradient);
        List<Function> functions = makeFunctions(gradient);
        // Gradients are currently restricted to sRGB
        PDFDeviceColorSpace colorSpace = new PDFDeviceColorSpace(PDFDeviceColorSpace.DEVICE_RGB);
        Function function = new Function(null, null, functions, bounds, null);
        int shadingType = gradient instanceof LinearGradientPaint ? 2 : 3;
        Shading shading = new Shading(shadingType, colorSpace, coords, function);
        return new Pattern(2, shading, matrix);
    }

    private static List<Double> makeTransform(MultipleGradientPaint gradient,
            AffineTransform baseTransform, AffineTransform transform) {
        AffineTransform gradientTransform = new AffineTransform(baseTransform);
        gradientTransform.concatenate(transform);
        gradientTransform.concatenate(gradient.getTransform());
        List<Double> matrix = new ArrayList<Double>(6);
        double[] m = new double[6];
        gradientTransform.getMatrix(m);
        for (double d : m) {
            matrix.add(d);
        }
        return matrix;
    }

    private static Color getsRGBColor(Color c) {
        // Color space must be consistent, so convert to sRGB if necessary
        // TODO really?
        return c.getColorSpace().isCS_sRGB() ? c : ColorUtil.toSRGBColor(c);
    }

    private static List<Float> makeBounds(MultipleGradientPaint gradient) {
        float[] fractions = gradient.getFractions();
        List<Float> bounds = new ArrayList<Float>(fractions.length);
        for (float offset : fractions) {
            if (0f < offset) {
                bounds.add(offset);
            }
        }
        float last = bounds.get(bounds.size() - 1);
        if (last == 1f) {
            bounds.remove(bounds.size() - 1);
        }
        return bounds;
    }

    private static List<Function> makeFunctions(MultipleGradientPaint gradient) {
        List<Color> colors = makeColors(gradient);
        List<Function> functions = new ArrayList<Function>();
        for (int currentPosition = 0, lastPosition = colors.size() - 1;
                currentPosition < lastPosition;
                currentPosition++) {
            Color currentColor = colors.get(currentPosition);
            Color nextColor = colors.get(currentPosition + 1);
            float[] c0 = currentColor.getColorComponents(null);
            float[] c1 = nextColor.getColorComponents(null);
            Function function = new Function(null, null, c0, c1, 1.0);
            functions.add(function);
        }
        return functions;
    }

    private static List<Color> makeColors(MultipleGradientPaint gradient) {
        Color[] svgColors = gradient.getColors();
        List<Color> gradientColors = new ArrayList<Color>(svgColors.length + 2);
        float[] fractions = gradient.getFractions();
        if (fractions[0] > 0f) {
            gradientColors.add(getsRGBColor(svgColors[0]));
        }
        for (Color c : svgColors) {
            gradientColors.add(getsRGBColor(c));
        }
        if (fractions[fractions.length - 1] < 1f) {
            gradientColors.add(getsRGBColor(svgColors[svgColors.length - 1]));
        }
        return gradientColors;
    }

    static void outputDoubles(StringBuilder out, DoubleFormatter doubleFormatter,
            List<? extends Number> numbers) {
        out.append("[ ");
        for (Number n : numbers) {
            out.append(doubleFormatter.formatDouble(n.doubleValue()));
            out.append(" ");
        }
        out.append("]");
    }

}
