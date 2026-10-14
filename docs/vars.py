import re, sys, json
lines = open(sys.argv[1]).read().split('\n')
# only static init
start = next(i for i,l in enumerate(lines) if 'static {}' in l)
out = {}; cur = None
for l in lines[start:]:
    m = re.search(r'new\s+#\d+\s+// class com/fiskmods/heroes/common/data/var/(DataVar\w*)', l)
    if m: cur = {'cls': m.group(1), 'type': None, 'def': None}; continue
    if cur is None: continue
    if cur['type'] is None:
        m = re.search(r'Method java/lang/(Boolean|Byte|Short|Integer|Float|Double|Long)\.valueOf', l)
        if m:
            cur['type'] = m.group(1); cur['def'] = cur.get('last'); continue
        m = re.search(r'(iconst_m?\d|fconst_\d|dconst_\d|bipush\s+-?\d+|sipush\s+-?\d+|ldc\w*\s+#\d+\s+// (?:float|int|double|long) (-?[\d.E-]+)f?)', l)
        if m:
            t = m.group(1)
            if m.group(2): v = m.group(2)
            else: v = re.sub(r'^(iconst_|fconst_|dconst_|bipush\s+|sipush\s+)', '', t).replace('m1','-1')
            cur['last'] = v; continue
        m = re.search(r'// String (.*)$', l)
        if m and cur['type'] is None and 'str' not in cur: cur['str'] = m.group(1)
    m = re.search(r'putstatic\s+#\d+\s+// Field (\w+):Lcom/fiskmods/heroes/common/data/var/(?:DataVar\w*);', l)
    if m:
        name = m.group(1).lower()
        t = cur['type'] or ('String' if 'str' in cur else 'Object')
        d = cur['def']
        if t == 'Boolean': d = d == '1'
        elif t in ('Float','Double'): d = float(d or 0)
        elif t in ('Byte','Short','Integer','Long'): d = int(float(d or 0))
        else: d = cur.get('str')
        out[name] = {'type': t.upper(), 'default': d, 'interp': 'Interp' in cur['cls'] or cur['cls'] in ('DataVarSuitOpenTimer','DataVarMaskOpen')}
        cur = None
json.dump(out, sys.stdout, indent=1, sort_keys=True)
