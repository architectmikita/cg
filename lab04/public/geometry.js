export function clipSegment(a, b, [xmin, ymin, xmax, ymax], epsilon = 0.0001) {
  function code([x, y]) {
    return (x < xmin ? 1 : 0) | (x > xmax ? 2 : 0) |
      (y < ymin ? 4 : 0) | (y > ymax ? 8 : 0);
  }
  let first = null, last = null;
  function keep(p, q) {
    if (first === null) first = p;
    last = q;
  }
  function visit(p, q, cp, cq) {
    if (cp & cq) return;
    if (!(cp | cq)) {
      keep(p, q);
      return;
    }
    const mid = [p[0] + (q[0] - p[0]) / 2, p[1] + (q[1] - p[1]) / 2];
    const cm = code(mid);
    if (Math.hypot(q[0] - p[0], q[1] - p[1]) <= epsilon) {
      if (!cp) keep(p, p);
      if (!cm) keep(mid, mid);
      if (!cq) keep(q, q);
      return;
    }
    visit(p, mid, cp, cm);
    visit(mid, q, cm, cq);
  }
  visit(a, b, code(a), code(b));
  return first === null ? null : [first, last];
}

function cross(a, b, p) {
  return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
}

function same(a, b) {
  return a[0] === b[0] && a[1] === b[1];
}

export function convexPolygon(points, label) {
  if (points.length > 1 && same(points[0], points.at(-1))) points = points.slice(0, -1);
  if (points.length < 3 || new Set(points.map(String)).size !== points.length) {
    throw new Error(label + ' — нужны как минимум три неповторяющиеся вершины.');
  }
  let area = 0;
  for (let i = 1; i + 1 < points.length; i++) area += cross(points[0], points[i], points[i + 1]);
  if (area === 0) throw new Error(label + ' — площадь должна быть ненулевой.');
  if (area < 0) points = points.slice().reverse();
  for (let i = 0; i < points.length; i++) {
    const a = points[i], b = points[(i + 1) % points.length];
    if (points.some(p => cross(a, b, p) < 0)) {
      throw new Error(label + ' — укажите вершины выпуклого многоугольника по порядку обхода.');
    }
  }
  return points;
}

export function clipPolygon(subject, window) {
  let output = subject;
  for (let i = 0; i < window.length && output.length; i++) {
    const a = window[i], b = window[(i + 1) % window.length];
    const input = output;
    output = [];
    function add(p) {
      if (output.length === 0 || !same(output.at(-1), p)) output.push(p);
    }
    let start = input.at(-1);
    let ds = cross(a, b, start);
    for (const end of input) {
      const de = cross(a, b, end);
      if ((ds >= 0) !== (de >= 0)) {
        const t = ds / (ds - de);
        add(t === 0 ? start : t === 1 ? end :
          [start[0] + t * (end[0] - start[0]), start[1] + t * (end[1] - start[1])]);
      }
      if (de >= 0) add(end);
      start = end;
      ds = de;
    }
    if (output.length > 1 && same(output[0], output.at(-1))) output.pop();
  }
  return output;
}
