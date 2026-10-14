import json, re, os, sys, collections
root = sys.argv[1]
mods = collections.Counter(); effects = collections.Counter(); props = collections.Counter()
data = collections.Counter(); anims = collections.Counter(); keys = collections.Counter()
hud = collections.Counter()
def strip(s):  # tolerate comments/trailing commas
    s = re.sub(r'//.*', '', s); s = re.sub(r',\s*([}\]])', r'\1', s); return s
for dp, _, fs in os.walk(root):
    for f in fs:
        p = os.path.join(dp, f); t = open(p, encoding='utf-8', errors='replace').read() if f.endswith(('.js','.json')) else ''
        if '/data/powers/' in p and f.endswith('.json'):
            try: j = json.loads(strip(t))
            except Exception as e: print('PARSE FAIL', p, e); continue
            for k in j.get('modifiers', {}): mods[k.split('|')[0]] += 1
            for h in j.get('hud', []) or []: hud[h.get('type')] += 1
        if f.endswith('.js'):
            for m in re.findall(r'createEffect\("([^"]+)"', t): effects[m] += 1
            for m in re.findall(r'bindProperty\("([^"]+)"', t): props[m] += 1
            for m in re.findall(r'(?:get|getInterpolated|set|setInterpolated|increment|setDataWithNotify)Data\w*\(\s*(?:entity\s*,\s*)?["\']([a-z_]+:[a-z_/0-9]+)', t): data[m] += 1
            for m in re.findall(r'bindAnimation\("([^"]+)"', t): anims[m] += 1
            for m in re.findall(r'addKeyBind(?:Func)?\("([A-Z_0-9]+)"', t): keys[m] += 1
for name, c in [('Modifiers (powers/*.json)', mods), ('Render effects (createEffect)', effects), ('Render properties (bindProperty)', props), ('HUD types', hud), ('Data vars', data), ('Animations', anims), ('Keybinds', keys)]:
    print(f'\n## {name}: {len(c)} distintos')
    for k, v in c.most_common(): print(f'- {k} ×{v}')
