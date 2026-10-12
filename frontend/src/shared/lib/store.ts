import { useSyncExternalStore } from "react";

export interface Store<T> {
  subscribe(listener: () => void): () => void;
  getSnapshot(): T;
  getServerSnapshot(): T;
  set(next: T): void;
}

export function createMemoryStore<T>(initial: T): Store<T> {
  let value = initial;
  const listeners = new Set<() => void>();
  return {
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    getSnapshot: () => value,
    getServerSnapshot: () => initial,
    set(next) {
      value = next;
      listeners.forEach((listener) => listener());
    },
  };
}

/** A localStorage-backed store that is safe to prerender and stays in sync across tabs. */
export function createPersistedStore<T>(key: string, fallback: T, parse: (value: unknown) => T): Store<T> {
  const listeners = new Set<() => void>();
  let cached = fallback;
  let cachedRaw: string | null = null;

  function getSnapshot(): T {
    if (typeof window === "undefined") return fallback;
    let raw: string | null;
    try {
      raw = window.localStorage.getItem(key);
    } catch {
      return cached;
    }
    if (raw !== cachedRaw) {
      cachedRaw = raw;
      try {
        cached = raw === null ? fallback : parse(JSON.parse(raw));
      } catch {
        cached = fallback;
      }
    }
    return cached;
  }

  return {
    subscribe(listener) {
      listeners.add(listener);
      const onStorage = (event: StorageEvent) => {
        if (event.key === key || event.key === null) listener();
      };
      window.addEventListener("storage", onStorage);
      return () => {
        listeners.delete(listener);
        window.removeEventListener("storage", onStorage);
      };
    },
    getSnapshot,
    getServerSnapshot: () => fallback,
    set(next) {
      cached = next;
      try {
        const raw = JSON.stringify(next);
        window.localStorage.setItem(key, raw);
        cachedRaw = raw;
      } catch {
        // Storage can be unavailable (private mode); the in-memory value still works for this tab.
      }
      listeners.forEach((listener) => listener());
    },
  };
}

export function useStore<T>(store: Store<T>): T {
  return useSyncExternalStore(store.subscribe, store.getSnapshot, store.getServerSnapshot);
}

const noopSubscribe = () => () => {};

/** False during prerender and hydration, true afterwards. */
export function useHydrated(): boolean {
  return useSyncExternalStore(noopSubscribe, () => true, () => false);
}
