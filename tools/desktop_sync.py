# -*- coding: utf-8 -*-
"""
Общий код Things 2 (телефон) и Things Desktop: что разошлось и как перенести правки телефона на десктоп.

Десктоп — копия кода телефона с добавками под мышь, окно и JDBC, а git у него нет. Чтобы правки телефона
не терялись, скрипт сравнивает файлы с одинаковым путём в обоих проектах по трём версиям:
  телефон сейчас · десктоп сейчас · «база» — версия телефона при последней синхронизации этого файла
(база хранится у десктопа в `.phone_sync/base`). По ним видно, кто менял файл:

  same      одинаковые
  phone     менялся только телефон — можно перенести как есть (pull)
  desktop   менялся только десктоп (его добавки) — ничего делать не нужно
  both      менялись оба — трёхстороннее слияние (pull пробует сам, при конфликте оставляет файл с метками рядом)
  unknown   различаются, а базы нет — решить вручную, потом `baseline ФАЙЛ`
  new       новый файл телефона — pull копирует его (если он не только для Android — см. phone_only в json)

Команды (python -X utf8 tools/desktop_sync.py …):
  status [--all]        что разошлось (--all — и одинаковые)
  diff ФАЙЛ             разница телефон → десктоп (и база, если есть)
  pull [ФАЙЛ…]          перенести правки телефона: phone и new копируются, both сливаются (git merge-file);
                        прежние файлы десктопа — в .phone_sync/backups/<время>/
  baseline [ФАЙЛ…]      запомнить нынешнюю версию телефона базой (после ручного слияния); без файлов —
                        для всех одинаковых
  init                  для файлов без базы (unknown) найти в истории git телефона версию, ближе всего к
                        нынешнему файлу десктопа, — с неё десктоп скорее всего и скопирован; она и станет базой
  strings               строки телефона (values*/strings.xml) → ресурсы десктопа и его R.kt
ФАЙЛ — путь относительно com/example (например ui/theme/Dimen.kt) или test/… для тестов.

Путь к десктопу — tools/desktop_sync.json («desktop») или переменная THINGS_DESKTOP.
"""
import datetime
import difflib
import filecmp
import json
import os
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(ROOT, 'tools', 'desktop_sync.json')
CONFIG = json.load(open(CONFIG_PATH, encoding='utf-8')) if os.path.exists(CONFIG_PATH) else {}
DESKTOP = os.environ.get('THINGS_DESKTOP') or os.path.join(ROOT, CONFIG.get('desktop', '../Things Desktop'))
DESKTOP = os.path.normpath(DESKTOP)

# Пары корней: (метка, телефон, десктоп)
ROOTS = [
    ('', os.path.join(ROOT, 'app/src/main/java/com/example'), os.path.join(DESKTOP, 'src/desktopMain/kotlin/com/example')),
    ('test/', os.path.join(ROOT, 'app/src/test/java/com/example'), os.path.join(DESKTOP, 'src/desktopTest/kotlin/com/example')),
]
STATE = os.path.join(DESKTOP, '.phone_sync')
BASE = os.path.join(STATE, 'base')
PHONE_ONLY = set(CONFIG.get('phone_only', []))
# Свои версии десктопа (JDBC, вход в Google, Android-обвязка): скрипт их не трогает
DESKTOP_OWNED = set(CONFIG.get('desktop_owned', []))


def norm(path):
    return path.replace('\\', '/')


def files_under(root):
    out = set()
    for dirpath, _, names in os.walk(root):
        for n in names:
            if n.endswith('.kt'):
                out.add(norm(os.path.relpath(os.path.join(dirpath, n), root)))
    return out


def entries():
    """(ключ, путь телефона, путь десктопа) для всех файлов телефона и общих"""
    result = []
    for label, phone_root, desk_root in ROOTS:
        phone = files_under(phone_root)
        desk = files_under(desk_root)
        for rel in sorted(phone | desk):
            key = label + rel
            result.append((key, os.path.join(phone_root, rel) if rel in phone else None,
                           os.path.join(desk_root, rel) if rel in desk else None))
    return result


def text(path):
    with open(path, encoding='utf-8', newline='') as f:
        return f.read().replace('\r\n', '\n')


def same(a, b):
    return a is not None and b is not None and os.path.exists(a) and os.path.exists(b) and text(a) == text(b)


def base_path(key):
    return os.path.join(BASE, key)


def status_of(key, phone, desk):
    if phone is None:
        return 'desktop-only'
    if key in DESKTOP_OWNED:
        return 'owned'
    if desk is None:
        return 'phone-only' if key in PHONE_ONLY else 'new'
    if same(phone, desk):
        return 'same'
    base = base_path(key)
    if not os.path.exists(base):
        return 'unknown'
    phone_changed = text(phone) != text(base)
    desk_changed = text(desk) != text(base)
    if phone_changed and not desk_changed:
        return 'phone'
    if desk_changed and not phone_changed:
        return 'desktop'
    return 'both'


CRLF, LF = chr(13) + chr(10), chr(10)


def write(path, content, like=None):
    """Записать; если [like] — файл с CRLF, переводы строк как у него"""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if like and os.path.exists(like):
        with open(like, encoding='utf-8', newline='') as f:
            if CRLF in f.read():
                content = content.replace(CRLF, LF).replace(LF, CRLF)
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(content)


def set_base(key, phone):
    write(base_path(key), text(phone))


def backup(key, desk, stamp):
    target = os.path.join(STATE, 'backups', stamp, key)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    shutil.copyfile(desk, target)


def find(keys):
    all_entries = {e[0]: e for e in entries()}
    missing = [k for k in keys if norm(k) not in all_entries]
    if missing:
        sys.exit('Нет таких файлов: ' + ', '.join(missing))
    return [all_entries[norm(k)] for k in keys]


def cmd_status(args):
    show_all = '--all' in args
    groups = {}
    for key, phone, desk in entries():
        groups.setdefault(status_of(key, phone, desk), []).append(key)
    titles = [
        ('phone', 'менялся только телефон — перенести: pull'),
        ('new', 'новые файлы телефона — перенести: pull'),
        ('both', 'менялись оба — pull попробует слить'),
        ('unknown', 'различаются, базы нет — слить вручную, затем baseline'),
        ('desktop', 'менялся только десктоп (его добавки)'),
        ('desktop-only', 'есть только на десктопе'),
        ('owned', 'своя версия десктопа (desktop_owned) — не синхронизируется'),
        ('phone-only', 'только для Android (phone_only)'),
        ('same', 'одинаковые'),
    ]
    for status, title in titles:
        items = groups.get(status, [])
        if not items or (status in ('same', 'phone-only', 'desktop-only', 'owned') and not show_all):
            if items:
                print('%-13s %4d  %s' % (status, len(items), title))
            continue
        print('%-13s %4d  %s' % (status, len(items), title))
        for k in items:
            print('      ' + k)


def cmd_diff(args):
    for key, phone, desk in find(args):
        if phone is None or desk is None:
            print(key, '— есть только в одном проекте')
            continue
        lines = difflib.unified_diff(text(desk).splitlines(), text(phone).splitlines(),
                                     'desktop/' + key, 'phone/' + key, lineterm='', n=2)
        print('\n'.join(lines))
        base = base_path(key)
        if os.path.exists(base) and text(base) != text(phone):
            print('\n--- правки телефона с последней синхронизации (база → телефон):')
            print('\n'.join(difflib.unified_diff(text(base).splitlines(), text(phone).splitlines(),
                                                 'base/' + key, 'phone/' + key, lineterm='', n=2)))


def merge3(desk, base, phone):
    """git merge-file: (текст, есть ли конфликты). Переводы строк приводятся к LF: у десктопа CRLF, у телефона LF —
    иначе git видит изменённой каждую строку"""
    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        paths = []
        for name, path in (('desktop', desk), ('base', base), ('phone', phone)):
            p = os.path.join(tmp, name)
            write(p, text(path))
            paths.append(p)
        r = subprocess.run(['git', 'merge-file', '-p', '-L', 'desktop', '-L', 'base', '-L', 'phone'] + paths,
                           capture_output=True)
    return r.stdout.decode('utf-8'), r.returncode != 0


def cmd_pull(args):
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    selected = find(args) if args else entries()
    copied, merged, conflicts = [], [], []
    for key, phone, desk in selected:
        status = status_of(key, phone, desk)
        if status == 'new':
            label, phone_root, desk_root = next(r for r in ROOTS if key.startswith(r[0]) and (r[0] or not key.startswith('test/')))
            target = os.path.join(desk_root, key[len(label):])
            write(target, text(phone))
            set_base(key, phone)
            copied.append(key)
        elif status == 'phone':
            backup(key, desk, stamp)
            write(desk, text(phone), like=desk)
            set_base(key, phone)
            copied.append(key)
        elif status == 'both':
            content, conflict = merge3(desk, base_path(key), phone)
            if conflict:
                out = os.path.join(STATE, 'conflicts', key)
                write(out, content)
                conflicts.append(key)
            else:
                backup(key, desk, stamp)
                write(desk, content, like=desk)
                set_base(key, phone)
                merged.append(key)
    for title, items in [('скопировано', copied), ('слито автоматически', merged)]:
        if items:
            print('%s (%d):' % (title, len(items)))
            for k in items:
                print('  ' + k)
    if conflicts:
        print('конфликты (%d) — файлы с метками <<<<<<< в %s; слейте в файл десктопа, затем baseline:' % (
            len(conflicts), os.path.join(STATE, 'conflicts')))
        for k in conflicts:
            print('  ' + k)
    if copied or merged:
        print('прежние файлы десктопа — в', os.path.join(STATE, 'backups', stamp))
    if not (copied or merged or conflicts):
        print('переносить нечего')


def cmd_baseline(args):
    targets = find(args) if args else [e for e in entries() if same(e[1], e[2])]
    n = 0
    for key, phone, desk in targets:
        if phone is not None:
            set_base(key, phone)
            n += 1
    print('база обновлена:', n)


def phone_repo_path(key):
    if key.startswith('test/'):
        return 'app/src/test/java/com/example/' + key[len('test/'):]
    return 'app/src/main/java/com/example/' + key


def git(*args):
    return subprocess.run(['git', '-C', ROOT] + list(args), capture_output=True).stdout


def cmd_init(_args):
    """База для файлов без неё: ближайшая к десктопу версия телефона из истории git"""
    for key, phone, desk in entries():
        if status_of(key, phone, desk) != 'unknown':
            continue
        desk_lines = text(desk).splitlines()
        best = None
        for commit in git('log', '--format=%H %ad', '--date=short', '-n', '80', '--', phone_repo_path(key)).decode().splitlines():
            if not commit.strip():
                continue
            sha, date = commit.split(' ', 1)
            blob = git('show', '%s:%s' % (sha, phone_repo_path(key))).decode('utf-8', 'replace').replace(chr(13) + chr(10), chr(10))
            score = sum(1 for l in difflib.unified_diff(blob.splitlines(), desk_lines, lineterm='', n=0)
                        if l[:1] in '+-' and not l.startswith(('+++', '---')))
            if best is None or score < best[0]:
                best = (score, sha[:7], date, blob)
        if best is None:
            print('%-60s нет истории в git' % key)
            continue
        write(base_path(key), best[3])
        print('%-60s база %s %s (отличий от десктопа: %d) → %s' % (key, best[1], best[2], best[0], status_of(key, phone, desk)))


# ---------------------------------------------------------------- строки

def cmd_strings(_args):
    """Строки телефона → ресурсы десктопа (те же файлы) и R.kt с ключами — сам текст десктоп берёт по языку"""
    phone_res = os.path.join(ROOT, 'app/src/main/res')
    desk_res = os.path.join(DESKTOP, 'src/desktopMain/resources')
    names, plurals = set(), set()
    for folder in sorted(os.listdir(phone_res)):
        src = os.path.join(phone_res, folder, 'strings.xml')
        if folder.startswith('values') and os.path.exists(src):
            os.makedirs(os.path.join(desk_res, folder), exist_ok=True)
            shutil.copyfile(src, os.path.join(desk_res, folder, 'strings.xml'))
    for folder in sorted(os.listdir(desk_res)):
        for file in ('strings.xml', 'strings_desktop.xml'):
            path = os.path.join(desk_res, folder, file)
            if folder.startswith('values') and os.path.exists(path):
                root = ET.parse(path).getroot()
                names |= {s.attrib['name'] for s in root.findall('string')}
                plurals |= {p.attrib['name'] for p in root.findall('plurals')}
    r_path = os.path.join(DESKTOP, 'src/desktopMain/kotlin/com/example/R.kt')
    lines = ['package com.example', '',
             '/**',
             ' * Ключи строк — как R на Android. Сам текст десктоп берёт по языку из ресурсов (values, values-uk, values-ru: strings.xml',
             ' * и strings_desktop.xml)',
             ' * ([com.example.desktop.i18n.DesktopStrings]). Файл создаёт tools/desktop_sync.py strings — руками не править.',
             ' */',
             'object R {', '    object string {']
    lines += ['        const val %s = "%s"' % (n, n) for n in sorted(names)]
    lines += ['    }', '', '    object plurals {']
    lines += ['        const val %s = "%s"' % (n, n) for n in sorted(plurals)]
    lines += ['    }', '',
              '    object drawable {', '        const val ic_notification_reminder = "ic_notification_reminder"', '    }', '',
              '    object style {', '        const val DialogWindowAnimationNone = 0', '        const val DialogWindowAnimationGrowOpen = 0', '    }',
              '}', '']
    write(r_path, '\n'.join(lines))
    print('строк: %d, множественных: %d → %s' % (len(names), len(plurals), r_path))


COMMANDS = {'init': cmd_init, 'status': cmd_status, 'diff': cmd_diff, 'pull': cmd_pull, 'baseline': cmd_baseline, 'strings': cmd_strings}

if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        print(__doc__)
        sys.exit(1)
    if not os.path.isdir(DESKTOP):
        sys.exit('Нет папки десктопа: ' + DESKTOP)
    COMMANDS[sys.argv[1]](sys.argv[2:])
