export function javaHashCode(input: string): number {
  let hash = 0;
  for (let index = 0; index < input.length; index += 1) {
    hash = (Math.imul(31, hash) + input.charCodeAt(index)) | 0;
  }
  return hash;
}

export function routeTabletForKey(key: string, totalTablets: number): number {
  if (totalTablets <= 0) {
    return 0;
  }

  const fallbackKey = key.length > 0 ? key : String(-2147483648);
  return Math.abs(javaHashCode(fallbackKey) % totalTablets);
}
