"use client";

import { useCallback, useEffect, useState } from "react";
import { toError } from "@/shared/lib/api";

export interface AsyncState<T> {
  data?: T;
  error?: Error;
  /** True while a request for the current dependencies is in flight. */
  loading: boolean;
  reload: () => void;
}

interface Settled<T> {
  scope: object;
  request: object;
  data?: T;
  error?: Error;
}

interface Identity {
  dependencies: ReadonlyArray<unknown>;
  /** Changes with the dependencies; data from another scope is never shown. */
  scope: object;
  /** Changes with the dependencies and on every reload; identifies one in-flight request. */
  request: object;
}

function sameDependencies(a: ReadonlyArray<unknown>, b: ReadonlyArray<unknown>): boolean {
  return a.length === b.length && a.every((value, index) => Object.is(value, b[index]));
}

/**
 * Runs `loader` whenever `dependencies` change. Data from a previous `reload()` stays visible
 * while the refresh runs; data for different dependencies never leaks through.
 */
export function useAsync<T>(loader: () => Promise<T>, dependencies: ReadonlyArray<unknown>): AsyncState<T> {
  const [identity, setIdentity] = useState<Identity>(() => ({ dependencies, scope: {}, request: {} }));
  const [settled, setSettled] = useState<Settled<T>>();

  // Adjusting state during render is React's supported way to reset on a prop change.
  let current = identity;
  if (!sameDependencies(identity.dependencies, dependencies)) {
    current = { dependencies, scope: {}, request: {} };
    setIdentity(current);
  }
  const { scope, request } = current;

  useEffect(() => {
    let active = true;
    loader().then(
      (data) => {
        if (active) setSettled({ scope, request, data });
      },
      (reason: unknown) => {
        if (!active) return;
        setSettled((previous) => ({
          scope,
          request,
          data: previous?.scope === scope ? previous.data : undefined,
          error: toError(reason),
        }));
      },
    );
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- `request` identifies one invocation of `loader`
  }, [request]);

  const reload = useCallback(() => setIdentity((previous) => ({ ...previous, request: {} })), []);
  const visible = settled?.scope === scope ? settled : undefined;
  return { data: visible?.data, error: visible?.error, loading: settled?.request !== request, reload };
}
