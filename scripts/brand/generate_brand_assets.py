#!/usr/bin/env python3
"""Generates Fern Wallet's icon and logo assets from one vector definition.

Outputs (paths relative to the repo root):
  - Launcher icons (adaptive foreground + Material You monochrome) for every app-icon
    choice:  shared/ui/core/src/main/res/drawable/fern_icon_<name>_foreground.xml
  - In-app previews with background: shared/ui/core/src/main/res/drawable/fern_icon_<name>.xml
  - Themed-icon silhouette: app/src/main/res/drawable/ic_launcher_monochrome.xml
  - In-app logo (onboarding): shared/ui/core/src/main/res/drawable/ivy_wallet_logo.xml
  - Play Store icon: app/src/main/ic_launcher-playstore.png
  - GitHub branding: docs/branding/*.png

Requires Pillow for the PNGs:  pip install pillow
Usage: python3 scripts/brand/generate_brand_assets.py
"""
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# ---- Artwork geometry (1024 x 1024 design grid, traced from the supplied logo) ----
def rrect(x0,y0,x1,y1,r):
    k=0.5523*r
    return [('M',(x0+r,y0)),('L',(x1-r,y0)),('C',(x1-r+k,y0),(x1,y0+r-k),(x1,y0+r)),
            ('L',(x1,y1-r)),('C',(x1,y1-r+k),(x1-r+k,y1),(x1-r,y1)),('L',(x0+r,y1)),
            ('C',(x0+r-k,y1),(x0,y1-r+k),(x0,y1-r)),('L',(x0,y0+r)),('C',(x0,y0+r-k),(x0+r-k,y0),(x0+r,y0)),('Z',)]
def circle(cx,cy,r):
    k=0.5523*r
    return [('M',(cx+r,cy)),('C',(cx+r,cy+k),(cx+k,cy+r),(cx,cy+r)),('C',(cx-k,cy+r),(cx-r,cy+k),(cx-r,cy)),
            ('C',(cx-r,cy-k),(cx-k,cy-r),(cx,cy-r)),('C',(cx+k,cy-r),(cx+r,cy-k),(cx+r,cy)),('Z',)]
CARD=[('M',(240,292)),('L',(700,140)),('C',(752,124),(790,160),(790,215)),('L',(790,352)),('L',(240,352)),('Z',)]
STRAP=[('M',(268,292)),('L',(790,292)),('L',(790,352)),('L',(268,352)),('C',(251,352),(238,339),(238,322)),('C',(238,305),(251,292),(268,292)),('Z',)]
BODY=rrect(172,352,852,842,72)
CLASP=rrect(700,495,895,688,48)
DOT=circle(797,591,44)
LEAF_BIG=[('M',(295,835)),('C',(290,640),(390,490),(545,452)),('C',(612,436),(655,415),(683,380)),
          ('C',(675,470),(625,545),(500,566)),('C',(392,586),(330,645),(303,760)),('Z',)]
LEAF_SMALL=[('M',(300,832)),('C',(330,700),(420,612),(605,570)),('C',(575,690),(470,792),(300,836)),('Z',)]

# The first entry is the default launcher icon; the others are user-selectable in Settings.
PALETTES={
 # name: background, card, strap, body, leafBig, leafSmall, clasp, dot
 'default':dict(bg='#F7F9F8',card='#4AF2B2',strap='#03291F',body='#077057',leaf='#6EF5AE',leaf2='#3FDC94',clasp='#03291F',dot='#73F2AE'),
 'mono':   dict(bg='#000000',card='#FFFFFF',strap='#000000',body='#FFFFFF',leaf='#000000',leaf2='#000000',clasp='#000000',dot='#FFFFFF',claspStroke='#FFFFFF'),
 'graphite':dict(bg='#000000',card='#E4E4E4',strap='#0B0B0B',body='#2F2F2F',leaf='#FFFFFF',leaf2='#BDBDBD',clasp='#121212',dot='#DCDCDC'),
 'midnight':dict(bg='#000000',card='#40EFAE',strap='#021F17',body='#085A44',leaf='#62F5A8',leaf2='#36D88D',clasp='#021F17',dot='#6CF5AB'),
 # Wordmark badge only (dark green tile).
 'badge':  dict(bg='#0A4A39',card='#5BEFA9',strap='#03291F',body='#0B6B52',leaf='#6EF5AE',leaf2='#3FDC94',clasp='#03291F',dot='#73F2AE'),
}
APP_ICONS=['default','mono','graphite','midnight']
LAYERS=[('card',CARD),('strap',STRAP),('body',BODY),('leaf',LEAF_BIG),('leaf2',LEAF_SMALL),('clasp',CLASP),('dot',DOT)]

def fmt(v): return f"{v:.1f}".rstrip('0').rstrip('.')
def path_data(segs):
    out=[]
    for s in segs:
        if s[0]=='Z': out.append('Z')
        else: out.append(s[0]+' '.join(f"{fmt(p[0])},{fmt(p[1])}" for p in s[1:]))
    return ''.join(out)

# Adaptive icon: 108dp viewport; artwork centred and scaled to stay inside the 66dp safe zone.
CX,CY=533.5,491.0
def group_attrs(scale):
    return f'android:scaleX="{scale}" android:scaleY="{scale}" android:translateX="{fmt(54-CX*scale)}" android:translateY="{fmt(54-CY*scale)}"'

def vector(pal, scale=0.066, size=108, with_bg=False, header=""):
    p=PALETTES[pal]; body=[]
    if with_bg:
        body.append(f'    <path android:fillColor="{p["bg"]}" android:pathData="M0,0h108v108h-108z" />')
    body.append(f'    <group {group_attrs(scale)}>')
    for key,segs in LAYERS:
        if key=='clasp' and 'claspStroke' in p:
            body.append(f'        <path android:fillColor="{p[key]}" android:strokeColor="{p["claspStroke"]}" android:strokeWidth="6" android:pathData="{path_data(segs)}" />')
        else:
            body.append(f'        <path android:fillColor="{p[key]}" android:pathData="{path_data(segs)}" />')
    body.append('    </group>')
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Fern Wallet logo{header}. Generated from the brand artwork; do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{size}dp"
    android:height="{size}dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
''' + '\n'.join(body) + '\n</vector>\n'

def monochrome_vector():
    # Themed (Material You) icon: silhouette of the wallet with the leaves cut out (even-odd).
    segs=CARD+STRAP[:0]+BODY+CLASP+LEAF_BIG+LEAF_SMALL
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Fern Wallet themed-icon silhouette. Generated; do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group {group_attrs(0.066)}>
        <path android:fillColor="#FF000000" android:pathData="{path_data(CARD)}" />
        <path android:fillColor="#FF000000" android:fillType="evenOdd" android:pathData="{path_data(BODY+LEAF_BIG+LEAF_SMALL)}" />
        <path android:fillColor="#FF000000" android:pathData="{path_data(CLASP)}" />
    </group>
</vector>
'''

# ---- Raster rendering (preview, Play Store icon, wordmark) ----
def flatten(segs, tf):
    polys=[]; cur=[]; pos=None
    for s in segs:
        if s[0]=='M': cur=[tf(s[1])]; pos=s[1]
        elif s[0]=='L': cur.append(tf(s[1])); pos=s[1]
        elif s[0]=='C':
            p0,p1,p2,p3=pos,s[1],s[2],s[3]
            for i in range(1,25):
                t=i/24; a=(1-t)**3; b=3*(1-t)**2*t; c=3*(1-t)*t*t; d=t**3
                cur.append(tf((a*p0[0]+b*p1[0]+c*p2[0]+d*p3[0], a*p0[1]+b*p1[1]+c*p2[1]+d*p3[1])))
            pos=p3
        elif s[0]=='Z': polys.append(cur); cur=[]
    return polys
def hexc(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))+(255,)

def render_art(pal, size, scale_px, cx, cy, img=None, bg=True, shadow=False):
    """Renders the artwork with its centre at (cx,cy); scale_px = px per design unit."""
    ss=4; W=size if isinstance(size,tuple) else (size,size)
    p=PALETTES[pal]
    canvas=img.resize((W[0]*ss,W[1]*ss)) if img is not None else Image.new('RGBA',(W[0]*ss,W[1]*ss),hexc(p['bg']) if bg else (0,0,0,0))
    tf=lambda pt: ((cx+(pt[0]-CX)*scale_px)*ss, (cy+(pt[1]-CY)*scale_px)*ss)
    if shadow:
        sh=Image.new('RGBA',canvas.size,(0,0,0,0)); sd=ImageDraw.Draw(sh)
        off=lambda pt: (tf(pt)[0], tf(pt)[1]+18*scale_px*ss)
        for poly in flatten(BODY,off)+flatten(CARD,off): sd.polygon(poly, fill=(0,0,0,60))
        canvas=Image.alpha_composite(canvas, sh.filter(ImageFilter.GaussianBlur(28*scale_px*ss)))
    d=ImageDraw.Draw(canvas)
    for key,segs in LAYERS:
        for poly in flatten(segs,tf):
            d.polygon(poly, fill=hexc(p[key]))
            if key=='clasp' and 'claspStroke' in p:
                d.line(poly+[poly[0]], fill=hexc(p['claspStroke']), width=max(1,int(6*scale_px*ss)))
    return canvas.resize(W, Image.LANCZOS)

def adaptive_preview(pal, size=256):
    # How a launcher shows it: 108dp canvas, masked to the central 72dp circle.
    full=render_art(pal, size*108//72, (size*108/72)/108*0.066*1, size*108//144, size*108//144)
    c=(full.size[0]-size)//2; full=full.crop((c,c,c+size,c+size))
    m=Image.new('L',(size*4,size*4),0); ImageDraw.Draw(m).ellipse((0,0,size*4-1,size*4-1),fill=255)
    full.putalpha(m.resize((size,size),Image.LANCZOS)); return full

def badge(pal, size, radius_frac=0.225, art_frac=0.74, bg=None):
    """Rounded-square app icon (Play Store / README)."""
    p=PALETTES[pal]; ss=4
    img=Image.new('RGBA',(size*ss,size*ss),(0,0,0,0)); d=ImageDraw.Draw(img)
    d.rounded_rectangle((0,0,size*ss-1,size*ss-1), int(size*ss*radius_frac), fill=hexc(bg or p['bg']))
    img=img.resize((size,size),Image.LANCZOS)
    scale=size*art_frac/723.0
    return render_art(pal,size,scale,size/2,size/2,img=img,shadow=(pal=='default'))


def wordmark(dark=False, height=360):
    """Badge + 'Fern Wallet' set in Raleway (the app's display face)."""
    from PIL import ImageFont
    font_dir=ROOT/'shared/ui/core/src/main/res/font'
    ink='#E9F7F1' if dark else '#062A20'
    ink2='#7FE3B8' if dark else '#0B6B52'
    pad=int(height*0.12); badge_size=height-2*pad
    f_bold=ImageFont.truetype(str(font_dir/'raleway_bold.ttf'), int(height*0.34))
    f_reg=ImageFont.truetype(str(font_dir/'raleway_regular.ttf'), int(height*0.34))
    gap=int(height*0.10); word_gap=int(height*0.06)
    w1=f_bold.getbbox('Fern')[2]; w2=f_reg.getbbox('Wallet')[2]
    width=pad+badge_size+gap+w1+word_gap+w2+pad
    img=Image.new('RGBA',(width,height),(0,0,0,0))
    b=badge('badge', badge_size, radius_frac=0.24, art_frac=0.78)
    img.paste(b,(pad,pad),b)
    d=ImageDraw.Draw(img)
    asc,desc=f_bold.getmetrics(); y=(height-(asc+desc))//2
    x=pad+badge_size+gap
    d.text((x,y),'Fern',font=f_bold,fill=ink)
    d.text((x+w1+word_gap,y),'Wallet',font=f_reg,fill=ink2)
    return img

def social_preview():
    """1280x640 GitHub social preview (Settings -> General -> Social preview)."""
    from PIL import ImageFont
    font_dir=ROOT/'shared/ui/core/src/main/res/font'
    img=Image.new('RGBA',(1280,640),hexc('#F4F8F6'))
    wm=wordmark(height=300)
    img.paste(wm,((1280-wm.size[0])//2,150),wm)
    f=ImageFont.truetype(str(font_dir/'opensans_semibold.ttf'),34)
    t='Open-source money manager for Android. No ads, no trackers.'
    w=f.getbbox(t)[2]
    ImageDraw.Draw(img).text(((1280-w)//2,480),t,font=f,fill=hexc('#3D5A50'))
    return img

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)
    print('wrote', path.relative_to(ROOT))

def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)
    print('wrote', path.relative_to(ROOT))

if __name__=='__main__':
    from pathlib import Path
    ROOT=Path(__file__).resolve().parents[2]
    ui=ROOT/'shared/ui/core/src/main/res/drawable'
    for name in APP_ICONS:
        write(ui/f'fern_icon_{name}_foreground.xml', vector(name, header=f' ({name}, adaptive foreground)'))
        # Launchers show the central 72 of 108dp, so previews scale the art by 108/72.
        write(ui/f'fern_icon_{name}.xml', vector(name, scale=0.066 * 1.5, with_bg=True, size=48, header=f' ({name}, with background)'))
    write(ROOT/'app/src/main/res/drawable/ic_launcher_monochrome.xml', monochrome_vector())
    # Quick Settings tile icon must be a single-color silhouette
    write(ui/'ic_tile_fern.xml', monochrome_vector().replace('#FF000000','#FFFFFFFF'))
    write(ui/'ivy_wallet_logo.xml', vector('default', scale=0.13, size=96, header=' (in-app)'))
    full=Image.new('RGBA',(512,512),hexc(PALETTES['default']['bg']))
    save(render_art('default',512,512*0.74/723.0,256,256,img=full,shadow=True), ROOT/'app/src/main/ic_launcher-playstore.png')
    br=ROOT/'docs/branding'
    save(badge('default',256), br/'icon.png')
    for name in APP_ICONS:
        save(badge(name,192), br/f'icon-{name}.png')
    save(wordmark(), br/'wordmark-light.png')
    save(wordmark(dark=True), br/'wordmark-dark.png')
    save(social_preview(), br/'social-preview.png')
