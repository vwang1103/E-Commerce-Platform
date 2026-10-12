import type { CategoryDto } from "@/shared/lib/types";

export interface Category {
  id: number;
  slug: string;
  name: string;
  blurb: string;
}

const BLURBS: Record<string, string> = {
  electronics: "Audio, power, and desk tech",
  "home-and-kitchen": "Quiet upgrades for every room",
  books: "Planners, notebooks, and reading",
  "fitness-and-outdoors": "Gear for training and the trail",
  "personal-care": "Everyday routines, made simple",
  "toys-and-games": "Play for every age",
};

export function toCategory(dto: CategoryDto): Category {
  return { id: dto.categoryId, slug: dto.slug, name: dto.name, blurb: BLURBS[dto.slug] ?? `Shop ${dto.name}` };
}

const GENERAL: Category = { id: 0, slug: "general", name: "General", blurb: "Everything else" };

export function findCategoryById(categories: readonly Category[], id: number): Category {
  return categories.find((category) => category.id === id) ?? GENERAL;
}

export function findCategoryBySlug(categories: readonly Category[], slug: string | null | undefined): Category | undefined {
  return categories.find((category) => category.slug === slug);
}
