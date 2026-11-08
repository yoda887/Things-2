"""Иконка Things — единственный источник. Геометрия снята с эталона (кадр 10,6 с video-inbox.mp4 и образец 72 px)
в единицах иконки 0..100 (вся иконка = 100×100).

Выдаёт:
  docs/icon/things_icon.svg                       — мастер, редактируемый (пути в абсолютных координатах)
  app/src/main/res/drawable/ic_launcher_*.xml     — адаптивная иконка Android (0..100 → видимая зона 18..90 из 108)
  <DESKTOP>/src/desktopMain/resources/things_icon.png, <DESKTOP>/icons/things.ico — растр для десктопа (та же геометрия через PIL)

Запуск из корня репозитория:  python docs/icon/icon_build.py [путь к Things Desktop]
"""
import os, sys
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DESKTOP = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(ROOT), "Things Desktop")

# ---- Цвета ----
BG_TOP, BG_BOT = "#5891F6", "#3476DE"
NAVY = "#0E2D5E"            # рамка
RIM = "#08193A"             # тёмная кромка рамки вокруг белого поля
LIP = "#4B566E"             # серый «торец» под белым полем
CHECK = "#4F5055"
# Подставка — один контур с плавным горизонтальным градиентом: (x в единицах иконки, цвет).
# Тень слева → светлая линия → тело → блик справа → тень справа.
TAB_STOPS = [(33.0, "#2B66C2"), (35.0, "#2660B8"), (50.0, "#245EB5"), (65.0, "#2761BA"), (67.3, "#2A65C0")]
# Светлая кромка голубого фона вдоль края подставки (огибает скругление), как в эталоне: (цвет, ширина видимой части)
# Мягкая: широкий прозрачный слой + узкий яркий. (сторона, №, цвет, видимая ширина, непрозрачность)
TAB_EDGES = [("left", 1, "#5A93EE", 1.6, 0.30), ("left", 2, "#5A93EE", 0.5, 0.55),
             ("right", 1, "#6AA0F2", 1.8, 0.40), ("right", 2, "#74A8F5", 0.6, 0.95)]
TAB_EDGE_START = 30         # кромка начинается с этого угла дуги (°), чтобы не было светлой черты вдоль низа рамки
# Тень рамки на подставке: тёмно-синий сходит в прозрачный — (расстояние от низа рамки, непрозрачность)
TAB_SHADE = [(0.0, 1.0), (0.9, 0.5), (2.4, 0.0)]
# Тень сверху на белом поле: (расстояние от верхнего края в единицах, цвет)
PAPER_SHADE = [(0, "#BCC4D6"), (1.8, "#CBD1DF"), (3.6, "#DFE3EB"), (6.0, "#F1F3F7"), (9.0, "#FFFFFF")]

# ---- Геометрия (x0, y0, x1, y1, радиус) ----
OUTER_R = 24.0              # скругление всей иконки (десктоп; на Android форму задаёт лаунчер)
# Кольца одинаковой толщины со всех сторон, углы концентричные. Внешний край тёмной кромки зафиксирован,
# белое поле занимает всё внутри неё.
RIM_BOX = (15.25, 15.25, 84.75, 84.75, 10.3)   # радиус = внутреннее скругление синей рамки
RIM_W = 1.0                 # тёмная кромка (снизу это серый торец + тёмная полоса)
LIP_SHARE = 1.0             # доля серого торца в нижней кромке
NAVY_W = 4.45               # тёмно-синяя рамка
FRAME_R = 14.25             # внешнее скругление синей рамки
WHITE_R = 6.0               # скругление белого квадрата
INNER = (RIM_BOX[0] + RIM_W, RIM_BOX[1] + RIM_W, RIM_BOX[2] - RIM_W, RIM_BOX[3] - RIM_W, WHITE_R)
LIP_Y1 = INNER[3] + RIM_W * LIP_SHARE
FRAME = (RIM_BOX[0] - NAVY_W, RIM_BOX[1] - NAVY_W, RIM_BOX[2] + NAVY_W, RIM_BOX[3] + NAVY_W, FRAME_R)
TAB_X = (TAB_STOPS[0][0], TAB_STOPS[-1][0])
TAB_FILLET = 2.8           # вогнутое скругление, которым подставка переходит в низ рамки
TAB_TOP = FRAME[3] - 3      # верх подставки спрятан под рамкой
BOTTOM = 125.0              # ниже края иконки (на Android 125 → 108, весь холст)
CHECK_PTS = [(36.0, 49.9), (47.55, 61.15), (68.35, 33.3)]
CHECK_W = 6.5
CHECK_BOW = 0.45            # прогиб длинного луча к центру иконки (середина дуги относительно прямой), как в эталоне


# ---- Пути: одна функция, две системы координат ----
class Space:
    def __init__(self, scale, off): self.s, self.o = scale, off
    def p(self, v): return self.o + v * self.s
    def l(self, v): return v * self.s


def num(v): return f"{v:.2f}".rstrip("0").rstrip(".")


def rrect(sp, b, r=0.0):
    x0, y0, x1, y1 = (sp.p(v) for v in b[:4]); r = sp.l(r); n = num
    if not r:
        return f"M{n(x0)} {n(y0)}H{n(x1)}V{n(y1)}H{n(x0)}Z"
    return (f"M{n(x0 + r)} {n(y0)}H{n(x1 - r)}A{n(r)} {n(r)} 0 0 1 {n(x1)} {n(y0 + r)}V{n(y1 - r)}"
            f"A{n(r)} {n(r)} 0 0 1 {n(x1 - r)} {n(y1)}H{n(x0 + r)}A{n(r)} {n(r)} 0 0 1 {n(x0)} {n(y1 - r)}"
            f"V{n(y0 + r)}A{n(r)} {n(r)} 0 0 1 {n(x0 + r)} {n(y0)}Z")


def tab_shape(sp):
    """Подставка: прямоугольник, входящий в низ рамки через вогнутые скругления TAB_FILLET."""
    x0, x1 = TAB_X; yb = FRAME[3]; f = TAB_FILLET; n = num; P = sp.p; r = sp.l(f)
    return (f"M{n(P(x0 - f))} {n(P(TAB_TOP))}H{n(P(x1 + f))}V{n(P(yb))}"
            f"A{n(r)} {n(r)} 0 0 0 {n(P(x1))} {n(P(yb + f))}V{n(P(BOTTOM))}H{n(P(x0))}V{n(P(yb + f))}"
            f"A{n(r)} {n(r)} 0 0 0 {n(P(x0 - f))} {n(P(yb))}Z")


def tab_edges(sp):
    """Контуры боковых краёв подставки (скругление + вертикаль) для светлой кромки: [(id, путь, цвет, ширина)].
    Обводка центрирована по краю и рисуется под подставкой — видна только половина со стороны фона."""
    x0, x1 = TAB_X; yb = FRAME[3]; f = TAB_FILLET; n = num; P = sp.p; r = sp.l(f)
    out = []
    for side, i, col, half, alpha in TAB_EDGES:
        (sx, sy), = edge_points(side)[:1]
        if side == "left":
            d = f"M{n(P(sx))} {n(P(sy))}A{n(r)} {n(r)} 0 0 1 {n(P(x0))} {n(P(yb + f))}V{n(P(BOTTOM))}"
        else:
            d = f"M{n(P(sx))} {n(P(sy))}A{n(r)} {n(r)} 0 0 0 {n(P(x1))} {n(P(yb + f))}V{n(P(BOTTOM))}"
        out.append((f"tab-edge-{side}-{i}", d, col, sp.l(2 * half), alpha))
    return out


def edge_points(side):
    """Точки края подставки (единицы иконки): дуга скругления от угла TAB_EDGE_START и вертикаль вниз."""
    x0, x1 = TAB_X; yb = FRAME[3]; f = TAB_FILLET
    ts = np.linspace(np.radians(TAB_EDGE_START), np.pi / 2, 120)
    if side == "left":
        cx, xe = x0 - f, x0
        arc = [(cx + f * np.sin(t), yb + f - f * np.cos(t)) for t in ts]
    else:
        cx, xe = x1 + f, x1
        arc = [(cx - f * np.sin(t), yb + f - f * np.cos(t)) for t in ts]
    return arc + [(xe, y) for y in np.linspace(yb + f, 100, 300)]


def check_ctrl():
    """Контрольная точка квадратичной кривой длинного луча: прогиб CHECK_BOW к центру иконки (вверх-влево)."""
    (vx, vy), (ex, ey) = CHECK_PTS[1], CHECK_PTS[2]
    dx, dy = ex - vx, ey - vy; ln = (dx * dx + dy * dy) ** 0.5
    nx, ny = dy / ln, -dx / ln          # нормаль, направленная к центру
    if nx * (50 - (vx + ex) / 2) + ny * (50 - (vy + ey) / 2) < 0: nx, ny = -nx, -ny
    return (vx + ex) / 2 + nx * 2 * CHECK_BOW, (vy + ey) / 2 + ny * 2 * CHECK_BOW


def check_path(sp):
    (ax, ay), (vx, vy), (ex, ey) = CHECK_PTS; cx, cy = check_ctrl(); P = lambda v: num(sp.p(v))
    return f"M{P(ax)} {P(ay)} L{P(vx)} {P(vy)} Q{P(cx)} {P(cy)} {P(ex)} {P(ey)}"


# Слои снизу вверх: (id, путь, заливка). Заливка — цвет или (ось "y"/"x", от, до, [(offset, цвет, альфа)]).
def layers(sp):
    x0, y0, x1, y1, ri = INNER; h = y1 - y0
    paper = ("y", y0, y1, [(d / h, c, 1.0) for d, c in PAPER_SHADE] + [(1, "#FFFFFF", 1.0)])
    tx0, tx1 = TAB_X; tw = tx1 - tx0
    tab = ("x", tx0, tx1, [((x - tx0) / tw, c, 1.0) for x, c in TAB_STOPS])
    yb = FRAME[3]; sh = TAB_SHADE[-1][0]
    shade = ("y", yb, yb + sh, [(d / sh, NAVY, a) for d, a in TAB_SHADE])
    return [
        ("tab", tab_shape(sp), tab),
        ("tab-shade", tab_shape(sp), shade),
        ("frame", rrect(sp, FRAME, FRAME[4]), NAVY),
        ("frame-rim", rrect(sp, RIM_BOX, RIM_BOX[4]), RIM),
        ("paper-lip", rrect(sp, (x0, y0, x1, LIP_Y1), ri), LIP),
        ("paper", rrect(sp, INNER, ri), paper),
    ]


# ---- SVG ----
def build_svg():
    sp = Space(1, 0); defs = []; body = []
    for i, (name, d, fill) in enumerate(layers(sp)):
        if isinstance(fill, tuple):
            axis, g0, g1, stops = fill
            st = "".join(f'\n      <stop offset="{o:.3f}" stop-color="{c}"' + (f' stop-opacity="{a:g}"' if a < 1 else "") + "/>"
                         for o, c, a in stops)
            xy = (f'x1="{num(g0)}" y1="0" x2="{num(g1)}" y2="0"' if axis == "x"
                  else f'x1="0" y1="{num(g0)}" x2="0" y2="{num(g1)}"')
            defs.append(f'    <linearGradient id="g-{name}" {xy} gradientUnits="userSpaceOnUse">{st}\n    </linearGradient>')
            fill = f"url(#g-{name})"
        body.append(f'    <path id="{name}" fill="{fill}" d="{d}"/>')
    edges = "\n".join(f'    <path id="{name}" d="{d}" fill="none" stroke="{c}" stroke-opacity="{a:g}" stroke-width="{num(w)}"/>'
                      for name, d, c, w, a in tab_edges(sp))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 100 100">
  <!-- Иконка Things. Сгенерировано docs/icon/icon_build.py — правки вносить туда (или прислать изменённые пути). -->
  <defs>
    <linearGradient id="g-background" x1="0" y1="0" x2="0" y2="100" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="{BG_TOP}"/>
      <stop offset="1" stop-color="{BG_BOT}"/>
    </linearGradient>
{chr(10).join(defs)}
    <clipPath id="shape"><path d="{rrect(sp, (0, 0, 100, 100), OUTER_R)}"/></clipPath>
  </defs>
  <g clip-path="url(#shape)">
    <path id="background" fill="url(#g-background)" d="M0 0H100V100H0Z"/>
{edges}
{chr(10).join(body)}
    <path id="check" d="{check_path(sp)}" fill="none" stroke="{CHECK}" stroke-width="{num(CHECK_W)}" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
</svg>
'''


# ---- Android vector drawables ----
def argb(c, a=1.0): return f"#{round(a * 255):02X}{c[1:].upper()}"


VEC_HEAD = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n{aapt}    android:width="108dp"\n'
            '    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')
AAPT = '    xmlns:aapt="http://schemas.android.com/aapt"\n'


def xml_path(sp, d, fill):
    if not isinstance(fill, tuple):
        return f'    <path\n        android:fillColor="{argb(fill)}"\n        android:pathData="{d}" />\n'
    axis, g0, g1, stops = fill
    items = "".join(f'                <item android:offset="{o:.3f}" android:color="{argb(c, a)}" />\n' for o, c, a in stops)
    if axis == "x":
        sx, sy, ex, ey = num(sp.p(g0)), "54", num(sp.p(g1)), "54"
    else:
        sx, sy, ex, ey = "54", num(sp.p(g0)), "54", num(sp.p(g1))
    return (f'    <path android:pathData="{d}">\n        <aapt:attr name="android:fillColor">\n            <gradient\n'
            f'                android:type="linear"\n                android:startX="{sx}"\n                android:startY="{sy}"\n'
            f'                android:endX="{ex}"\n                android:endY="{ey}">\n{items}'
            f'            </gradient>\n        </aapt:attr>\n    </path>\n')


def check_xml(sp, col):
    return (f'    <path\n        android:strokeColor="{argb(col)}"\n        android:strokeWidth="{num(sp.l(CHECK_W))}"\n'
            f'        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n        android:pathData="{check_path(sp)}" />\n')


def build_xml(out_dir):
    sp = Space(0.72, 18)
    head = '<?xml version="1.0" encoding="utf-8"?>\n'
    gen = "Сгенерировано docs/icon/icon_build.py из things_icon.svg-геометрии — вручную не править."
    fg = (head + f'<!-- Передний план иконки Things: рамка с подставкой, белое поле, галочка. {gen} -->\n' + VEC_HEAD.format(aapt=AAPT)
          + "".join(f'    <path\n        android:strokeColor="{argb(c)}"\n        android:strokeAlpha="{a:g}"\n'
                    f'        android:strokeWidth="{num(w)}"\n        android:pathData="{d}" />\n' for _, d, c, w, a in tab_edges(sp))
          + "".join(xml_path(sp, d, f) for _, d, f in layers(sp)) + check_xml(sp, CHECK) + "</vector>\n")
    bg = (head + f'<!-- Фон иконки — голубой градиент сверху вниз. {gen} -->\n' + VEC_HEAD.format(aapt=AAPT)
          + xml_path(sp, "M0 0H108V108H0Z", ("y", 0, 100, [(0, BG_TOP, 1.0), (1, BG_BOT, 1.0)])) + "</vector>\n")
    mono = (head + f'<!-- Монохромная иконка (тематические значки Android 13+). {gen} -->\n' + VEC_HEAD.format(aapt="")
            + f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:fillType="evenOdd"\n'
              f'        android:pathData="{rrect(sp, FRAME, FRAME[4])} {rrect(sp, INNER, INNER[4])}" />\n'
            + f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{tab_shape(sp)}" />\n'
            + check_xml(sp, "#FFFFFF") + "</vector>\n")
    for name, txt in (("foreground", fg), ("background", bg), ("monochrome", mono)):
        with open(os.path.join(out_dir, f"ic_launcher_{name}.xml"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(txt)


# ---- Растр для десктопа: та же геометрия, нарисованная PIL с 4× сглаживанием ----
def render_png(size=512):
    N = size * 4; k = N / 100
    R = lambda *v: [x * k for x in v]

    def mask(draw_fn):
        m = Image.new("L", (N, N), 0); draw_fn(ImageDraw.Draw(m)); return m

    def rr(b, r=0.0):
        return mask(lambda d: d.rounded_rectangle(R(*b[:4]), radius=r * k, fill=255))

    def rgba(c, a=1.0): return np.array([int(c[i:i + 2], 16) for i in (1, 3, 5)] + [a * 255], float)

    def paint(img, m, fill):
        if not isinstance(fill, tuple):
            img.paste(Image.new("RGBA", (N, N), tuple(int(v) for v in rgba(fill))), (0, 0), m); return
        axis, g0, g1, stops = fill
        ts = (np.arange(N) / k - g0) / (g1 - g0)
        cols = np.stack([np.interp(ts, [o for o, _, _ in stops], [rgba(c, a)[i] for _, c, a in stops]) for i in range(4)], 1)
        arr = np.repeat(cols[None, :, :], N, 0) if axis == "x" else np.repeat(cols[:, None, :], N, 1)
        layer = Image.fromarray(np.round(arr).astype(np.uint8), "RGBA")
        la = np.array(layer.getchannel("A"), float) * np.array(m, float) / 255
        layer.putalpha(Image.fromarray(la.astype(np.uint8)))
        img.alpha_composite(layer)

    img = Image.new("RGBA", (N, N))
    paint(img, mask(lambda d: d.rectangle((0, 0, N, N), fill=255)), ("y", 0, 100, [(0, BG_TOP, 1), (1, BG_BOT, 1)]))
    x0, y0, x1, y1, ri = INNER

    def tab_mask():
        tx0, tx1 = TAB_X; yb = FRAME[3]; f = TAB_FILLET

        def dr(d):
            d.rectangle(R(tx0 - f, TAB_TOP, tx1 + f, yb + f), fill=255)
            for cx in (tx0 - f, tx1 + f): d.ellipse(R(cx - f, yb, cx + f, yb + 2 * f), fill=0)
            d.rectangle(R(tx0, TAB_TOP, tx1, 100), fill=255)
        return mask(dr)
    tm = tab_mask()
    masks = {"tab": tm, "tab-shade": tm, "frame": rr(FRAME, FRAME[4]), "frame-rim": rr(RIM_BOX, RIM_BOX[4]),
             "paper-lip": rr((x0, y0, x1, LIP_Y1), ri), "paper": rr(INNER, ri)}
    for side, _, col, half, alpha in TAB_EDGES:
        em = Image.new("L", (N, N), 0); de = ImageDraw.Draw(em)
        for x, y in edge_points(side):
            de.ellipse(((x - half) * k, (y - half) * k, (x + half) * k, (y + half) * k), fill=round(255 * alpha))
        img.paste(Image.new("RGBA", (N, N), tuple(int(v) for v in rgba(col))), (0, 0), em)
    for name, _, fill in layers(Space(1, 0)):
        paint(img, masks[name], fill)
    # Галочка: штампуем круги вдоль линии — круглые концы и стык, длинный луч по той же кривой Безье
    d = ImageDraw.Draw(img); w = CHECK_W * k
    (ax, ay), (vx, vy), (ex, ey) = CHECK_PTS; cx, cy = check_ctrl()
    line = [(ax + (vx - ax) * t, ay + (vy - ay) * t) for t in np.linspace(0, 1, 300)]
    line += [((1 - t) ** 2 * vx + 2 * (1 - t) * t * cx + t * t * ex, (1 - t) ** 2 * vy + 2 * (1 - t) * t * cy + t * t * ey)
             for t in np.linspace(0, 1, 600)]
    for x, y in line:
        d.ellipse((x * k - w / 2, y * k - w / 2, x * k + w / 2, y * k + w / 2), fill=CHECK)
    img.putalpha(Image.fromarray((np.array(img.getchannel("A"), float)
                                  * np.array(rr((0, 0, 100, 100), OUTER_R), float) / 255).astype(np.uint8)))
    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    svg = os.path.join(here, "things_icon.svg")
    with open(svg, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(build_svg())
    build_xml(os.path.join(ROOT, "app", "src", "main", "res", "drawable"))
    img = render_png(512)
    img.save(os.path.join(DESKTOP, "src", "desktopMain", "resources", "things_icon.png"))
    render_png(256).save(os.path.join(DESKTOP, "icons", "things.ico"), sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])
    print("svg, xml, png, ico written")
