"""Generate layout-port variants for Zalith's side-by-side screens.
Stacks a two-pane horizontal split (ConstraintLayout weight chain or horizontal LinearLayout) vertically.
Keeps every view, id and type, so ViewBinding stays identical."""
import sys, os, xml.dom.minidom as m

C = 'app:layout_constraint'
def ids(n):
    return n.getAttribute('android:id').replace('@+id/', '').replace('@id/', '')
def elems(n):
    return [c for c in n.childNodes if c.nodeType == 1]
def setc(n, **kw):
    for k, v in kw.items():
        key = C + k
        if v is None:
            if n.hasAttribute(key): n.removeAttribute(key)
        else:
            n.setAttribute(key, v)

def stack_constraint(parent, ratio):
    ch = [c for c in elems(parent) if c.hasAttribute(C + 'Horizontal_weight')]
    if len(ch) != 2: return False
    L, R = ch
    if ids(R) not in L.getAttribute(C + 'End_toStartOf'):
        L, R = R, L
        if ids(R) not in L.getAttribute(C + 'End_toStartOf'): return False
    for n in (L, R):
        n.setAttribute('android:layout_width', '0dp'); n.setAttribute('android:layout_height', '0dp')
        setc(n, Horizontal_weight=None, End_toStartOf=None, Start_toEndOf=None, Start_toStartOf='parent', End_toEndOf='parent')
    if L.hasAttribute(C + 'Top_toBottomOf'): setc(L, Top_toTopOf=None)
    else: setc(L, Top_toTopOf='parent')
    setc(L, Bottom_toBottomOf=None, Bottom_toTopOf='@+id/' + ids(R))
    # a shadow strip that sat between the two panes no longer has a place: hide it
    for c in elems(parent):
        if c.tagName == 'View' and ids(L) in c.getAttribute(C + 'Start_toEndOf'):
            c.setAttribute('android:visibility', 'gone')
    setc(R, Top_toTopOf=None, Top_toBottomOf=None, Bottom_toBottomOf='parent', Height_percent=str(ratio))
    # shadow strip on the left edge of the side panel -> thin line on its top edge
    for c in elems(R):
        if ids(c) == 'shadowView' and c.tagName == 'View':
            c.setAttribute('android:layout_width', '0dp'); c.setAttribute('android:layout_height', '1dp')
            setc(c, Start_toStartOf='parent', End_toEndOf='parent', Top_toTopOf='parent', Bottom_toBottomOf=None)
            for d in elems(R):
                if 'shadowView' in d.getAttribute(C + 'Start_toEndOf'):
                    if d.getAttribute('android:layout_height') == 'match_parent': d.setAttribute('android:layout_height', '0dp')
                    setc(d, Start_toEndOf=None, Start_toStartOf='parent', Top_toTopOf=None, Top_toBottomOf='@+id/shadowView', Bottom_toBottomOf='parent')
    return True

def stack_linear(root):
    if root.tagName != 'LinearLayout' or root.getAttribute('android:orientation') != 'horizontal': return False
    kids = [c for c in elems(root) if c.hasAttribute('android:layout_weight')]
    if len(kids) < 2: return False
    root.setAttribute('android:orientation', 'vertical')
    for c in kids:
        if c.getAttribute('android:layout_width') == '0dp':
            c.setAttribute('android:layout_width', 'match_parent'); c.setAttribute('android:layout_height', '0dp')
    return True

def main(src_dir, out_dir, files, ratio):
    os.makedirs(out_dir, exist_ok=True)
    for f in files:
        doc = m.parse(os.path.join(src_dir, f)); root = doc.documentElement
        done = stack_linear(root) or stack_constraint(root, ratio)
        if not done:
            print('SKIP', f); continue
        with open(os.path.join(out_dir, f), 'w', encoding='utf-8') as fh:
            fh.write(doc.toxml(encoding='utf-8').decode('utf-8'))
        print('OK  ', f)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[4:], float(sys.argv[3]))
