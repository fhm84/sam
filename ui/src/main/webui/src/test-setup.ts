// Node >= 23 ships an experimental `localStorage` global that evaluates to
// `undefined` unless Node is started with --localstorage-file, and it shadows
// the localStorage the test environment would otherwise provide. Install an
// in-memory implementation when the global is missing or non-functional.
const localStorageBroken = (() => {
  try {
    globalThis.localStorage.setItem('__probe__', '1');
    globalThis.localStorage.removeItem('__probe__');
    return false;
  } catch {
    return true;
  }
})();

if (localStorageBroken) {
  const store = new Map<string, string>();
  const inMemoryStorage: Storage = {
    get length() {
      return store.size;
    },
    key: (index: number) => [...store.keys()][index] ?? null,
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, String(value));
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => {
      store.clear();
    },
  };
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    value: inMemoryStorage,
  });
}
