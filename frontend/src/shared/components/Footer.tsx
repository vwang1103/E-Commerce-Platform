"use client";

import Link from "next/link";
import { useCategories } from "@/features/catalog/hooks/useCategories";

export function Footer() {
  const { categories } = useCategories();

  return (
    <footer className="mt-28 border-t border-slate-200 bg-white">
      <div className="container-page grid gap-10 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="text-3xl font-bold tracking-tight text-slate-700"><span className="text-emerald-600">arc</span>ade<span className="text-emerald-500">.</span></div>
          <p className="mt-3 max-w-xs text-sm text-slate-500">
            Useful things for work, home, and everyday life—all in one easy place.
          </p>
        </div>
        <FooterColumn title="Shop">
          <Link href="/shop/">All products</Link>
          {categories.map((category) => (
            <Link key={category.id} href={`/shop/?category=${category.slug}`}>
              {category.name}
            </Link>
          ))}
        </FooterColumn>
        <FooterColumn title="Orders">
          <Link href="/cart/">Cart</Link>
          <Link href="/orders/">Order history</Link>
          <Link href="/orders/?tab=pending">Pay for an order</Link>
        </FooterColumn>
        <FooterColumn title="Sell">
          <Link href="/seller/">Seller console</Link>
          <p className="text-slate-500">Create products, set opening stock, and manage prices.</p>
        </FooterColumn>
      </div>
      <div className="border-t border-slate-100">
        <div className="container-page flex flex-col gap-1 py-5 text-xs text-slate-500 sm:flex-row sm:justify-between">
          <span>© 2026 Arcade. All rights reserved.</span>
          <span>Thoughtful products, straightforward shopping.</span>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, children }: Readonly<{ title: string; children: React.ReactNode }>) {
  return (
    <div>
      <h2 className="text-sm font-semibold">{title}</h2>
      <div className="mt-3 grid gap-2 text-sm text-slate-600 [&_a:hover]:text-emerald-700 [&_a:hover]:underline">{children}</div>
    </div>
  );
}
