"use client";

import Image from "next/image";
import Link from "next/link";
import { useMemo } from "react";
import { ProductGrid } from "@/features/catalog/components/ProductCard";
import { loadCatalog } from "@/features/catalog/lib/catalog";
import { ErrorState, ProductGridSkeleton } from "@/shared/components/Feedback";
import { Icon, type IconName } from "@/shared/components/Icon";
import { useAsync } from "@/shared/hooks/useAsync";

const SHOP_PROMISES: ReadonlyArray<{ icon: IconName; title: string; detail: string; tone: string }> = [
  {
    icon: "bolt",
    title: "Find it quickly",
    detail: "Search the whole shop and get useful matches as you type.",
    tone: "border-emerald-200 bg-emerald-50 text-emerald-700",
  },
  {
    icon: "shield",
    title: "Stock held at checkout",
    detail: "Your items are set aside while you finish your order.",
    tone: "border-orange-200 bg-orange-50 text-orange-700",
  },
  {
    icon: "package",
    title: "Clear order updates",
    detail: "See when an order is waiting, paid, or needs your attention.",
    tone: "border-violet-200 bg-violet-50 text-violet-700",
  },
];

export default function HomePage() {
  const catalog = useAsync(loadCatalog, []);
  const products = useMemo(() => catalog.data ?? [], [catalog.data]);
  const newest = useMemo(() => [...products].sort((a, b) => b.productId - a.productId), [products]);
  const affordable = useMemo(() => products.filter((product) => product.priceMinor <= 5000).slice(0, 4), [products]);

  return (
    <>
      <section className="container-page py-8 sm:py-10">
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <div className="relative min-h-[38rem] overflow-hidden rounded-[2rem] bg-[#dcefc7] px-7 py-10 sm:min-h-[32rem] sm:px-12 sm:py-14">
            <div className="relative z-10 max-w-xl">
              <p className="inline-flex items-center gap-2 rounded-full bg-white/65 px-4 py-2 text-sm font-medium text-emerald-800">
                <span className="rounded-full bg-emerald-600 px-2.5 py-1 text-[10px] font-bold tracking-wider text-white uppercase">New</span>
                Everyday favorites, freshly picked
              </p>
              <h1 className="mt-6 max-w-lg text-4xl leading-[1.04] font-semibold tracking-tight text-slate-800 sm:text-6xl">
                Good finds for better days.
              </h1>
              <p className="mt-5 max-w-md text-base leading-7 text-slate-600 sm:text-lg">
                Shop useful tech, home comforts, outdoor gear, and thoughtful gifts without the clutter.
              </p>
              <Link href="/shop/" className="btn mt-8 rounded-xl bg-slate-800 px-7 text-white hover:bg-slate-900">
                Shop the collection <Icon name="arrow-right" className="size-4" />
              </Link>
            </div>
            <Image
              src="/store-assets/hero_model_img.png"
              alt="Customer enjoying colorful headphones"
              width={520}
              height={620}
              priority
              className="absolute right-[-2rem] bottom-0 h-[43%] w-auto object-contain sm:right-[-4rem] sm:h-[72%] lg:right-[-5rem] lg:h-[88%] xl:right-[-3rem]"
            />
          </div>

          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-1">
            <Link href="/shop/?sort=newest" className="group flex min-h-52 items-center justify-between overflow-hidden rounded-[2rem] bg-[#ffe6c7] p-7">
              <div className="relative z-10">
                <p className="max-w-32 text-2xl font-semibold leading-tight text-slate-800">Fresh arrivals</p>
                <span className="mt-5 flex items-center gap-1 text-sm text-slate-600">View more <Icon name="arrow-right" className="size-4 transition group-hover:translate-x-1" /></span>
              </div>
              <Image src="/store-assets/hero_product_img1.png" alt="" width={170} height={170} className="-mr-6 w-36 object-contain sm:w-40" />
            </Link>
            <Link href="/shop/?price=under-25&sort=price-asc" className="group flex min-h-52 items-center justify-between overflow-hidden rounded-[2rem] bg-[#dceafa] p-7">
              <div className="relative z-10">
                <p className="max-w-32 text-2xl font-semibold leading-tight text-slate-800">Little prices, great picks</p>
                <span className="mt-5 flex items-center gap-1 text-sm text-slate-600">Shop under $25 <Icon name="arrow-right" className="size-4 transition group-hover:translate-x-1" /></span>
              </div>
              <Image src="/store-assets/hero_product_img2.png" alt="" width={170} height={200} className="-mr-5 w-32 object-contain sm:w-36" />
            </Link>
          </div>
        </div>

      </section>

      <ProductSection
        id="new-heading"
        title="Latest products"
        detail="A few of the newest things in the shop"
        href="/shop/?sort=newest"
        loading={catalog.loading && !catalog.data}
        error={catalog.error}
        retry={catalog.reload}
      >
        <ProductGrid products={newest.slice(0, 4)} />
      </ProductSection>

      {affordable.length > 0 && (
        <ProductSection id="favorites-heading" title="Easy favorites" detail="Useful picks under $50" href="/shop/?sort=price-asc">
          <ProductGrid products={affordable} />
        </ProductSection>
      )}

      <section className="container-page py-24" aria-labelledby="service-heading">
        <div className="mx-auto max-w-xl text-center">
          <h2 id="service-heading" className="text-2xl font-semibold tracking-tight text-slate-800">Shopping that stays simple</h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">Helpful details when you need them, and no noise when you do not.</p>
        </div>
        <ul className="mt-12 grid gap-6 md:grid-cols-3">
          {SHOP_PROMISES.map((promise) => (
            <li key={promise.title} className={`relative rounded-2xl border px-7 pt-10 pb-8 text-center ${promise.tone}`}>
              <span className="absolute -top-5 left-1/2 flex size-10 -translate-x-1/2 items-center justify-center rounded-xl bg-current shadow-sm">
                <Icon name={promise.icon} className="size-5 text-white" />
              </span>
              <h3 className="font-semibold text-slate-800">{promise.title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{promise.detail}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="container-page">
        <div className="flex flex-col items-start justify-between gap-6 rounded-[2rem] bg-[#eef3df] p-8 sm:flex-row sm:items-center sm:p-10">
          <div>
            <p className="text-xs font-semibold tracking-wider text-emerald-700 uppercase">For sellers</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-800">Bring your next product to the shop.</h2>
            <p className="mt-1.5 max-w-lg text-slate-600">Add the details, choose the opening stock, and make it ready to browse.</p>
          </div>
          <Link href="/seller/" className="btn rounded-xl bg-slate-800 text-white hover:bg-slate-900">Open seller tools</Link>
        </div>
      </section>
    </>
  );
}

function ProductSection({
  id,
  title,
  detail,
  href,
  loading = false,
  error,
  retry,
  children,
}: Readonly<{
  id: string;
  title: string;
  detail: string;
  href: string;
  loading?: boolean;
  error?: Error | null;
  retry?: () => void;
  children: React.ReactNode;
}>) {
  return (
    <section className="container-page pt-20" aria-labelledby={id}>
      <div className="flex flex-col items-center text-center">
        <h2 id={id} className="text-2xl font-semibold tracking-tight text-slate-800">{title}</h2>
        <div className="mt-2 flex items-center gap-4 text-sm text-slate-500">
          <span>{detail}</span>
          <Link href={href} className="flex items-center gap-1 font-medium text-emerald-600 hover:text-emerald-700">
            View all <Icon name="arrow-right" className="size-4" />
          </Link>
        </div>
      </div>
      <div className="mt-10">
        {error ? <ErrorState error={error} title="We couldn't load the catalog" retry={retry} /> : loading ? <ProductGridSkeleton count={4} /> : children}
      </div>
    </section>
  );
}
