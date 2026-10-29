package com.anubhab.mountaintv;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Cached 60% continuous corners. Geometry adapted from figma-squircle (MIT,
 * Copyright 2021 Tien Pham); notice in assets/FIGMA-SQUIRCLE-LICENSE.txt. */
final class SmoothSurfaceDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float radius;
    private final int[] colors;
    SmoothSurfaceDrawable(int fill, float radius) { this(new int[]{fill},radius); }
    SmoothSurfaceDrawable(int[] colors, float radius) { this.colors=colors; this.radius=radius; paint.setColor(colors[0]); }
    @Override protected void onBoundsChange(Rect b) {
        continuousPath(path,new RectF(b),radius);
        if(colors.length>1) paint.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,colors,null,Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas) { canvas.drawPath(path,paint); }
    @Override public void getOutline(Outline outline) {
        if(!path.isEmpty() && path.isConvex()) outline.setConvexPath(path);
        else outline.setRoundRect(getBounds(),radius);
    }
    @Override public void setAlpha(int a) { paint.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }

    static void continuousPath(Path path,RectF bounds,float requestedRadius) {
        path.reset(); float w=bounds.width(),h=bounds.height();
        if(w<=0 || h<=0) return;
        float r=Math.min(requestedRadius,Math.min(w,h)/3.2f),s=.6f;
        if(r<=0) { path.addRect(bounds,Path.Direction.CW); return; }
        float p=(1+s)*r, beta=(float)Math.PI/4*s;
        float arc=(float)(Math.sin(Math.PI/4*(1-s))*r*Math.sqrt(2));
        float c=(float)(r*Math.tan(beta/2)*Math.cos(beta)),d=(float)(c*Math.tan(beta));
        float b=(p-arc-c-d)/3,a=2*b;
        path.moveTo(w-p,0);
        for(int turn=0;turn<4;turn++) {
            float ox=(turn<2?w:0),oy=(turn==1||turn==2?h:0);
            if(turn>0) path.lineTo(x(ox,-p,0,turn),y(oy,-p,0,turn));
            path.cubicTo(x(ox,-p+a,0,turn),y(oy,-p+a,0,turn),x(ox,-p+a+b,0,turn),y(oy,-p+a+b,0,turn),x(ox,-p+a+b+c,d,turn),y(oy,-p+a+b+c,d,turn));
            float cx=x(ox,-r,r,turn),cy=y(oy,-r,r,turn);
            path.arcTo(cx-r,cy-r,cx+r,cy+r,-90+27+90*turn,36,false);
            path.cubicTo(x(ox,0,p-a-b,turn),y(oy,0,p-a-b,turn),x(ox,0,p-a,turn),y(oy,0,p-a,turn),x(ox,0,p,turn),y(oy,0,p,turn));
        }
        path.close(); path.offset(bounds.left,bounds.top);
    }
    private static float x(float origin,float x,float y,int turn) { return origin+(turn==0?x:turn==1?-y:turn==2?-x:y); }
    private static float y(float origin,float x,float y,int turn) { return origin+(turn==0?y:turn==1?x:turn==2?-y:-x); }
}
