export function stepwise(x, y, endX, endY) {
  const dx = endX - x, dy = endY - y;
  const steps = Math.max(Math.abs(dx), Math.abs(dy));
  if (!steps) return [[x, y]];
  const points = [];
  for (let i = 0; i <= steps; i++) {
    points.push([Math.round(x + i * dx / steps), Math.round(y + i * dy / steps)]);
  }
  return points;
}

export function dda(x, y, endX, endY) {
  const steps = Math.max(Math.abs(endX - x), Math.abs(endY - y));
  if (!steps) return [[x, y]];
  const dx = (endX - x) / steps, dy = (endY - y) / steps;
  const points = [];
  for (let i = 0; i < steps; i++) {
    points.push([Math.round(x), Math.round(y)]);
    x += dx;
    y += dy;
  }
  points.push([endX, endY]);
  return points;
}

export function line(x, y, endX, endY) {
  const dx = Math.abs(endX - x), dy = Math.abs(endY - y);
  const sx = Math.sign(endX - x), sy = Math.sign(endY - y);
  let error = dx - dy;
  const points = [];
  while (true) {
    points.push([x, y]);
    if (x === endX && y === endY) return points;
    const twice = 2 * error;
    if (twice > -dy) {
      error -= dy;
      x += sx;
    }
    if (twice < dx) {
      error += dx;
      y += sy;
    }
  }
}

export function circle(cx, cy, radius) {
  let x = 0, y = radius, error = 3 - 2 * radius;
  const points = [];
  while (x <= y) {
    points.push([cx + x, cy + y]);
    if (x) points.push([cx - x, cy + y]);
    if (y) points.push([cx + x, cy - y]);
    if (x && y) points.push([cx - x, cy - y]);
    if (x !== y) {
      points.push([cx + y, cy + x], [cx - y, cy + x]);
      if (x) points.push([cx + y, cy - x], [cx - y, cy - x]);
    }
    if (error < 0) {
      error += 4 * x + 6;
    } else {
      error += 4 * (x - y) + 10;
      y--;
    }
    x++;
  }
  return points;
}
