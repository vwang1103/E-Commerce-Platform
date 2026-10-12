"use client";

import { useEffect, useMemo } from "react";
import { api } from "@/shared/lib/api";
import { findCategoryById, findCategoryBySlug, toCategory, type Category } from "@/features/catalog/lib/categories";
import { createMemoryStore, useStore } from "@/shared/lib/store";

const categoryStore = createMemoryStore<readonly Category[]>([]);
const loadedStore = createMemoryStore(false);
let requested = false;

function ensureCategories(): void {
  if (requested) return;
  requested = true;
  api.listCategories().then(
    (list) => {
      categoryStore.set(list.map(toCategory));
      loadedStore.set(true);
    },
    () => {
      // Allow a later mount to retry.
      requested = false;
    },
  );
}

/** Categories from the catalog service, shared by every component on the page. */
export function useCategories() {
  const categories = useStore(categoryStore);
  const loaded = useStore(loadedStore);
  useEffect(ensureCategories, []);
  return useMemo(
    () => ({
      categories,
      /** True once the service has answered. */
      loaded,
      byId: (id: number) => findCategoryById(categories, id),
      bySlug: (slug: string | null | undefined) => findCategoryBySlug(categories, slug),
    }),
    [categories, loaded],
  );
}
