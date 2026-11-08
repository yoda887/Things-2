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
TAB = "#245EB5"             # подставка
TAB_SIDE = "#2D6AC6"        # тени по бокам подставки
TAB_LINE = "#3672CB"        # тонкая светлая линия слева
TAB_HI_COL = "#5893EA"      # блик справа
# Тень сверху на белом поле: (расстояние от верхнего края в единицах, цвет)
PAPER_SHADE = [(0, "#C3CBDC"), (2.9, "#E3E6EE"), (7.0, "#FFFFFF")]

# ---- Геометрия (x0, y0, x1, y1, радиус) ----
OUTER_R = 24.0              # скругление всей иконки (десктоп; на Android форму задаёт лаунчер)
# Кольца одинаковой толщины со всех сторон, углы концентричные. Внешний край тёмной кромки зафиксирован,
# белое поле занимает всё внутри неё.
RIM_BOX = (14.15, 14.15, 85.85, 85.85, 10.3)   # радиус = внутреннее скругление синей рамки
RIM_W = 1.0                 # тёмная кромка (снизу это серый торец + тёмная полоса)
LIP_SHARE = 0.6             # доля серого торца в нижней кромке
NAVY_W = 3.35               # тёмно-синяя рамка
FRAME_R = 12.65             # внешнее скругление синей рамки
WHITE_R = 7.0               # скругление белого квадрата
INNER = (RIM_BOX[0] + RIM_W, RIM_BOX[1] + RIM_W, RIM_BOX[2] - RIM_W, RIM_BOX[3] - RIM_W, WHITE_R)
LIP_Y1 = INNER[3] + RIM_W * LIP_SHARE
FRAME = (RIM_BOX[0] - NAVY_W, RIM_BOX[1] - NAVY_W, RIM_BOX[2] + NAVY_W, RIM_BOX[3] + NAVY_W, FRAME_R)
_TD = FRAME[3] - 88.45      # подставка привязана к низу рамки (снята при низе рамки 88,45)
TAB_SIDES = [("tab-shadow-left", (32.0, 88.0 + _TD, 33.0), TAB_SIDE), ("tab-line-left", (32.9, 88.0 + _TD, 33.6), TAB_LINE),
             ("tab-shadow-right", (66.3, 88.0 + _TD, 68.3), TAB_SIDE)]
TAB_JOINT = (33.6, 83.0, 66.3, 89.75 + _TD)    # тёмно-синий стык рамки и подставки
TAB_BODY = (33.6, 89.95 + _TD, 64.6)
TAB_HI = (64.6, 89.25 + _TD, 66.3)
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


# Слои снизу вверх: (id, путь, заливка). Заливка — цвет или ("grad", y0, y1, [(offset, цвет, альфа)]).
def layers(sp):
    x0, y0, x1, y1, ri = INNER; h = y1 - y0
    paper = ("grad", y0, y1, [(d / h, c, 1.0) for d, c in PAPER_SHADE] + [(1, "#FFFFFF", 1.0)])
    return [(name, rrect(sp, (b[0], b[1], b[2], BOTTOM)), col) for name, b, col in TAB_SIDES] + [
        ("tab-joint", rrect(sp, TAB_JOINT), NAVY),
        ("frame", rrect(sp, FRAME, FRAME[4]), NAVY),
        ("tab-body", rrect(sp, (*TAB_BODY, BOTTOM)), TAB),
        ("tab-highlight", rrect(sp, (*TAB_HI, BOTTOM)), TAB_HI_COL),
        ("frame-rim", rrect(sp, RIM_BOX, RIM_BOX[4]), RIM),
        ("paper-lip", rrect(sp, (x0, y0, x1, LIP_Y1), ri), LIP),
        ("paper", rrect(sp, INNER, ri), paper),
    ]


# ---- SVG ----
def build_svg():
    sp = Space(1, 0); defs = []; body = []
    for i, (name, d, fill) in enumerate(layers(sp)):
        if isinstance(fill, tuple):
            _, gy0, gy1, stops = fill
            st = "".join(f'\n      <stop offset="{o:.3f}" stop-color="{c}"' + (f' stop-opacity="{a:g}"' if a < 1 else "") + "/>"
                         for o, c, a in stops)
            defs.append(f'    <linearGradient id="g-{name}" x1="0" y1="{num(gy0)}" x2="0" y2="{num(gy1)}" '
                        f'gradientUnits="userSpaceOnUse">{st}\n    </linearGradient>')
            fill = f"url(#g-{name})"
        body.append(f'    <path id="{name}" fill="{fill}" d="{d}"/>')
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
    _, gy0, gy1, stops = fill
    items = "".join(f'                <item android:offset="{o:.3f}" android:color="{argb(c, a)}" />\n' for o, c, a in stops)
    return (f'    <path android:pathData="{d}">\n        <aapt:attr name="android:fillColor">\n            <gradient\n'
            f'                android:type="linear"\n                android:startX="54"\n                android:startY="{num(sp.p(gy0))}"\n'
            f'                android:endX="54"\n                android:endY="{num(sp.p(gy1))}">\n{items}'
            f'            </gradient>\n        </aapt:attr>\n    </path>\n')


def check_xml(sp, col):
    return (f'    <path\n        android:strokeColor="{argb(col)}"\n        android:strokeWidth="{num(sp.l(CHECK_W))}"\n'
            f'        android:strokeLineCap="round"\n        android:strokeLineJoin="round"\n        android:pathData="{check_path(sp)}" />\n')


def build_xml(out_dir):
    sp = Space(0.72, 18)
    head = '<?xml version="1.0" encoding="utf-8"?>\n'
    gen = "Сгенерировано docs/icon/icon_build.py из things_icon.svg-геометрии — вручную не править."
    fg = (head + f'<!-- Передний план иконки Things: рамка с подставкой, белое поле, галочка. {gen} -->\n' + VEC_HEAD.format(aapt=AAPT)
          + "".join(xml_path(sp, d, f) for _, d, f in layers(sp)) + check_xml(sp, CHECK) + "</vector>\n")
    bg = (head + f'<!-- Фон иконки — голубой градиент сверху вниз. {gen} -->\n' + VEC_HEAD.format(aapt=AAPT)
          + xml_path(sp, "M0 0H108V108H0Z", ("grad", 0, 100, [(0, BG_TOP, 1.0), (1, BG_BOT, 1.0)])) + "</vector>\n")
    mono = (head + f'<!-- Монохромная иконка (тематические значки Android 13+). {gen} -->\n' + VEC_HEAD.format(aapt="")
            + f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:fillType="evenOdd"\n'
              f'        android:pathData="{rrect(sp, FRAME, FRAME[4])} {rrect(sp, INNER, INNER[4])}" />\n'
            + f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{rrect(sp, (TAB_BODY[0], FRAME[3] - 1, TAB_HI[2], BOTTOM))}" />\n'
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
        _, gy0, gy1, stops = fill
        ys = (np.arange(N) / k - gy0) / (gy1 - gy0)
        cols = np.stack([np.interp(ys, [o for o, _, _ in stops], [rgba(c, a)[i] for _, c, a in stops]) for i in range(4)], 1)
        layer = Image.fromarray(np.repeat(cols[:, None, :], N, 1).astype(np.uint8), "RGBA")
        la = np.array(layer.getchannel("A"), float) * np.array(m, float) / 255
        layer.putalpha(Image.fromarray(la.astype(np.uint8)))
        img.alpha_composite(layer)

    img = Image.new("RGBA", (N, N))
    paint(img, mask(lambda d: d.rectangle((0, 0, N, N), fill=255)), ("grad", 0, 100, [(0, BG_TOP, 1), (1, BG_BOT, 1)]))
    x0, y0, x1, y1, ri = INNER
    masks = {name: rr((b[0], b[1], b[2], 100)) for name, b, _ in TAB_SIDES}
    masks.update({"tab-joint": rr(TAB_JOINT), "frame": rr(FRAME, FRAME[4]), "tab-body": rr((*TAB_BODY, 100)),
                  "tab-highlight": rr((*TAB_HI, 100)), "frame-rim": rr(RIM_BOX, RIM_BOX[4]),
                  "paper-lip": rr((x0, y0, x1, LIP_Y1), ri), "paper": rr(INNER, ri)})
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
